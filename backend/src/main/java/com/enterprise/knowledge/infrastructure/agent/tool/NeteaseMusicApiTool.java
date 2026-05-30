package com.enterprise.knowledge.infrastructure.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class NeteaseMusicApiTool {

    private static final String SEARCH_URL = "https://music.163.com/api/search/get";
    private static final String LYRIC_URL = "https://music.163.com/api/song/lyric";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AtomicInteger requestCount = new AtomicInteger(0);

    public NeteaseMusicApiTool() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public MusicSearchResult searchMusic(String keyword, int limit) {
        requestCount.incrementAndGet();
        MusicSearchResult result = new MusicSearchResult();
        result.setKeyword(keyword);

        try {
            String formData = "s=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8) +
                            "&type=1&limit=" + limit + "&offset=0";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SEARCH_URL))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Referer", "https://music.163.com/")
                    .header("Cookie", "appver=8.9.70; os=pc; osver=Microsoft-Windows-10-Professional-build-64bit-19041; channel=pc; WEVNSM=1.0.0")
                    .POST(HttpRequest.BodyPublishers.ofString(formData))
                    .timeout(java.time.Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                result.setSuccess(true);
                result.setRawJson(response.body());
                result.setSongs(parseSearchResults(response.body()));
            } else {
                result.setSuccess(false);
                result.setErrorMessage("HTTP错误: " + response.statusCode());
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage("请求失败: " + e.getMessage());
        }

        return result;
    }

    public LyricResult getLyric(long songId) {
        requestCount.incrementAndGet();
        LyricResult result = new LyricResult();
        result.setSongId(songId);

        try {
            String url = LYRIC_URL + "?id=" + songId + "&lv=1&kv=1&tv=-1";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .header("Referer", "https://music.163.com/")
                    .header("Cookie", "appver=8.9.70; os=pc; osver=Microsoft-Windows-10-Professional-build-64bit-19041; channel=pc; WEVNSM=1.0.0")
                    .GET()
                    .timeout(java.time.Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                result.setSuccess(true);
                result.setRawJson(response.body());
                parseLyricResult(response.body(), result);
            } else {
                result.setSuccess(false);
                result.setErrorMessage("HTTP错误: " + response.statusCode());
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage("获取歌词失败: " + e.getMessage());
        }

        return result;
    }

    private List<SongInfo> parseSearchResults(String json) {
        List<SongInfo> songs = new ArrayList<>();

        try {
            JsonNode rootNode = objectMapper.readTree(json);
            JsonNode resultNode = rootNode.path("result");

            if (resultNode.has("songs")) {
                JsonNode songsArray = resultNode.get("songs");

                if (songsArray != null && songsArray.isArray()) {
                    for (JsonNode songNode : songsArray) {
                        if (songs.size() >= 10) break;

                        SongInfo song = new SongInfo();
                        song.setId(songNode.has("id") ? songNode.get("id").asLong() : 0);
                        song.setName(songNode.has("name") ? songNode.get("name").asText() : "");

                        if (songNode.has("artists")) {
                            JsonNode artistsNode = songNode.get("artists");
                            if (artistsNode != null && artistsNode.isArray() && artistsNode.size() > 0) {
                                song.setArtist(artistsNode.get(0).has("name") ? artistsNode.get(0).get("name").asText() : "");
                            }
                        }

                        if (songNode.has("album")) {
                            JsonNode albumNode = songNode.get("album");
                            if (albumNode != null && albumNode.has("name")) {
                                song.setAlbum(albumNode.get("name").asText());
                            }
                        }

                        if (song.getId() > 0 && song.getName() != null && !song.getName().isEmpty()) {
                            songs.add(song);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("解析搜索结果失败：" + e.getMessage());
        }

        return songs;
    }

    private void parseLyricResult(String json, LyricResult result) {
        try {
            JsonNode rootNode = objectMapper.readTree(json);

            if (rootNode.has("lrc") && rootNode.get("lrc").has("lyric")) {
                String lyric = rootNode.get("lrc").get("lyric").asText();
                lyric = lyric.replace("\\n", "\n").replace("\\r", "");
                result.setLyric(lyric);
                result.setHasLyric(true);
            }

            if (rootNode.has("tlyric") && rootNode.get("tlyric").has("lyric")) {
                String tlyric = rootNode.get("tlyric").get("lyric").asText();
                tlyric = tlyric.replace("\\n", "\n").replace("\\r", "");
                result.setTranslation(tlyric);
            }
        } catch (Exception e) {
            System.err.println("解析歌词失败：" + e.getMessage());
        }
    }

    public int getRequestCount() {
        return requestCount.get();
    }

    public static class MusicSearchResult {
        private boolean success;
        private String keyword;
        private List<SongInfo> songs;
        private String rawJson;
        private String errorMessage;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getKeyword() { return keyword; }
        public void setKeyword(String keyword) { this.keyword = keyword; }
        public List<SongInfo> getSongs() { return songs; }
        public void setSongs(List<SongInfo> songs) { this.songs = songs; }
        public String getRawJson() { return rawJson; }
        public void setRawJson(String rawJson) { this.rawJson = rawJson; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }

    public static class LyricResult {
        private boolean success;
        private long songId;
        private String lyric;
        private String translation;
        private boolean hasLyric;
        private String rawJson;
        private String errorMessage;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public long getSongId() { return songId; }
        public void setSongId(long songId) { this.songId = songId; }
        public String getLyric() { return lyric; }
        public void setLyric(String lyric) { this.lyric = lyric; }
        public String getTranslation() { return translation; }
        public void setTranslation(String translation) { this.translation = translation; }
        public boolean isHasLyric() { return hasLyric; }
        public void setHasLyric(boolean hasLyric) { this.hasLyric = hasLyric; }
        public String getRawJson() { return rawJson; }
        public void setRawJson(String rawJson) { this.rawJson = rawJson; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }

    public static class SongInfo {
        private long id;
        private String name;
        private String artist;
        private String album;
        private int duration;

        public long getId() { return id; }
        public void setId(long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getArtist() { return artist; }
        public void setArtist(String artist) { this.artist = artist; }
        public String getAlbum() { return album; }
        public void setAlbum(String album) { this.album = album; }
        public int getDuration() { return duration; }
        public void setDuration(int duration) { this.duration = duration; }
    }
}
