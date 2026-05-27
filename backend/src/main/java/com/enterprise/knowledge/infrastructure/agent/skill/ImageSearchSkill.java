package com.enterprise.knowledge.infrastructure.agent.skill;

import com.enterprise.knowledge.infrastructure.agent.tool.ImageSearchTool;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class ImageSearchSkill implements LearningSkill {

    private final ImageSearchTool imageSearchTool;
    private double confidence = 0.85;

    public ImageSearchSkill(ImageSearchTool imageSearchTool) {
        this.imageSearchTool = imageSearchTool;
    }

    @Override
    public String getId() {
        return "image-search-skill";
    }

    @Override
    public String getName() {
        return "图片搜索推荐";
    }

    @Override
    public String getDescription() {
        return "联网搜索相关图片，并提供质量评估和智能推荐建议";
    }

    @Override
    public List<String> getKeywords() {
        return Arrays.asList("图片", "照片", "图", "看图", "搜索图片", "提供图片", "汽车图片", "风景图片");
    }

    @Override
    public String execute(String input) {
        return imageSearchTool.searchImages(input);
    }

    @Override
    public void learn(String context, String feedback) {
        if (feedback.contains("有用") || feedback.contains("满意")) {
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
