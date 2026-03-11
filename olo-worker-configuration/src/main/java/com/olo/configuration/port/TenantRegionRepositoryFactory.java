package com.olo.configuration.port;

import com.olo.configuration.region.TenantRegionRepository;

/**
 * Creates DB adapter used by tenant-region resolution.
 */
public interface TenantRegionRepositoryFactory {

  TenantRegionRepository create(DbConnectionSettings dbSettings);
}
