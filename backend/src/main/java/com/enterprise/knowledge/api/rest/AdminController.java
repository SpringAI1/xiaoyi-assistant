package com.enterprise.knowledge.api.rest;

import com.enterprise.knowledge.infrastructure.monitor.SystemMonitor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 系统监控API
 * 提供系统健康检查、性能统计等
 */
@RestController
@RequestMapping("/api/v1/system")
public class AdminController {

    private final SystemMonitor systemMonitor;

    public AdminController(SystemMonitor systemMonitor) {
        this.systemMonitor = systemMonitor;
    }

    /**
     * 系统健康检查
     */
    @GetMapping("/health")
    public Map<String, Object> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());
        health.put("service", "小易助手");
        
        return health;
    }

    /**
     * 获取系统概览
     */
    @GetMapping("/overview")
    public Map<String, Object> getSystemOverview() {
        SystemMonitor.SystemOverview overview = systemMonitor.getSystemOverview();
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", Map.of(
            "totalRequests", overview.getTotalRequests(),
            "totalErrors", overview.getTotalErrors(),
            "errorRate", String.format("%.2f%%", overview.getErrorRate()),
            "avgResponseTime", overview.getAvgResponseTime() + "ms",
            "uptimeSeconds", overview.getUptimeSeconds(),
            "activeApis", overview.getActiveApis()
        ));
        
        return response;
    }

    /**
     * 获取所有API统计
     */
    @GetMapping("/api-stats")
    public Map<String, Object> getApiStats() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", systemMonitor.getAllApiStats());
        
        return response;
    }

    /**
     * 获取特定API详情
     */
    @GetMapping("/api-stats/{endpoint}")
    public Map<String, Object> getApiDetails(@PathVariable String endpoint) {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> details = systemMonitor.getApiDetails(endpoint);
        
        if (details.isEmpty()) {
            response.put("status", "error");
            response.put("message", "API not found");
        } else {
            response.put("status", "success");
            response.put("data", details);
        }
        
        return response;
    }

    /**
     * 重置统计数据
     */
    @PostMapping("/reset-stats")
    public Map<String, Object> resetStats() {
        systemMonitor.resetStats();
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "统计数据已重置");
        
        return response;
    }

    /**
     * 获取系统信息
     */
    @GetMapping("/system-info")
    public Map<String, Object> getSystemInfo() {
        Map<String, Object> info = new HashMap<>();
        
        info.put("status", "success");
        info.put("data", Map.of(
            "javaVersion", System.getProperty("java.version"),
            "os", System.getProperty("os.name"),
            "osVersion", System.getProperty("os.version"),
            "availableProcessors", Runtime.getRuntime().availableProcessors(),
            "totalMemory", Runtime.getRuntime().totalMemory(),
            "freeMemory", Runtime.getRuntime().freeMemory()
        ));
        
        return info;
    }
}
