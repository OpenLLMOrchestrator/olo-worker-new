package com.olo.worker.workflow;

import com.olo.workflow.input.model.Input;
import com.olo.workflow.input.util.InputResolver;

public class InputResolverActivityImpl implements InputResolverActivity {
  @Override
  public String resolveToString(Input input) {
    return InputResolver.resolveToString(input);
  }
}
