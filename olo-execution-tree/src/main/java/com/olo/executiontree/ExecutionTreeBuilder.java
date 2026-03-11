package com.olo.executiontree;

import com.olo.configuration.Configuration;

/**
 * Builds execution trees and snapshots from pipeline configuration per tenant/queue.
 * Used during bootstrap phase PIPELINE_LOADING to produce immutable ExecutionConfigSnapshot.
 *
 * @see ExecutionConfigSnapshot
 * @see ExecutionTreeCompiler
 */
public interface ExecutionTreeBuilder {
  /**
   * Builds an immutable execution config snapshot for the given tenant and queue from config.
   */
  ExecutionConfigSnapshot buildForTenant(String tenantId, String queueId, Configuration config);
}
