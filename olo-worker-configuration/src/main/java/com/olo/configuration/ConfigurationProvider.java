package com.olo.configuration;

import com.olo.configuration.impl.config.SnapshotConfiguration;
import com.olo.configuration.region.TenantRegionResolver;
import com.olo.configuration.snapshot.CompositeConfigurationSnapshot;
import com.olo.configuration.snapshot.ConfigurationSnapshot;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holder for the current configuration. Runtime path: multiple region snapshots
 * ({@code Map<Region, CompositeConfigurationSnapshot>}). Resolution uses tenant region:
 * {@code tenantRegion = TenantRegionResolver.getRegion(tenantId); snapshot = snapshotMap.get(tenantRegion)}.
 * When Redis is not configured, a {@link Configuration} is set directly (defaults + env).
 * <p><strong>Immutable replacement:</strong> The loader always builds new composites and assigns the map in one volatile write.
 * Workers never see half-old section combinations.
 * </p>
 */
public final class ConfigurationProvider {

  /** Volatile: region → sectioned snapshot. Null when using defaults-only (no Redis). */
  private static volatile Map<String, CompositeConfigurationSnapshot> snapshotByRegion;
  /** Primary (worker) region used for {@link #get()} when no tenant context. */
  private static volatile String primaryRegion;
  /** Volatile: defaults + env only; set when Redis is not configured. Null when snapshot map is set. */
  private static volatile Configuration defaultConfiguration;

  private ConfigurationProvider() {}

  /** Sets the flat configuration (defaults + env, no Redis). */
  public static void set(Configuration configuration) {
    defaultConfiguration = configuration;
    snapshotByRegion = null;
    primaryRegion = null;
  }

  /**
   * Sets the snapshot by wrapping it in a composite (core only). Use after first load of worker region so {@link #get()} works.
   */
  public static void setSnapshot(ConfigurationSnapshot s) {
    defaultConfiguration = null;
    if (s == null) {
      snapshotByRegion = null;
      primaryRegion = null;
      return;
    }
    CompositeConfigurationSnapshot c = new CompositeConfigurationSnapshot(s.getRegion());
    c.setCore(s, s.getVersion());
    snapshotByRegion = Collections.singletonMap(s.getRegion(), c);
    primaryRegion = s.getRegion();
  }

  /** Sets the sectioned snapshot for a single region (used during bootstrap before multi-region load). */
  public static void setComposite(CompositeConfigurationSnapshot c) {
    defaultConfiguration = null;
    if (c == null) {
      snapshotByRegion = null;
      primaryRegion = null;
      return;
    }
    snapshotByRegion = Collections.singletonMap(c.getRegion(), c);
    primaryRegion = c.getRegion();
  }

  /**
   * Sets the multi-region snapshot map. Worker can process tenants from any of these regions;
   * resolution is by {@link TenantRegionResolver#getRegion(String)}.
   */
  public static void setSnapshotMap(Map<String, CompositeConfigurationSnapshot> map, String primaryRegionValue) {
    defaultConfiguration = null;
    if (map == null || map.isEmpty()) {
      snapshotByRegion = null;
      primaryRegion = null;
      return;
    }
    snapshotByRegion = new ConcurrentHashMap<>(map);
    primaryRegion = primaryRegionValue != null && !primaryRegionValue.isBlank()
        ? primaryRegionValue.trim()
        : map.keySet().iterator().next();
  }

  /** Returns the sectioned snapshot for the primary (worker) region, or null if using defaults-only. */
  public static CompositeConfigurationSnapshot getComposite() {
    return getComposite(primaryRegion);
  }

  /** Returns the sectioned snapshot for the given region, or null if absent. */
  public static CompositeConfigurationSnapshot getComposite(String region) {
    Map<String, CompositeConfigurationSnapshot> map = snapshotByRegion;
    if (map == null || region == null) return null;
    return map.get(region);
  }

  /** Returns an immutable copy of the current region → composite map; null if using defaults-only. */
  public static Map<String, CompositeConfigurationSnapshot> getSnapshotMap() {
    Map<String, CompositeConfigurationSnapshot> map = snapshotByRegion;
    return map == null ? null : Map.copyOf(map);
  }

  /** Returns the current configuration view (global) from the primary region. Never null after bootstrap. */
  public static Configuration get() {
    Map<String, CompositeConfigurationSnapshot> map = snapshotByRegion;
    if (map != null && primaryRegion != null) {
      CompositeConfigurationSnapshot s = map.get(primaryRegion);
      if (s != null) {
        return SnapshotConfiguration.global(s.getCore());
      }
    }
    return defaultConfiguration;
  }

  /** Returns the current configuration; throws if not set. */
  public static Configuration require() {
    Configuration c = get();
    if (c == null) {
      throw new IllegalStateException("Configuration not set. Call Bootstrap.run() or loadAndSetDefault() at bootstrap.");
    }
    return c;
  }

  /** Returns the current core snapshot for the primary region, or null if using defaults-only. */
  public static ConfigurationSnapshot getSnapshot() {
    return getSnapshot(primaryRegion);
  }

  /** Returns the core snapshot for the given region, or null if absent. */
  public static ConfigurationSnapshot getSnapshot(String region) {
    CompositeConfigurationSnapshot c = getComposite(region);
    return c != null ? c.getCore() : null;
  }

  /**
   * Configuration for a tenant. Resolves tenant's region via {@link TenantRegionResolver#getRegion(String)}
   * and returns config from that region's snapshot. Use {@code ConfigurationProvider.forTenant(tenantId)}.
   */
  public static Configuration forTenant(String tenantId) {
    return forContext(tenantId, null);
  }

  /**
   * Configuration for context (global → region → tenant → resource). Resolves by tenant region, then
   * uses resource IDs in type:name form (e.g. pipeline:chat, connection:openai).
   */
  public static Configuration forContext(String tenantId, String resourceId) {
    if (tenantId == null || tenantId.isBlank()) {
      return get();
    }
    String tenantRegion = TenantRegionResolver.getRegion(tenantId);
    if (tenantRegion == null || tenantRegion.isBlank()) {
      tenantRegion = Regions.DEFAULT_REGION;
    }
    CompositeConfigurationSnapshot composite = getComposite(tenantRegion);
    if (composite == null) {
      return get();
    }
    return SnapshotConfiguration.forContext(composite.getCore(), tenantId, resourceId);
  }

  /** Updates the composite for a single region (used by refresh). Replaces the map with a copy containing the update. */
  public static void putComposite(String region, CompositeConfigurationSnapshot composite) {
    Map<String, CompositeConfigurationSnapshot> map = snapshotByRegion;
    if (map == null || region == null) return;
    Map<String, CompositeConfigurationSnapshot> next = new ConcurrentHashMap<>(map);
    if (composite != null) {
      next.put(region, composite);
    } else {
      next.remove(region);
    }
    snapshotByRegion = next;
  }
}
