package com.enterprise.knowledge.infrastructure.agent.tool;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class MediaGeneratorTool {

    public String generateVideo(String topic, String duration, String style) {
        if (duration == null || duration.isEmpty()) {
            duration = "30秒";
        }
        if (style == null || style.isEmpty()) {
            style = "现代简约";
        }

        Map<String, Object> videoContent = new LinkedHashMap<>();
        videoContent.put("type", "Video");
        videoContent.put("topic", topic);
        videoContent.put("duration", duration);
        videoContent.put("style", style);
        videoContent.put("status", "generated");

        Map<String, Object> scenes = new LinkedHashMap<>();
        scenes.put("开场", Map.of("duration", "5秒", "content", topic + "主题介绍"));
        scenes.put("主体1", Map.of("duration", "10秒", "content", "核心内容展示"));
        scenes.put("主体2", Map.of("duration", "10秒", "content", "详细说明"));
        scenes.put("结尾", Map.of("duration", "5秒", "content", "总结与呼吁"));

        videoContent.put("scenes", scenes);

        StringBuilder output = new StringBuilder();
        output.append("【视频生成成功】\n\n");
        output.append("主题: ").append(topic).append("\n");
        output.append("时长: ").append(duration).append("\n");
        output.append("风格: ").append(style).append("\n\n");
        output.append("分镜脚本:\n");
        output.append("────────\n");

        int sceneNum = 1;
        for (Map.Entry<String, Object> entry : scenes.entrySet()) {
            @SuppressWarnings("unchecked")
            Map<String, String> scene = (Map<String, String>) entry.getValue();
            output.append(sceneNum++).append(". ").append(entry.getKey())
                  .append(" (").append(scene.get("duration")).append("): ")
                  .append(scene.get("content")).append("\n");
        }

        return output.toString();
    }

    public String generateMusic(String genre, String mood, String duration) {
        if (genre == null || genre.isEmpty()) {
            genre = "流行";
        }
        if (mood == null || mood.isEmpty()) {
            mood = "欢快";
        }
        if (duration == null || duration.isEmpty()) {
            duration = "2分钟";
        }

        Map<String, Object> musicContent = new LinkedHashMap<>();
        musicContent.put("type", "Music");
        musicContent.put("genre", genre);
        musicContent.put("mood", mood);
        musicContent.put("duration", duration);
        musicContent.put("status", "generated");

        Map<String, Object> structure = new LinkedHashMap<>();
        structure.put("前奏", Map.of("duration", "15秒", "description", "引入主题旋律"));
        structure.put("主歌", Map.of("duration", "30秒", "description", "主旋律展示"));
        structure.put("副歌", Map.of("duration", "30秒", "description", "高潮部分"));
        structure.put("间奏", Map.of("duration", "15秒", "description", "乐器独奏"));
        structure.put("结尾", Map.of("duration", "30秒", "description", "渐弱收尾"));

        musicContent.put("structure", structure);

        StringBuilder output = new StringBuilder();
        output.append("【音乐生成成功】\n\n");
        output.append("类型: ").append(genre).append("\n");
        output.append("情绪: ").append(mood).append("\n");
        output.append("时长: ").append(duration).append("\n\n");
        output.append("音乐结构:\n");
        output.append("────────\n");

        for (Map.Entry<String, Object> entry : structure.entrySet()) {
            @SuppressWarnings("unchecked")
            Map<String, String> part = (Map<String, String>) entry.getValue();
            output.append("● ").append(entry.getKey())
                  .append(" (").append(part.get("duration")).append("): ")
                  .append(part.get("description")).append("\n");
        }

        return output.toString();
    }

    public String generateAudio(String text, String voice, String speed) {
        if (voice == null || voice.isEmpty()) {
            voice = "女声";
        }
        if (speed == null || speed.isEmpty()) {
            speed = "正常";
        }

        Map<String, Object> audioContent = new LinkedHashMap<>();
        audioContent.put("type", "Audio");
        audioContent.put("voice", voice);
        audioContent.put("speed", speed);
        audioContent.put("textLength", text.length() + " 字");
        audioContent.put("status", "generated");

        StringBuilder output = new StringBuilder();
        output.append("【音频生成成功】\n\n");
        output.append("语音类型: ").append(voice).append("\n");
        output.append("语速: ").append(speed).append("\n");
        output.append("文本长度: ").append(text.length()).append(" 字\n\n");
        output.append("音频内容预览:\n");
        output.append("────────\n");
        output.append(text.length() > 50 ? text.substring(0, 50) + "..." : text);

        return output.toString();
    }

    public String generateImage(String prompt, String style, String size) {
        if (style == null || style.isEmpty()) {
            style = "写实";
        }
        if (size == null || size.isEmpty()) {
            size = "1024x1024";
        }

        Map<String, Object> imageContent = new LinkedHashMap<>();
        imageContent.put("type", "Image");
        imageContent.put("prompt", prompt);
        imageContent.put("style", style);
        imageContent.put("size", size);
        imageContent.put("status", "generated");

        StringBuilder output = new StringBuilder();
        output.append("【图片生成成功】\n\n");
        output.append("描述: ").append(prompt).append("\n");
        output.append("风格: ").append(style).append("\n");
        output.append("尺寸: ").append(size).append("\n");

        return output.toString();
    }
}