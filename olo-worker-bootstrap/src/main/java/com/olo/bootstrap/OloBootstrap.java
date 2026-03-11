package com.olo.bootstrap;

import com.olo.bootstrap.orchestrator.BootstrapOrchestrator;
import com.olo.bootstrap.phases.BootstrapContributor;
import com.olo.bootstrap.phases.BootstrapContext;
import com.olo.bootstrap.phases.BootstrapState;
import com.olo.bootstrap.registry.BootstrapServiceRegistry;
import com.olo.bootstrap.runtime.WorkerRuntime;
import com.olo.bootstrap.runtime.WorkerRuntimeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Public entry point for worker bootstrap. Delegates to {@link BootstrapOrchestrator}
 * for phase execution and to {@link WorkerRuntimeBuilder} for the runtime view.
 *
 * <p>Flow: OloWorkerApplication → OloBootstrap.initializeWorker() → WorkerBootstrapContext / WorkerRuntime
 * → Temporal workers start using the configured context.
 *
 * @see com.olo.bootstrap.orchestrator.BootstrapOrchestrator
 * @see com.olo.bootstrap.phases.BootstrapContributor
 */
public final class OloBootstrap {

  private static final Logger log = LoggerFactory.getLogger(OloBootstrap.class);

  private OloBootstrap() {}

  /**
   * Initializes the worker: runs bootstrap phases via contributors, then builds and returns
   * the runtime context for starting Temporal (or local) workers.
   *
   * @param contributors contributors to run (should be dependency-ordered)
   * @return the built WorkerRuntime; never null if no phase failed
   */
  public static WorkerRuntime initializeWorker(List<? extends BootstrapContributor> contributors) {
    List<BootstrapContributor> list = contributors != null
        ? new ArrayList<>(contributors)
        : new ArrayList<>();
    BootstrapServiceRegistry registry = new DefaultBootstrapServiceRegistry();
    BootstrapContext context = new DefaultBootstrapContext(registry);
    BootstrapOrchestrator orchestrator = new BootstrapOrchestrator(list);
    orchestrator.run(context);
    return new WorkerRuntimeBuilder(registry).build();
  }

  private static final class DefaultBootstrapServiceRegistry implements BootstrapServiceRegistry {
    private final java.util.Map<Class<?>, Object> map = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public <T> void register(Class<T> serviceType, T implementation) {
      map.put(serviceType, implementation);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> serviceType) {
      return (T) map.get(serviceType);
    }
  }

  private static final class DefaultBootstrapContext implements BootstrapContext {
    private final BootstrapServiceRegistry registry;
    private volatile BootstrapState state = BootstrapState.INITIALIZING;

    DefaultBootstrapContext(BootstrapServiceRegistry registry) {
      this.registry = registry;
    }

    @Override
    public BootstrapServiceRegistry getRegistry() {
      return registry;
    }

    @Override
    public BootstrapState getState() {
      return state;
    }
  }
}
