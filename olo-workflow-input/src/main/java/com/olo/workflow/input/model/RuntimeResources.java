package com.olo.workflow.input.model;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;

@Value
@Builder
@Jacksonized
public class RuntimeResources {
  Map<String, Map<String, Object>> plugins;
  Map<String, Map<String, Object>> features;
}
