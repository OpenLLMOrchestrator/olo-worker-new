package com.olo.executiontree.scope;

/** Simple implementation of {@link FeatureDef}. */
public record SimpleFeatureDef(String id) implements FeatureDef {
    @Override
    public String getId() { return id != null ? id : ""; }
}
