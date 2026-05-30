package com.enterprise.knowledge.infrastructure.agent.tool;

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
public class WebSearchTool {

    private static final String NETEASE_SEARCH_API = "https://music.163.com/api/search/get";
    private static final String NETEASE_LYRIC_API = "https://music.163.com/api/song/lyric";
    
    private static final List<String> MUSIC_KEYWORDS = Arrays.asList(
            "歌词", "歌曲", "歌", "唱", "音乐", "旋律", "节奏", "专辑", "单曲",
            "听", "播放", "翻唱", "原唱", "伴奏", "MV", "现场", "演唱会"
    );

    public String search(String query) {
        try {
            if (isMusicQuery(query)) {
                String result = searchMusicLyrics(query);
                if (result != null) {
                    return result;
                }
            }
            
            String webResult = performRealWebSearch(query);
            if (webResult != null && !webResult.isEmpty()) {
                return webResult;
            }
            
            return generateSmartAnswer(query);
            
        } catch (Exception e) {
            return generateSmartAnswer(query);
        }
    }

    private boolean isMusicQuery(String query) {
        if (query == null || query.isEmpty()) {
            return false;
        }
        
        String lowerQuery = query.toLowerCase();
        
        for (String keyword : MUSIC_KEYWORDS) {
            if (lowerQuery.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        
        String[] artists = {"周杰伦", "邓紫棋", "薛之谦", "李荣浩", "林俊杰", "陈奕迅", "张杰", "许嵩", "华晨宇", "王菲", "孙燕姿", "王力宏", "陶喆", "蔡依林", "S.H.E", "五月天", "Beyond", "朴树", "许巍", "汪峰"};
        for (String artist : artists) {
            if (query.contains(artist)) {
                return true;
            }
        }
        
        return false;
    }

    private String searchMusicLyrics(String query) {
        try {
            String artist = extractArtist(query);
            String song = extractSong(query);
            
            if (artist == null && song.isEmpty()) {
                return null;
            }
            
            String searchKeyword = song;
            if (artist != null) {
                searchKeyword = artist + " " + song;
            }
            
            String songId = searchSongId(searchKeyword);
            if (songId == null) {
                return generateArtistSuggestion(artist, song);
            }
            
            String lyrics = fetchLyrics(songId);
            if (lyrics != null && !lyrics.isEmpty()) {
                return formatLyricsResponse(artist, song, lyrics);
            }
            
            return generateArtistSuggestion(artist, song);
            
        } catch (Exception e) {
            return generateSmartAnswer(query);
        }
    }

    private String searchSongId(String keyword) {
        try {
            String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
            String url = NETEASE_SEARCH_API + "?s=" + encodedKeyword + "&type=1&offset=0&limit=10";
            
            String response = fetchUrl(url);
            if (response == null) {
                return null;
            }
            
            Pattern pattern = Pattern.compile("\"id\":(\\d+)");
            Matcher matcher = pattern.matcher(response);
            
            if (matcher.find()) {
                return matcher.group(1);
            }
            
        } catch (Exception e) {
            // 忽略错误
        }
        return null;
    }

    private String fetchLyrics(String songId) {
        try {
            String url = NETEASE_LYRIC_API + "?id=" + songId + "&lv=-1&kv=-1&tv=-1";
            
            String response = fetchUrl(url);
            if (response == null) {
                return null;
            }
            
            Pattern lyricPattern = Pattern.compile("\"lyric\":\"([^\"]+)\"");
            Matcher matcher = lyricPattern.matcher(response);
            
            if (matcher.find()) {
                String lyrics = matcher.group(1);
                return decodeUnicode(lyrics);
            }
            
            Pattern klyricPattern = Pattern.compile("\"klyric\":\"([^\"]+)\"");
            matcher = klyricPattern.matcher(response);
            
            if (matcher.find()) {
                String lyrics = matcher.group(1);
                return decodeUnicode(lyrics);
            }
            
        } catch (Exception e) {
            // 忽略错误
        }
        return null;
    }

    private String decodeUnicode(String input) {
        if (input == null) {
            return null;
        }
        
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < input.length()) {
            if (input.charAt(i) == '\\' && i + 1 < input.length() && input.charAt(i + 1) == 'u') {
                String hex = input.substring(i + 2, i + 6);
                try {
                    sb.append((char) Integer.parseInt(hex, 16));
                    i += 6;
                } catch (NumberFormatException e) {
                    sb.append(input.charAt(i));
                    i++;
                }
            } else {
                sb.append(input.charAt(i));
                i++;
            }
        }
        
        return sb.toString();
    }

    private String formatLyricsResponse(String artist, String song, String lyrics) {
        StringBuilder result = new StringBuilder();
        result.append("🎵 【网易云音乐 - 实时获取】\n\n");
        
        if (artist != null) {
            result.append("《").append(song).append("》 - ").append(artist).append("\n\n");
        } else {
            result.append("《").append(song).append("》\n\n");
        }
        
        String[] lines = lyrics.split("\n");
        for (String line : lines) {
            String cleanLine = line.trim();
            if (!cleanLine.isEmpty() && !cleanLine.matches("\\[\\d+:\\d+\\.?\\d*\\]")) {
                result.append(cleanLine).append("\n");
            }
        }
        
        result.append("\n💡 来源：网易云音乐");
        
        return result.toString();
    }

    private String generateArtistSuggestion(String artist, String song) {
        StringBuilder result = new StringBuilder();
        
        if (artist != null && !song.isEmpty()) {
            result.append("🎵 正在搜索 ").append(artist).append("《").append(song).append("》...\n\n");
        } else if (artist != null) {
            result.append("🎵 ").append(artist).append(" 的热门歌曲：\n\n");
            result.append(getArtistPopularSongs(artist));
            return result.toString();
        } else if (!song.isEmpty()) {
            result.append("🎵 正在搜索《").append(song).append("》...\n\n");
        }
        
        result.append("💡 提示：您可以在以下平台查看完整歌词：\n");
        result.append("- 网易云音乐：https://music.163.com/\n");
        result.append("- QQ音乐：https://y.qq.com/\n");
        result.append("- 酷狗音乐：https://www.kugou.com/");
        
        return result.toString();
    }

    private String getArtistPopularSongs(String artist) {
        Map<String, List<String>> artistSongs = new HashMap<>();
        artistSongs.put("周杰伦", Arrays.asList("晴天", "七里香", "稻香", "告白气球", "青花瓷", "夜曲"));
        artistSongs.put("邓紫棋", Arrays.asList("泡沫", "光年之外", "来自天堂的魔鬼", "再见", "多远都要在一起"));
        artistSongs.put("薛之谦", Arrays.asList("演员", "绅士", "认真的雪", "丑八怪", "意外"));
        artistSongs.put("李荣浩", Arrays.asList("李白", "模特", "年少有为", "戒烟", "爸爸妈妈"));
        artistSongs.put("林俊杰", Arrays.asList("江南", "可惜没如果", "修炼爱情", "背对背拥抱", "曹操"));
        artistSongs.put("陈奕迅", Arrays.asList("十年", "浮夸", "富士山下", "K歌之王", "稳稳的幸福"));
        artistSongs.put("张杰", Arrays.asList("逆战", "这就是爱", "天下", "我们都一样"));
        artistSongs.put("许嵩", Arrays.asList("素颜", "有何不可", "断桥残雪", "玫瑰花的葬礼"));
        artistSongs.put("华晨宇", Arrays.asList("烟火里的尘埃", "齐天", "寒鸦少年", "好想爱这个世界啊"));
        
        List<String> songs = artistSongs.get(artist);
        if (songs != null) {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < songs.size(); i++) {
                result.append(i + 1).append(". ").append(songs.get(i)).append("\n");
            }
            result.append("\n💡 你可以说：\"").append(artist).append(songs.get(0)).append("的歌词\"来获取具体歌曲歌词！");
            return result.toString();
        }
        
        return "";
    }

