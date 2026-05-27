package com.enterprise.knowledge.infrastructure.agent.skill;

import com.enterprise.knowledge.infrastructure.agent.tool.SearchTool;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class SearchSkill implements LearningSkill {

    private final SearchTool searchTool;
    private double confidence = 0.75;

    public SearchSkill(SearchTool searchTool) {
        this.searchTool = searchTool;
    }

    @Override
    public String getId() {
        return "search-skill";
    }

    @Override
    public String getName() {
        return "网络搜索";
    }

    @Override
    public String getDescription() {
        return "实时联网搜索最新信息和热点话题";
    }

    @Override
    public List<String> getKeywords() {
        return Arrays.asList("搜索", "查一下", "最新", "新闻", "资讯", "了解");
    }

    @Override
    public String execute(String input) {
        return searchTool.webSearch(input);
    }

    @Override
    public void learn(String context, String feedback) {
        if (feedback.contains("有用") || feedback.contains("帮助")) {
            confidence = Math.min(1.0, confidence + 0.03);
        } else {
            confidence = Math.max(0.1, confidence - 0.03);
        }
    }

    @Override
    public double getConfidence() {
        return confidence;
    }
}
