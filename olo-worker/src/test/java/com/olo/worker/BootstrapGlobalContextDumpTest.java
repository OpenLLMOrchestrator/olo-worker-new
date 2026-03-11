package com.olo.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.olo.configuration.Bootstrap;
import com.olo.configuration.Configuration;
import com.olo.configuration.ConfigurationProvider;
import com.olo.configuration.Regions;
import com.olo.configuration.region.TenantRegionResolver;
import com.olo.configuration.snapshot.CompositeConfigurationSnapshot;
import com.olo.configuration.snapshot.ConfigurationSnapshot;
import com.olo.worker.cache.CachePortRegistrar;
import com.olo.worker.db.DbPortRegistrar;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Integration test: runs full bootstrap (Redis + DB), then dumps the constructed global context
 * to a JSON file for debugging. Not included in the normal build cycle because it requires
 * external resources (Redis, DB). Run explicitly with:
 * <pre>
 *   ./gradlew :olo-worker:integrationTest
 * </pre>
 * Output file: {@code build/global-context-debug.json} or path from system property
 * {@code olo.bootstrap.dump.output}.
 */
@Tag("integration")
class BootstrapGlobalContextDumpTest {

  private static final String OUTPUT_PROP = "olo.bootstrap.dump.output";
  private static final String DEFAULT_OUTPUT = "build/global-context-debug.json";
  private static final String DEFAULT_DB_PASSWORD = "pgpass";

  private final ObjectMapper mapper = new ObjectMapper()
      .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
      .enable(SerializationFeature.INDENT_OUTPUT);

  @AfterEach
  void tearDown() {
    Bootstrap.stopTenantRegionRefreshScheduler();
    Bootstrap.stopRefreshScheduler();
  }

  @Test
  void runBootstrapAndDumpGlobalContextToJson() throws Exception {
    Configuration bootstrapConfig = com.olo.configuration.Bootstrap.loadConfiguration();
    runEnsureSchemaScript(bootstrapConfig);
    DbConnInfo db = buildDbConnInfo(bootstrapConfig);

    DbPortRegistrar.registerDefaults();
    CachePortRegistrar.registerDefaults();
    Bootstrap.run(true);

    Map<String, Object> globalContext = buildGlobalContextDump(db);
    String json = mapper.writeValueAsString(globalContext);

    Path output = Paths.get(System.getProperty(OUTPUT_PROP, DEFAULT_OUTPUT));
    Path parent = output.getParent();
    if (parent != null && !Files.exists(parent)) {
      Files.createDirectories(parent);
    }
    Files.writeString(output, json);
    System.out.println("Global context dumped to: " + output.toAbsolutePath());
  }

  /**
   * Runs the ensure-schema script from configuration/debug. Creates tables if not exist
   * and inserts default data (idempotent). Loads from classpath first, then from
   * configuration/debug relative to project root.
   */
  private void runEnsureSchemaScript(Configuration config) throws Exception {
    DbConnInfo db = buildDbConnInfo(config);
    if (db.jdbcUrl.isEmpty()) {
      // No DB configured: nothing to ensure/seed.
      return;
    }

    String sql = loadEnsureSchemaSql();
    if (sql == null || sql.isBlank()) {
      throw new IllegalStateException("Could not load configuration/debug/ensure-schema.sql from classpath or path");
    }

    try (Connection conn = DriverManager.getConnection(db.jdbcUrl, db.user, db.password);
         Statement st = conn.createStatement()) {
      for (String statement : splitSqlStatements(sql)) {
        if (!statement.isBlank() && !statement.stripLeading().startsWith("--")) {
          st.execute(statement);
        }
      }
      // Seed a minimal GlobalContext shape idempotently for debugging.
      // Does NOT overwrite existing rows; uses ON CONFLICT DO NOTHING so existing environments are not disturbed.
      ensurePipelineTablesAndSeed(conn);
    }
  }

  private String loadEnsureSchemaSql() throws Exception {
    // 1) Classpath (e.g. when copied to src/test/resources by Gradle)
    try (InputStream in = BootstrapGlobalContextDumpTest.class.getResourceAsStream("/configuration/debug/ensure-schema.sql")) {
      if (in != null) {
        return new String(in.readAllBytes(), StandardCharsets.UTF_8);
      }
    }
    // 2) Path relative to working directory (configuration/debug or ../configuration/debug)
    Path p = Paths.get("configuration/debug/ensure-schema.sql");
    if (!Files.exists(p)) {
      p = Paths.get("../configuration/debug/ensure-schema.sql");
    }
    if (Files.exists(p)) {
      return Files.readString(p);
    }
    return null;
  }

