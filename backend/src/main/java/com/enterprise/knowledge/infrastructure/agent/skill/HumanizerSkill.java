package com.enterprise.knowledge.infrastructure.agent.skill;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class HumanizerSkill implements Skill {

    private static final String SKILL_ID = "humanizer";
    private static final String SKILL_NAME = "文本美化";
    private static final String SKILL_DESCRIPTION = "美化文本格式，添加emoji、分段、格式化，让文本更易读和吸引人。";

    private static final Map<String, String> EMOJI_MAP = new HashMap<>();
    static {
        EMOJI_MAP.put("标题", "📌");
        EMOJI_MAP.put("介绍", "📝");
        EMOJI_MAP.put("注意", "⚠️");
        EMOJI_MAP.put("警告", "🚨");
        EMOJI_MAP.put("成功", "✅");
        EMOJI_MAP.put("错误", "❌");
        EMOJI_MAP.put("提示", "💡");
        EMOJI_MAP.put("示例", "📋");
        EMOJI_MAP.put("步骤", "🔧");
        EMOJI_MAP.put("结果", "🎯");
        EMOJI_MAP.put("问题", "❓");
        EMOJI_MAP.put("答案", "💬");
        EMOJI_MAP.put("代码", "💻");
        EMOJI_MAP.put("链接", "🔗");
        EMOJI_MAP.put("图片", "🖼️");
        EMOJI_MAP.put("文件", "📁");
        EMOJI_MAP.put("视频", "🎬");
        EMOJI_MAP.put("音乐", "🎵");
        EMOJI_MAP.put("数据", "📊");
        EMOJI_MAP.put("时间", "⏰");
        EMOJI_MAP.put("地点", "📍");
        EMOJI_MAP.put("人物", "👤");
        EMOJI_MAP.put("想法", "💭");
        EMOJI_MAP.put("计划", "📅");
        EMOJI_MAP.put("目标", "🎯");
        EMOJI_MAP.put("完成", "✅");
        EMOJI_MAP.put("进行中", "🔄");
        EMOJI_MAP.put("待办", "📝");
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
        return Arrays.asList("美化", "格式", "emoji", "排版", "整理", "humanize", "format", "文本");
    }

    @Override
    public String execute(String input) {
        String content = extractContent(input);
        
        if (content.isEmpty()) {
            return "📝 请提供需要美化的文本内容！\n\n示例：美化 今天天气很好，我去公园散步。";
        }
        
        return humanizeText(content);
    }

    private String extractContent(String input) {
        String[] prefixes = {"美化", "格式", "整理", "humanize", "format"};
        for (String prefix : prefixes) {
            if (input.toLowerCase().startsWith(prefix.toLowerCase())) {
                return input.substring(prefix.length()).trim();
            }
        }
        return input;
    }

    private String humanizeText(String text) {
        StringBuilder result = new StringBuilder();
        
        text = addEmojis(text);
        text = formatParagraphs(text);
        text = formatLists(text);
        text = formatHeadings(text);
        text = addSpacing(text);
        
        result.append("✨ 美化后的文本：\n\n");
        result.append(text);
        
        return result.toString();
    }

    private String addEmojis(String text) {
        String result = text;
        
        for (Map.Entry<String, String> entry : EMOJI_MAP.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue() + " " + entry.getKey());
        }
        
        result = result.replace("。", "。\n");
        result = result.replace("！", "！\n");
        result = result.replace("？", "？\n");
        
        return result;
    }

    private String formatParagraphs(String text) {
        String[] paragraphs = text.split("\n\n");
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < paragraphs.length; i++) {
            String paragraph = paragraphs[i].trim();
            if (!paragraph.isEmpty()) {
                if (paragraph.length() > 50) {
                    result.append(paragraph).append("\n\n");
                } else {
                    result.append("📌 ").append(paragraph).append("\n");
                }
            }
        }
        
        return result.toString().trim();
    }

    private String formatLists(String text) {
        String result = text;
        
        result = result.replaceAll("([一二三四五六七八九十]、)", "\n• ");
        result = result.replaceAll("(\\d+\\.)", "\n• ");
        result = result.replaceAll("(\\*|\\-|\\+) ", "\n• ");
        
        Pattern pattern = Pattern.compile("([（(]\\d+[)）])");
        Matcher matcher = pattern.matcher(result);
        result = matcher.replaceAll("\n• ");
        
        return result;
    }

    private String formatHeadings(String text) {
        String result = text;
        
        result = result.replaceAll("^(#{1,3})\\s*(.+)$", "\n$1 $2\n");
        result = result.replaceAll("^【(.+)】$", "📌 **$1**\n");
        result = result.replaceAll("^《(.+)》$", "📖 **$1**\n");
        
        return result;
    }

    private String addSpacing(String text) {
        String[] lines = text.split("\n");
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            
            if (line.isEmpty()) {
                if (i > 0 && !lines[i-1].trim().isEmpty()) {
                    result.append("\n");
                }
            } else {
                if (line.startsWith("• ") || line.startsWith("📌")) {
                    result.append(line).append("\n");
                } else {
                    result.append("  ").append(line).append("\n");
                }
            }
        }
        
        return result.toString().trim();
    }

    @Override
    public int getPriority() {
        return 5;
    }
}
