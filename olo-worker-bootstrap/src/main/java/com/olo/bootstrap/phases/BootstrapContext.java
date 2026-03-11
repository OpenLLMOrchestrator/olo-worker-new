package com.olo.bootstrap.phases;

import com.olo.bootstrap.registry.BootstrapServiceRegistry;

/**
 * Context passed to {@link BootstrapContributor}s during each phase.
 */
public interface BootstrapContext {
  BootstrapServiceRegistry getRegistry();
  BootstrapState getState();
}
