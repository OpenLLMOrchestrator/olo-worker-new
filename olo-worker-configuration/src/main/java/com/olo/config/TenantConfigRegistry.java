package com.olo.config;

/**
 * Registry for tenant config. Stub when using new configuration module;
 * use {@link com.olo.configuration.ConfigurationProvider} for config.
 */
public final class TenantConfigRegistry {

    private static volatile TenantConfigRegistry instance;

    public static TenantConfigRegistry getInstance() {
        if (instance == null) {
            synchronized (TenantConfigRegistry.class) {
                if (instance == null) instance = new TenantConfigRegistry();
            }
        }
        return instance;
    }

    public TenantConfig get(String tenantId) {
        return TenantConfig.EMPTY;
    }

    private TenantConfigRegistry() {}
}
