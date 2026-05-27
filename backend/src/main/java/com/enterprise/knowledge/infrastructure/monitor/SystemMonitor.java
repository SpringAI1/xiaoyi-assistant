package com.enterprise.knowledge.infrastructure.monitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 企业级系统监控组件
 * 追踪API调用、性能指标、错误统计等
 */
@Component
public class SystemMonitor {

    private static final Logger logger = LoggerFactory.getLogger(SystemMonitor.class);

    // API调用统计
    private final Map<String, ApiMetrics> apiMetrics = new ConcurrentHashMap<>();
    
    // 性能追踪
    private final Map<String, List<Long>> performanceHistory = new ConcurrentHashMap<>();
    
    // 错误统计
    private final Map<String, AtomicInteger> errorCounts = new ConcurrentHashMap<>();
    
    // 全局计数器
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong totalErrors = new AtomicLong(0);
    private final AtomicLong startTime = new AtomicLong(System.currentTimeMillis());

    /**
     * 记录API调用
     */
    public void recordApiCall(String endpoint, long durationMs, boolean success) {
        totalRequests.incrementAndGet();
        
        // 更新API指标
        ApiMetrics metrics = apiMetrics.computeIfAbsent(endpoint, k -> new ApiMetrics());
        metrics.recordCall(durationMs, success);
        
        // 记录性能历史
        performanceHistory.computeIfAbsent(endpoint, k -> Collections.synchronizedList(new ArrayList<>()))
            .add(durationMs);
        
        // 限制历史记录大小
        List<Long> history = performanceHistory.get(endpoint);
        while (history.size() > 1000) {
            history.remove(0);
        }
        
        if (!success) {
            totalErrors.incrementAndGet();
            errorCounts.computeIfAbsent(endpoint, k -> new AtomicInteger(0)).incrementAndGet();
        }
        
        if (durationMs > 1000) {
            logger.warn("慢API调用: {} 耗时 {}ms", endpoint, durationMs);
        }
    }

    /**
     * 获取系统概览
     */
    public SystemOverview getSystemOverview() {
        SystemOverview overview = new SystemOverview();
        
        overview.setTotalRequests(totalRequests.get());
        overview.setTotalErrors(totalErrors.get());
        overview.setUptimeSeconds((System.currentTimeMillis() - startTime.get()) / 1000);
        
        // 计算错误率
        if (totalRequests.get() > 0) {
            double errorRate = (double) totalErrors.get() / totalRequests.get() * 100;
            overview.setErrorRate(errorRate);
        }
        
        // 平均响应时间
        long totalDuration = 0;
        int count = 0;
        for (List<Long> history : performanceHistory.values()) {
            for (Long duration : history) {
                totalDuration += duration;
                count++;
            }
        }
        if (count > 0) {
            overview.setAvgResponseTime(totalDuration / count);
        }
        
        overview.setActiveApis(apiMetrics.size());
        
        return overview;
    }

    /**
     * 获取API详情
     */
    public Map<String, Object> getApiDetails(String endpoint) {
        Map<String, Object> details = new HashMap<>();
        ApiMetrics metrics = apiMetrics.get(endpoint);
        
        if (metrics != null) {
            details.put("endpoint", endpoint);
            details.put("callCount", metrics.getCallCount());
            details.put("successCount", metrics.getSuccessCount());
            details.put("errorCount", metrics.getErrorCount());
            details.put("avgDuration", metrics.getAverageDuration());
            details.put("minDuration", metrics.getMinDuration());
            details.put("maxDuration", metrics.getMaxDuration());
            details.put("successRate", metrics.getSuccessRate());
        }
        
        return details;
    }

    /**
     * 获取所有API统计
     */
    public List<Map<String, Object>> getAllApiStats() {
        List<Map<String, Object>> stats = new ArrayList<>();
        
        for (String endpoint : apiMetrics.keySet()) {
            stats.add(getApiDetails(endpoint));
        }
        
        // 按调用次数排序
        stats.sort((a, b) -> Long.compare(
            (Long) b.getOrDefault("callCount", 0L),
            (Long) a.getOrDefault("callCount", 0L)
        ));
        
        return stats;
    }

    /**
     * 重置统计
     */
    public void resetStats() {
        apiMetrics.clear();
        performanceHistory.clear();
        errorCounts.clear();
        totalRequests.set(0);
        totalErrors.set(0);
        startTime.set(System.currentTimeMillis());
        logger.info("监控统计已重置");
    }

    // ==================== 内部类 ====================

    private static class ApiMetrics {
        private final AtomicLong callCount = new AtomicLong(0);
        private final AtomicLong successCount = new AtomicLong(0);
        private final AtomicLong errorCount = new AtomicLong(0);
        private final AtomicLong totalDuration = new AtomicLong(0);
        private final AtomicLong minDuration = new AtomicLong(Long.MAX_VALUE);
        private final AtomicLong maxDuration = new AtomicLong(0);

        public void recordCall(long duration, boolean success) {
            callCount.incrementAndGet();
            totalDuration.addAndGet(duration);
            
            if (success) {
                successCount.incrementAndGet();
            } else {
                errorCount.incrementAndGet();
            }
            
            // 更新最小/最大
            if (duration < minDuration.get()) {
                minDuration.set(duration);
            }
            if (duration > maxDuration.get()) {
                maxDuration.set(duration);
            }
        }

        public long getCallCount() { return callCount.get(); }
        public long getSuccessCount() { return successCount.get(); }
        public long getErrorCount() { return errorCount.get(); }
        
        public long getAverageDuration() {
            return callCount.get() > 0 ? totalDuration.get() / callCount.get() : 0;
        }
        
        public long getMinDuration() {
            return minDuration.get() == Long.MAX_VALUE ? 0 : minDuration.get();
        }
        
        public long getMaxDuration() { return maxDuration.get(); }
        
        public double getSuccessRate() {
            return callCount.get() > 0 ? (double) successCount.get() / callCount.get() * 100 : 100;
        }
    }

    public static class SystemOverview {
        private long totalRequests;
        private long totalErrors;
        private double errorRate;
        private long uptimeSeconds;
        private long avgResponseTime;
        private int activeApis;

        public long getTotalRequests() { return totalRequests; }
        public void setTotalRequests(long requests) { this.totalRequests = requests; }

        public long getTotalErrors() { return totalErrors; }
        public void setTotalErrors(long errors) { this.totalErrors = errors; }

        public double getErrorRate() { return errorRate; }
        public void setErrorRate(double rate) { this.errorRate = rate; }

        public long getUptimeSeconds() { return uptimeSeconds; }
        public void setUptimeSeconds(long uptime) { this.uptimeSeconds = uptime; }

        public long getAvgResponseTime() { return avgResponseTime; }
        public void setAvgResponseTime(long time) { this.avgResponseTime = time; }

        public int getActiveApis() { return activeApis; }
        public void setActiveApis(int count) { this.activeApis = count; }
    }
}