  private static List<String> splitSqlStatements(String sql) {
    // Strip full-line comments so ";" inside a comment (e.g. "-- a; b") is not split
    String withoutLineComments = sql.lines()
        .filter(line -> !line.stripLeading().startsWith("--"))
        .collect(Collectors.joining("\n"));
    return Arrays.stream(withoutLineComments.split(";"))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .collect(Collectors.toList());
  }

  private record DbConnInfo(String jdbcUrl, String user, String password) {}

  private static DbConnInfo buildDbConnInfo(Configuration config) {
    String jdbcUrl = config.get("olo.db.url", "").trim();
    if (jdbcUrl.isEmpty()) {
      String host = config.get("olo.db.host", "").trim();
      if (!host.isEmpty()) {
        int port = config.getInteger("olo.db.port", 5432);
        String name = config.get("olo.db.name", "olo").trim();
        jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + name;
      }
    }
    String user = config.get("olo.db.username", config.get("olo.db.user", "olo")).trim();
    if (user.isEmpty()) user = "olo";
    String password = config.get("olo.db.password", DEFAULT_DB_PASSWORD).trim();
    return new DbConnInfo(jdbcUrl, user, password);
  }

  private static void ensurePipelineTablesAndSeed(Connection conn) throws Exception {
    try (Statement st = conn.createStatement()) {
      // Tables (IF NOT EXISTS) so the test can run even if ensure-schema.sql is stale.
      st.execute("""
          CREATE TABLE IF NOT EXISTS olo_pipeline_template (
              region      VARCHAR(64)  NOT NULL,
              pipeline_id VARCHAR(255) NOT NULL,
              version     BIGINT       NOT NULL,
              is_active   BOOLEAN      NOT NULL DEFAULT true,
              tree_json   JSONB        NOT NULL,
              checksum    VARCHAR(64),
              updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
              PRIMARY KEY (region, pipeline_id, version)
          )
          """);
      st.execute("CREATE INDEX IF NOT EXISTS idx_olo_pipeline_template_region ON olo_pipeline_template(region)");
      st.execute("CREATE INDEX IF NOT EXISTS idx_olo_pipeline_template_region_pipeline ON olo_pipeline_template(region, pipeline_id)");
      st.execute("CREATE INDEX IF NOT EXISTS idx_olo_pipeline_template_active ON olo_pipeline_template(region, pipeline_id, is_active)");

      st.execute("""
          CREATE TABLE IF NOT EXISTS olo_tenant_pipeline_override (
              tenant_id        VARCHAR(64)  NOT NULL,
              pipeline_id      VARCHAR(255) NOT NULL,
              pipeline_version BIGINT       NOT NULL,
              override_json    JSONB        NOT NULL,
              updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
              PRIMARY KEY (tenant_id, pipeline_id, pipeline_version)
          )
          """);
      st.execute("CREATE INDEX IF NOT EXISTS idx_olo_tenant_pipeline_override_tenant ON olo_tenant_pipeline_override(tenant_id)");
      st.execute("CREATE INDEX IF NOT EXISTS idx_olo_tenant_pipeline_override_pipeline ON olo_tenant_pipeline_override(pipeline_id, pipeline_version)");
    }

    // Seed tenants in us-east (does not override an existing mapping).
    try (PreparedStatement ps = conn.prepareStatement("""
        INSERT INTO olo_configuration_region (tenant_id, region) VALUES (?, ?)
        ON CONFLICT (tenant_id) DO NOTHING
        """)) {
      ps.setString(1, "tenant-A");
      ps.setString(2, "us-east");
      ps.executeUpdate();
      ps.setString(1, "tenant-B");
      ps.setString(2, "us-east");
      ps.executeUpdate();
    }

    // Seed region pipelines: us-east order-processing v2 (active), settlement v1 (active).
    try (PreparedStatement ps = conn.prepareStatement("""
        INSERT INTO olo_pipeline_template (region, pipeline_id, version, is_active, tree_json)
        VALUES (?, ?, ?, ?, ?::jsonb)
        ON CONFLICT (region, pipeline_id, version) DO NOTHING
        """)) {
      ps.setString(1, "us-east");
      ps.setString(2, "order-processing");
      ps.setLong(3, 2L);
      ps.setBoolean(4, true);
      ps.setString(5, "{\"id\":\"order-processing\",\"name\":\"order-processing\",\"version\":2,\"workflowId\":\"order-processing\",\"description\":\"Order processing pipeline v2\",\"inputContract\":{\"strict\":false,\"parameters\":[]},\"variableRegistry\":[],\"scope\":{\"plugins\":[],\"features\":[]},\"executionTree\":{\"id\":\"root\",\"displayName\":\"OrderProcessing\",\"type\":\"SEQUENCE\",\"children\":[]},\"outputContract\":{\"parameters\":[]},\"resultMapping\":[]}");
      ps.executeUpdate();

      ps.setString(1, "us-east");
      ps.setString(2, "settlement");
      ps.setLong(3, 1L);
      ps.setBoolean(4, true);
      ps.setString(5, "{\"id\":\"settlement\",\"name\":\"settlement\",\"version\":1,\"workflowId\":\"settlement\",\"description\":\"Settlement pipeline v1\",\"inputContract\":{\"strict\":false,\"parameters\":[]},\"variableRegistry\":[],\"scope\":{\"plugins\":[],\"features\":[]},\"executionTree\":{\"id\":\"root\",\"displayName\":\"Settlement\",\"type\":\"SEQUENCE\",\"children\":[]},\"outputContract\":{\"parameters\":[]},\"resultMapping\":[]}");
      ps.executeUpdate();
    }

    // Seed tenant override: tenant-A overrides order-processing v2 (empty patch by default).
    try (PreparedStatement ps = conn.prepareStatement("""
        INSERT INTO olo_tenant_pipeline_override (tenant_id, pipeline_id, pipeline_version, override_json)
        VALUES (?, ?, ?, ?::jsonb)
        ON CONFLICT (tenant_id, pipeline_id, pipeline_version) DO NOTHING
        """)) {
      ps.setString(1, "tenant-A");
      ps.setString(2, "order-processing");
      ps.setLong(3, 2L);
      ps.setString(4, "{}");
      ps.executeUpdate();
    }
  }

