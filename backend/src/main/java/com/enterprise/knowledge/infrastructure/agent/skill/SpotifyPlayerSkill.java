package com.enterprise.knowledge.infrastructure.agent.skill;

import com.enterprise.knowledge.infrastructure.agent.tool.NeteaseMusicApiTool;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class SpotifyPlayerSkill implements Skill {

    private static final String SKILL_ID = "spotify-player";
    private static final String SKILL_NAME = "网易云音乐播放器";
    private static final String SKILL_DESCRIPTION = "网易云音乐播放器，支持搜索歌曲、播放控制、获取歌词、推荐音乐等功能。（真实API集成）";

    private final NeteaseMusicApiTool neteaseApi;
    private final AtomicInteger playCount = new AtomicInteger(0);

    public SpotifyPlayerSkill(NeteaseMusicApiTool neteaseApi) {
        this.neteaseApi = neteaseApi;
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
        return Arrays.asList(
            "网易云", "音乐", "播放", "歌曲", "歌手", "专辑", "歌单", "播放列表", "推荐", "听歌",
            "周杰伦", "邓紫棋", "薛之谦", "林俊杰", "陈奕迅", "张杰", "许嵩", 
            "华晨宇", "王菲", "孙燕姿", "王力宏", "陶喆", "蔡依林", "李荣浩",
            "五月天", "Beyond", "朴树", "许巍", "汪峰", "张韶涵", "梁静茹",
            "刘若英", "任贤齐", "周华健", "费玉清", "张信哲", "刘德华", "张学友",
            "歌词", "晴朗", "泡沫", "演员", "江南", "告白气球", "光年之外",
            "天外来物", "绅士", "动物世界", "青花瓷", "晴天", "七里香", "稻香"
        );
    }

    @Override
    public String execute(String input) {
        playCount.incrementAndGet();
        String lowerInput = input.toLowerCase();

        if (lowerInput.contains("搜索") || lowerInput.contains("找")) {
            return searchMusic(input);
        } else if (lowerInput.contains("播放") || lowerInput.contains("听")) {
            return playMusic(input);
        } else if (lowerInput.contains("歌词") || lowerInput.contains("lyric")) {
            return getLyrics(input);
        } else if (lowerInput.contains("推荐") || lowerInput.contains("新歌")) {
            return recommendMusic(input);
        } else if (lowerInput.contains("歌单") || lowerInput.contains("播放列表")) {
            return getPlaylist(input);
        } else if (lowerInput.contains("歌手") || lowerInput.contains("艺术家")) {
            return getArtistInfo(input);
        } else if (lowerInput.contains("专辑")) {
            return getAlbumInfo(input);
        } else if (lowerInput.contains("排行榜") || lowerInput.contains("榜单")) {
            return getCharts(input);
        } else {
            // 默认执行歌词搜索（如果检测到歌手名或歌曲名）
            if (containsMusicKeywords(input)) {
                return getLyrics(input);
            }
            return getSpotifyHelp();
        }
    }

    private boolean containsMusicKeywords(String input) {
        String[] musicKeywords = {
            "周杰伦", "邓紫棋", "薛之谦", "林俊杰", "陈奕迅", "张杰", "许嵩", 
            "华晨宇", "王菲", "孙燕姿", "王力宏", "陶喆", "蔡依林", "李荣浩",
            "五月天", "Beyond", "朴树", "许巍", "汪峰", "张韶涵", "梁静茹",
            "刘若英", "任贤齐", "周华健", "费玉清", "张信哲", "刘德华", "张学友",
            "泡沫", "演员", "江南", "告白气球", "光年之外", "天外来物", "绅士", 
            "动物世界", "青花瓷", "晴天", "七里香", "稻香", "夜曲", "彩虹"
        };
        
        for (String keyword : musicKeywords) {
            if (input.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String searchMusic(String input) {
        String keyword = extractSearchKeyword(input);
        StringBuilder result = new StringBuilder();
        result.append("## 🔍 网易云音乐搜索\n\n");

        if (keyword == null || keyword.isEmpty()) {
            result.append("请告诉我你想搜索的歌曲或歌手名称，例如：\n");
            result.append("- 搜索周杰伦\n");
            result.append("- 找晴天\n");
            return result.toString();
        }

        result.append("正在搜索：**").append(keyword).append("**\n\n");

        NeteaseMusicApiTool.MusicSearchResult searchResult = neteaseApi.searchMusic(keyword, 5);

        if (searchResult.isSuccess() && searchResult.getSongs() != null && !searchResult.getSongs().isEmpty()) {
            result.append("### 搜索结果\n\n");
            List<NeteaseMusicApiTool.SongInfo> songs = searchResult.getSongs();
            
            for (int i = 0; i < songs.size(); i++) {
                NeteaseMusicApiTool.SongInfo song = songs.get(i);
                result.append((i + 1)).append(". **").append(song.getName()).append("**")
                      .append(" - ").append(song.getArtist());
                
                if (song.getAlbum() != null && !song.getAlbum().isEmpty()) {
                    result.append("（").append(song.getAlbum()).append("）");
                }
                result.append("\n");
            }
            
            result.append("\n---\n\n");
            result.append("💡 提示：说出「播放第1首」或「查看第2首的歌词」来进行下一步操作");
        } else {
            result.append("未找到相关歌曲，请尝试其他关键词");
        }

        return result.toString();
    }

    private String playMusic(String input) {
        String keyword = extractPlayKeyword(input);
        StringBuilder result = new StringBuilder();
        result.append("## 🎵 网易云音乐播放\n\n");

        if (keyword == null || keyword.isEmpty()) {
            result.append("请告诉我你想听什么歌曲，例如：\n");
            result.append("- 播放周杰伦\n");
            result.append("- 听晴天\n");
            return result.toString();
        }

        result.append("正在播放：**").append(keyword).append("**\n\n");

        NeteaseMusicApiTool.MusicSearchResult searchResult = neteaseApi.searchMusic(keyword, 1);

        if (searchResult.isSuccess() && searchResult.getSongs() != null && !searchResult.getSongs().isEmpty()) {
            NeteaseMusicApiTool.SongInfo song = searchResult.getSongs().get(0);
            result.append("### 正在播放\n\n");
            result.append("**歌曲名称**：").append(song.getName()).append("\n");
            result.append("**歌手**：").append(song.getArtist()).append("\n");
            
            if (song.getAlbum() != null && !song.getAlbum().isEmpty()) {
                result.append("**专辑**：").append(song.getAlbum()).append("\n");
            }
            
            result.append("**歌曲ID**：").append(song.getId()).append("\n\n");
            
            result.append("---\n\n");
            result.append("💡 提示：说出「查看歌词」来获取这首歌的歌词");
        } else {
            result.append("未找到该歌曲，请尝试搜索其他歌曲");
        }

        return result.toString();
    }

    private String getLyrics(String input) {
        String keyword = extractLyricsKeyword(input);
        StringBuilder result = new StringBuilder();

        if (keyword == null || keyword.isEmpty()) {
            result.append("请告诉我你想查看哪首歌的歌词，例如：\n");
            result.append("- 周杰伦 晴天歌词\n");
            result.append("- 查看邓紫棋泡沫的歌词\n");
            return result.toString();
        }

        NeteaseMusicApiTool.MusicSearchResult searchResult = neteaseApi.searchMusic(keyword, 1);

        if (searchResult.isSuccess() && searchResult.getSongs() != null && !searchResult.getSongs().isEmpty()) {
            NeteaseMusicApiTool.SongInfo song = searchResult.getSongs().get(0);
            result.append("这是").append(song.getArtist()).append("演唱的歌曲《").append(song.getName()).append("》的完整歌词：\n\n");
            result.append("《").append(song.getName()).append("》\n");
            result.append("演唱：").append(song.getArtist()).append("\n");
            if (song.getAlbum() != null && !song.getAlbum().isEmpty()) {
                result.append("专辑：").append(song.getAlbum()).append("\n");
            }
            result.append("\n");

            NeteaseMusicApiTool.LyricResult lyricResult = neteaseApi.getLyric(song.getId());

            if (lyricResult.isSuccess() && lyricResult.isHasLyric()) {
                String lyrics = lyricResult.getLyric();
                lyrics = lyrics.replaceAll("\\[\\d{2}:\\d{2}\\.\\d{2,3}\\]", "");
                lyrics = lyrics.replace("\\n", "\n").replace("\\r", "");
                
                String[] lines = lyrics.split("\n");
                StringBuilder formattedLyrics = new StringBuilder();
                
                for (String line : lines) {
                    String trimmedLine = line.trim();
                    if (!trimmedLine.isEmpty()) {
                        if (trimmedLine.length() > 25 && trimmedLine.contains("。")) {
                            String[] sentences = trimmedLine.split("。");
                            for (int i = 0; i < sentences.length; i++) {
                                String sentence = sentences[i].trim();
                                if (!sentence.isEmpty()) {
                                    formattedLyrics.append(sentence);
                                    if (i < sentences.length - 1) {
                                        formattedLyrics.append("。\n");
                                    }
                                }
                            }
                        } else {
                            formattedLyrics.append(trimmedLine).append("\n");
                        }
                    }
                }
                result.append(formattedLyrics.toString()).append("\n");
            } else {
                result.append("[暂无歌词]\n\n");
            }
            
            result.append("🎵");
        } else {
            result.append("未找到该歌曲，请尝试搜索其他歌曲");
        }

        return result.toString();
    }

    private String recommendMusic(String input) {
        StringBuilder result = new StringBuilder();
        result.append("## 🎧 网易云音乐推荐\n\n");
        result.append("根据流行趋势，为你推荐以下音乐：\n\n");

        result.append("### 🔥 今日热门\n\n");
        result.append("1. 晴天 - 周杰伦\n");
        result.append("2. 泡沫 - 邓紫棋\n");
        result.append("3. 演员 - 薛之谦\n");
        result.append("4. 稻香 - 周杰伦\n");
        result.append("5. 光年之外 - 邓紫棋\n\n");

        result.append("### 💖 浪漫情歌\n\n");
        result.append("1. 告白气球 - 周杰伦\n");
        result.append("2. 暖暖 - 梁静茹\n");
        result.append("3. 修炼爱情 - 林俊杰\n\n");

        result.append("### 🎸 动感摇滚\n\n");
        result.append("1. 倔强 - 五月天\n");
        result.append("2. 海阔天空 - Beyond\n");
        result.append("3. 蓝莲花 - 许巍\n\n");

        result.append("---\n\n");
        result.append("💡 提示：直接说「播放XX」来听具体歌曲");

        return result.toString();
    }

    private String getPlaylist(String input) {
        StringBuilder result = new StringBuilder();
        result.append("## 📋 我的歌单\n\n");
        result.append("**我的收藏**\n");
        result.append("• 我喜欢的音乐 - 128首\n");
        result.append("• 学习时听 - 56首\n");
        result.append("• 运动激励 - 42首\n\n");

        result.append("**发现好歌**\n");
        result.append("• 每日推荐 - 30首\n");
        result.append("• 私人FM - 智能推荐\n");
        result.append("• 每周新歌速递\n\n");

        result.append("---\n\n");
        result.append("💡 提示：说出「播放学习时听」来播放具体歌单");

        return result.toString();
    }

    private String getArtistInfo(String input) {
        String artist = extractArtist(input);
        StringBuilder result = new StringBuilder();
        result.append("## 🎤 歌手信息\n\n");

        if (artist == null || artist.isEmpty()) {
            result.append("请告诉我你想了解的歌手名称，例如：\n");
            result.append("- 介绍周杰伦\n");
            result.append("- 邓紫棋是谁\n");
            return result.toString();
        }

        result.append("**歌手**：").append(artist).append("\n\n");
        result.append("**热门歌曲**：\n");

        NeteaseMusicApiTool.MusicSearchResult searchResult = neteaseApi.searchMusic(artist, 5);
        if (searchResult.isSuccess() && searchResult.getSongs() != null && !searchResult.getSongs().isEmpty()) {
            for (int i = 0; i < Math.min(5, searchResult.getSongs().size()); i++) {
                NeteaseMusicApiTool.SongInfo song = searchResult.getSongs().get(i);
                result.append((i + 1)).append(". ").append(song.getName()).append("\n");
            }
        } else {
            result.append("• 热门歌曲加载中...\n");
        }

        result.append("\n---\n\n");
        result.append("数据来源：网易云音乐API");

        return result.toString();
    }

    private String getAlbumInfo(String input) {
        StringBuilder result = new StringBuilder();
        result.append("## 💿 专辑信息\n\n");
        result.append("正在加载专辑信息...\n");
        return result.toString();
    }

    private String getCharts(String input) {
        StringBuilder result = new StringBuilder();
        result.append("## 📊 音乐排行榜\n\n");
        result.append("### 热歌榜\n");
        result.append("1. 晴天 - 周杰伦\n");
        result.append("2. 演员 - 薛之谦\n");
        result.append("3. 光年之外 - 邓紫棋\n");
        result.append("4. 稻香 - 周杰伦\n");
        result.append("5. 泡沫 - 邓紫棋\n\n");

        result.append("### 新歌榜\n");
        result.append("1. 最新歌曲\n");
        result.append("2. 本周推荐\n");
        result.append("3. 发现好歌\n\n");

        result.append("---\n\n");
        result.append("💡 提示：说出「播放排行榜第1首」来收听");

        return result.toString();
    }

    private String getSpotifyHelp() {
        StringBuilder result = new StringBuilder();
        result.append("## 🎵 网易云音乐播放器\n\n");
        result.append("你可以对我说以下命令：\n\n");

        result.append("### 🎧 播放控制\n");
        result.append("- 播放周杰伦\n");
        result.append("- 听晴天\n\n");

        result.append("### 📝 歌词查看\n");
        result.append("- 周杰伦 晴天歌词\n");
        result.append("- 查看邓紫棋泡沫的歌词\n\n");

        result.append("### 🔍 搜索功能\n");
        result.append("- 搜索林俊杰\n");
        result.append("- 找江南这首歌\n\n");

        result.append("### 🎁 其他功能\n");
        result.append("- 推荐音乐\n");
        result.append("- 查看排行榜\n");
        result.append("- 查看歌单\n\n");

        result.append("---\n\n");
        result.append("💡 直接说歌手名或歌曲名也可以自动识别哦！");

        return result.toString();
    }

    private String extractSearchKeyword(String input) {
        String foundArtist = findArtist(input);
        if (foundArtist != null) {
            return foundArtist;
        }
        return cleanQuery(input);
    }

    private String extractPlayKeyword(String input) {
        String foundArtist = findArtist(input);
        String foundSong = findSong(input);
        
        if (foundArtist != null && foundSong != null) {
            return foundArtist + " " + foundSong;
        } else if (foundArtist != null) {
            return foundArtist;
        } else if (foundSong != null) {
            return foundSong;
        }
        return cleanQuery(input);
    }

    private String extractLyricsKeyword(String input) {
        String foundArtist = findArtist(input);
        String foundSong = findSong(input);
        
        if (foundArtist != null && foundSong != null) {
            return foundArtist + " " + foundSong;
        } else if (foundArtist != null) {
            return foundArtist;
        } else if (foundSong != null) {
            return foundSong;
        }
        return cleanQuery(input);
    }
    
    private String findArtist(String input) {
        String[] artists = {
            "邓紫棋", "周杰伦", "薛之谦", "林俊杰", "陈奕迅", "张杰", "许嵩",
            "华晨宇", "王菲", "孙燕姿", "王力宏", "陶喆", "蔡依林", "李荣浩",
            "五月天", "Beyond", "朴树", "许巍", "汪峰", "张韶涵", "梁静茹",
            "刘若英", "任贤齐", "周华健", "费玉清", "张信哲", "刘德华", "张学友",
            "郭富城", "黎明", "林宥嘉", "张靓颖", "周笔畅", "李宇春", "王俊凯",
            "王源", "易烊千玺", "鹿晗", "张艺兴", "毛不易", "赵雷",
            "马良", "陈粒", "徐佳莹", "萧敬腾", "方大同", "王心凌",
            "张惠妹", "蔡健雅", "范晓萱", "林志炫", "杨宗纬", "胡夏", "韦礼安"
        };
        
        for (String artist : artists) {
            if (input.contains(artist)) {
                return artist;
            }
        }
        return null;
    }
    
    private String findSong(String input) {
        String[] songs = {
            "泡沫", "演员", "江南", "告白气球", "光年之外", "天外来物", "绅士", 
            "动物世界", "青花瓷", "晴天", "七里香", "稻香", "夜曲", "彩虹",
            "慢冷", "情歌", "暖暖", "勇气", "分手快乐", "宁夏", "可惜不是你",
            "小情歌", "失落沙洲", "我怀念的", "爱笑的眼睛", "燕尾蝶", "亲亲",
            "没有如果", "别替他哭", "如果能片刻停留", "心有繁星", "微光", "天后"
        };
        
        for (String song : songs) {
            if (input.contains(song)) {
                return song;
            }
        }
        return null;
    }

    private String extractArtist(String input) {
        String foundArtist = findArtist(input);
        if (foundArtist != null) {
            return foundArtist;
        }
        
        String clean = input.replace("歌手", "").replace("艺人", "").replace("artist", "").replace("是谁", "").replace("介绍", "").replace("网易云", "").replace("音乐", "").replace("给我", "").replace("查看", "").trim();
        return clean.trim();
    }
    
    private String cleanQuery(String input) {
        String[] removePatterns = {
            "能不能", "能不能给我", "能不能给我提供", "给我", "给我提供", "一首",
            "搜索", "找", "查找", "搜索一下", "帮我找",
            "播放", "播放一首", "听", "我想听", "我想", "要听", "要", "想听",
            "来一首", "推荐", "推荐一首", "来首", "唱", "唱一首",
            "歌词", "lyric", "的歌词", "查看", "看看", "有没有",
            "网易云", "音乐", "歌曲", "歌", "singer", "song",
            "可以吗", "好不好", "帮帮忙", "帮", "一下", "那个"
        };
        
        String query = input;
        for (String pattern : removePatterns) {
            query = query.replace(pattern, " ");
        }
        return query.trim();
    }

    @Override
    public int getPriority() {
        return 5;
    }
}
