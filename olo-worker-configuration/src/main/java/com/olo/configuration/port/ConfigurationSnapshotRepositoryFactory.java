package com.olo.configuration.port;

import com.olo.configuration.snapshot.ConfigurationSnapshotRepository;

/**
 * Creates DB-backed snapshot repository used by admin snapshot building.
 */
public interface ConfigurationSnapshotRepositoryFactory {

  ConfigurationSnapshotRepository create(DbConnectionSettings dbSettings);
}
