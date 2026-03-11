package com.olo.worker.workflow;

import com.olo.workflow.input.model.Input;
import com.olo.workflow.input.model.OloWorkerRequest;
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;

import java.time.Duration;

public class OloChatWorkflowImpl implements OloChatWorkflow {
  private final InputResolverActivity inputResolver = Workflow.newActivityStub(
      InputResolverActivity.class,
      ActivityOptions.newBuilder()
          .setStartToCloseTimeout(Duration.ofSeconds(10))
          .build()
  );

  @Override
  public String run(OloWorkerRequest request) {
    String runId = request == null ? null : request.getRunId();
    String pipeline = request != null && request.getRouting() != null ? request.getRouting().getPipeline() : null;
    Workflow.getLogger(OloChatWorkflowImpl.class).info("Workflow started runId={} pipeline={}", runId, pipeline);

    String userQuery = null;
    Input userQueryInput = request == null ? null : request.getInput("userQuery");
    if (userQueryInput != null) {
      userQuery = inputResolver.resolveToString(userQueryInput);
    }

    if (userQuery == null || userQuery.isBlank()) {
      userQuery = "(missing userQuery)";
    }

    String result = "Received userQuery: " + userQuery;
    Workflow.getLogger(OloChatWorkflowImpl.class).info("Workflow completed runId={} result={}", runId, result);
    return result;
  }
}

