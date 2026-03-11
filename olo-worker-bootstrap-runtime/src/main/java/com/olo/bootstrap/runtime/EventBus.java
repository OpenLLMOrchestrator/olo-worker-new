package com.olo.bootstrap.runtime;

/**
 * Event bus for execution events (logging, metrics, UI, human-approval).
 * Lives for the lifetime of the worker; registered by the bootstrap loader.
 */
public interface EventBus {
  void publish(Object event);
}
