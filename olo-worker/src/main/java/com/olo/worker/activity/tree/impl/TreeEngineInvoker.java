package com.olo.worker.activity.tree.impl;

import com.olo.bootstrap.runtime.OloRuntimeContext;
import com.olo.input.model.InputItem;
import com.olo.worker.engine.ExecutionEngine;
import com.olo.plugin.PluginExecutorFactory;

import java.util.LinkedHashMap;
import java.util.Map;

final class TreeEngineInvoker {

    static String run(TreeContextResolver.ResolvedContext ctx,
                      OloRuntimeContext runtimeContext,
                      PluginExecutorFactory pluginExecutorFactory,
                      com.olo.node.DynamicNodeBuilder dynamicNodeBuilder,
                      com.olo.node.NodeFeatureEnricher nodeFeatureEnricher) {
        var executor = pluginExecutorFactory.create(ctx.tenantId, ctx.nodeInstanceCache);
        Map<String, Object> inputValues = inputValuesFrom(runtimeContext.getWorkflowInput());
        return ExecutionEngine.run(
                runtimeContext.getPipelineDefinition(),
                ctx.effectiveQueue,
                inputValues,
                executor,
                ctx.tenantId,
                ctx.tenantConfigMap,
                ctx.runId,
                ctx.config,
                dynamicNodeBuilder,
                nodeFeatureEnricher);
    }

    private static Map<String, Object> inputValuesFrom(com.olo.input.model.WorkflowInput input) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (input != null && input.getInputs() != null) {
            for (InputItem item : input.getInputs()) {
                if (item != null && item.getName() != null) {
                    out.put(item.getName(), item.getValue() != null ? item.getValue() : "");
                }
            }
        }
        return out;
    }
}
