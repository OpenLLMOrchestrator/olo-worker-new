package com.olo.bootstrap.phases;

/**
 * Lifecycle state of bootstrap. Prevents misuse (e.g. using registries before initialization).
 */
public enum BootstrapState {
  CREATED,
  INITIALIZING,
  INITIALIZED,
  FAILED
}
