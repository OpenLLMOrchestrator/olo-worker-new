package com.olo.worker.cache.factory;

import com.olo.configuration.port.CacheConnectionSettings;
import com.olo.configuration.port.ConfigurationSnapshotStoreFactory;
import com.olo.configuration.snapshot.ConfigurationSnapshotStore;
import com.olo.worker.cache.impl.snapshot.RedisConfigurationSnapshotStore;
import io.lettuce.core.RedisClient;

/**
 * Cache factory for Redis snapshot store.
 */
public final class RedisConfigurationSnapshotStoreFactory implements ConfigurationSnapshotStoreFactory {

  @Override
  public ConfigurationSnapshotStore create(CacheConnectionSettings cacheSettings) {
    if (cacheSettings == null || !cacheSettings.isConfigured()) return null;
    RedisClient client = RedisClient.create(cacheSettings.redisUri());
    return new RedisConfigurationSnapshotStore(client);
  }
}
