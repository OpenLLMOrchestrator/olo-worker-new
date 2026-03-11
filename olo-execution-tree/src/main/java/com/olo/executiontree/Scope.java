package com.olo.executiontree;

import java.util.Map;
import java.util.Set;

public final class Scope {
  private final Map<String, Object> plugins;
  private final Set<String> features;

  public Scope(Map<String, Object> plugins, Set<String> features) {
    this.plugins = plugins == null ? Map.of() : Map.copyOf(plugins);
    this.features = features == null ? Set.of() : Set.copyOf(features);
  }

  public Map<String, Object> getPlugins() { return plugins; }
  public Set<String> getFeatures() { return features; }
  public boolean hasPlugin(String pluginId) { return plugins.containsKey(pluginId); }
  public boolean hasFeature(String featureId) { return features.contains(featureId); }
}
