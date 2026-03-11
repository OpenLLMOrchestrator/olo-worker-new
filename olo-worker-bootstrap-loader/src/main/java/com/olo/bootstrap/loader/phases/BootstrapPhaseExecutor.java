package com.olo.bootstrap.loader.phases;

public interface BootstrapPhaseExecutor {
  void execute(BootstrapPhase phase, BootstrapContext context);
}
