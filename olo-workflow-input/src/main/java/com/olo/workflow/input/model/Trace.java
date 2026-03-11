package com.olo.workflow.input.model;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class Trace {
  String traceId;
  String spanId;
  String parentSpanId;
}
