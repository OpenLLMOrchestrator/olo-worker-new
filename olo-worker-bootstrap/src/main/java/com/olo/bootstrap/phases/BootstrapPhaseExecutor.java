package com.olo.bootstrap.phases;

/**
 * Executes a single bootstrap phase (e.g. invokes contributors for that phase).
 * Used by {@link com.olo.bootstrap.orchestrator.BootstrapOrchestrator}.
 */
public interface BootstrapPhaseExecutor {
  void execute(BootstrapPhase phase, BootstrapContext context);
}
