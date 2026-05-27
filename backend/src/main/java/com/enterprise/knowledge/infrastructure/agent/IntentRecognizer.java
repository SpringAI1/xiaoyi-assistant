package com.enterprise.knowledge.infrastructure.agent;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class IntentRecognizer {

    public enum Intent {
        FACT_QUERY,          // 事实查询
        TOOL_REQUIRED,       // 需要工具
        DATA_ANALYSIS,       // 数据分析
        CREATIVE_WRITING,    // 创意写作
        DOCUMENT_GENERATION, // 文档生成（PPT、Word、Excel等）
        MEDIA_GENERATION,    // 多媒体生成（视频、音乐、音频、图片）
        WEATHER_QUERY,       // 天气查询
        CHAT                 // 普通聊天
    }

    public Intent recognize(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Intent.CHAT;
        }

        String lowerQuery = query.toLowerCase();

        // 先进行上下文分析，识别特殊情况
        ContextAnalysis analysis = analyzeContext(lowerQuery);

        // 多媒体生成意图
        if (lowerQuery.contains("生成") && (lowerQuery.contains("视频") || 
            lowerQuery.contains("video"))) {
            return Intent.MEDIA_GENERATION;
        }
        
        if (lowerQuery.contains("生成") && (lowerQuery.contains("音乐") || 
            lowerQuery.contains("music") || lowerQuery.contains("音频"))) {
            return Intent.MEDIA_GENERATION;
        }
        
        if (lowerQuery.contains("生成") && (lowerQuery.contains("图片") || 
            lowerQuery.contains("image") || lowerQuery.contains("photo"))) {
            return Intent.MEDIA_GENERATION;
        }

        // 天气查询意图 - 增强判断
        if (!analysis.isMusicQuery && !analysis.isEntertainmentQuery &&
            (lowerQuery.contains("天气") || lowerQuery.contains("温度") ||
            lowerQuery.contains("预报") || lowerQuery.contains("空气质量") ||
            lowerQuery.contains("气象"))) {
            return Intent.WEATHER_QUERY;
        }

        // 文档生成意图
        if (lowerQuery.contains("生成") && (lowerQuery.contains("ppt") || 
            lowerQuery.contains("powerpoint") || lowerQuery.contains("演示文稿"))) {
            return Intent.DOCUMENT_GENERATION;
        }
        
        if (lowerQuery.contains("生成") && (lowerQuery.contains("word") || 
            lowerQuery.contains("文档") || lowerQuery.contains("报告"))) {
            return Intent.DOCUMENT_GENERATION;
        }
        
        if (lowerQuery.contains("生成") && (lowerQuery.contains("excel") || 
            lowerQuery.contains("表格") || lowerQuery.contains("数据"))) {
            return Intent.DOCUMENT_GENERATION;
        }

        // 事实查询
        if (lowerQuery.contains("什么") || lowerQuery.contains("如何") || 
            lowerQuery.contains("为什么") || lowerQuery.contains("多少") ||
            lowerQuery.contains("谁") || lowerQuery.contains("哪里") ||
            lowerQuery.contains("何时") || lowerQuery.contains("哪个")) {
            return Intent.FACT_QUERY;
        }

        // 数据分析
        if (lowerQuery.contains("计算") || lowerQuery.contains("分析") ||
            lowerQuery.contains("统计") || lowerQuery.contains("图表")) {
            return Intent.DATA_ANALYSIS;
        }

        // 创意写作
        if (lowerQuery.contains("写") || lowerQuery.contains("创作") ||
            lowerQuery.contains("文案") || lowerQuery.contains("文章")) {
            return Intent.CREATIVE_WRITING;
        }

        // 需要工具
        if (lowerQuery.contains("搜索") || lowerQuery.contains("查找") ||
            lowerQuery.contains("查询") || lowerQuery.contains("获取")) {
            return Intent.TOOL_REQUIRED;
        }

        return Intent.CHAT;
    }
    
    // 上下文分析类
    private ContextAnalysis analyzeContext(String query) {
        ContextAnalysis analysis = new ContextAnalysis();
        
        // 音乐相关关键词
        List<String> musicKeywords = Arrays.asList(
            "周杰伦", "歌词", "歌曲", "音乐", "歌手", "专辑", "单曲", 
            "演唱会", "作曲", "作词", "歌名", "华语", "流行歌", "经典歌曲",
            "晴天歌词", "晴天的歌", "歌词什么", "什么歌词"
        );
        
        for (String keyword : musicKeywords) {
            if (query.contains(keyword)) {
                analysis.isMusicQuery = true;
                break;
            }
        }
        
        // 娱乐相关关键词
        List<String> entertainmentKeywords = Arrays.asList(
            "电影", "电视剧", "演员", "明星", "综艺", "节目", "艺人"
        );
        
        for (String keyword : entertainmentKeywords) {
            if (query.contains(keyword)) {
                analysis.isEntertainmentQuery = true;
                break;
            }
        }
        
        return analysis;
    }
    
    // 上下文分析结果类
    private static class ContextAnalysis {
        boolean isMusicQuery = false;
        boolean isEntertainmentQuery = false;
    }
}