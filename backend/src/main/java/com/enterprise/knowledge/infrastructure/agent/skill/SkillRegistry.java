package com.enterprise.knowledge.infrastructure.agent.skill;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SkillRegistry {

    private final Map<String, Skill> skills = new ConcurrentHashMap<>();
    private final Map<String, Integer> skillUsage = new ConcurrentHashMap<>();
    private final Map<String, Double> skillConfidence = new ConcurrentHashMap<>();

    public void registerSkill(Skill skill) {
        skills.put(skill.getId(), skill);
        skillUsage.put(skill.getId(), 0);
        skillConfidence.put(skill.getId(), skill instanceof LearningSkill ? 
            ((LearningSkill) skill).getConfidence() : 0.8);
    }

    public Skill getSkill(String skillId) {
        return skills.get(skillId);
    }

    public List<Skill> getAllSkills() {
        return new ArrayList<>(skills.values());
    }

    public List<Skill> getRecommendSkills(String context) {
        List<Skill> recommended = new ArrayList<>();
        String lowerContext = context.toLowerCase();
        
        // 先进行上下文分析，识别查询主题
        boolean isMusicQuery = isMusicQuery(lowerContext);
        boolean isWeatherQuery = isWeatherQuery(lowerContext);
        
        // 如果是音乐相关查询，我们绝对不推荐天气技能
        if (isMusicQuery) {
            // 对于音乐查询，只推荐搜索技能，如果匹配的话
            for (Skill skill : skills.values()) {
                if (!skill.isEnabled()) continue;
                
                if (skill.getId().contains("search") || 
                    skill.getName().contains("搜索")) {
                    for (String keyword : skill.getKeywords()) {
                        if (lowerContext.contains(keyword.toLowerCase())) {
                            recommended.add(skill);
                            break;
                        }
                    }
                }
                // 其他技能不推荐，特别是天气技能
            }
        } else {
            // 普通情况的处理逻辑
            for (Skill skill : skills.values()) {
                if (!skill.isEnabled()) continue;
                
                boolean shouldRecommend = false;
                
                // 对天气技能进行特殊处理
                if (skill.getId().equals("weather-skill")) {
                    if (isWeatherQuery) {
                        shouldRecommend = true;
                    }
                } else {
                    for (String keyword : skill.getKeywords()) {
                        if (lowerContext.contains(keyword.toLowerCase())) {
                            shouldRecommend = true;
                            break;
                        }
                    }
                }
                
                if (shouldRecommend) {
                    recommended.add(skill);
                }
            }
        }
        
        // 按优先级、使用频率、置信度综合排序
        recommended.sort((a, b) -> {
            int priorityCompare = Integer.compare(b.getPriority(), a.getPriority());
            if (priorityCompare != 0) return priorityCompare;
            
            int usageCompare = Integer.compare(
                skillUsage.getOrDefault(b.getId(), 0),
                skillUsage.getOrDefault(a.getId(), 0)
            );
            if (usageCompare != 0) return usageCompare;
            
            return Double.compare(
                skillConfidence.getOrDefault(b.getId(), 0.5),
                skillConfidence.getOrDefault(a.getId(), 0.5)
            );
        });
        
        return recommended;
    }
    
    // 判断是否是音乐相关查询
    private boolean isMusicQuery(String query) {
        List<String> musicKeywords = Arrays.asList(
            "周杰伦", "歌词", "歌曲", "音乐", "歌手", "专辑", "单曲", 
            "演唱会", "作曲", "作词", "晴天歌词", "晴天的歌",
            "歌名", "华语", "流行歌", "经典歌曲"
        );
        
        for (String keyword : musicKeywords) {
            if (query.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    // 判断是否是明确的天气查询
    private boolean isWeatherQuery(String query) {
        List<String> weatherKeywords = Arrays.asList(
            "天气", "温度", "预报", "空气质量", "气象", "今天天气", 
            "明天天气", "天气预报", "多少度", "下雨吗", "有风吗"
        );
        
        for (String keyword : weatherKeywords) {
            if (query.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    public void recordUsage(String skillId) {
        skillUsage.merge(skillId, 1, Integer::sum);
    }

    public Map<String, Integer> getSkillUsageStats() {
        return new HashMap<>(skillUsage);
    }

    public Map<String, Object> getSkillsReport() {
        Map<String, Object> report = new HashMap<>();
        List<Map<String, Object>> skillsList = new ArrayList<>();
        
        for (Skill skill : skills.values()) {
            Map<String, Object> skillInfo = new HashMap<>();
            skillInfo.put("id", skill.getId());
            skillInfo.put("name", skill.getName());
            skillInfo.put("description", skill.getDescription());
            skillInfo.put("priority", skill.getPriority());
            skillInfo.put("usage", skillUsage.getOrDefault(skill.getId(), 0));
            skillInfo.put("confidence", skillConfidence.getOrDefault(skill.getId(), 0.0));
            skillInfo.put("enabled", skill.isEnabled());
            skillsList.add(skillInfo);
        }
        
        report.put("totalSkills", skills.size());
        report.put("skills", skillsList);
        return report;
    }

    public void learnFromInteraction(String context, String feedback) {
        for (Skill skill : skills.values()) {
            if (skill instanceof LearningSkill) {
                ((LearningSkill) skill).learn(context, feedback);
                double newConfidence = ((LearningSkill) skill).getConfidence();
                skillConfidence.put(skill.getId(), newConfidence);
            }
        }
    }
}
