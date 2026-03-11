package com.olo.bootstrap.orchestrator;

import com.olo.bootstrap.phases.BootstrapContext;
import com.olo.bootstrap.phases.BootstrapContributor;
import com.olo.bootstrap.phases.BootstrapPhase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Runs bootstrap phases in order and invokes contributors per phase. Keeps OloBootstrap
 * as a thin entry point; orchestration and dependency ordering live here.
 *
 * @see com.olo.bootstrap.OloBootstrap
 * @see com.olo.bootstrap.phases.BootstrapContributor
 */
public final class BootstrapOrchestrator {

  private static final Logger log = LoggerFactory.getLogger(BootstrapOrchestrator.class);

  private final List<BootstrapContributor> contributors;

  public BootstrapOrchestrator(List<BootstrapContributor> contributors) {
    this.contributors = contributors != null ? List.copyOf(contributors) : List.of();
  }

  /**
   * Runs all phases in order, invoking each contributor that participates in that phase.
   * Contributors should be topologically sorted by dependency before being passed to the constructor.
   */
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
