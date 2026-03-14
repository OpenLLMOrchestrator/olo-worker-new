package com.olo.config;

/**
 * Config and tenant normalization. Used by protocol and worker; use
 * {@link com.olo.configuration.ConfigurationProvider} for full config.
 */
public final class OloConfig {

    private OloConfig() {}

    /** Normalizes tenant ID; null or blank returns default tenant. */
    public static String normalizeTenantId(String tenantId) {
        return (tenantId != null && !tenantId.isBlank()) ? tenantId.trim() : "default";
    }
}