  private Map<String, Object> buildGlobalContextDump(DbConnInfo db) {
    Map<String, Object> root = new LinkedHashMap<>();

    Configuration config = ConfigurationProvider.require();
    root.put("globalConfig", config.asMap());

    CompositeConfigurationSnapshot primaryComposite = ConfigurationProvider.getComposite();
    if (primaryComposite != null) {
      root.put("primaryRegion", primaryComposite.getRegion());
    }

    Map<String, CompositeConfigurationSnapshot> snapshotMap = ConfigurationProvider.getSnapshotMap();
    if (snapshotMap != null) {
      root.put("servedRegions", Regions.getRegions(config));
      Map<String, Object> regions = new LinkedHashMap<>();
      for (Map.Entry<String, CompositeConfigurationSnapshot> e : snapshotMap.entrySet()) {
        regions.put(e.getKey(), snapshotSummary(e.getValue()));
      }
      root.put("snapshotsByRegion", regions);
    }

    Map<String, String> tenantToRegion = TenantRegionResolver.getTenantToRegionMap();
    if (!tenantToRegion.isEmpty()) {
      root.put("tenantToRegion", tenantToRegion);
    }

    // DB-backed GlobalContext view for debugging (region pipelines + tenant overrides).
    // This does NOT affect runtime; it is only an introspection aid.
    try {
      if (!db.jdbcUrl.isEmpty()) {
        root.put("dbGlobalContext", loadDbGlobalContext(db));
      }
    } catch (Exception e) {
      root.put("dbGlobalContextError", e.getMessage());
    }

    return root;
  }

