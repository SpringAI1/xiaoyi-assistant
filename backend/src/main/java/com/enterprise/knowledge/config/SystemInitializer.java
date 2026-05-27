package com.enterprise.knowledge.config;

import com.enterprise.knowledge.infrastructure.knowledgegraph.KnowledgeGraph;
import com.enterprise.knowledge.infrastructure.monitoring.HealthCheckService;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class SystemInitializer {
    
    private final HealthCheckService healthCheckService;
    private final KnowledgeGraph knowledgeGraph;
    
    public SystemInitializer(HealthCheckService healthCheckService, KnowledgeGraph knowledgeGraph) {
        this.healthCheckService = healthCheckService;
        this.knowledgeGraph = knowledgeGraph;
    }
    
    @PostConstruct
    public void init() {
        System.out.println("🚀 正在初始化小易助手系统...");
        
        healthCheckService.registerComponent("SkillManager", "CORE");
        healthCheckService.registerComponent("KnowledgeGraph", "CORE");
        healthCheckService.registerComponent("ModelGateway", "CORE");
        healthCheckService.registerComponent("DocumentParser", "FILE");
        healthCheckService.registerComponent("ChatService", "SERVICE");
        
        knowledgeGraph.initializeDefaultKnowledge();
        knowledgeGraph.addEntity("小易助手", "product", "智能助手系统，企业级AI助手");
        knowledgeGraph.addEntity("企业知识", "domain", "企业内部知识库");
        
        System.out.println("✅ 小易助手系统初始化完成！");
    }
}
