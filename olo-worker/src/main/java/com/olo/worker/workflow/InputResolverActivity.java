package com.olo.worker.workflow;

import com.olo.workflow.input.model.Input;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface InputResolverActivity {
  @ActivityMethod
  String resolveToString(Input input);
}

