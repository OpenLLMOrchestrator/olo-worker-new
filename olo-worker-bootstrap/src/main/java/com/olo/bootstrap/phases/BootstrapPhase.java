package com.olo.bootstrap.phases;

/**
 * Bootstrap phases in execution order. All wiring and discovery happens during these phases;
 * runtime does not perform discovery or scanning.
 *
 * @see com.olo.bootstrap.OloBootstrap
 * @see com.olo.bootstrap.phases.BootstrapContributor
 */
public enum BootstrapPhase {
  /** Core infrastructure: logging, config, basic registries. */
  CORE_SERVICES,
  /** Resolve environment: paths, plugin dirs, tenants, queues. */
  ENVIRONMENT_LOAD,
  /** Verify Redis, DB, secrets manager reachable before starting workers. */
  INFRASTRUCTURE_READY,
  /** Discover plugin descriptors (e.g. META-INF/olo-plugin.json) and populate PluginRegistry. */
  PLUGIN_DISCOVERY,
  /** Discover feature descriptors and populate FeatureRegistry. */
  FEATURE_DISCOVERY,
  /** Wire connection, secret, and event providers. */
  RESOURCE_PROVIDERS,
  /** Load and validate pipelines; build execution trees and snapshots. */
  PIPELINE_LOADING,
  /** Validate plugin refs, connection refs, feature attachments. */
  VALIDATION,
  /** Build WorkerBootstrapContext / WorkerRuntime from registries. */
  CONTEXT_BUILD,
  /** Start Temporal (or local) workers. */
  WORKER_START
}
