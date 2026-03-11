package com.olo.configuration.port;

import com.olo.configuration.region.TenantRegionCache;

/**
 * Creates cache adapter used by tenant-region resolution.
 */
public interface TenantRegionCacheFactory {

  TenantRegionCache create(CacheConnectionSettings cacheSettings);
}