  private static Map<String, Object> loadDbGlobalContext(DbConnInfo db) throws Exception {
    Map<String, Object> out = new LinkedHashMap<>();

    try (Connection conn = DriverManager.getConnection(db.jdbcUrl, db.user, db.password)) {
      // Tenants (tenant -> region) from DB (not from runtime resolver).
      Map<String, String> tenantToRegion = new LinkedHashMap<>();
      try (PreparedStatement ps = conn.prepareStatement("""
          SELECT tenant_id, region
          FROM olo_configuration_region
          ORDER BY tenant_id
          """);
           ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          String tenantId = rs.getString(1);
          String region = rs.getString(2);
          if (tenantId != null && !tenantId.isBlank() && region != null && !region.isBlank()) {
            tenantToRegion.put(tenantId, region);
          }
        }
      }

      // Regions -> pipelines (active only)
      Map<String, Object> regions = new LinkedHashMap<>();
      try (PreparedStatement ps = conn.prepareStatement("""
          SELECT pipeline_id, version, checksum, updated_at, tree_json
          FROM olo_pipeline_template
          WHERE region = ? AND is_active = true
          ORDER BY pipeline_id, version DESC
          """)) {
        // Probe the tenant regions + default.
        Set<String> probeRegions = new java.util.LinkedHashSet<>();
        probeRegions.addAll(tenantToRegion.values());
        probeRegions.add("default");
        for (String region : probeRegions) {
          if (region == null || region.isBlank()) continue;
          ps.setString(1, region);
          try (ResultSet rs = ps.executeQuery()) {
            Map<String, Object> pipelines = new LinkedHashMap<>();
            while (rs.next()) {
              String pipelineId = rs.getString(1);
              long version = rs.getLong(2);
              String checksum = rs.getString(3);
              Object updatedAt = rs.getObject(4);
              Object treeJson = rs.getObject(5);
              Map<String, Object> p = new LinkedHashMap<>();
              p.put("version", version);
              if (checksum != null && !checksum.isBlank()) p.put("checksum", checksum);
              if (updatedAt != null) p.put("updatedAt", updatedAt.toString());
              if (treeJson != null) p.put("treeJson", treeJson.toString());
              pipelines.put(pipelineId, p);
            }
            if (!pipelines.isEmpty()) {
              Map<String, Object> r = new LinkedHashMap<>();
              r.put("pipelines", pipelines);
              regions.put(region, r);
            }
          }
        }
      }
      out.put("Regions", regions);

      // Tenants (tenant -> region)
      if (!tenantToRegion.isEmpty()) {
        out.put("Tenants", new LinkedHashMap<>(tenantToRegion));
      }

      // TenantOverrides (tenant -> pipelineId:vVersion -> overrideJson)
      Map<String, Object> tenantOverrides = new LinkedHashMap<>();
      try (PreparedStatement ps = conn.prepareStatement("""
          SELECT tenant_id, pipeline_id, pipeline_version, override_json, updated_at
          FROM olo_tenant_pipeline_override
          ORDER BY tenant_id, pipeline_id, pipeline_version
          """);
           ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          String tenantId = rs.getString(1);
          String pipelineId = rs.getString(2);
          long ver = rs.getLong(3);
          Object override = rs.getObject(4);
          Object updatedAt = rs.getObject(5);
          String key = pipelineId + ":v" + ver;

          @SuppressWarnings("unchecked")
          Map<String, Object> t = (Map<String, Object>) tenantOverrides.computeIfAbsent(tenantId, k -> new LinkedHashMap<String, Object>());
          Map<String, Object> o = new LinkedHashMap<>();
          if (override != null) o.put("overrideJson", override.toString());
          if (updatedAt != null) o.put("updatedAt", updatedAt.toString());
          t.put(key, o);
        }
      }
      out.put("TenantOverrides", tenantOverrides);
    }

    return out;
  }

  private Map<String, Object> snapshotSummary(CompositeConfigurationSnapshot composite) {
    Map<String, Object> summary = new LinkedHashMap<>();
    summary.put("region", composite.getRegion());
    summary.put("snapshotId", composite.getSnapshotId());
    summary.put("coreVersion", composite.getCoreVersion());
    summary.put("pipelinesVersion", composite.getPipelinesVersion());
    summary.put("connectionsVersion", composite.getConnectionsVersion());
    summary.put("regionalSettingsVersion", composite.getRegionalSettingsVersion());

    ConfigurationSnapshot core = composite.getCore();
    if (core != null) {
      summary.put("coreLastUpdated", core.getLastUpdated());
      summary.put("coreGlobalConfigKeys", core.getGlobalConfig().keySet().stream().sorted().collect(Collectors.toList()));
      summary.put("coreRegionConfigKeys", core.getRegionConfig().keySet().stream().sorted().collect(Collectors.toList()));
      summary.put("coreTenantIds", core.getTenantConfig().isEmpty() ? List.of() : core.getTenantConfig().keySet().stream().sorted().collect(Collectors.toList()));
      summary.put("coreResourceIds", core.getResourceConfig().isEmpty() ? List.of() : core.getResourceConfig().keySet().stream().sorted().collect(Collectors.toList()));
    }

    Set<String> pipelineIds = composite.getPipelines().keySet();
    summary.put("pipelineIds", pipelineIds.isEmpty() ? List.of() : pipelineIds.stream().sorted().collect(Collectors.toList()));

    Set<String> connectionIds = composite.getConnections().keySet();
    summary.put("connectionIds", connectionIds.isEmpty() ? List.of() : connectionIds.stream().sorted().collect(Collectors.toList()));

    return summary;
  }
}
