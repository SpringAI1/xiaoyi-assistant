package com.enterprise.knowledge.api.rest;

import com.enterprise.knowledge.infrastructure.agent.model.ModelGateway;
import com.enterprise.knowledge.infrastructure.observability.ConversationTracer;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
public class SystemAdminController {

    private final ModelGateway modelGateway;
    private final ConversationTracer conversationTracer;

    public SystemAdminController(ModelGateway modelGateway, ConversationTracer conversationTracer) {
        this.modelGateway = modelGateway;
        this.conversationTracer = conversationTracer;
    }

    @GetMapping("/models")
    public Map<String, Object> getModels() {
        return Map.of(
                "currentModel", modelGateway.getCurrentModel(),
                "availableModels", modelGateway.getAvailableModels()
        );
    }

    @PostMapping("/models/switch")
    public Map<String, Object> switchModel(@RequestParam String modelName) {
        modelGateway.switchModel(modelName);
        return Map.of(
                "success", true,
                "message", "模型已切换至: " + modelName,
                "currentModel", modelGateway.getCurrentModel()
        );
    }

    @GetMapping("/trace/{sessionId}")
    public String getTraceReport(@PathVariable String sessionId) {
        return conversationTracer.generateTraceReport(sessionId);
    }

    @GetMapping("/statistics")
    public Map<String, Object> getStatistics() {
        return conversationTracer.getStatistics();
    }

    @DeleteMapping("/trace/{sessionId}")
    public Map<String, Object> clearSession(@PathVariable String sessionId) {
        conversationTracer.clearSession(sessionId);
        return Map.of(
                "success", true,
                "message", "会话追踪记录已清除: " + sessionId
        );
    }
}
