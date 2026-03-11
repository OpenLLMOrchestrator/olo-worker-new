package com.olo.worker.cache.factory;

import com.olo.configuration.port.CacheConnectionSettings;
import com.olo.configuration.port.TenantRegionCacheFactory;
import com.olo.configuration.region.TenantRegionCache;
import com.olo.worker.cache.impl.region.RedisTenantRegionCache;
import io.lettuce.core.RedisClient;

/**
 * Cache factory for tenant-region cache.
 */
public final class RedisTenantRegionCacheFactory implements TenantRegionCacheFactory {

  @Override
  public TenantRegionCache create(CacheConnectionSettings cacheSettings) {
    if (cacheSettings == null || !cacheSettings.isConfigured()) return null;
    return new RedisTenantRegionCache(RedisClient.create(cacheSettings.redisUri()));
  }
}
