package com.olo.executiontree;

import java.util.Objects;

public final class VariableDeclaration {
  private final String name;
  private final String type;
  private final VariableScope scope;

  public VariableDeclaration(String name, String type, VariableScope scope) {
    this.name = Objects.requireNonNull(name, "name");
    this.type = type != null ? type : "string";
    this.scope = scope != null ? scope : VariableScope.INTERNAL;
  }

  public String getName() { return name; }
  public String getType() { return type; }
  public VariableScope getScope() { return scope; }
}
