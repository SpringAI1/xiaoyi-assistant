package com.enterprise.knowledge.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    @Value("${knowledge.rate-limit.max-requests:100}")
    private int maxRequests;

    @Value("${knowledge.rate-limit.time-window-seconds:60}")
    private int timeWindowSeconds;

    private final Map<String, RateLimitEntry> rateLimits = new ConcurrentHashMap<>();

    public boolean checkAndIncrement(String clientId) {
        long now = System.currentTimeMillis();
        long windowStart = now - (timeWindowSeconds * 1000);

        RateLimitEntry entry = rateLimits.computeIfAbsent(clientId, k -> new RateLimitEntry());

        synchronized (entry) {
            if (entry.windowStart < windowStart) {
                entry.count = 0;
                entry.windowStart = windowStart;
            }

            if (entry.count >= maxRequests) {
                return false;
            }

            entry.count++;
            return true;
        }
    }

    public int getRemaining(String clientId) {
        long now = System.currentTimeMillis();
        long windowStart = now - (timeWindowSeconds * 1000);

        RateLimitEntry entry = rateLimits.get(clientId);
        if (entry == null || entry.windowStart < windowStart) {
            return maxRequests;
        }
        return maxRequests - entry.count;
    }

    private static class RateLimitEntry {
        int count = 0;
        long windowStart = 0;
    }
}