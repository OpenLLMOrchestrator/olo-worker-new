package com.olo.configuration.impl.source;

import com.olo.configuration.ConfigurationConstants;
import com.olo.configuration.source.ConfigurationSource;

import java.util.HashMap;
import java.util.Map;

/** @deprecated Unused; kept only for historical reference. */
@Deprecated
public final class EnvConfigurationSource implements ConfigurationSource {

  @Override
  public Map<String, String> load(Map<String, String> current) {
    throw new UnsupportedOperationException("EnvConfigurationSource is deprecated and no longer supported. Use EnvironmentConfigurationSource instead.");
  }
}
