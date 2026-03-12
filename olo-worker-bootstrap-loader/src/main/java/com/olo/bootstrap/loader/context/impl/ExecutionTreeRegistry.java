package com.olo.bootstrap.loader.context.impl;

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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable, per-region cache of compiled execution trees. Internal to the loader;
 * access is via {@link com.olo.bootstrap.loader.context.GlobalContext}.
 */
public final class ExecutionTreeRegistry {

  private static final Logger log = LoggerFactory.getLogger(ExecutionTreeRegistry.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static volatile Map<String, CompiledPipeline> compiledByChecksum = Map.of();
  /** Region → pipelineId → compiled pipeline (fast lookup cache). */
  private static volatile Map<String, Map<String, CompiledPipeline>> byRegion = Map.of();

  private ExecutionTreeRegistry() {}

  static CompiledPipeline get(String region, String pipelineId) {
    if (region == null || pipelineId == null) return null;
    Map<String, Map<String, CompiledPipeline>> snapshot = byRegion;
    Map<String, CompiledPipeline> regionMap = snapshot.get(region);
    if (regionMap == null) return null;
    return regionMap.get(pipelineId);
  }

  static void removeRegion(String region) {
    if (region == null || region.isEmpty()) return;
    Map<String, Map<String, CompiledPipeline>> currentByRegion = byRegion;
    if (!currentByRegion.containsKey(region)) return;

    Map<String, Map<String, CompiledPipeline>> nextByRegion = new LinkedHashMap<>(currentByRegion);
    nextByRegion.remove(region);
    byRegion = Map.copyOf(nextByRegion);
    log.info("Removed execution tree cache for region={}", region);
  }

  @SuppressWarnings("unchecked")
  static void rebuildForRegion(CompositeConfigurationSnapshot composite) {
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
      Map<String, Object> root;
      if (value instanceof Map) {
        root = (Map<String, Object>) value;
      } else if (value instanceof JsonNode) {
        root = MAPPER.convertValue(value, new TypeReference<Map<String, Object>>() {});
      } else if (value instanceof String) {
        try {
          root = MAPPER.readValue((String) value, new TypeReference<Map<String, Object>>() {});
        } catch (Exception parseEx) {
          log.warn("Pipeline {} invalid JSON string: {}", pipelineId, parseEx.getMessage());
          continue;
        }
      } else {
        continue;
      }
      try {
        String checksum = sha256(MAPPER.writeValueAsString(root));
        CompiledPipeline compiled = newCompiledByChecksum.get(checksum);
        if (compiled == null) {
          long version = root.get("version") instanceof Number
              ? ((Number) root.get("version")).longValue() : 0L;

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

          VariableRegistry vars = ExecutionTreeCompiler.compileVariableRegistry(
              (java.util.List<Map<String, Object>>) root.getOrDefault("variableRegistry", java.util.List.of()));
          Scope scope = ExecutionTreeCompiler.compileScope(
              (Map<String, Object>) root.getOrDefault("scope", Map.of()),
              root.get("features"));

          Map<String, Object> treeMap = root.get("executionTree") instanceof Map
              ? (Map<String, Object>) root.get("executionTree") : Map.of();
          List<String> allowedTenantIds = toAllowedTenantIds(root.get("allowedTenantIds"));
          ExecutionTreeNode rootNode = ExecutionTreeCompiler.compileNode(treeMap, allowedTenantIds);

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

  private static List<String> toAllowedTenantIds(Object v) {
    if (v == null) return null;
    if (v instanceof List) {
      List<String> out = new ArrayList<>();
      for (Object o : (List<?>) v) {
        if (o != null) out.add(o.toString());
      }
      return out.isEmpty() ? null : out;
    }
    return null;
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
