package com.enterprise.knowledge.infrastructure.agent.model;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ModelGateway {

    private final Map<String, String> modelConfigs = new HashMap<>();
    private String currentModel = "qianfan";

    public ModelGateway() {
        initializeModels();
    }

    private void initializeModels() {
        modelConfigs.put("qianfan", "通义千问 (Qianfan) - 当前使用");
        modelConfigs.put("openai", "OpenAI GPT-4 (需配置API Key)");
        modelConfigs.put("ollama", "本地 Ollama (需本地部署)");
    }

    public ChatLanguageModel getModel() {
        return null;
    }

    public ChatLanguageModel getModel(String modelName) {
        return null;
    }

    public void switchModel(String modelName) {
        if (modelConfigs.containsKey(modelName)) {
            this.currentModel = modelName;
            System.out.println("✅ 模型已切换至: " + modelName);
        } else {
            System.out.println("⚠️ 不支持的模型: " + modelName + "，使用默认模型");
        }
    }

    public Map<String, String> getAvailableModels() {
        return new HashMap<>(modelConfigs);
    }

    public String getCurrentModel() {
        return currentModel;
    }
}
