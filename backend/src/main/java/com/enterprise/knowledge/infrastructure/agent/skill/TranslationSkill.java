package com.enterprise.knowledge.infrastructure.agent.skill;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class TranslationSkill implements Skill {
    
    @Override
    public String getId() {
        return "translation-skill";
    }
    
    @Override
    public String getName() {
        return "智能翻译";
    }
    
    @Override
    public String getDescription() {
        return "中英文互译，支持简单翻译功能";
    }
    
    @Override
    public List<String> getKeywords() {
        return Arrays.asList("翻译", "translate", "英文", "中文", "英译中", "中译英");
    }
    
    @Override
    public int getPriority() {
        return 7;
    }
    
    @Override
    public String execute(String input) {
        try {
            String text = extractText(input);
            
            if (text == null || text.isEmpty()) {
                return "请提供需要翻译的内容，例如：'翻译 Hello World' 或 '翻译你好'";
            }
            
            String result = translate(text);
            
            StringBuilder sb = new StringBuilder();
            sb.append("🌍 翻译结果\n\n");
            sb.append("原文：").append(text).append("\n");
            sb.append("译文：").append(result).append("\n\n");
            sb.append("💡 提示：目前支持基础中英文互译");
            
            return sb.toString();
            
        } catch (Exception e) {
            return "翻译出错：" + e.getMessage();
        }
    }
    
    private String extractText(String input) {
        String[] keywords = {"翻译", "translate"};
        for (String keyword : keywords) {
            if (input.contains(keyword)) {
                int idx = input.indexOf(keyword);
                return input.substring(idx + keyword.length()).trim();
            }
        }
        return input.trim();
    }
    
    private String translate(String text) {
        if (isEnglish(text)) {
            return englishToChinese(text);
        } else {
            return chineseToEnglish(text);
        }
    }
    
    private boolean isEnglish(String text) {
        return text.matches("^[a-zA-Z0-9\\s\\p{Punct}]+$");
    }
    
    private String englishToChinese(String text) {
        String lowerText = text.toLowerCase();
        
        if (lowerText.contains("hello")) return "你好";
        if (lowerText.contains("world")) return "世界";
        if (lowerText.contains("thank")) return "谢谢";
        if (lowerText.contains("good")) return "好";
        if (lowerText.contains("morning")) return "早上好";
        if (lowerText.contains("bye")) return "再见";
        if (lowerText.contains("love")) return "爱";
        if (lowerText.contains("time")) return "时间";
        if (lowerText.contains("day")) return "天";
        if (lowerText.contains("night")) return "晚上";
        
        return "（演示版翻译：" + text + "）";
    }
    
    private String chineseToEnglish(String text) {
        if (text.contains("你好")) return "Hello";
        if (text.contains("谢谢")) return "Thank you";
        if (text.contains("好")) return "Good";
        if (text.contains("再见")) return "Goodbye";
        if (text.contains("爱")) return "Love";
        if (text.contains("时间")) return "Time";
        if (text.contains("世界")) return "World";
        
        return "（Demo translation: " + text + "）";
    }
}
