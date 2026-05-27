package com.enterprise.knowledge.infrastructure.agent;

import com.enterprise.knowledge.domain.ChatMessage;
import com.enterprise.knowledge.infrastructure.agent.skill.Skill;
import com.enterprise.knowledge.infrastructure.agent.skill.SkillRegistry;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

@Component
public class EnhancedAgentOrchestrator {

    private final SkillRegistry skillRegistry;
    
    private static final List<String> MUSIC_KEYWORDS = Arrays.asList(
        "周杰伦", "歌词", "歌曲", "音乐", "歌手", "专辑", "单曲",
        "演唱会", "作曲", "作词", "歌名", "华语", "流行歌", "经典歌曲",
        "晴天的歌词", "晴天歌词", "晴天的歌", "什么歌", "哪首歌"
    );

    public EnhancedAgentOrchestrator(SkillRegistry skillRegistry) {
        this.skillRegistry = skillRegistry;
    }

    public OrchestratorDecision decideAction(String query, String sessionId) {
        OrchestratorDecision decision = new OrchestratorDecision();
        String lowerQuery = query.toLowerCase();
        
        // 1. 首先检查是否为音乐相关查询
        boolean isMusicQuery = isMusicQuery(query);
        if (isMusicQuery) {
            // 音乐相关查询，直接使用RAG或普通对话，绝对不使用天气技能
            decision.setActionType(ActionType.USE_RAG);
            decision.setConfidence(0.95);
            decision.setExplanation("识别为音乐相关查询，使用RAG引擎");
            return decision;
        }
        
        // 2. 检查是否为明确的天气查询
        boolean isWeatherQuery = isWeatherQuery(query);
        if (isWeatherQuery) {
            Skill weatherSkill = findSkillById("weather-skill");
            if (weatherSkill != null) {
                decision.setActionType(ActionType.USE_SKILL);
                decision.setTargetSkill(weatherSkill);
                decision.setConfidence(0.9);
                decision.setExplanation("识别为天气查询，使用天气技能");
                return decision;
            }
        }
        
        // 3. 检查其他技能
        List<Skill> recommendedSkills = skillRegistry.getRecommendSkills(query);
        if (!recommendedSkills.isEmpty()) {
            Skill bestSkill = recommendedSkills.get(0);
            decision.setActionType(ActionType.USE_SKILL);
            decision.setTargetSkill(bestSkill);
            decision.setConfidence(0.85);
            return decision;
        }
        
        // 4. 检查是否为事实查询
        if (isFactQuery(query)) {
            decision.setActionType(ActionType.USE_RAG);
            decision.setConfidence(0.8);
            return decision;
        }
        
        // 5. 默认普通对话
        decision.setActionType(ActionType.CHAT);
        decision.setConfidence(0.7);
        return decision;
    }

    public void updateContext(String sessionId, String query, String response, Skill usedSkill) {
        // 上下文管理可以在这里实现
    }

    private boolean isMusicQuery(String query) {
        String lowerQuery = query.toLowerCase();
        for (String keyword : MUSIC_KEYWORDS) {
            if (lowerQuery.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private boolean isWeatherQuery(String query) {
        String lowerQuery = query.toLowerCase();
        List<String> weatherKeywords = Arrays.asList(
            "天气", "温度", "预报", "空气质量", "气象", "今天天气",
            "明天天气", "天气预报", "多少度", "下雨吗", "有风吗",
            "晴天还是下雨", "天气怎么样", "气温"
        );
        
        for (String keyword : weatherKeywords) {
            if (lowerQuery.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean isFactQuery(String query) {
        String lowerQuery = query.toLowerCase();
        return lowerQuery.contains("什么") || 
               lowerQuery.contains("如何") || 
               lowerQuery.contains("为什么") ||
               lowerQuery.contains("介绍") ||
               lowerQuery.contains("解释");
    }

    private Skill findSkillById(String skillId) {
        for (Skill skill : skillRegistry.getAllSkills()) {
            if (skill.getId().equals(skillId)) {
                return skill;
            }
        }
        return null;
    }

    public static class OrchestratorDecision {
        private ActionType actionType;
        private Skill targetSkill;
        private double confidence;
        private String explanation;

        public ActionType getActionType() { return actionType; }
        public void setActionType(ActionType type) { this.actionType = type; }

        public Skill getTargetSkill() { return targetSkill; }
        public void setTargetSkill(Skill skill) { this.targetSkill = skill; }

        public double getConfidence() { return confidence; }
        public void setConfidence(double conf) { this.confidence = conf; }

        public String getExplanation() { return explanation; }
        public void setExplanation(String explanation) { this.explanation = explanation; }
    }

    public enum ActionType {
        USE_SKILL,      
        USE_RAG,        
        CHAT,           
        CLARIFY,        
        CONTINUE_SKILL  
    }
}
