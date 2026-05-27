package com.enterprise.knowledge.infrastructure.agent.skill;

import java.util.List;

public interface Skill {
    String getId();
    String getName();
    String getDescription();
    List<String> getKeywords();
    String execute(String input);
    default int getPriority() { return 5; } // 默认优先级
    default boolean isEnabled() { return true; }
}
