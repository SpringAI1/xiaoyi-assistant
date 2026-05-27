package com.enterprise.knowledge.api.rest;

import com.enterprise.knowledge.infrastructure.monitoring.HealthCheckService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/health")
public class HealthController {
    
    private final HealthCheckService healthCheckService;
    
    public HealthController(HealthCheckService healthCheckService) {
        this.healthCheckService = healthCheckService;
    }
    
    @GetMapping
    public Map<String, Object> getHealth() {
        return healthCheckService.getFullHealthReport();
    }
    
    @GetMapping("/status")
    public Map<String, String> getStatus() {
        return Map.of(
            "status", "UP",
            "timestamp", String.valueOf(System.currentTimeMillis())
        );
    }
}
