package com.olo.bootstrap.loader.registry;

import com.olo.bootstrap.runtime.ServiceRegistry;

/**
 * Registry for services created during bootstrap. Contributors register implementations
 * here; the loader then passes this to {@link com.olo.bootstrap.runtime.WorkerRuntimeBuilder}
 * to build {@link com.olo.bootstrap.runtime.WorkerRuntime}. Extends {@link ServiceRegistry}
 * so it can be used as the runtime's service source.
 */
public interface BootstrapServiceRegistry extends ServiceRegistry {
  <T> void register(Class<T> serviceType, T implementation);
}
