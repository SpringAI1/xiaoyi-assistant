package com.enterprise.knowledge.infrastructure.agent;

import com.enterprise.knowledge.infrastructure.agent.tool.BuiltInTools;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class ToolExecutor {

    private final BuiltInTools builtInTools;

    public String execute(String toolName, Map<String, String> parameters) {
        return switch (toolName.toLowerCase()) {
            case "calculator" -> builtInTools.calculate(parameters.get("expression"));
            case "weather" -> builtInTools.getWeather(parameters.get("city"));
            case "datetime" -> builtInTools.getCurrentDateTime();
            case "company_policy" -> builtInTools.getCompanyPolicy(parameters.get("topic"));
            default -> "未知工具: " + toolName;
        };
    }

    public String getAvailableTools() {
        return builtInTools.getAvailableTools();
    }
}