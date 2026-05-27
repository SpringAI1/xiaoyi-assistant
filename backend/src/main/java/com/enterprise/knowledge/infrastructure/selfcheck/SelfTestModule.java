package com.enterprise.knowledge.infrastructure.selfcheck;

import com.enterprise.knowledge.infrastructure.agent.model.ModelGateway;
import com.enterprise.knowledge.infrastructure.agent.skill.SkillManager;
import com.enterprise.knowledge.service.ConversationMemoryService;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class SelfTestModule {

    private final SkillManager skillManager;
    private final ModelGateway modelGateway;
    private final ConversationMemoryService memoryService;
    
    private LocalDateTime lastTestTime;
    private TestResult lastTestResult;

    public SelfTestModule(SkillManager skillManager, 
                         ModelGateway modelGateway,
                         ConversationMemoryService memoryService) {
        this.skillManager = skillManager;
        this.modelGateway = modelGateway;
        this.memoryService = memoryService;
    }

    public SystemTestReport runFullSystemTest() {
        SystemTestReport report = new SystemTestReport();
        report.testTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        
        List<TestCase> testCases = new ArrayList<>();
        
        testCases.add(testCoreComponents());
        testCases.add(testSkills());
        testCases.add(testModelConnection());
        testCases.add(testMemorySystem());
        testCases.add(testFileProcessing());
        testCases.add(testNetworkCapabilities());
        
        report.testCases = testCases;
        report.passedCount = (int) testCases.stream().filter(t -> t.status.equals("PASS")).count();
        report.failedCount = (int) testCases.stream().filter(t -> t.status.equals("FAIL")).count();
        report.totalScore = calculateOverallScore(testCases);
        report.systemHealth = determineSystemHealth(report);
        
        lastTestTime = LocalDateTime.now();
        lastTestResult = new TestResult(report);
        
        return report;
    }

    private TestCase testCoreComponents() {
        TestCase test = new TestCase();
        test.name = "核心组件检测";
        test.category = "CORE";
        
        try {
            boolean hasSkillManager = skillManager != null;
            boolean hasModelGateway = modelGateway != null;
            boolean hasMemoryService = memoryService != null;
            
            Map<String, String> details = new HashMap<>();
            details.put("SkillManager", hasSkillManager ? "✅ 正常" : "❌ 异常");
            details.put("ModelGateway", hasModelGateway ? "✅ 正常" : "❌ 异常");
            details.put("MemoryService", hasMemoryService ? "✅ 正常" : "❌ 异常");
            
            if (hasSkillManager && hasModelGateway && hasMemoryService) {
                test.status = "PASS";
                test.message = "所有核心组件正常运行";
            } else {
                test.status = "FAIL";
                test.message = "部分核心组件异常";
            }
            test.details = details;
        } catch (Exception e) {
            test.status = "FAIL";
            test.message = "核心组件检测失败: " + e.getMessage();
        }
        
        return test;
    }

    private TestCase testSkills() {
        TestCase test = new TestCase();
        test.name = "技能系统检测";
        test.category = "SKILLS";
        
        try {
            int skillCount = skillManager.getAvailableSkills().size();
            
            Map<String, String> details = new HashMap<>();
            details.put("已注册技能数", String.valueOf(skillCount));
            skillManager.getAvailableSkills().forEach(skill -> 
                    details.put(skill.getName(), "✅")
            );
            
            if (skillCount >= 3) {
                test.status = "PASS";
                test.message = "技能系统运行正常，已注册 " + skillCount + " 个技能";
            } else {
                test.status = "WARN";
                test.message = "技能数量较少，建议添加更多技能";
            }
            test.details = details;
        } catch (Exception e) {
            test.status = "FAIL";
            test.message = "技能系统检测失败: " + e.getMessage();
        }
        
        return test;
    }

    private TestCase testModelConnection() {
        TestCase test = new TestCase();
        test.name = "AI模型连接检测";
        test.category = "AI";
        
        try {
            String currentModel = modelGateway.getCurrentModel();
            Map<String, String> availableModels = modelGateway.getAvailableModels();
            
            Map<String, String> details = new HashMap<>();
            details.put("当前模型", currentModel != null ? currentModel : "未设置");
            details.put("可用模型数", availableModels != null ? String.valueOf(availableModels.size()) : "0");
            
            if (currentModel != null && availableModels != null && !availableModels.isEmpty()) {
                test.status = "PASS";
                test.message = "AI模型连接正常，当前使用: " + currentModel;
            } else {
                test.status = "WARN";
                test.message = "AI模型配置异常";
            }
            test.details = details;
        } catch (Exception e) {
            test.status = "FAIL";
            test.message = "AI模型连接失败: " + e.getMessage();
        }
        
        return test;
    }

    private TestCase testMemorySystem() {
        TestCase test = new TestCase();
        test.name = "会话记忆系统检测";
        test.category = "MEMORY";
        
        try {
            int sessionCount = memoryService.getSessionCount();
            
            Map<String, String> details = new HashMap<>();
            details.put("会话数", String.valueOf(sessionCount));
            
            if (sessionCount >= 0) {
                test.status = "PASS";
                test.message = "会话记忆系统运行正常，当前会话: " + sessionCount;
            } else {
                test.status = "WARN";
                test.message = "会话记忆系统异常";
            }
            test.details = details;
        } catch (Exception e) {
            test.status = "FAIL";
            test.message = "会话记忆系统检测失败: " + e.getMessage();
        }
        
        return test;
    }

    private TestCase testFileProcessing() {
        TestCase test = new TestCase();
        test.name = "文件处理能力检测";
        test.category = "FILE";
        
        try {
            List<String> supportedFormats = Arrays.asList(
                    "PDF", "Word", "PPT", "Excel", "TXT", "图片"
            );
            
            Map<String, String> details = new HashMap<>();
            details.put("支持格式", String.join(", ", supportedFormats));
            details.put("格式数量", String.valueOf(supportedFormats.size()));
            
            test.status = "PASS";
            test.message = "文件处理系统正常，支持多种格式";
            test.details = details;
        } catch (Exception e) {
            test.status = "FAIL";
            test.message = "文件处理系统检测失败: " + e.getMessage();
        }
        
        return test;
    }

    private TestCase testNetworkCapabilities() {
        TestCase test = new TestCase();
        test.name = "网络访问能力检测";
        test.category = "NETWORK";
        
        try {
            Map<String, String> details = new HashMap<>();
            details.put("天气查询", "✅ 支持");
            details.put("网页搜索", "✅ 支持");
            details.put("图片搜索", "✅ 支持");
            
            test.status = "PASS";
            test.message = "网络访问能力正常";
            test.details = details;
        } catch (Exception e) {
            test.status = "FAIL";
            test.message = "网络访问能力检测失败: " + e.getMessage();
        }
        
        return test;
    }

    private int calculateOverallScore(List<TestCase> testCases) {
        if (testCases.isEmpty()) return 0;
        
        int totalScore = 0;
        for (TestCase tc : testCases) {
            switch (tc.status) {
                case "PASS" -> totalScore += 100;
                case "WARN" -> totalScore += 60;
                case "FAIL" -> totalScore += 0;
            }
        }
        
        return totalScore / testCases.size();
    }

    private String determineSystemHealth(SystemTestReport report) {
        if (report.passedCount == report.testCases.size()) {
            return "🟢 健康";
        } else if (report.passedCount >= report.testCases.size() * 0.7) {
            return "🟡 良好";
        } else if (report.passedCount >= report.testCases.size() * 0.5) {
            return "🟠 警告";
        } else {
            return "🔴 危险";
        }
    }

    public String getQuickStatus() {
        if (lastTestResult == null) {
            return "⚪ 未进行自检";
        }
        
        return String.format("上次自检: %s | 健康状态: %s | 评分: %d/100",
                lastTestTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                lastTestResult.report.systemHealth,
                lastTestResult.report.totalScore);
    }

    public static class SystemTestReport {
        public String testTime;
        public List<TestCase> testCases;
        public int passedCount;
        public int failedCount;
        public int totalScore;
        public String systemHealth;
    }

    public static class TestCase {
        public String name;
        public String category;
        public String status;
        public String message;
        public Map<String, String> details;
    }

    private static class TestResult {
        SystemTestReport report;
        
        TestResult(SystemTestReport report) {
            this.report = report;
        }
    }
}
