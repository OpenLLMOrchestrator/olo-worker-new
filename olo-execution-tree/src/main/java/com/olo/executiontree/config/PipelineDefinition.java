package com.olo.executiontree.config;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.olo.executiontree.inputcontract.InputContract;
import com.olo.executiontree.outputcontract.ResultMapping;
import com.olo.executiontree.scope.Scope;
import com.olo.executiontree.tree.ExecutionTreeNode;
import com.olo.executiontree.variableregistry.VariableRegistryEntry;

import java.util.List;

/**
 * Contract for pipeline definition used by worker and plan services.
 * Implemented by {@link com.olo.executiontree.PipelineDefinition}.
 * Type info is required so activity payload configJson can be deserialized from JSON.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
@JsonSubTypes(@JsonSubTypes.Type(com.olo.executiontree.PipelineDefinition.class))
public interface PipelineDefinition {

    String getName();
    Scope getScope();
    ExecutionTreeNode getExecutionTree();
    ExecutionType getExecutionType();
    InputContract getInputContract();
    List<VariableRegistryEntry> getVariableRegistry();
    List<ResultMapping> getResultMapping();
}
