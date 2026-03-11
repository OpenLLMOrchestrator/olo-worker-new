package com.olo.bootstrap.loader.phases;

import com.olo.bootstrap.loader.registry.BootstrapServiceRegistry;

public interface BootstrapContext {
  BootstrapServiceRegistry getRegistry();
  BootstrapState getState();
}
