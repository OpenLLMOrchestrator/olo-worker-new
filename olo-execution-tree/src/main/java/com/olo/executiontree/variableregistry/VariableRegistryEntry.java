package com.olo.executiontree.variableregistry;

/** Single entry in the pipeline variable registry. */
public interface VariableRegistryEntry {
    String getName();
    VariableScope getScope();
}
