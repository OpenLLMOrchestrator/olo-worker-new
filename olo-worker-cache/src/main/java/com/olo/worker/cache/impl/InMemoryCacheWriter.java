package com.olo.worker.cache.impl;

import com.olo.input.producer.CacheWriter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of CacheWriter. Useful for local/dev when Redis is not required.
 */
public final class InMemoryCacheWriter implements CacheWriter {

    private final Map<String, String> store = new ConcurrentHashMap<>();

    @Override
    public void put(String key, String value) {
        store.put(key, value);
    }

    public String get(String key) {
        return store.get(key);
    }
}
