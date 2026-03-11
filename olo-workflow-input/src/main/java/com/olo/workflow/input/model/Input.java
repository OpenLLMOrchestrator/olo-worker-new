package com.olo.workflow.input.model;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;

/**
 * Single input (name, type, storage, value). Storage can use resource-based or legacy keys.
 * Preferred: {@code type}, {@code resource} (e.g. olo:resource:connection:redis:cache), {@code key} or {@code path}.
 * Legacy: {@code mode} (LOCAL|CACHE|FILE|S3), {@code provider}, {@code key}/ {@code path}.
 */
@Value
@Builder
@Jacksonized
public class Input {
  String name;
  /** Type as string (e.g. STRING, FILE); see {@link com.olo.workflow.input.model.enums.InputType} for known values. */
  String type;
  /** Storage: use "type" + "resource" + "key" (or "path") to reference connections; or legacy "mode" + "provider" + "key". */
  Map<String, Object> storage;
  Object value;
}
