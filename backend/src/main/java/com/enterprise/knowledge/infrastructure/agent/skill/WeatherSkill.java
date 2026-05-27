package com.enterprise.knowledge.infrastructure.agent.skill;

import com.enterprise.knowledge.infrastructure.agent.tool.WeatherTool;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class WeatherSkill implements LearningSkill {

    private final WeatherTool weatherTool;
    private double confidence = 0.8;
    
    private static final List<String> MUSIC_KEYWORDS = Arrays.asList(
        "周杰伦", "歌词", "歌曲", "音乐", "歌手", "专辑", "单曲",
        "演唱会", "作曲", "作词", "歌名", "华语", "流行歌", "经典歌曲",
        "晴天歌词", "晴天的歌", "什么歌", "哪首歌"
    );

    public WeatherSkill(WeatherTool weatherTool) {
        this.weatherTool = weatherTool;
    }

    @Override
    public String getId() {
        return "weather-skill";
    }

    @Override
    public String getName() {
        return "天气查询";
    }

    @Override
    public String getDescription() {
        return "查询全国各大城市的实时天气信息";
    }

    @Override
    public List<String> getKeywords() {
        return Arrays.asList("天气", "温度", "预报", "空气质量", "气象", "今天天气", "明天天气");
    }

    @Override
    public String execute(String input) {
        // 先检查是否为音乐相关查询，如果是，直接返回空，让系统用其他方式处理
        if (isMusicQuery(input)) {
            return null;
        }
        return weatherTool.getCurrentWeather(input);
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

    @Override
    public void learn(String context, String feedback) {
        if (feedback.contains("正确") || feedback.contains("准确")) {
            confidence = Math.min(1.0, confidence + 0.05);
        } else {
            confidence = Math.max(0.1, confidence - 0.05);
        }
    }

    @Override
    public double getConfidence() {
        return confidence;
    }
}
