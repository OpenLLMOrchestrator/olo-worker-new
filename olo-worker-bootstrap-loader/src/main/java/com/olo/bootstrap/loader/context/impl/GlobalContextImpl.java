package com.olo.bootstrap.loader.context.impl;

import com.olo.bootstrap.loader.context.GlobalContext;
import com.olo.configuration.Configuration;
import com.olo.configuration.ConfigurationProvider;
import com.olo.configuration.region.TenantRegionResolver;
import com.olo.configuration.snapshot.CompositeConfigurationSnapshot;
import com.olo.executiontree.CompiledPipeline;

import java.util.Map;

/** Implementation of {@link GlobalContext}; delegates to configuration and execution tree registry. */
public final class GlobalContextImpl implements GlobalContext {

  @Override
  public Configuration getConfig() {
    return ConfigurationProvider.require();
  }

  @Override
  public Configuration getConfigForTenant(String tenantId) {
    return ConfigurationProvider.forTenant(tenantId);
  }

  @Override
  public Map<String, CompositeConfigurationSnapshot> getSnapshotMap() {
    return ConfigurationProvider.getSnapshotMap();
  }

  @Override
  public CompositeConfigurationSnapshot getPrimaryComposite() {
    return ConfigurationProvider.getComposite();
  }

  @Override
  public Map<String, String> getTenantToRegionMap() {
    return TenantRegionResolver.getTenantToRegionMap();
  }

  @Override
  public CompiledPipeline getCompiledPipeline(String region, String pipelineId) {
    return ExecutionTreeRegistry.get(region, pipelineId);
  }

  @Override
  public CompiledPipeline getCompiledPipelineForTenant(String tenantId, String pipelineId) {
    if (tenantId == null || tenantId.isBlank()) {
      CompositeConfigurationSnapshot primary = ConfigurationProvider.getComposite();
      if (primary == null) return null;
      return ExecutionTreeRegistry.get(primary.getRegion(), pipelineId);
    }
    String region = TenantRegionResolver.getRegion(tenantId);
    if (region == null || region.isBlank()) {
      CompositeConfigurationSnapshot primary = ConfigurationProvider.getComposite();
      region = primary != null ? primary.getRegion() : null;
    }
    if (region == null || region.isBlank()) return null;
    return ExecutionTreeRegistry.get(region, pipelineId);
  }

  @Override
  public void rebuildTreeForRegion(CompositeConfigurationSnapshot composite) {
    ExecutionTreeRegistry.rebuildForRegion(composite);
  }

  @Override
  public void removeTreeForRegion(String region) {
    ExecutionTreeRegistry.removeRegion(region);
  }
}
