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
public class BaiduSearchSkill implements Skill {

    private static final String SKILL_ID = "baidu-search";
    private static final String SKILL_NAME = "百度搜索";
    private static final String SKILL_DESCRIPTION = "使用百度搜索引擎获取最新信息，支持网页搜索、图片搜索、新闻搜索等。";

    private final HttpClient httpClient;

    public BaiduSearchSkill() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(10))
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
        return Arrays.asList("百度", "搜索", "baidu", "查找", "查询", "搜一下", "帮我查");
    }

    @Override
    public String execute(String input) {
        String query = extractQuery(input);
        
        if (query.isEmpty()) {
            return "🔍 请告诉我你想搜索什么内容！";
        }
        
        return search(query);
    }

    private String extractQuery(String input) {
        String clean = input.trim();
        
        String[] prefixes = {"百度搜索", "百度", "搜索", "帮我查", "查一下", "搜一下"};
        for (String prefix : prefixes) {
            if (clean.startsWith(prefix)) {
                clean = clean.substring(prefix.length()).trim();
                break;
            }
        }
        
        return clean;
    }

    private String search(String query) {
        StringBuilder result = new StringBuilder();
        result.append("🔍 【百度搜索结果】\n\n");
        result.append("搜索关键词：").append(query).append("\n\n");
        
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = "https://www.baidu.com/s?wd=" + encodedQuery + "&pn=0&rn=10";
            
            String html = fetchUrl(url);
            if (html != null) {
                List<SearchResult> results = parseResults(html);
                
                if (results.isEmpty()) {
                    result.append("📭 未找到相关结果");
                } else {
                    int count = 0;
                    Set<String> seenTitles = new HashSet<>();
                    
                    for (SearchResult item : results) {
                        if (count >= 5) break;
                        if (seenTitles.contains(item.title)) continue;
                        
                        seenTitles.add(item.title);
                        
                        result.append(count + 1).append(". ").append(item.title).append("\n");
                        result.append("   📡 来源：").append(item.source).append("\n");
                        
                        if (item.description != null && !item.description.isEmpty()) {
                            String desc = item.description;
                            if (desc.length() > 150) {
                                desc = desc.substring(0, 150) + "...";
                            }
                            result.append("   📝 ").append(desc).append("\n");
                        }
                        
                        if (item.url != null && !item.url.isEmpty()) {
                            result.append("   🔗 ").append(item.url).append("\n");
                        }
                        
                        result.append("\n");
                        count++;
                    }
                }
            } else {
                result.append("⚠️ 搜索服务暂时不可用");
            }
        } catch (Exception e) {
            result.append("⚠️ 搜索失败：").append(e.getMessage());
        }
        
        return result.toString();
    }

    private String fetchUrl(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml")
                    .timeout(java.time.Duration.ofSeconds(8))
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

    private List<SearchResult> parseResults(String html) {
        List<SearchResult> results = new ArrayList<>();
        
        Pattern resultPattern = Pattern.compile("<div class=\"result-op c-container.*?</div>", Pattern.DOTALL);
        Matcher matcher = resultPattern.matcher(html);
        
        int index = 0;
        while (matcher.find() && index < 10) {
            String item = matcher.group();
            
            String title = extractBetweenTags(item, "<h3", "</h3>");
            String url = extractLink(item);
            String description = extractBetweenTags(item, "<span class=\"c-abstract\">", "</span>");
            
            if (!title.isEmpty()) {
                SearchResult r = new SearchResult();
                r.title = cleanHtml(title);
                r.url = url;
                r.description = cleanHtml(description);
                r.source = extractSource(url);
                results.add(r);
                index++;
            }
        }
        
        return results;
    }

    private String extractBetweenTags(String text, String startTag, String endTag) {
        int start = text.indexOf(startTag);
        if (start == -1) return "";
        
        start += startTag.length();
        int end = text.indexOf(endTag, start);
        if (end == -1) end = text.length();
        
        return text.substring(start, end);
    }

    private String extractLink(String text) {
        Pattern linkPattern = Pattern.compile("<a[^>]+href=\"([^\"]+)\"");
        Matcher matcher = linkPattern.matcher(text);
        
        if (matcher.find()) {
            String url = matcher.group(1);
            if (url.contains("baidu.com/link")) {
                return resolveBaiduLink(url);
            }
            return url;
        }
        return "";
    }

    private String resolveBaiduLink(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0")
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .timeout(java.time.Duration.ofSeconds(5))
                    .build();
            
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            
            Map<String, List<String>> headers = response.headers().map();
            if (headers.containsKey("location")) {
                return headers.get("location").get(0);
            }
            if (headers.containsKey("Location")) {
                return headers.get("Location").get(0);
            }
        } catch (Exception e) {
            // 忽略错误
        }
        return url;
    }

    private String cleanHtml(String text) {
        if (text == null) return "";
        
        return text.replaceAll("<[^>]*>", " ")
                   .replaceAll("\\s+", " ")
                   .trim();
    }

    private String extractSource(String url) {
        if (url == null || url.isEmpty()) return "百度搜索";
        
        try {
            URI uri = URI.create(url);
            return uri.getHost();
        } catch (Exception e) {
            return "百度搜索";
        }
    }

    private static class SearchResult {
        String title;
        String url;
        String description;
        String source;
    }

    @Override
    public int getPriority() {
        return 4;
    }
}
