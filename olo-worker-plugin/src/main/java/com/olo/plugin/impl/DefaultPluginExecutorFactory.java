package com.olo.plugin.impl;

import com.olo.plugin.PluginExecutorFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of {@link PluginExecutorFactory} that creates {@link RegistryPluginExecutor}
 * instances. Bootstrap wires this so the worker receives the factory without depending on PluginRegistry.
 */
public final class DefaultPluginExecutorFactory implements PluginExecutorFactory {

    @Override
    public com.olo.plugin.PluginExecutor create(String tenantId, Map<String, ?> nodeInstanceCache) {
        Map<String, ?> cache = nodeInstanceCache != null ? nodeInstanceCache : new ConcurrentHashMap<>();
        return new RegistryPluginExecutor(tenantId, cache);
    }
}
