package com.olo.worker;

import com.olo.configuration.Bootstrap;
import com.olo.configuration.Configuration;
import com.olo.configuration.ConfigurationProvider;
import com.olo.worker.cache.CachePortRegistrar;
import com.olo.worker.db.DbPortRegistrar;
import com.olo.worker.execution.ExecutionTreeRegistry;
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
    // Only the loader touches Redis/DB; after bootstrap, runtime uses only in-memory config.
    Bootstrap.run();
    Configuration config = ConfigurationProvider.require();

    // Build initial execution tree cache for all loaded regions.
    var snapshotMap = ConfigurationProvider.getSnapshotMap();
    if (snapshotMap != null) {
      snapshotMap.values().forEach(ExecutionTreeRegistry::rebuildForRegion);
    } else {
      var primary = ConfigurationProvider.getComposite();
      if (primary != null) {
        ExecutionTreeRegistry.rebuildForRegion(primary);
      }
    }

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

