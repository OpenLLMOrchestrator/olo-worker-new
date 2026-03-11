package com.olo.bootstrap.runtime;

import com.olo.bootstrap.registry.BootstrapServiceRegistry;

/**
 * Runtime façade exposed to the worker. Access to services (plugins, features, events, etc.).
 */
public interface WorkerRuntime {
  BootstrapServiceRegistry services();
}
