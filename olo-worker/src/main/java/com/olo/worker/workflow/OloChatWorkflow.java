package com.olo.worker.workflow;

import com.olo.workflow.input.model.OloWorkerRequest;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface OloChatWorkflow {
  @WorkflowMethod
  String run(OloWorkerRequest request);
}

