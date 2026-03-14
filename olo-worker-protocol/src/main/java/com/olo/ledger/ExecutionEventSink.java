package com.olo.ledger;

/**
 * Sink for execution events (e.g. chat UI). Contract in protocol.
 */
@FunctionalInterface
public interface ExecutionEventSink {

    void emit(String runId, ExecutionEvent event);

    /** No-op implementation. */
    static ExecutionEventSink noOp() {
        return (runId, event) -> {};
    }
}
