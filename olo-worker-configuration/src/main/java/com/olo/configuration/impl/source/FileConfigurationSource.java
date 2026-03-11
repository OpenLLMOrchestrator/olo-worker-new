package com.olo.configuration.impl.source;

import com.olo.configuration.source.ConfigurationSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/** @deprecated Unused; kept only for historical reference. */
@Deprecated
public final class FileConfigurationSource implements ConfigurationSource {

  @Override
  public Map<String, String> load(Map<String, String> current) {
    throw new UnsupportedOperationException("FileConfigurationSource is deprecated and no longer supported. Use DefaultsConfigurationSource instead.");
  }
}
