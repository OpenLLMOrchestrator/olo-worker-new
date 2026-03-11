package com.olo.executiontree;

/**
 * Scope of a variable in the pipeline variable registry.
 *
 * @see VariableDeclaration
 * @see VariableRegistry
 */
public enum VariableScope {
  /** Input: seeded from workflow input. */
  IN,
  /** Internal: intermediate state; not part of input or output contract. */
  INTERNAL,
  /** Output: may be used in resultMapping. */
  OUT
}
