package com.olo.worker.db;

import com.olo.configuration.port.ConfigurationPortRegistry;
import com.olo.worker.db.factory.JdbcConfigurationSnapshotRepositoryFactory;
import com.olo.worker.db.factory.JdbcTenantRegionRepositoryFactory;

/**
 * Registers DB factory implementations and DbClient initializer into configuration port registry.
 */
public final class DbPortRegistrar {

  private DbPortRegistrar() {}

  public static void registerDefaults() {
    ConfigurationPortRegistry.registerDbClientInitializer(new DbClientInitializerImpl());
    ConfigurationPortRegistry.registerTenantRegionRepositoryFactory(new JdbcTenantRegionRepositoryFactory());
    ConfigurationPortRegistry.registerSnapshotRepositoryFactory(new JdbcConfigurationSnapshotRepositoryFactory());
  }
}
