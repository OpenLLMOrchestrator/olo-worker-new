package com.olo.bootstrap.loader.validation;

import com.olo.executiontree.config.PipelineConfiguration;
import com.olo.features.FeatureRegistry;
import com.olo.plugin.PluginRegistry;

/** Validates config version and plugin/feature contracts. No-op when not wired. */
public final class ConfigCompatibilityValidator {

    public ConfigCompatibilityValidator(Object pluginExecutorFactory, Object featureRegistryOrNull,
                                        PluginRegistry pluginRegistry, FeatureRegistry featureRegistry) {}

    public void validateOrThrow(String tenantKey, PipelineConfiguration config) throws ConfigIncompatibleException {}
}
