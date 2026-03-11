package com.olo.bootstrap.phases;

import java.util.Set;

/**
 * Extension point for bootstrap. Contributors participate in one or more phases;
 * ordering between contributors can be expressed via {@link #dependsOn()}.
 *
 * @see BootstrapOrchestrator
 * @see BootstrapPhase
 */
public interface BootstrapContributor {
  /**
   * Phases this contributor participates in.
   */
  Set<BootstrapPhase> phases();

  /**
   * Other contributor types this one depends on (executed first). Used for topological ordering.
   */
  default Set<Class<? extends BootstrapContributor>> dependsOn() {
    return Set.of();
  }

  /**
   * Called once per phase that this contributor participates in.
   */
  void contribute(BootstrapPhase phase, BootstrapContext context);
}
