package com.enterprise.knowledge.infrastructure.agent.tool;

import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class SearchTool {

    public String webSearch(String query) {
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            
            StringBuilder result = new StringBuilder();
            result.append("🔍 搜索结果：").append(query).append("\n\n");
            
            List<SearchResult> results = performSearch(query);
            
            for (int i = 0; i < results.size(); i++) {
                SearchResult item = results.get(i);
                result.append(i + 1).append(". ").append(item.title).append("\n");
                result.append("   ").append(item.snippet).append("\n");
                result.append("   来源: ").append(item.source).append("\n\n");
            }
            
            return result.toString();
            
        } catch (Exception e) {
            return "搜索失败: " + e.getMessage();
        }
    }

    private List<SearchResult> performSearch(String query) {
        List<SearchResult> results = new ArrayList<>();
        
        if (query.contains("天气")) {
            results.add(new SearchResult(
                "实时天气查询 - 中国天气网",
                "根据最新气象数据，为您提供全国各大城市的实时天气信息，包括温度、湿度、风力等详细数据。",
                "中国天气网"
            ));
            results.add(new SearchResult(
                "天气预报15天查询",
                "提供未来15天的天气预报服务，帮助您提前做好出行计划。",
                "天气API"
            ));
        } else if (query.contains("新闻") || query.contains("最新")) {
            results.add(new SearchResult(
                "今日热点新闻汇总",
                "涵盖科技、财经、娱乐等多个领域的最新资讯，让您随时掌握世界动态。",
                "新闻聚合平台"
            ));
            results.add(new SearchResult(
                "科技前沿动态",
                "人工智能、大数据、云计算等前沿技术的最新发展趋势和应用案例。",
                "TechNews"
            ));
        } else if (query.contains("股票") || query.contains("财经")) {
            results.add(new SearchResult(
                "实时股票行情",
                "提供沪深两市、港股、美股等市场的实时行情数据和分析报告。",
                "财经数据平台"
            ));
            results.add(new SearchResult(
                "财经新闻与分析",
                "每日财经新闻速递，专业分析师解读市场走势。",
                "财经资讯"
            ));
        } else {
            results.add(new SearchResult(
                "综合搜索结果 - 百科知识",
                "关于\"" + query + "\"的详细介绍和相关信息，涵盖定义、历史、应用等方面。",
                "知识百科"
            ));
            results.add(new SearchResult(
                "相关文章推荐",
                "精选多篇关于\"" + query + "\"的深度文章，帮助您全面了解相关知识。",
                "内容平台"
            ));
            results.add(new SearchResult(
                "最新资讯动态",
                "关于\"" + query + "\"的最新消息和发展动态，让您掌握最新趋势。",
                "资讯平台"
            ));
        }
        
        return results;
    }

    public String getTrendingTopics() {
        StringBuilder result = new StringBuilder();
        result.append("🔥 今日热点话题\n\n");
        
        String[] topics = {
            "人工智能发展新突破",
            "新能源汽车市场动态",
            "数字经济发展趋势",
            "企业数字化转型",
            "元宇宙技术应用"
        };
        
        for (int i = 0; i < topics.length; i++) {
            result.append(i + 1).append(". ").append(topics[i]).append("\n");
        }
        
        return result.toString();
    }

    private static class SearchResult {
        String title;
        String snippet;
        String source;

        SearchResult(String title, String snippet, String source) {
            this.title = title;
            this.snippet = snippet;
            this.source = source;
        }
    }
}
