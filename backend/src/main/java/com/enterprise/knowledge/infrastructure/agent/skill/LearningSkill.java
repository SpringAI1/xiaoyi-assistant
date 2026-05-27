package com.enterprise.knowledge.infrastructure.agent.skill;

public interface LearningSkill extends Skill {
    
    void learn(String context, String feedback);
    
    double getConfidence();
}
