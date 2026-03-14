package com.olo.planner;

/** Resolves prompt templates by key. Contract in protocol; implementations in planner or worker modules. */
public interface PromptTemplateProvider {

    String getTemplate(String key);

    /** Default no-op implementation that returns empty string. */
    static PromptTemplateProvider getDefault() {
        return key -> "";
    }
}