    private String performRealWebSearch(String query) {
        StringBuilder result = new StringBuilder();
        result.append("## 搜索结果\n\n");
        result.append("**关键词**：").append(query).append("\n\n");
        
        List<SearchResult> results = new ArrayList<>();
        
        results.addAll(searchBing(query));
        results.addAll(searchBaidu(query));
        
        if (results.isEmpty()) {
            return null;
        }
        
        int count = 0;
        for (SearchResult item : results) {
            if (count >= 3) break;
            
            result.append("### ").append(count + 1).append(". ").append(item.title).append("\n\n");
            
            if (item.content != null && !item.content.isEmpty()) {
                String cleanContent = cleanHtml(item.content);
                if (cleanContent.length() > 250) {
                    cleanContent = cleanContent.substring(0, 250) + "...";
                }
                result.append(cleanContent).append("\n\n");
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

    private List<SearchResult> searchBing(String query) {
        List<SearchResult> results = new ArrayList<>();
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = "https://www.bing.com/search?q=" + encodedQuery;
            
            String html = fetchUrl(url);
            if (html != null) {
                results = parseBingResults(html);
            }
        } catch (Exception e) {
            // 忽略错误
        }
        return results;
    }

    private List<SearchResult> searchBaidu(String query) {
        List<SearchResult> results = new ArrayList<>();
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = "https://www.baidu.com/s?wd=" + encodedQuery;
            
            String html = fetchUrl(url);
            if (html != null) {
                results = parseBaiduResults(html);
            }
        } catch (Exception e) {
            // 忽略错误
        }
        return results;
    }

    private List<SearchResult> parseBingResults(String html) {
        List<SearchResult> results = new ArrayList<>();
        
        Pattern resultPattern = Pattern.compile("<li class=\"b_algo\".*?</li>", Pattern.DOTALL);
        Matcher matcher = resultPattern.matcher(html);
        
        while (matcher.find()) {
            String item = matcher.group();
            
            String title = extractBetweenTags(item, "<h2>", "</h2>");
            String url = extractLink(item);
            String snippet = extractBetweenTags(item, "<p>", "</p>");
            
            if (!title.isEmpty()) {
                results.add(new SearchResult(cleanHtml(title), url, cleanHtml(snippet), "必应搜索"));
            }
        }
        
        return results;
    }

    private List<SearchResult> parseBaiduResults(String html) {
        List<SearchResult> results = new ArrayList<>();
        
        Pattern resultPattern = Pattern.compile("<div class=\"result-op c-container.*?</div>", Pattern.DOTALL);
        Matcher matcher = resultPattern.matcher(html);
        
        while (matcher.find()) {
            String item = matcher.group();
            
            String title = extractBetweenTags(item, "<h3", "</h3>");
            String url = extractLink(item);
            String snippet = extractBetweenTags(item, "<span class=\"c-abstract\">", "</span>");
            
            if (title.isEmpty()) {
                title = extractBetweenTags(item, "<h3 class=\"t\">", "</h3>");
            }
            
            if (!title.isEmpty()) {
                results.add(new SearchResult(cleanHtml(title), url, cleanHtml(snippet), "百度搜索"));
            }
        }
        
        return results;
    }

    private String generateSmartAnswer(String query) {
        String artist = extractArtist(query);
        String song = extractSong(query);
        
        if (artist != null || !song.isEmpty()) {
            return generateArtistSuggestion(artist, song);
        }
        
        return "📡 正在为您搜索相关信息...\n\n" +
               "💡 提示：请告诉我具体的歌手和歌曲名，例如：\"周杰伦晴天的歌词\"";
    }

    private String fetchUrl(String url) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/json")
                    .header("Referer", "https://music.163.com/")
                    .timeout(java.time.Duration.ofSeconds(10))
                    .GET()
                    .build();
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                return response.body();
            }
        } catch (Exception e) {
            // 忽略网络错误
        }
        return null;
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
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0")
                    .timeout(java.time.Duration.ofSeconds(5))
                    .GET()
                    .build();
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
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

    private String extractArtist(String query) {
        String[] artists = {"周杰伦", "邓紫棋", "薛之谦", "李荣浩", "林俊杰", "陈奕迅", "张杰", "许嵩", "华晨宇", "王菲", "孙燕姿", "王力宏", "陶喆", "蔡依林", "S.H.E", "五月天", "Beyond", "朴树", "许巍", "汪峰", "张韶涵", "梁静茹", "刘若英", "任贤齐", "周华健", "费玉清", "张信哲", "刘德华", "张学友", "郭富城", "黎明"};
        for (String artist : artists) {
            if (query.contains(artist)) {
                return artist;
            }
        }
        return null;
    }

    private String extractSong(String query) {
        String clean = query.replace("歌词", "").replace("歌曲", "").replace("歌", "").trim();
        String artist = extractArtist(query);
        if (artist != null) {
            clean = clean.replace(artist, "").trim();
        }
        return clean;
    }

    public List<String> getTopicInformation(String topic) {
        List<String> info = new ArrayList<>();
        info.add("主题概述：关于" + topic + "的基本介绍");
        return info;
    }

    private static class SearchResult {
        String title;
        String url;
        String content;
        String source;

        SearchResult(String title, String url, String content, String source) {
            this.title = title;
            this.url = url;
            this.content = content;
            this.source = source;
        }
    }
}
