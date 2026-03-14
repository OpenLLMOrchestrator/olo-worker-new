package com.olo.executiontree;

import com.olo.executiontree.config.ExecutionType;
import com.olo.executiontree.inputcontract.InputContract;
import com.olo.executiontree.inputcontract.InputContractImpl;
import com.olo.executiontree.outputcontract.ResultMapping;
import com.olo.executiontree.outputcontract.ResultMappingImpl;
import com.olo.executiontree.tree.CompilerNodeAdapter;
import com.olo.executiontree.variableregistry.VariableRegistryEntry;
import com.olo.executiontree.variableregistry.VariableRegistryEntryAdapter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Pipeline definition: name, input/output contract, variable registry, scope,
 * and the execution tree root. Immutable once built; used to produce ExecutionConfigSnapshot.
 * Implements {@link com.olo.executiontree.config.PipelineDefinition} for worker use.
 *
 * @see ExecutionTreeNode
 * @see VariableRegistry
 * @see Scope
 */
public final class PipelineDefinition implements com.olo.executiontree.config.PipelineDefinition {
  private final String name;
  private final Map<String, Object> inputContract;
  private final VariableRegistry variableRegistry;
  private final Scope scope;
  private final ExecutionTreeNode executionTree;
  private final Map<String, Object> outputContract;
  private final Map<String, String> resultMapping;
  private final String executionType;
  private final boolean isDebugPipeline;
  private final boolean isDynamicPipeline;

  public PipelineDefinition(
      String name,
      Map<String, Object> inputContract,
      VariableRegistry variableRegistry,
      Scope scope,
      ExecutionTreeNode executionTree,
      Map<String, Object> outputContract,
      Map<String, String> resultMapping,
      String executionType,
      boolean isDebugPipeline,
      boolean isDynamicPipeline) {
    this.name = name;
    this.inputContract = inputContract == null ? Map.of() : Map.copyOf(inputContract);
    this.variableRegistry = variableRegistry;
    this.scope = scope;
    this.executionTree = executionTree;
    this.outputContract = outputContract == null ? Map.of() : Map.copyOf(outputContract);
    this.resultMapping = resultMapping == null ? Map.of() : Map.copyOf(resultMapping);
    this.executionType = executionType != null ? executionType : "SYNC";
    this.isDebugPipeline = isDebugPipeline;
    this.isDynamicPipeline = isDynamicPipeline;
  }

  public String getName() { return name; }
  @Override
  public InputContract getInputContract() { return new InputContractImpl(inputContract, false); }
  public Map<String, Object> getInputContractMap() { return inputContract; }
  @Override
  public List<VariableRegistryEntry> getVariableRegistry() {
    return variableRegistry != null && variableRegistry.getDeclarations() != null
        ? variableRegistry.getDeclarations().stream().map(VariableRegistryEntryAdapter::new).collect(Collectors.toList())
        : List.of();
  }
  public VariableRegistry getVariableRegistryRaw() { return variableRegistry; }
  public Scope getScope() { return scope; }
  /** Returns the execution tree as protocol/worker type. */
  @Override
  public com.olo.executiontree.tree.ExecutionTreeNode getExecutionTree() {
    return executionTree != null ? new CompilerNodeAdapter(executionTree) : null;
  }
  /** Returns the raw compiler execution tree (for serialization). */
  public ExecutionTreeNode getExecutionTreeRoot() {
    return executionTree;
  }
  public Map<String, Object> getOutputContract() { return outputContract; }
  @Override
  public List<ResultMapping> getResultMapping() {
    return resultMapping != null
        ? resultMapping.entrySet().stream().map(e -> new ResultMappingImpl(e.getKey())).collect(Collectors.toList())
        : List.of();
  }
  public Map<String, String> getResultMappingMap() { return resultMapping; }
  @Override
  public ExecutionType getExecutionType() {
    try {
      return ExecutionType.valueOf(executionType != null ? executionType.toUpperCase() : "SYNC");
    } catch (Exception e) {
      return ExecutionType.SYNC;
    }
  }

  /** Whether this pipeline is marked as a debug pipeline in its config. */
  public boolean isDebugPipeline() {
    return isDebugPipeline;
  }

  /** Whether this pipeline is marked as a dynamic pipeline in its config. */
  public boolean isDynamicPipeline() {
    return isDynamicPipeline;
  }
}
