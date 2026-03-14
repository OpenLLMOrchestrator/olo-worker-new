package com.olo.config;

import com.olo.input.model.WorkflowInput;

/**
 * Session cache for workflow input and active workflow counts.
 * Contract in protocol; implementations in configuration or dedicated modules.
 */
public interface OloSessionCache {

    void cacheUpdate(WorkflowInput input);

    void incrActiveWorkflows(String tenantId);

    void decrActiveWorkflows(String tenantId);
}
