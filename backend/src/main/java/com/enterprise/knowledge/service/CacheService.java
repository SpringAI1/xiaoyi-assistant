package com.enterprise.knowledge.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CacheService {

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public void put(String key, Object value, long ttlSeconds) {
        cache.put(key, new CacheEntry(value, System.currentTimeMillis() + ttlSeconds * 1000));
    }

    public Object get(String key) {
        CacheEntry entry = cache.get(key);
        if (entry != null && entry.expiryTime > System.currentTimeMillis()) {
            return entry.value;
        } else if (entry != null) {
            cache.remove(key);
        }
        return null;
    }

    public void remove(String key) {
        cache.remove(key);
    }

    public void clear() {
        cache.clear();
    }

    public long size() {
        return cache.size();
    }

    private static class CacheEntry {
        Object value;
        long expiryTime;

        CacheEntry(Object value, long expiryTime) {
            this.value = value;
            this.expiryTime = expiryTime;
        }
    }
}