package com.olo.workflow.input.util;

import com.olo.workflow.input.model.Input;

import java.util.Map;

/**
 * Resolves an {@link Input} to a string value based on its storage.
 * Supports resource-based storage (type + resource + key) and legacy (mode + provider + key).
 * LOCAL is supported; CACHE, FILE, S3 require external resolution (e.g. in activities).
 */
public final class InputResolver {

  private InputResolver() {}

  /**
   * Resolves the input to a string. For LOCAL storage returns the value; for other types throws.
   *
   * @param input the input (may be null)
   * @return the string value for LOCAL, or null if input is null
   * @throws IllegalStateException for CACHE, FILE, S3 or unknown type/mode
   */
  public static String resolveToString(Input input) {
    if (input == null) {
      return null;
    }
    Map<String, Object> storage = input.getStorage();
    if (storage == null) {
      throw new IllegalStateException("Input storage is missing for input=" + input.getName());
    }
    // Prefer type + resource; fallback to legacy mode
    String type = getString(storage, "type");
    String mode = getString(storage, "mode");
    String effective = type != null && !type.isBlank() ? type : mode;

    if ("LOCAL".equalsIgnoreCase(effective)) {
      Object v = input.getValue();
      return v == null ? null : String.valueOf(v);
    }

    if ("CACHE".equalsIgnoreCase(effective)) {
      String key = getString(storage, "key");
      String resource = getString(storage, "resource");
      throw new IllegalStateException("CACHE input resolution not implemented. resource=" + resource + " key=" + key);
    }

    if ("FILE".equalsIgnoreCase(effective)) {
      String path = getString(storage, "path");
      String resource = getString(storage, "resource");
      throw new IllegalStateException("FILE input resolution not implemented. resource=" + resource + " path=" + path);
    }

    if ("S3".equalsIgnoreCase(effective)) {
      throw new IllegalStateException("S3 input resolution not implemented");
    }

    if (effective == null || effective.isBlank()) {
      throw new IllegalStateException("Input storage.type (or mode) is missing for input=" + input.getName());
    }

    throw new IllegalStateException("Unsupported input storage type=" + effective + " for input=" + input.getName());
  }

  private static String getString(Map<String, Object> map, String key) {
    if (map == null) {
      return null;
    }
    Object v = map.get(key);
    return v == null ? null : String.valueOf(v);
  }
}
