package com.enterprise.knowledge.api.rest;

import com.enterprise.knowledge.infrastructure.selfcheck.SelfTestModule;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/self-test")
public class SelfTestController {

    private final SelfTestModule selfTestModule;

    public SelfTestController(SelfTestModule selfTestModule) {
        this.selfTestModule = selfTestModule;
    }

    @GetMapping("/run")
    public Map<String, Object> runSelfTest() {
        SelfTestModule.SystemTestReport report = selfTestModule.runFullSystemTest();
        
        Map<String, Object> result = new HashMap<>();
        result.put("testTime", report.testTime);
        result.put("systemHealth", report.systemHealth);
        result.put("totalScore", report.totalScore);
        result.put("passedCount", report.passedCount);
        result.put("failedCount", report.failedCount);
        result.put("totalTests", report.testCases.size());
        
        Map<String, String> testDetails = new HashMap<>();
        for (SelfTestModule.TestCase tc : report.testCases) {
            testDetails.put(tc.name, tc.status + " - " + tc.message);
        }
        result.put("testDetails", testDetails);
        
        return result;
    }

    @GetMapping("/status")
    public Map<String, Object> getQuickStatus() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", selfTestModule.getQuickStatus());
        result.put("timestamp", java.time.LocalDateTime.now().toString());
        return result;
    }

    @GetMapping("/report")
    public Map<String, Object> getDetailedReport() {
        SelfTestModule.SystemTestReport report = selfTestModule.runFullSystemTest();
        
        Map<String, Object> result = new HashMap<>();
        result.put("testTime", report.testTime);
        result.put("systemHealth", report.systemHealth);
        result.put("totalScore", report.totalScore);
        
        return result;
    }
}
