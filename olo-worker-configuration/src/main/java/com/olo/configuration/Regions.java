package com.olo.configuration;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Local configuration for Region: comma-separated list of regions this instance serves.
 * Tenant→region mapping is stored in DB table {@code olo_configuration_region} and can be cached in Redis.
 */
public final class Regions {

  /** Config key for comma-separated list of regions. */
  public static final String CONFIG_KEY = "olo.region";

  /** Default region when not configured. */
  public static final String DEFAULT_REGION = "default";

  private Regions() {}

  /**
   * Returns the configured comma-separated region string (e.g. "default" or "default,us-east,eu-west").
   */
  public static String getRegionList(Configuration config) {
    if (config == null) {
      return DEFAULT_REGION;
    }
    String v = config.get(CONFIG_KEY, DEFAULT_REGION);
    return v != null ? v.trim() : DEFAULT_REGION;
  }

  /**
   * Returns the list of regions from config (comma-separated, trimmed, non-empty).
   * If empty or missing, returns a list containing only {@value #DEFAULT_REGION}.
   */
  public static List<String> getRegions(Configuration config) {
    String list = getRegionList(config);
    if (list.isEmpty()) {
      return Collections.singletonList(DEFAULT_REGION);
    }
    List<String> regions = Arrays.stream(list.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .collect(Collectors.toList());
    return regions.isEmpty() ? Collections.singletonList(DEFAULT_REGION) : regions;
  }
}
