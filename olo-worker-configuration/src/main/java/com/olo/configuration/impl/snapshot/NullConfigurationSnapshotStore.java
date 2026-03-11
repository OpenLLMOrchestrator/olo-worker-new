package com.olo.configuration.impl.snapshot;

import com.olo.configuration.snapshot.ConfigurationSnapshot;
import com.olo.configuration.snapshot.ConfigurationSnapshotStore;
import com.olo.configuration.snapshot.SnapshotMetadata;

/**
 * No-op store when Redis is not configured. Always returns null; put is a no-op.
 */
public final class NullConfigurationSnapshotStore implements ConfigurationSnapshotStore {

  @Override
  public ConfigurationSnapshot getSnapshot(String region) {
    return null;
  }

  @Override
  public SnapshotMetadata getMeta(String region) {
    return null;
  }

  @Override
  public void put(String region, ConfigurationSnapshot snapshot) {}
}
