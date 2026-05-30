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
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class MultiSearchEngineSkill implements Skill {

    private static final String SKILL_ID = "multi-search-engine";
    private static final String SKILL_NAME = "多搜索引擎";
    private static final String SKILL_DESCRIPTION = "同时调用多个搜索引擎（百度、必应、搜狗）进行搜索，整合并返回最相关的结果。";

    private final HttpClient httpClient;
    private final AtomicInteger searchCount = new AtomicInteger(0);

    public MultiSearchEngineSkill() {
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
        return Arrays.asList("搜索", "查询", "找", "查找", "搜索一下", "帮我查", "百度", "bing", "必应", "引擎");
    }

    @Override
    public String execute(String input) {
        searchCount.incrementAndGet();
        
        String query = extractQuery(input);
        if (query.isEmpty()) {
            return "🔍 请告诉我你想搜索什么内容！";
        }
        
        return performMultiSearch(query);
    }

    private String extractQuery(String input) {
        String clean = input.trim();
        
        String[] prefixes = {"搜索", "帮我查", "查找", "查一下", "搜一下", "搜索一下"};
        for (String prefix : prefixes) {
            if (clean.startsWith(prefix)) {
                clean = clean.substring(prefix.length()).trim();
                break;
            }
        }
        
        return clean;
    }

    private String performMultiSearch(String query) {
        StringBuilder result = new StringBuilder();
        result.append("## 搜索结果\n\n");
        result.append("**关键词**：").append(query).append("\n\n");
        
        List<SearchResult> allResults = new ArrayList<>();
        
        allResults.addAll(searchBaidu(query));
        allResults.addAll(searchBing(query));
        allResults.addAll(searchSogou(query));
        
        if (allResults.isEmpty()) {
            return "未找到相关搜索结果，请尝试其他关键词。";
        }
        
        allResults.sort((a, b) -> Integer.compare(b.relevance, a.relevance));
        
        int count = 0;
        Set<String> seenTitles = new HashSet<>();
        
        for (SearchResult item : allResults) {
            if (count >= 5) break;
            if (seenTitles.contains(item.title)) continue;
            
            seenTitles.add(item.title);
            
            result.append("### ").append(count + 1).append(". ").append(item.title).append("\n\n");
            
            if (item.description != null && !item.description.isEmpty()) {
                String desc = item.description;
                if (desc.length() > 250) {
                    desc = desc.substring(0, 250) + "...";
                }
                result.append(desc).append("\n\n");
            }
            
            result.append("**来源**：").append(item.source);
            if (item.url != null && !item.url.isEmpty()) {
                result.append(" | [查看详情](").append(item.url).append(")");
            }
            result.append("\n\n");
            result.append("---\n\n");
            
            count++;
        }
        
        result.append("> 💡 提示：如需更详细的资料，可告诉我哦！");
        
        return result.toString();
    }

    private List<SearchResult> searchBaidu(String query) {
        List<SearchResult> results = new ArrayList<>();
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = "https://www.baidu.com/s?wd=" + encodedQuery + "&pn=0&rn=10";
            
            String html = fetchUrl(url);
            if (html != null) {
                results = parseBaiduResults(html);
                for (SearchResult r : results) {
                    r.source = "百度搜索";
                    r.relevance += 30;
                }
            }
        } catch (Exception e) {
            // 忽略错误
        }
        return results;
    }

    private List<SearchResult> searchBing(String query) {
        List<SearchResult> results = new ArrayList<>();
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = "https://www.bing.com/search?q=" + encodedQuery + "&count=10";
            
            String html = fetchUrl(url);
            if (html != null) {
                results = parseBingResults(html);
                for (SearchResult r : results) {
                    r.source = "必应搜索";
                    r.relevance += 25;
                }
            }
        } catch (Exception e) {
            // 忽略错误
        }
        return results;
    }

    private List<SearchResult> searchSogou(String query) {
        List<SearchResult> results = new ArrayList<>();
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = "https://www.sogou.com/web?query=" + encodedQuery + "&page=1";
            
            String html = fetchUrl(url);
            if (html != null) {
                results = parseSogouResults(html);
                for (SearchResult r : results) {
                    r.source = "搜狗搜索";
                    r.relevance += 20;
                }
            }
        } catch (Exception e) {
            // 忽略错误
        }
        return results;
    }

    private String fetchUrl(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .timeout(java.time.Duration.ofSeconds(8))
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                return response.body();
            }
        } catch (Exception e) {
            // 忽略网络错误
        }
        return null;
    }

    private List<SearchResult> parseBaiduResults(String html) {
        List<SearchResult> results = new ArrayList<>();
        
        Pattern resultPattern = Pattern.compile("<div class=\"result-op c-container.*?</div>", Pattern.DOTALL);
        Matcher matcher = resultPattern.matcher(html);
        
        int index = 0;
        while (matcher.find() && index < 5) {
            String item = matcher.group();
            
            String title = extractBetweenTags(item, "<h3", "</h3>");
            String url = extractLink(item);
            String description = extractBetweenTags(item, "<span class=\"c-abstract\">", "</span>");
            
            if (!title.isEmpty()) {
                SearchResult r = new SearchResult();
                r.title = cleanHtml(title);
                r.url = url;
                r.description = cleanHtml(description);
                r.relevance = 50 - (index * 5);
                results.add(r);
                index++;
            }
        }
        
        return results;
    }

    private List<SearchResult> parseBingResults(String html) {
        List<SearchResult> results = new ArrayList<>();
        
        Pattern resultPattern = Pattern.compile("<li class=\"b_algo\".*?</li>", Pattern.DOTALL);
        Matcher matcher = resultPattern.matcher(html);
        
        int index = 0;
        while (matcher.find() && index < 5) {
            String item = matcher.group();
            
            String title = extractBetweenTags(item, "<h2>", "</h2>");
            String url = extractLink(item);
            String description = extractBetweenTags(item, "<p>", "</p>");
            
            if (!title.isEmpty()) {
                SearchResult r = new SearchResult();
                r.title = cleanHtml(title);
                r.url = url;
                r.description = cleanHtml(description);
                r.relevance = 50 - (index * 5);
                results.add(r);
                index++;
            }
        }
        
        return results;
    }

    private List<SearchResult> parseSogouResults(String html) {
        List<SearchResult> results = new ArrayList<>();
        
        Pattern resultPattern = Pattern.compile("<div class=\"rb\".*?</div>", Pattern.DOTALL);
        Matcher matcher = resultPattern.matcher(html);
        
        int index = 0;
        while (matcher.find() && index < 5) {
            String item = matcher.group();
            
            String title = extractBetweenTags(item, "<h3", "</h3>");
            String url = extractLink(item);
            String description = extractBetweenTags(item, "<p class=\"ft\">", "</p>");
            
            if (title.isEmpty()) {
                title = extractBetweenTags(item, "<h2", "</h2>");
            }
            
            if (!title.isEmpty()) {
                SearchResult r = new SearchResult();
                r.title = cleanHtml(title);
                r.url = url;
                r.description = cleanHtml(description);
                r.relevance = 50 - (index * 5);
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
                return resolveShortUrl(url);
            }
            if (url.contains("sogou.com/link")) {
                return resolveShortUrl(url);
            }
            return url;
        }
        return "";
    }

    private String resolveShortUrl(String url) {
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

        String clean = text.replaceAll("<[^>]+>", "")
                          .replaceAll("&nbsp;", " ")
                          .replaceAll("&amp;", "&")
                          .replaceAll("&lt;", "<")
                          .replaceAll("&gt;", ">")
                          .replaceAll("&quot;", "\"")
                          .replaceAll("&#39;", "'")
                          .replaceAll("&ldquo;", "\"")
                          .replaceAll("&rdquo;", "\"")
                          .replaceAll("&mdash;", "—")
                          .replaceAll("&hellip;", "…")
                          .replaceAll("\\s+", " ")
                          .trim();

        return clean.replaceAll("\\s{2,}", " ").trim();
    }

    private int getSearchCount() {
        int count = 0;
        try {
            searchBaidu("test");
            count++;
        } catch (Exception e) {}
        
        try {
            searchBing("test");
            count++;
        } catch (Exception e) {}
        
        try {
            searchSogou("test");
            count++;
        } catch (Exception e) {}
        
        return count;
    }

    @Override
    public int getPriority() {
        return 4;
    }

    private static class SearchResult {
        String title;
        String url;
        String description;
        String source;
        int relevance = 0;
    }
}
