package com.olo.bootstrap.loader.orchestrator;

import com.olo.bootstrap.loader.phases.BootstrapContext;
import com.olo.bootstrap.loader.phases.BootstrapContributor;
import com.olo.bootstrap.loader.phases.BootstrapPhase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Runs bootstrap phases in order and invokes contributors per phase.
 * Used by {@link com.olo.bootstrap.loader.BootstrapLoader}.
 */
public final class BootstrapOrchestrator {

  private static final Logger log = LoggerFactory.getLogger(BootstrapOrchestrator.class);

  private final List<BootstrapContributor> contributors;

  public BootstrapOrchestrator(List<BootstrapContributor> contributors) {
    this.contributors = contributors != null ? List.copyOf(contributors) : List.of();
  }

  public void run(BootstrapContext context) {
    for (BootstrapPhase phase : BootstrapPhase.values()) {
      log.debug("Bootstrap phase: {}", phase);
      for (BootstrapContributor c : contributors) {
        if (c.phases().contains(phase)) {
          c.contribute(phase, context);
        }
      }
    }
  }
}
