package com.olo.workflow.input.model;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Value
@Builder
@Jacksonized
public class RuntimeModels {
  String strategy;
  String primary;
  List<String> fallback;
}
