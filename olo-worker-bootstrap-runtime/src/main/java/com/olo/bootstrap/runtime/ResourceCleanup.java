package com.olo.bootstrap.runtime;

/**
 * Shutdown hook: close plugin pools, flush event bus, close connections.
 * Registered by the bootstrap loader; invoked when the worker stops.
 */
public interface ResourceCleanup {
  void onExit();
}
