package com.olo.configuration.port;

import com.olo.configuration.snapshot.ConfigurationSnapshotStore;

/**
 * Creates a distributed snapshot store adapter from cache connection settings.
 */
public interface ConfigurationSnapshotStoreFactory {

  ConfigurationSnapshotStore create(CacheConnectionSettings cacheSettings);
}
