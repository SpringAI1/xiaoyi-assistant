package com.enterprise.knowledge.infrastructure.agent.tool;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class BaiduSearchApiTool {

    private static final String BAIDU_SEARCH_URL = "https://www.baidu.com/s";
    private static final String BAIDU_MOBILE_URL = "https://sp0.baidu.com/5LMaNlT9KgQFm2e22I1M_/";
    private static final String PC_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final String MOBILE_USER_AGENT = "Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0 Mobile/15E148 Safari/604.1";

    private final HttpClient httpClient;
    private final AtomicInteger requestCount = new AtomicInteger(0);

    public BaiduSearchApiTool() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public SearchResult search(String keyword, int pageNum) {
        requestCount.incrementAndGet();
        SearchResult result = new SearchResult();
        result.setKeyword(keyword);

        try {
            String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
            String url = BAIDU_SEARCH_URL + "?wd=" + encodedKeyword + "&pn=" + (pageNum * 10) + "&rn=10";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", PC_USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .header("Referer", "https://www.baidu.com/")
                    .GET()
                    .timeout(java.time.Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                result.setSuccess(true);
                result.setRawHtml(response.body());
                parseSearchResults(response.body(), result);
            } else {
                result.setSuccess(false);
                result.setErrorMessage("HTTP错误: " + response.statusCode());
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage("搜索失败: " + e.getMessage());
        }

        return result;
    }

    public SearchResult searchMobile(String keyword) {
        requestCount.incrementAndGet();
        SearchResult result = new SearchResult();
        result.setKeyword(keyword);

        try {
            String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
            String url = BAIDU_MOBILE_URL + "q?word=" + encodedKeyword + "&pn=0&rn=10";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", MOBILE_USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml")
                    .header("Accept-Language", "zh-CN,zh;q=0.9")
                    .GET()
                    .timeout(java.time.Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                result.setSuccess(true);
                result.setRawHtml(response.body());
                parseMobileSearchResults(response.body(), result);
            } else {
                result.setSuccess(false);
                result.setErrorMessage("HTTP错误: " + response.statusCode());
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage("搜索失败: " + e.getMessage());
        }

        return result;
    }

    private void parseSearchResults(String html, SearchResult result) {
        List<SearchResultItem> items = new ArrayList<>();

        try {
            Pattern resultPattern = Pattern.compile("<div class=\"result[^\"]*\".*?</div>\\s*</div>", Pattern.DOTALL);
            Matcher matcher = resultPattern.matcher(html);

            while (matcher.find() && items.size() < 10) {
                String itemHtml = matcher.group();

                SearchResultItem item = new SearchResultItem();

                String title = extractTitle(itemHtml);
                if (title != null && !title.isEmpty()) {
                    item.setTitle(title);

                    String url = extractUrl(itemHtml);
                    item.setUrl(url);

                    String description = extractDescription(itemHtml);
                    item.setDescription(description);

                    String source = extractSource(url);
                    item.setSource(source);

                    items.add(item);
                }
            }

            result.setItems(items);
            result.setTotalResults(items.size());

        } catch (Exception e) {
            System.err.println("解析搜索结果失败：" + e.getMessage());
        }
    }

    private void parseMobileSearchResults(String html, SearchResult result) {
        List<SearchResultItem> items = new ArrayList<>();

        try {
            Pattern resultPattern = Pattern.compile("<div class=\"c-container[^\"]*\".*?</div>\\s*</div>", Pattern.DOTALL);
            Matcher matcher = resultPattern.matcher(html);

            while (matcher.find() && items.size() < 10) {
                String itemHtml = matcher.group();

                SearchResultItem item = new SearchResultItem();

                String title = extractMobileTitle(itemHtml);
                if (title != null && !title.isEmpty()) {
                    item.setTitle(title);

                    String url = extractMobileUrl(itemHtml);
                    item.setUrl(url);

                    String description = extractMobileDescription(itemHtml);
                    item.setDescription(description);

                    item.setSource(extractSource(url));

                    items.add(item);
                }
            }

            result.setItems(items);
            result.setTotalResults(items.size());

        } catch (Exception e) {
            System.err.println("解析移动搜索结果失败：" + e.getMessage());
        }
    }

    private String extractTitle(String html) {
        try {
            Pattern pattern = Pattern.compile("<h3[^>]*>\\s*<a[^>]*>(.*?)</a>\\s*</h3>", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(html);
            if (matcher.find()) {
                return cleanHtml(matcher.group(1));
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    private String extractMobileTitle(String html) {
        try {
            Pattern pattern = Pattern.compile("<h3[^>]*class=\"[^\"]*c-title[^\"]*\"[^>]*>(.*?)</h3>", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(html);
            if (matcher.find()) {
                return cleanHtml(matcher.group(1));
            }

            pattern = Pattern.compile("<a[^>]*class=\"[^\"]*c-title[^\"]*\"[^>]*>(.*?)</a>", Pattern.DOTALL);
            matcher = pattern.matcher(html);
            if (matcher.find()) {
                return cleanHtml(matcher.group(1));
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    private String extractUrl(String html) {
        try {
            Pattern pattern = Pattern.compile("<a[^>]+href=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(html);
            if (matcher.find()) {
                String url = matcher.group(1);
                if (url.contains("baidu.com/link") || url.contains("www.baidu.com/link")) {
                    return resolveShortUrl(url);
                }
                return url;
            }
        } catch (Exception e) {
            // ignore
        }
        return "";
    }

    private String extractMobileUrl(String html) {
        try {
            Pattern pattern = Pattern.compile("href=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(html);
            if (matcher.find()) {
                String url = matcher.group(1);
                if (url.startsWith("/")) {
                    url = "https://www.baidu.com" + url;
                }
                if (url.contains("baidu.com/link")) {
                    return resolveShortUrl(url);
                }
                return url;
            }
        } catch (Exception e) {
            // ignore
        }
        return "";
    }

    private String extractDescription(String html) {
        try {
            Pattern pattern = Pattern.compile("<span class=\"[^\"]*c-span-last[^\"]*\"[^>]*>(.*?)</span>", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(html);
            if (matcher.find()) {
                return cleanHtml(matcher.group(1));
            }

            pattern = Pattern.compile("<div[^>]*class=\"[^\"]*c-abstract[^\"]*\"[^>]*>(.*?)</div>", Pattern.DOTALL);
            matcher = pattern.matcher(html);
            if (matcher.find()) {
                return cleanHtml(matcher.group(1));
            }
        } catch (Exception e) {
            // ignore
        }
        return "";
    }

    private String extractMobileDescription(String html) {
        try {
            Pattern pattern = Pattern.compile("<div[^>]*class=\"[^\"]*c-span-last[^\"]*\"[^>]*>(.*?)</div>", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(html);
            if (matcher.find()) {
                return cleanHtml(matcher.group(1));
            }

            pattern = Pattern.compile("<div[^>]*class=\"[^\"]*c-summary[^\"]*\"[^>]*>(.*?)</div>", Pattern.DOTALL);
            matcher = pattern.matcher(html);
            if (matcher.find()) {
                return cleanHtml(matcher.group(1));
            }
        } catch (Exception e) {
            // ignore
        }
        return "";
    }

    private String extractSource(String url) {
        if (url == null || url.isEmpty()) {
            return "百度搜索";
        }
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (host != null) {
                if (host.startsWith("www.")) {
                    host = host.substring(4);
                }
                return host;
            }
        } catch (Exception e) {
            // ignore
        }
        return "百度搜索";
    }

    private String resolveShortUrl(String shortUrl) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(shortUrl))
                    .header("User-Agent", PC_USER_AGENT)
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .timeout(java.time.Duration.ofSeconds(5))
                    .build();

            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());

            Optional<String> location = response.headers().firstValue("location");
            if (location.isPresent()) {
                return location.get();
            }
            location = response.headers().firstValue("Location");
            if (location.isPresent()) {
                return location.get();
            }
        } catch (Exception e) {
            // ignore
        }
        return shortUrl;
    }

    private String cleanHtml(String html) {
        if (html == null) return "";

        String text = html.replaceAll("<[^>]+>", "")
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

        return text.replaceAll("\\s{2,}", " ").trim();
    }

    public int getRequestCount() {
        return requestCount.get();
    }

    public static class SearchResult {
        private boolean success;
        private String keyword;
        private List<SearchResultItem> items;
        private int totalResults;
        private String rawHtml;
        private String errorMessage;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getKeyword() { return keyword; }
        public void setKeyword(String keyword) { this.keyword = keyword; }
        public List<SearchResultItem> getItems() { return items; }
        public void setItems(List<SearchResultItem> items) { this.items = items; }
        public int getTotalResults() { return totalResults; }
        public void setTotalResults(int totalResults) { this.totalResults = totalResults; }
        public String getRawHtml() { return rawHtml; }
        public void setRawHtml(String rawHtml) { this.rawHtml = rawHtml; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }

    public static class SearchResultItem {
        private String title;
        private String url;
        private String description;
        private String source;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
    }
}
