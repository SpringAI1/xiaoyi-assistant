package com.enterprise.knowledge.infrastructure.agent.skill;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class OpenAiImageGenSkill implements Skill {

    private static final String SKILL_ID = "openai-image-gen";
    private static final String SKILL_NAME = "AI图像生成";
    private static final String SKILL_DESCRIPTION = "使用AI生成图像，支持多种风格和尺寸，根据文本描述创建图片。";

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
        return Arrays.asList("生成图片", "图像", "画图", "画画", "create image", "生成图像", "ai图像");
    }

    @Override
    public String execute(String input) {
        String prompt = extractPrompt(input);
        
        if (prompt.isEmpty()) {
            return "🎨 请提供图像描述！\n\n示例：生成图片 一只可爱的猫咪在草地上玩耍";
        }
        
        return generateImage(prompt);
    }

    private String extractPrompt(String input) {
        String[] prefixes = {"生成图片", "生成图像", "画图", "画画", "image", "图片"};
        String clean = input.trim();
        
        for (String prefix : prefixes) {
            if (clean.toLowerCase().startsWith(prefix.toLowerCase())) {
                clean = clean.substring(prefix.length()).trim();
                break;
            }
        }
        
        return clean;
    }

    private String generateImage(String prompt) {
        StringBuilder result = new StringBuilder();
        
        String imageUrl = generateImageUrl(prompt);
        
        result.append("🎨 【AI图像生成】\n\n");
        result.append("📝 描述：").append(prompt).append("\n\n");
        result.append("🖼️ 生成结果：\n");
        result.append(imageUrl).append("\n\n");
        result.append("💡 提示：点击链接查看图片\n");
        result.append("🔧 可用风格：写实、卡通、油画、素描、水彩、赛博朋克、像素风\n");
        
        return result.toString();
    }

    private String generateImageUrl(String prompt) {
        String encodedPrompt = prompt.replace(" ", "+");
        String safePrompt = sanitizePrompt(encodedPrompt);
        
        return String.format("https://neeko-copilot.bytedance.net/api/text_to_image?prompt=%s&image_size=landscape_16_9", safePrompt);
    }

    private String sanitizePrompt(String prompt) {
        StringBuilder sb = new StringBuilder();
        for (char c : prompt.toCharArray()) {
            if (c >= 32 && c <= 126) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    @Override
    public int getPriority() {
        return 5;
    }
}
