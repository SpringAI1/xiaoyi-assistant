package com.enterprise.knowledge.infrastructure.agent.skill;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class PptGeneratorSkill implements LearningSkill {

    private double confidence = 0.85;

    @Override
    public String getId() {
        return "ppt-generator-skill";
    }

    @Override
    public String getName() {
        return "PPT生成";
    }

    @Override
    public String getDescription() {
        return "根据主题生成专业的PPT演示文稿，支持多种模板样式";
    }

    @Override
    public List<String> getKeywords() {
        return Arrays.asList("ppt", "演示文稿", "幻灯片", "生成");
    }

    @Override
    public String execute(String input) {
        String lowerInput = input.toLowerCase();
        
        // 解析模板选择
        String template = "business";
        if (lowerInput.contains("科技") || lowerInput.contains("tech")) {
            template = "tech";
        } else if (lowerInput.contains("创意") || lowerInput.contains("creative")) {
            template = "creative";
        } else if (lowerInput.contains("简约") || lowerInput.contains("minimal")) {
            template = "minimal";
        } else if (lowerInput.contains("教育") || lowerInput.contains("education")) {
            template = "education";
        }
        
        // 提取主题
        String topic = input.replaceAll("生成", "").replaceAll("ppt", "").replaceAll("PPT", "")
                          .replaceAll("演示文稿", "").replaceAll("幻灯片", "")
                          .replaceAll("商务", "").replaceAll("科技", "")
                          .replaceAll("创意", "").replaceAll("简约", "")
                          .replaceAll("教育", "").replaceAll("business", "")
                          .replaceAll("tech", "").replaceAll("creative", "")
                          .replaceAll("minimal", "").replaceAll("education", "")
                          .trim();
        
        if (topic.isEmpty()) {
            topic = "企业宣传";
        }
        
        String downloadUrl = "/api/v1/generate/ppt/download?topic=" + 
            java.net.URLEncoder.encode(topic, java.nio.charset.StandardCharsets.UTF_8) + 
            "&template=" + template;
        
        String templateName = switch(template) {
            case "tech" -> "科技风格";
            case "creative" -> "创意设计";
            case "minimal" -> "现代简约";
            case "education" -> "教育风格";
            default -> "商务专业";
        };
        
        return String.format("""
            ✅ PPT生成成功！
            
            🎨 使用模板：%s
            
            📊 PPT主题：%s
            
            📋 PPT包含以下页面：
            1. 封面页
            2. 目录页
            3. 公司概览
            4. 数据概览（新增图表）
            5. 核心业务
            6. 竞争优势
            7. 发展战略
            8. 流程演示（新增）
            9. 未来展望
            10. 结束页
            
            📥 下载链接：%s
            
            💡 提示：您可以指定模板类型，例如"生成科技风格产品介绍PPT"
            """, templateName, topic, downloadUrl);
    }

    @Override
    public void learn(String context, String feedback) {
        if (feedback.contains("好") || feedback.contains("漂亮")) {
            confidence = Math.min(1.0, confidence + 0.05);
        } else {
            confidence = Math.max(0.1, confidence - 0.05);
        }
    }

    @Override
    public double getConfidence() {
        return confidence;
    }
}
