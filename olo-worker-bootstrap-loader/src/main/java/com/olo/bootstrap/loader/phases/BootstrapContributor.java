package com.olo.bootstrap.loader.phases;

import java.util.Set;

public interface BootstrapContributor {
  Set<BootstrapPhase> phases();
  default Set<Class<? extends BootstrapContributor>> dependsOn() { return Set.of(); }
  void contribute(BootstrapPhase phase, BootstrapContext context);
}
