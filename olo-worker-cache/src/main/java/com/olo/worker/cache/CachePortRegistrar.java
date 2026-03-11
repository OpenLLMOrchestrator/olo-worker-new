package com.olo.worker.cache;

import com.olo.configuration.port.ConfigurationPortRegistry;
import com.olo.worker.cache.factory.RedisConfigChangeSubscriberFactory;
import com.olo.worker.cache.factory.RedisConfigurationSnapshotStoreFactory;
import com.olo.worker.cache.factory.RedisTenantRegionCacheFactory;

/**
 * Registers cache factory implementations into configuration port registry.
 */
public final class CachePortRegistrar {

  private CachePortRegistrar() {}

  public static void registerDefaults() {
    ConfigurationPortRegistry.registerSnapshotStoreFactory(new RedisConfigurationSnapshotStoreFactory());
    ConfigurationPortRegistry.registerTenantRegionCacheFactory(new RedisTenantRegionCacheFactory());
    ConfigurationPortRegistry.registerConfigChangeSubscriberFactory(new RedisConfigChangeSubscriberFactory());
  }
}
