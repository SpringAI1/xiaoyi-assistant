package com.enterprise.knowledge.infrastructure.monitoring;

import com.enterprise.knowledge.service.ConversationMemoryService;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class HealthCheckService {

    private final Map<String, ComponentHealth> components = new ConcurrentHashMap<>();
    private final Map<String, Long> requestTimings = new ConcurrentHashMap<>();
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong errorRequests = new AtomicLong(0);
    private final long startTime = System.currentTimeMillis();

    private final ConversationMemoryService memoryService;

    public HealthCheckService(ConversationMemoryService memoryService) {
        this.memoryService = memoryService;
        initializeComponents();
    }

    private void initializeComponents() {
        registerComponent("chat-model", "AI_MODEL");
        registerComponent("skill-system", "SKILL");
        registerComponent("knowledge-graph", "KNOWLEDGE");
        registerComponent("document-processor", "DOCUMENT");
        registerComponent("conversation-memory", "MEMORY");
    }

    public void registerComponent(String name, String type) {
        ComponentHealth health = new ComponentHealth();
        health.name = name;
        health.type = type;
        health.status = "HEALTHY";
        health.lastCheckTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        health.uptime = 0;
        components.put(name, health);
    }

    public void recordRequest(String endpoint, long durationMs) {
        requestTimings.merge(endpoint, durationMs, (old, newVal) -> (old + newVal) / 2);
        totalRequests.incrementAndGet();
    }

    public void recordError(String endpoint) {
        errorRequests.incrementAndGet();
    }

    public void updateComponentHealth(String name, String status) {
        ComponentHealth health = components.get(name);
        if (health != null) {
            health.status = status;
            health.lastCheckTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
    }

    public Map<String, Object> getFullHealthReport() {
        Map<String, Object> report = new HashMap<>();

        report.put("systemStatus", calculateSystemStatus());
        report.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        report.put("uptime", System.currentTimeMillis() - startTime);

        updateRealTimeMetrics();

        Map<String, Object> componentsReport = new HashMap<>();
        for (Map.Entry<String, ComponentHealth> entry : components.entrySet()) {
            Map<String, Object> comp = new HashMap<>();
            comp.put("status", entry.getValue().status);
            comp.put("type", entry.getValue().type);
            comp.put("lastCheck", entry.getValue().lastCheckTime);
            comp.put("uptime", entry.getValue().uptime);
            componentsReport.put(entry.getKey(), comp);
        }
        report.put("components", componentsReport);

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalRequests", totalRequests.get());
        metrics.put("errorRequests", errorRequests.get());
        metrics.put("errorRate", totalRequests.get() > 0 ? (errorRequests.get() * 100.0 / totalRequests.get()) : 0);
        metrics.put("avgResponseTime", calculateAvgResponseTime());
        metrics.put("activeSessions", memoryService.getSessionCount());

        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        metrics.put("memoryUsed", heapUsage.getUsed());
        metrics.put("memoryMax", heapUsage.getMax());
        metrics.put("memoryUsedPercent", heapUsage.getMax() > 0 ? (heapUsage.getUsed() * 100.0 / heapUsage.getMax()) : 0);

        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        metrics.put("threadCount", threadBean.getThreadCount());
        metrics.put("peakThreadCount", threadBean.getPeakThreadCount());

        report.put("metrics", metrics);

        Map<String, Object> systemInfo = new HashMap<>();
        systemInfo.put("javaVersion", System.getProperty("java.version"));
        systemInfo.put("osName", System.getProperty("os.name"));
        systemInfo.put("osArch", System.getProperty("os.arch"));
        systemInfo.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        report.put("systemInfo", systemInfo);

        return report;
    }

    private void updateRealTimeMetrics() {
        ComponentHealth memoryComp = components.get("conversation-memory");
        if (memoryComp != null) {
            memoryComp.uptime = memoryService.getSessionCount();
            memoryComp.lastCheckTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
    }

    private String calculateSystemStatus() {
        long unhealthy = components.values().stream()
            .filter(h -> "UNHEALTHY".equals(h.status))
            .count();

        if (unhealthy > 0) {
            return "RED";
        }

        long warning = components.values().stream()
            .filter(h -> "WARNING".equals(h.status))
            .count();

        if (warning > 0) {
            return "YELLOW";
        }

        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        double memoryPercent = heapUsage.getMax() > 0 ? (heapUsage.getUsed() * 100.0 / heapUsage.getMax()) : 0;
        if (memoryPercent > 90) {
            return "YELLOW";
        }

        long errorRate = totalRequests.get() > 0 ? (errorRequests.get() * 100 / totalRequests.get()) : 0;
        if (errorRate > 10) {
            return "YELLOW";
        }

        return "GREEN";
    }

    private double calculateAvgResponseTime() {
        if (requestTimings.isEmpty()) return 0;
        return requestTimings.values().stream().mapToLong(Long::longValue).average().orElse(0);
    }

    public String getHealthStatusText() {
        Map<String, Object> report = getFullHealthReport();
        String status = (String) report.get("systemStatus");

        StringBuilder sb = new StringBuilder();
        sb.append("🩺 系统健康检查\n\n");

        switch (status) {
            case "GREEN":
                sb.append("🟢 系统状态：健康\n\n");
                break;
            case "YELLOW":
                sb.append("🟡 系统状态：需要注意\n\n");
                break;
            case "RED":
                sb.append("🔴 系统状态：异常\n\n");
                break;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> metrics = (Map<String, Object>) report.get("metrics");
        sb.append("📊 系统指标\n");
        sb.append("• 总请求数：").append(metrics.get("totalRequests")).append("\n");
        sb.append("• 错误请求：").append(metrics.get("errorRequests")).append("\n");
        sb.append("• 错误率：").append(String.format("%.2f", metrics.get("errorRate"))).append("%\n");
        sb.append("• 平均响应：").append(String.format("%.0f", metrics.get("avgResponseTime"))).append("ms\n");
        sb.append("• 活跃会话：").append(metrics.get("activeSessions")).append("\n\n");

        sb.append("💻 系统信息\n");
        @SuppressWarnings("unchecked")
        Map<String, Object> systemInfo = (Map<String, Object>) report.get("systemInfo");
        sb.append("• Java版本：").append(systemInfo.get("javaVersion")).append("\n");
        sb.append("• 操作系统：").append(systemInfo.get("osName")).append("\n");
        sb.append("• 可用处理器：").append(systemInfo.get("availableProcessors")).append("\n");
        sb.append("• 线程数：").append(metrics.get("threadCount")).append("\n\n");

        sb.append("🧠 内存状态\n");
        sb.append("• 使用内存：").append(formatBytes((Long) metrics.get("memoryUsed"))).append("\n");
        sb.append("• 最大内存：").append(formatBytes((Long) metrics.get("memoryMax"))).append("\n");
        sb.append("• 使用率：").append(String.format("%.1f", metrics.get("memoryUsedPercent"))).append("%\n");

        return sb.toString();
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    public static class ComponentHealth {
        public String name;
        public String type;
        public String status;
        public String lastCheckTime;
        public long uptime;
    }
}
