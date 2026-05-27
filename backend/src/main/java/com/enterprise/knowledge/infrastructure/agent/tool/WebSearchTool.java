package com.enterprise.knowledge.infrastructure.agent.tool;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class WebSearchTool {

    private static final String SEARCH_API = "https://www.googleapis.com/customsearch/v1";
    private static final String API_KEY = "AIzaSyA9j2tQ8K493f9L4h6Y7wH7g6Z6x5Q5w3E2r1T0";
    private static final String CX = "012345678901234567891:abcdefghijk";

    public String search(String query) {
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            
            StringBuilder result = new StringBuilder();
            result.append("【搜索结果】\n\n");
            
            // 使用mock数据模拟搜索结果（在实际生产中应使用真实搜索API）
            List<SearchResult> mockResults = generateMockSearchResults(query);
            
            for (int i = 0; i < mockResults.size(); i++) {
                SearchResult item = mockResults.get(i);
                result.append(i + 1).append(". ").append(item.title).append("\n");
                result.append("   URL: ").append(item.url).append("\n");
                result.append("   摘要: ").append(item.snippet).append("\n\n");
            }
            
            return result.toString();
            
        } catch (Exception e) {
            return "搜索失败: " + e.getMessage();
        }
    }

    private List<SearchResult> generateMockSearchResults(String query) {
        List<SearchResult> results = new ArrayList<>();
        
        if (query.contains("企业宣传") || query.contains("企业介绍")) {
            results.add(new SearchResult(
                "企业宣传PPT制作指南 - 专业模板推荐",
                "https://www.example.com/ppt-guide",
                "企业宣传PPT是展示企业文化、产品和服务的重要工具。一份优秀的企业宣传PPT应该包括公司简介、核心业务、产品展示、团队介绍、发展历程等内容。"
            ));
            results.add(new SearchResult(
                "2024企业宣传PPT设计趋势",
                "https://www.example.com/ppt-trends-2024",
                "2024年企业宣传PPT设计趋势包括：极简风格、数据可视化、动画效果、响应式设计等。使用现代配色方案和专业字体可以提升PPT的专业感。"
            ));
            results.add(new SearchResult(
                "企业宣传PPT模板下载",
                "https://www.example.com/ppt-templates",
                "精选企业宣传PPT模板，涵盖科技、金融、制造等多个行业。支持PowerPoint和Google Slides格式，可直接编辑使用。"
            ));
            results.add(new SearchResult(
                "如何制作吸引人的企业宣传PPT",
                "https://www.example.com/ppt-tips",
                "制作企业宣传PPT的关键要素：清晰的结构、优质的图片、简洁的文字、合适的配色。建议遵循6x6原则，每页不超过6行，每行不超过6个字。"
            ));
        } else if (query.contains("产品介绍") || query.contains("产品宣传")) {
            results.add(new SearchResult(
                "产品介绍PPT制作技巧",
                "https://www.example.com/product-ppt",
                "产品介绍PPT需要突出产品特点、优势和价值。建议使用对比图表、产品截图、用户评价等元素增强说服力。"
            ));
            results.add(new SearchResult(
                "产品发布会PPT设计指南",
                "https://www.example.com/product-launch",
                "产品发布会PPT应注重故事性和视觉冲击力。使用动态效果和互动元素可以吸引观众注意力，提升演讲效果。"
            ));
        } else {
            results.add(new SearchResult(
                "PPT制作技巧与方法",
                "https://www.example.com/ppt-skills",
                "制作专业PPT的核心技巧：明确目标、结构化内容、视觉设计、演讲配合。好的PPT应该是演讲者的辅助工具，而不是主角。"
            ));
            results.add(new SearchResult(
                "专业PPT设计模板",
                "https://www.example.com/professional-templates",
                "专业设计团队打造的PPT模板，适用于各种商务场合。包含完整的图表、图示和配色方案，可直接使用。"
            ));
        }
        
        return results;
    }

    public List<String> getTopicInformation(String topic) {
        List<String> info = new ArrayList<>();
        
        if (topic.contains("企业宣传") || topic.contains("企业介绍")) {
            info.add("公司简介：企业宣传PPT的核心是展示公司的整体形象和实力");
            info.add("核心业务：突出企业的主营业务和核心竞争力");
            info.add("产品展示：展示企业的主要产品或服务");
            info.add("团队介绍：介绍核心团队成员和组织结构");
            info.add("发展历程：展示企业的发展里程碑和成就");
            info.add("企业文化：传达企业的价值观和使命");
            info.add("未来展望：展示企业的发展规划和目标");
        } else if (topic.contains("产品介绍")) {
            info.add("产品概述：产品的基本信息和定位");
            info.add("产品特点：产品的核心功能和优势");
            info.add("技术参数：产品的技术规格和性能指标");
            info.add("应用场景：产品的使用场景和适用人群");
            info.add("用户评价：用户反馈和使用案例");
            info.add("价格方案：产品的定价和优惠政策");
        } else {
            info.add("主题概述：关于" + topic + "的基本介绍");
            info.add("核心要点：" + topic + "的关键内容");
            info.add("相关资源：与" + topic + "相关的参考资料");
            info.add("发展趋势：" + topic + "的未来发展方向");
        }
        
        return info;
    }

    private static class SearchResult {
        String title;
        String url;
        String snippet;

        SearchResult(String title, String url, String snippet) {
            this.title = title;
            this.url = url;
            this.snippet = snippet;
        }
    }
}
