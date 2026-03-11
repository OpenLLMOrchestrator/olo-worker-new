package com.olo.bootstrap.registry;

/**
 * Registry for services created during bootstrap. Used by contributors to register
 * and by the runtime builder to obtain PluginExecutorFactory, FeatureRuntime, EventBus, etc.
 *
 * @see com.olo.bootstrap.runtime.WorkerRuntimeBuilder
 */
public interface BootstrapServiceRegistry {
  /**
   * Register a service implementation for the given type.
   */
  <T> void register(Class<T> serviceType, T implementation);

  /**
   * Obtain a registered service, or null if not registered.
   */
  <T> T get(Class<T> serviceType);
}
