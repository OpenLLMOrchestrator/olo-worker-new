package com.olo.executiontree.load;

import com.olo.executiontree.config.PipelineConfiguration;

/** Stub: global config storage by tenant/queue. No-op when not wired. */
public final class GlobalConfigurationContext {
    private GlobalConfigurationContext() {}
    public static void put(String tenantKey, String queueName, PipelineConfiguration config) {}
}
