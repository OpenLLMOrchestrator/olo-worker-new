package com.olo.config.impl;

import com.olo.config.OloSessionCache;
import com.olo.input.model.WorkflowInput;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** In-memory session cache stub. Use for development; replace with Redis-backed impl in production. */
public final class InMemorySessionCache implements OloSessionCache {

    private final ConcurrentHashMap<String, AtomicInteger> activeCountByTenant = new ConcurrentHashMap<>();

    @Override
    public void cacheUpdate(WorkflowInput input) {
        // no-op; real impl would store in Redis
    }

    @Override
    public void incrActiveWorkflows(String tenantId) {
        activeCountByTenant.computeIfAbsent(tenantId == null ? "default" : tenantId, k -> new AtomicInteger(0)).incrementAndGet();
    }

    @Override
    public void decrActiveWorkflows(String tenantId) {
        AtomicInteger c = activeCountByTenant.get(tenantId == null ? "default" : tenantId);
        if (c != null) c.decrementAndGet();
    }
}
