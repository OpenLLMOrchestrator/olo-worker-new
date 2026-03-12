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

    RunContext ctx = prepareRequest(parsed);
    String target = ctx.target;
    String namespace = ctx.namespace;
    OloWorkerRequest request = ctx.request;

    // Derive task queue from region + pipeline: olo.<region>.<pipeline>
    String region = request.getRegion();
    if (region == null || region.isBlank()) {
      region = "default";
    }
    String pipeline = request.getRouting() != null ? request.getRouting().getPipeline() : null;
    if (pipeline == null || pipeline.isBlank()) {
      throw new IllegalArgumentException("routing.pipeline is required to derive task queue (olo.<region>.<pipeline>)");
    }
    String taskQueue = "olo." + region + "." + pipeline;

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

  private static RunContext prepareRequest(Args parsed) throws IOException {
    String target = env("TEMPORAL_TARGET", "localhost:7233");
    String namespace = env("TEMPORAL_NAMESPACE", "default");

    String payloadJson = parsed.json;
    if (payloadJson == null && parsed.jsonFile != null) {
      payloadJson = Files.readString(Path.of(parsed.jsonFile), StandardCharsets.UTF_8);
    }
    if (payloadJson == null) {
      throw new IllegalArgumentException("Provide --jsonFile <path> or --json <payload>");
    }

    OloWorkerRequest request = OloPayloadParser.parse(payloadJson);
    OloPayloadValidator.validate(request);
    return new RunContext(target, namespace, request);
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

  private static final class RunContext {
    final String target;
    final String namespace;
    final OloWorkerRequest request;

    RunContext(String target, String namespace, OloWorkerRequest request) {
      this.target = target;
      this.namespace = namespace;
      this.request = request;
    }
  }
}

