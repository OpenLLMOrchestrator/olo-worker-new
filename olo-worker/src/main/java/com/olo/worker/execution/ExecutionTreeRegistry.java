package com.olo.worker.execution;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.olo.configuration.snapshot.CompositeConfigurationSnapshot;
import com.olo.executiontree.CompiledPipeline;
import com.olo.executiontree.ExecutionTreeCompiler;
import com.olo.executiontree.ExecutionTreeNode;
import com.olo.executiontree.PipelineDefinition;
import com.olo.executiontree.Scope;
import com.olo.executiontree.VariableRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable, per-region cache of compiled execution trees.
 *
 * <p>Design:
 * <ul>
 *   <li>Deduplicates compiled pipelines across regions by checksum.</li>
 *   <li>Per-region registry maps pipelineId -> CompiledPipeline.</li>
 *   <li>Refresh builds a fresh structure and swaps it atomically (volatile write).</li>
 * </ul>
 * </p>
 */
public final class ExecutionTreeRegistry {

  private static final Logger log = LoggerFactory.getLogger(ExecutionTreeRegistry.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();

  /**
   * Global compiled pipeline registry by checksum to deduplicate identical definitions across regions.
   * immutable map, replaced atomically.
   */
  private static volatile Map<String, CompiledPipeline> compiledByChecksum = Map.of();

  /**
   * Per-region registry: region -> (pipelineId -> CompiledPipeline).
   * immutable map, replaced atomically.
   */
  private static volatile Map<String, Map<String, CompiledPipeline>> byRegion = Map.of();

  private ExecutionTreeRegistry() {}

  /**
   * Returns compiled pipeline for region + pipelineId, or null if missing.
   */
  public static CompiledPipeline get(String region, String pipelineId) {
    if (region == null || pipelineId == null) return null;
    Map<String, Map<String, CompiledPipeline>> snapshot = byRegion;
    Map<String, CompiledPipeline> regionMap = snapshot.get(region);
    if (regionMap == null) return null;
    return regionMap.get(pipelineId);
  }

  /**
   * Rebuilds compiled pipelines for a single region from the composite snapshot's pipelines map.
   * Called from the configuration refresh path after a new CompositeConfigurationSnapshot is installed.
   */
  @SuppressWarnings("unchecked")
  public static void rebuildForRegion(CompositeConfigurationSnapshot composite) {
    if (composite == null) return;
    String region = composite.getRegion();
    Map<String, Object> pipelines = composite.getPipelines();
    if (pipelines == null || pipelines.isEmpty()) pipelines = Map.of();

    Map<String, CompiledPipeline> currentCompiledByChecksum = compiledByChecksum;
    Map<String, Map<String, CompiledPipeline>> currentByRegion = byRegion;

    Map<String, CompiledPipeline> newRegionMap = new LinkedHashMap<>();
    Map<String, CompiledPipeline> newCompiledByChecksum = new LinkedHashMap<>(currentCompiledByChecksum);

    for (Map.Entry<String, Object> e : pipelines.entrySet()) {
      String pipelineId = e.getKey();
      Object value = e.getValue();
      if (!(value instanceof JsonNode)) {
        continue;
      }
      JsonNode json = (JsonNode) value;
      try {
        String checksum = sha256(json.toString());
        CompiledPipeline compiled = newCompiledByChecksum.get(checksum);
        if (compiled == null) {
          Map<String, Object> root = MAPPER.convertValue(json, new TypeReference<Map<String, Object>>() {});
          long version = root.getOrDefault("version", 0L) instanceof Number
              ? ((Number) root.get("version")).longValue() : 0L;

          // TODO: replace with a typed DTO if schema stabilizes.
          Map<String, Object> inputContract = root.get("inputContract") instanceof Map
              ? (Map<String, Object>) root.get("inputContract") : Map.of();
          Map<String, Object> outputContract = root.get("outputContract") instanceof Map
              ? (Map<String, Object>) root.get("outputContract") : Map.of();
          Map<String, Object> resultMappingRaw = root.get("resultMapping") instanceof Map
              ? (Map<String, Object>) root.get("resultMapping") : Map.of();
          Map<String, String> resultMapping = new LinkedHashMap<>();
          for (Map.Entry<String, Object> rm : resultMappingRaw.entrySet()) {
            resultMapping.put(rm.getKey(), rm.getValue() == null ? "" : rm.getValue().toString());
          }

          // Variable registry and scope from config.
          VariableRegistry vars = ExecutionTreeCompiler.compileVariableRegistry(
              (java.util.List<Map<String, Object>>) root.getOrDefault("variableRegistry", java.util.List.of()));
          Scope scope = ExecutionTreeCompiler.compileScope(
              (Map<String, Object>) root.getOrDefault("scope", Map.of()),
              root.get("features"));

          Map<String, Object> treeMap = root.get("executionTree") instanceof Map
              ? (Map<String, Object>) root.get("executionTree") : Map.of();
          ExecutionTreeNode rootNode = ExecutionTreeCompiler.compileNode(treeMap);

          PipelineDefinition def = new PipelineDefinition(
              pipelineId,
              inputContract,
              vars,
              scope,
              rootNode,
              outputContract,
              resultMapping,
              "SYNC");
          compiled = new CompiledPipeline(pipelineId, version, checksum, def);
          newCompiledByChecksum.put(checksum, compiled);
        }
        newRegionMap.put(pipelineId, compiled);
      } catch (Exception ex) {
        log.warn("Failed to compile pipeline for region={} pipelineId={}: {}", region, pipelineId, ex.getMessage());
      }
    }

    Map<String, Map<String, CompiledPipeline>> newByRegion = new LinkedHashMap<>(currentByRegion);
    newByRegion.put(region, Map.copyOf(newRegionMap));

    compiledByChecksum = Map.copyOf(newCompiledByChecksum);
    byRegion = Map.copyOf(newByRegion);

    log.info("Rebuilt execution tree registry for region={} pipelines={}", region, newRegionMap.size());
  }

  private static String sha256(String s) throws Exception {
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    byte[] digest = md.digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    StringBuilder sb = new StringBuilder(digest.length * 2);
    for (byte b : digest) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }
}

