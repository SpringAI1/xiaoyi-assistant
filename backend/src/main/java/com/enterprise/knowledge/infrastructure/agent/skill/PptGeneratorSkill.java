package com.enterprise.knowledge.infrastructure.agent.skill;

import com.enterprise.knowledge.infrastructure.agent.tool.DocumentGeneratorTool;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class PptGeneratorSkill implements LearningSkill {

    private double confidence = 0.85;
    private final DocumentGeneratorTool documentGeneratorTool;

    public PptGeneratorSkill(DocumentGeneratorTool documentGeneratorTool) {
        this.documentGeneratorTool = documentGeneratorTool;
    }

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
        return "根据自然语言描述生成专业的PPT演示文稿，支持多种模板样式，参考WPS CLI Anything风格";
    }

    @Override
    public List<String> getKeywords() {
        return Arrays.asList("ppt", "演示文稿", "幻灯片", "生成", "制作", "汇报");
    }

    @Override
    public String execute(String input) {
        try {
            // 使用WPS风格的自然语言解析生成PPT
            return documentGeneratorTool.generatePPTByDescription(input);
        } catch (Exception e) {
            // 如果AI解析失败，使用传统方法
            return generateFallbackPPT(input);
        }
    }

    private String generateFallbackPPT(String input) {
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
        } else if (lowerInput.contains("创业") || lowerInput.contains("startup")) {
            template = "startup";
        } else if (lowerInput.contains("金融") || lowerInput.contains("financial")) {
            template = "financial";
        } else if (lowerInput.contains("自然") || lowerInput.contains("green")) {
            template = "nature";
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
            case "startup" -> "创业风格";
            case "financial" -> "金融蓝调";
            case "nature" -> "自然清新";
            default -> "商务专业";
        };

        return String.format("""
            ✅ PPT生成成功！

            🎨 使用模板：%s

            📊 PPT主题：%s

            📋 PPT包含以下页面（WPS风格）：
            1. 封面页 - 专业设计
            2. 目录页 - 双列布局
            3. 公司概览 - 数据卡片
            4. 核心要点 - 卡片式展示
            5. 数据分析 - 柱状图+饼图
            6. 竞争对比 - 表格对比
            7. 发展历程 - 时间线
            8. 流程演示 - 步骤图
            9. 核心价值 - 引用页
            10. 结束页 - 联系方式

            📥 下载链接：%s

            💡 提示：您可以使用自然语言描述，例如"帮我生成一个关于人工智能发展的PPT，风格科技风"
            """, templateName, topic, downloadUrl);
    }

    @Override
    public void learn(String context, String feedback) {
        if (feedback.contains("好") || feedback.contains("漂亮") || feedback.contains("专业")) {
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
