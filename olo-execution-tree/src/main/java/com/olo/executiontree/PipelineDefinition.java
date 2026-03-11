package com.olo.executiontree;

import java.util.Map;

/**
 * Pipeline definition: name, input/output contract, variable registry, scope,
 * and the execution tree root. Immutable once built; used to produce ExecutionConfigSnapshot.
 *
 * @see ExecutionTreeNode
 * @see VariableRegistry
 * @see Scope
 */
public final class PipelineDefinition {
  private final String name;
  private final Map<String, Object> inputContract;
  private final VariableRegistry variableRegistry;
  private final Scope scope;
  private final ExecutionTreeNode executionTree;
  private final Map<String, Object> outputContract;
  private final Map<String, String> resultMapping;
  private final String executionType;

  public PipelineDefinition(
      String name,
      Map<String, Object> inputContract,
      VariableRegistry variableRegistry,
      Scope scope,
      ExecutionTreeNode executionTree,
      Map<String, Object> outputContract,
      Map<String, String> resultMapping,
      String executionType) {
    this.name = name;
    this.inputContract = inputContract == null ? Map.of() : Map.copyOf(inputContract);
    this.variableRegistry = variableRegistry;
    this.scope = scope;
    this.executionTree = executionTree;
    this.outputContract = outputContract == null ? Map.of() : Map.copyOf(outputContract);
    this.resultMapping = resultMapping == null ? Map.of() : Map.copyOf(resultMapping);
    this.executionType = executionType != null ? executionType : "SYNC";
  }

  public String getName() { return name; }
  public Map<String, Object> getInputContract() { return inputContract; }
  public VariableRegistry getVariableRegistry() { return variableRegistry; }
  public Scope getScope() { return scope; }
  public ExecutionTreeNode getExecutionTree() { return executionTree; }
  public Map<String, Object> getOutputContract() { return outputContract; }
  public Map<String, String> getResultMapping() { return resultMapping; }
  public String getExecutionType() { return executionType; }
}
