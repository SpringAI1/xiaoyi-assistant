package com.enterprise.knowledge.infrastructure.agent.skill;

import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.List;

@Component
public class SkillManager {

    private final SkillRegistry skillRegistry;
    private final List<Skill> skills;

    public SkillManager(SkillRegistry skillRegistry, List<Skill> skills) {
        this.skillRegistry = skillRegistry;
        this.skills = skills;
    }

    @PostConstruct
    public void init() {
        for (Skill skill : skills) {
            skillRegistry.registerSkill(skill);
        }
        System.out.println("✅ 技能系统初始化完成，已注册 " + skills.size() + " 个技能");
    }

    public String executeBestSkill(String query) {
        List<Skill> recommendedSkills = skillRegistry.getRecommendSkills(query);
        
        if (recommendedSkills.isEmpty()) {
            return null;
        }
        
        for (Skill skill : recommendedSkills) {
            try {
                String result = skill.execute(query);
                if (result != null) {
                    skillRegistry.recordUsage(skill.getId());
                    return result;
                }
            } catch (Exception e) {
                continue;
            }
        }
        
        return null;
    }

    public List<Skill> getAvailableSkills() {
        return skillRegistry.getAllSkills();
    }

    public String getSkillsInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("🎯 可用技能列表\n\n");
        
        for (Skill skill : skillRegistry.getAllSkills()) {
            sb.append("• ").append(skill.getName());
            sb.append(" - ").append(skill.getDescription());
            sb.append("\n");
        }
        
        sb.append("\n💡 使用提示：直接说出您的需求，我会自动选择合适的技能");
        return sb.toString();
    }

    public void learnFromFeedback(String context, String feedback) {
        skillRegistry.learnFromInteraction(context, feedback);
    }
}
