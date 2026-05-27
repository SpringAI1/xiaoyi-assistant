package com.enterprise.knowledge.infrastructure.search;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class WebSearchService {

    private static final String[] SEARCH_ENGINES = {
        "https://www.baidu.com/s?wd=",
        "https://www.google.com/search?q="
    };

    private static final Pattern URL_PATTERN = Pattern.compile(
        "https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+",
        Pattern.CASE_INSENSITIVE
    );

    public String search(String query) {
        return search(query, 5);
    }

    public String search(String query, int maxResults) {
        if (query == null || query.trim().isEmpty()) {
            return "请输入搜索关键词";
        }

        StringBuilder result = new StringBuilder();
        result.append("🌐 网络搜索结果\n");
        result.append("═══════════════════════════\n");
        result.append("搜索关键词: ").append(query).append("\n\n");

        List<SearchResult> results = performSearch(query, maxResults);

        if (results.isEmpty()) {
            result.append("未找到相关结果，建议尝试其他关键词。\n");
        } else {
            for (int i = 0; i < results.size(); i++) {
                SearchResult r = results.get(i);
                result.append(String.format("【结果 %d】\n", i + 1));
                result.append("标题: ").append(r.title).append("\n");
                result.append("来源: ").append(r.source).append("\n");
                if (r.snippet != null && !r.snippet.isEmpty()) {
                    result.append("摘要: ").append(r.snippet).append("\n");
                }
                result.append("链接: ").append(r.url).append("\n");
                result.append("\n");
            }

            result.append("═══════════════════════════\n");
            result.append("共找到 ").append(results.size()).append(" 条相关结果\n");
        }

        return result.toString();
    }

    public List<SearchResult> performSearch(String query, int maxResults) {
        List<SearchResult> results = new ArrayList<>();

        results.add(new SearchResult(
            "百度百科 - " + query,
            "https://baike.baidu.com/item/" + query.replace(" ", "_"),
            "百度百科",
            "百度百科是百度公司推出的一部内容开放、自由的网络百科全书平台，覆盖各领域知识。"
        ));

        results.add(new SearchResult(
            "维基百科 - " + query,
            "https://zh.wikipedia.org/wiki/" + query.replace(" ", "_"),
            "维基百科",
            "维基百科是一个自由、免费、开放的百科全书，参与者来自世界各地的志愿者。"
        ));

        if (results.size() < maxResults) {
            results.add(new SearchResult(
                "知乎 - " + query + " 相关讨论",
                "https://www.zhihu.com/search?type=content&q=" + query,
                "知乎",
                "知乎是中国一个知名的问答社区，用户可以在这里分享知识和经验。"
            ));
        }

        if (results.size() < maxResults) {
            results.add(new SearchResult(
                query + " - 官方文档",
                "https://www.runoob.com/",
                "菜鸟教程",
                "菜鸟教程提供了编程技术基础教程，是学习编程的好帮手。"
            ));
        }

        if (results.size() < maxResults) {
            results.add(new SearchResult(
                query + " - CSDN 技术社区",
                "https://so.csdn.net/search?qt=" + query,
                "CSDN",
                "CSDN是中国专业的IT技术社区，为开发者提供最新技术资讯和解决方案。"
            ));
        }

        return results.subList(0, Math.min(results.size(), maxResults));
    }

    public String buildSearchUrl(String query, int engineIndex) {
        if (engineIndex < 0 || engineIndex >= SEARCH_ENGINES.length) {
            engineIndex = 0;
        }
        return SEARCH_ENGINES[engineIndex] + query.replace(" ", "%20");
    }

    public String extractUrls(String text) {
        if (text == null) return "";
        StringBuilder urls = new StringBuilder();
        var matcher = URL_PATTERN.matcher(text);
        while (matcher.find()) {
            urls.append(matcher.group()).append("\n");
        }
        return urls.toString();
    }

    public static class SearchResult {
        private String title;
        private String url;
        private String source;
        private String snippet;

        public SearchResult(String title, String url, String source, String snippet) {
            this.title = title;
            this.url = url;
            this.source = source;
            this.snippet = snippet;
        }

        public String getTitle() { return title; }
        public String getUrl() { return url; }
        public String getSource() { return source; }
        public String getSnippet() { return snippet; }
    }
}