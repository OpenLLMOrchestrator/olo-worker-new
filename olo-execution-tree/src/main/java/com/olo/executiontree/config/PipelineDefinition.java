package com.olo.executiontree.config;

import com.olo.executiontree.inputcontract.InputContract;
import com.olo.executiontree.outputcontract.ResultMapping;
import com.olo.executiontree.scope.Scope;
import com.olo.executiontree.tree.ExecutionTreeNode;
import com.olo.executiontree.variableregistry.VariableRegistryEntry;

import java.util.List;

/**
 * Contract for pipeline definition used by worker and plan services.
 * Implemented by {@link com.olo.executiontree.PipelineDefinition}.
 */
public interface PipelineDefinition {

    String getName();
    Scope getScope();
    ExecutionTreeNode getExecutionTree();
    ExecutionType getExecutionType();
    InputContract getInputContract();
    List<VariableRegistryEntry> getVariableRegistry();
    List<ResultMapping> getResultMapping();
}
