package com.olo.configuration;

import com.olo.configuration.impl.source.EnvironmentConfigurationSource;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationLoaderTest {

  @Test
  void loadFromStreamAndOverlayEnv() {
    String props = "olo.temporal.target=default:7233\nolo.redis.host=redis.local\n";
    Configuration config = Bootstrap.loadConfiguration(new ByteArrayInputStream(props.getBytes(StandardCharsets.UTF_8)));
    assertEquals("default:7233", config.get("olo.temporal.target"));
    assertEquals("redis.local", config.get("olo.redis.host"));
    assertNull(config.get("missing.key"));
    assertEquals("fallback", config.get("missing.key", "fallback"));
  }

  @Test
  void loadFromClasspath() {
    Configuration config = Bootstrap.loadConfiguration();
    assertEquals("localhost:7233", config.get("olo.temporal.target"));
    assertEquals("default", config.get("olo.temporal.namespace"));
    assertEquals("localhost", config.get("olo.db.host"));
    assertEquals(5432, config.getInteger("olo.db.port", -1));
    assertEquals(10, config.getInteger("olo.db.pool.size", 0));
    assertFalse(config.getBoolean("olo.configuration.checksum", true));
  }

  @Test
  void envKeyToConfigKey() {
    assertEquals("olo.db.host", EnvironmentConfigurationSource.envKeyToConfigKey("DB_HOST"));
    assertEquals("olo.temporal.target", EnvironmentConfigurationSource.envKeyToConfigKey("TEMPORAL_TARGET"));
  }
}
