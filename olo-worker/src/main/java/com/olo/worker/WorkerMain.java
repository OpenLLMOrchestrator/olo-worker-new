package com.olo.worker;

import com.olo.bootstrap.loader.context.GlobalContext;
import com.olo.bootstrap.loader.context.GlobalContextProvider;
import com.olo.configuration.Bootstrap;
import com.olo.configuration.ConfigurationProvider;
import com.olo.worker.cache.CachePortRegistrar;
import com.olo.worker.db.DbPortRegistrar;
import com.olo.worker.workflow.InputResolverActivityImpl;
import com.olo.worker.workflow.OloChatWorkflowImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

public final class WorkerMain {
  public static void main(String[] args) {
    DbPortRegistrar.registerDefaults();
    CachePortRegistrar.registerDefaults();

    GlobalContext globalContext = GlobalContextProvider.getGlobalContext();
    // Whenever config is updated (bootstrap setSnapshotMap or refresh putComposite / Redis Pub/Sub),
    // rebuild the execution tree cache for that region so runtime uses fully compiled trees with no JSON parsing.
    ConfigurationProvider.addSnapshotChangeListener((region, composite) -> {
      if (composite != null) {
        globalContext.rebuildTreeForRegion(composite);
      } else {
        globalContext.removeTreeForRegion(region);
      }
    });

    // Only the loader touches Redis/DB; after bootstrap, runtime uses only in-memory config.
    Bootstrap.run();
    var config = globalContext.getConfig();

    String target = env("TEMPORAL_TARGET", config.get("olo.temporal.target", "localhost:7233"));
    String namespace = env("TEMPORAL_NAMESPACE", config.get("olo.temporal.namespace", "default"));
    String taskQueue = env("TASK_QUEUE", config.get("olo.temporal.task_queue", "olo-chat-queue-ollama"));

    WorkflowServiceStubs service = WorkflowServiceStubs.newServiceStubs(
        WorkflowServiceStubsOptions.newBuilder().setTarget(target).build()
    );

    WorkflowClient client = WorkflowClient.newInstance(
        service,
        WorkflowClientOptions.newBuilder().setNamespace(namespace).build()
    );

    WorkerFactory factory = WorkerFactory.newInstance(client);
    Worker worker = factory.newWorker(taskQueue);

    worker.registerWorkflowImplementationTypes(OloChatWorkflowImpl.class);
    worker.registerActivitiesImplementations(new InputResolverActivityImpl());

    factory.start();
    System.out.println("Worker started. target=" + target + " namespace=" + namespace + " taskQueue=" + taskQueue);
  }

  private static String env(String name, String defaultValue) {
    String v = System.getenv(name);
    return (v == null || v.isBlank()) ? defaultValue : v;
  }
}

