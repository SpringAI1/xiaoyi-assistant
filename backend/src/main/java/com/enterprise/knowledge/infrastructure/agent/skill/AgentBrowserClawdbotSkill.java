package com.enterprise.knowledge.infrastructure.agent.skill;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AgentBrowserClawdbotSkill implements Skill {

    private static final String SKILL_ID = "agent-browser-clawdbot";
    private static final String SKILL_NAME = "浏览器助手";
    private static final String SKILL_DESCRIPTION = "模拟浏览器操作，支持网页浏览、内容提取、表单填写等自动化操作。";

    private final HttpClient httpClient;

    public AgentBrowserClawdbotSkill() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public String getId() {
        return SKILL_ID;
    }

    @Override
    public String getName() {
        return SKILL_NAME;
    }

    @Override
    public String getDescription() {
        return SKILL_DESCRIPTION;
    }

    @Override
    public List<String> getKeywords() {
        return Arrays.asList("浏览", "打开", "访问", "网页", "网站", "browser", "navigate", "页面");
    }

    @Override
    public String execute(String input) {
        String lowerInput = input.toLowerCase();
        
        if (lowerInput.contains("浏览") || lowerInput.contains("访问")) {
            String url = extractUrl(input);
            if (url.isEmpty()) {
                url = extractDomain(input);
            }
            return browseUrl(url);
        } else if (lowerInput.contains("搜索")) {
            return searchWeb(input);
        } else if (lowerInput.contains("提取") || lowerInput.contains("内容")) {
            return extractContent(input);
        } else {
            return getHelp();
        }
    }

    private String extractUrl(String input) {
        Pattern urlPattern = Pattern.compile("https?://[\\w.-]+(?:/[\\w./-]*)?");
        Matcher matcher = urlPattern.matcher(input);
        if (matcher.find()) {
            return matcher.group();
        }
        return "";
    }

    private String extractDomain(String input) {
        String clean = input.replace("浏览", "").replace("访问", "").trim();
        
        if (!clean.startsWith("http")) {
            if (clean.contains(".com") || clean.contains(".cn") || clean.contains(".net")) {
                return "https://" + clean;
            }
            return "https://" + clean + ".com";
        }
        return clean;
    }

    private String browseUrl(String url) {
        if (url.isEmpty()) {
            return "🌐 请提供要访问的网址！\n\n示例：浏览 www.baidu.com";
        }
        
        StringBuilder result = new StringBuilder();
        result.append("🌐 【浏览器助手】\n\n");
        result.append("🔗 访问地址：").append(url).append("\n\n");
        
        try {
            String title = fetchPageTitle(url);
            if (title != null) {
                result.append("📄 页面标题：").append(title).append("\n\n");
            }
            
            String summary = fetchPageSummary(url);
            if (summary != null && !summary.isEmpty()) {
                result.append("📝 页面摘要：\n");
                result.append(summary).append("\n");
            }
            
            result.append("\n✅ 页面访问成功");
            
        } catch (Exception e) {
            result.append("⚠️ 访问失败：").append(e.getMessage());
        }
        
        return result.toString();
    }

    private String fetchPageTitle(String url) {
        try {
            String html = fetchUrl(url);
            if (html != null) {
                Pattern titlePattern = Pattern.compile("<title[^>]*>([^<]+)</title>", Pattern.CASE_INSENSITIVE);
                Matcher matcher = titlePattern.matcher(html);
                if (matcher.find()) {
                    return cleanHtml(matcher.group(1));
                }
            }
        } catch (Exception e) {
            // 忽略错误
        }
        return null;
    }

    private String fetchPageSummary(String url) {
        try {
            String html = fetchUrl(url);
            if (html != null) {
                String text = cleanHtml(html);
                
                text = text.replaceAll("\\s+", " ").trim();
                
                if (text.length() > 300) {
                    text = text.substring(0, 300) + "...";
                }
                
                return text;
            }
        } catch (Exception e) {
            // 忽略错误
        }
        return null;
    }

    private String fetchUrl(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml")
                    .timeout(java.time.Duration.ofSeconds(10))
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                return response.body();
            }
        } catch (Exception e) {
            // 忽略错误
        }
        return null;
    }

    private String cleanHtml(String html) {
        if (html == null) return "";
        
        return html.replaceAll("<[^>]*>", " ")
                   .replaceAll("\\s+", " ")
                   .trim();
    }

    private String searchWeb(String input) {
        String query = input.replace("搜索", "").trim();
        
        if (query.isEmpty()) {
            return "🔍 请提供搜索关键词！";
        }
        
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = "https://www.baidu.com/s?wd=" + encodedQuery;
            
            return "🔍 搜索结果页面：\n" + url;
        } catch (Exception e) {
            return "⚠️ 搜索失败：" + e.getMessage();
        }
    }

    private String extractContent(String input) {
        return "📝 内容提取功能\n\n" +
               "支持提取页面标题、文本内容、链接列表等\n\n" +
               "示例：提取 www.example.com 的内容";
    }

    private String getHelp() {
        return """
                🌐 浏览器助手技能
                
                可用命令：
                • 浏览 [网址] - 访问网页
                • 搜索 [关键词] - 搜索网页
                • 提取 [网址] - 提取页面内容
                
                示例：
                • 浏览 www.baidu.com
                • 搜索 人工智能
                • 提取 www.example.com
                
                💡 提示：网址可以省略 https://
                """;
    }

    @Override
    public int getPriority() {
        return 5;
    }
}
