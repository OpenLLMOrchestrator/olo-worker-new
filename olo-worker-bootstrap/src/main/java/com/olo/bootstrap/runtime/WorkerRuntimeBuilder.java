package com.olo.bootstrap.runtime;

import com.olo.bootstrap.registry.BootstrapServiceRegistry;

/**
 * Builds WorkerRuntime from the bootstrap service registry after all phases complete.
 */
public final class WorkerRuntimeBuilder {
  private final BootstrapServiceRegistry registry;

  public WorkerRuntimeBuilder(BootstrapServiceRegistry registry) {
    this.registry = registry;
  }

  public WorkerRuntime build() {
    return new DefaultWorkerRuntime(registry);
  }

  private static final class DefaultWorkerRuntime implements WorkerRuntime {
    private final BootstrapServiceRegistry registry;

    DefaultWorkerRuntime(BootstrapServiceRegistry registry) {
      this.registry = registry;
    }

    @Override
    public BootstrapServiceRegistry services() {
      return registry;
    }
  }
}
