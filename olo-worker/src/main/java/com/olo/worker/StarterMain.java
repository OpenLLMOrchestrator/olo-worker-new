package com.olo.worker;

import com.olo.workflow.input.model.OloWorkerRequest;
import com.olo.workflow.input.parser.OloPayloadParser;
import com.olo.workflow.input.validation.OloPayloadValidator;
import com.olo.worker.workflow.OloChatWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public final class StarterMain {
  public static void main(String[] args) throws IOException {
    Args parsed = Args.parse(args);

    String target = env("TEMPORAL_TARGET", "localhost:7233");
    String namespace = env("TEMPORAL_NAMESPACE", "default");
    String taskQueue = env("TASK_QUEUE", "olo-chat-queue-ollama");

    String payloadJson = parsed.json;
    if (payloadJson == null && parsed.jsonFile != null) {
      payloadJson = Files.readString(Path.of(parsed.jsonFile), StandardCharsets.UTF_8);
    }
    if (payloadJson == null) {
      throw new IllegalArgumentException("Provide --jsonFile <path> or --json <payload>");
    }

    OloWorkerRequest request = OloPayloadParser.parse(payloadJson);
    OloPayloadValidator.validate(request);

    String workflowId = "olo-" + (request.getRunId() == null ? UUID.randomUUID() : request.getRunId());

    WorkflowServiceStubs service = WorkflowServiceStubs.newServiceStubs(
        WorkflowServiceStubsOptions.newBuilder().setTarget(target).build()
    );

    WorkflowClient client = WorkflowClient.newInstance(
        service,
        WorkflowClientOptions.newBuilder().setNamespace(namespace).build()
    );

    OloChatWorkflow wf = client.newWorkflowStub(
        OloChatWorkflow.class,
        WorkflowOptions.newBuilder()
            .setTaskQueue(taskQueue)
            .setWorkflowId(workflowId)
            .build()
    );

    String result = wf.run(request);
    System.out.println("Workflow result: " + result);
  }

  private static String env(String name, String defaultValue) {
    String v = System.getenv(name);
    return (v == null || v.isBlank()) ? defaultValue : v;
  }

  private static final class Args {
    final String jsonFile;
    final String json;

    private Args(String jsonFile, String json) {
      this.jsonFile = jsonFile;
      this.json = json;
    }

    static Args parse(String[] args) {
      String jsonFile = null;
      String json = null;
      for (int i = 0; i < args.length; i++) {
        String a = args[i];
        if ("--jsonFile".equalsIgnoreCase(a) && i + 1 < args.length) {
          jsonFile = args[++i];
        } else if ("--json".equalsIgnoreCase(a) && i + 1 < args.length) {
          json = args[++i];
        }
      }
      return new Args(jsonFile, json);
    }
  }
}

