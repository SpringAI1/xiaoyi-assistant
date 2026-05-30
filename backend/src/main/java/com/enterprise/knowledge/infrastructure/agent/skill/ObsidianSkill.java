package com.enterprise.knowledge.infrastructure.agent.skill;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ObsidianSkill implements Skill {

    private static final String SKILL_ID = "obsidian";
    private static final String SKILL_NAME = "Obsidian笔记管理";
    private static final String SKILL_DESCRIPTION = "管理Obsidian笔记，支持创建、搜索、整理和链接笔记。";

    private final Map<String, Note> notes = new ConcurrentHashMap<>();
    private final Map<String, List<String>> tags = new ConcurrentHashMap<>();

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
        return Arrays.asList("笔记", "obsidian", "记录", "日记", "文档", "整理", "链接", "标签");
    }

    @Override
    public String execute(String input) {
        String lowerInput = input.toLowerCase();
        
        if (lowerInput.contains("创建") || lowerInput.contains("新建")) {
            return createNote(input);
        } else if (lowerInput.contains("搜索") || lowerInput.contains("查找")) {
            return searchNotes(input);
        } else if (lowerInput.contains("标签") || lowerInput.contains("分类")) {
            return manageTags(input);
        } else if (lowerInput.contains("列表") || lowerInput.contains("全部")) {
            return listNotes();
        } else if (lowerInput.contains("删除") || lowerInput.contains("移除")) {
            return deleteNote(input);
        } else {
            return getHelp();
        }
    }

    private String createNote(String input) {
        String title = extractTitle(input);
        String content = extractContent(input);
        
        Note note = new Note(title, content);
        notes.put(title, note);
        
        addTags(note, extractTags(input));
        
        return String.format("""
                ✅ 笔记创建成功！
                
                📝 标题：%s
                📄 内容：%s
                🏷️ 标签：%s
                ⏰ 创建时间：%s
                
                💡 提示：可以使用链接语法 [[其他笔记]] 创建笔记链接
                """, title, content.isEmpty() ? "(空)" : content, 
                note.tags.isEmpty() ? "(无)" : String.join(", ", note.tags),
                note.createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }

    private String searchNotes(String input) {
        String keyword = extractKeyword(input);
        
        List<Note> found = new ArrayList<>();
        for (Note note : notes.values()) {
            if (note.title.toLowerCase().contains(keyword.toLowerCase()) ||
                note.content.toLowerCase().contains(keyword.toLowerCase())) {
                found.add(note);
            }
        }
        
        if (found.isEmpty()) {
            return "🔍 未找到包含 \"" + keyword + "\" 的笔记";
        }
        
        StringBuilder result = new StringBuilder();
        result.append("🔍 搜索结果（").append(found.size()).append("条）：\n\n");
        
        for (Note note : found) {
            result.append("📝 ").append(note.title).append("\n");
            String preview = note.content.length() > 50 ? note.content.substring(0, 50) + "..." : note.content;
            result.append("   ").append(preview).append("\n");
            if (!note.tags.isEmpty()) {
                result.append("   🏷️ ").append(String.join(", ", note.tags)).append("\n");
            }
            result.append("\n");
        }
        
        return result.toString();
    }

    private String manageTags(String input) {
        String tag = extractTag(input);
        
        if (tag.isEmpty()) {
            StringBuilder result = new StringBuilder();
            result.append("🏷️ 所有标签：\n\n");
            
            for (Map.Entry<String, List<String>> entry : tags.entrySet()) {
                result.append("• ").append(entry.getKey());
                result.append(" (").append(entry.getValue().size()).append("条笔记)\n");
            }
            
            if (tags.isEmpty()) {
                result.append("暂无标签");
            }
            
            return result.toString();
        }
        
        List<String> tagNotes = tags.get(tag);
        if (tagNotes == null || tagNotes.isEmpty()) {
            return "🏷️ 标签 \"" + tag + "\" 下暂无笔记";
        }
        
        StringBuilder result = new StringBuilder();
        result.append("🏷️ 标签 \"").append(tag).append("\" 下的笔记（").append(tagNotes.size()).append("条）：\n\n");
        
        for (String noteTitle : tagNotes) {
            result.append("• ").append(noteTitle).append("\n");
        }
        
        return result.toString();
    }

    private String listNotes() {
        if (notes.isEmpty()) {
            return "📭 暂无笔记，使用\"创建笔记 标题 - 内容\"来创建新笔记";
        }
        
        StringBuilder result = new StringBuilder();
        result.append("📋 笔记列表（共").append(notes.size()).append("条）：\n\n");
        
        List<String> titles = new ArrayList<>(notes.keySet());
        titles.sort(String::compareTo);
        
        for (String title : titles) {
            Note note = notes.get(title);
            result.append("📝 ").append(title);
            if (!note.tags.isEmpty()) {
                result.append(" 🏷️").append(String.join(",", note.tags));
            }
            result.append("\n");
        }
        
        result.append("\n💡 提示：输入\"搜索 关键词\"来查找笔记");
        
        return result.toString();
    }

    private String deleteNote(String input) {
        String title = extractTitle(input);
        
        if (notes.remove(title) != null) {
            tags.values().forEach(list -> list.remove(title));
            return "🗑️ 笔记 \"" + title + "\" 删除成功！";
        }
        
        return "❌ 未找到笔记 \"" + title + "\"";
    }

    private String getHelp() {
        return """
                📚 Obsidian笔记管理技能
                
                可用命令：
                • 创建笔记 [标题] - [内容] - 创建新笔记
                • 搜索 [关键词] - 搜索笔记
                • 标签 [标签名] - 查看标签下的笔记
                • 列表 - 显示所有笔记
                • 删除 [标题] - 删除笔记
                
                示例：
                • 创建笔记 学习计划 - 今天学习Java
                • 搜索 学习
                • 标签 工作
                
                🏷️ 标签语法：在内容中使用 #标签名 添加标签
                🔗 链接语法：使用 [[笔记名]] 创建笔记链接
                """;
    }

    private String extractTitle(String input) {
        input = input.replace("创建笔记", "").replace("删除笔记", "").replace("删除", "").trim();
        int dashIndex = input.indexOf("-");
        if (dashIndex > 0) {
            return input.substring(0, dashIndex).trim();
        }
        return input;
    }

    private String extractContent(String input) {
        int dashIndex = input.indexOf("-");
        if (dashIndex > 0) {
            return input.substring(dashIndex + 1).trim();
        }
        return "";
    }

    private String extractKeyword(String input) {
        return input.replace("搜索", "").replace("查找", "").trim();
    }

    private String extractTag(String input) {
        return input.replace("标签", "").replace("分类", "").trim();
    }

    private List<String> extractTags(String input) {
        List<String> foundTags = new ArrayList<>();
        Pattern pattern = Pattern.compile("#(\\w+)");
        Matcher matcher = pattern.matcher(input);
        while (matcher.find()) {
            foundTags.add(matcher.group(1));
        }
        return foundTags;
    }

    private void addTags(Note note, List<String> newTags) {
        for (String tag : newTags) {
            note.tags.add(tag);
            tags.computeIfAbsent(tag, k -> new ArrayList<>()).add(note.title);
        }
    }

    private static class Note {
        String title;
        String content;
        LocalDateTime createdAt;
        Set<String> tags = new HashSet<>();

        Note(String title, String content) {
            this.title = title;
            this.content = content;
            this.createdAt = LocalDateTime.now();
        }
    }
}
