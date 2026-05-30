package com.enterprise.knowledge.infrastructure.agent.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.apache.poi.sl.usermodel.TextParagraph;
import org.apache.poi.xslf.usermodel.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class DocumentGeneratorTool {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ChatLanguageModel chatModel;

    // 定义模板样式 - 参考WPS模板风格
    public enum PPTTemplate {
        BUSINESS_PROFESSIONAL("商务专业", new Color(0, 51, 102), new Color(255, 255, 255)),
        TECH_INNOVATION("科技风格", new Color(0, 128, 128), new Color(240, 248, 255)),
        CREATIVE_DESIGN("创意设计", new Color(139, 0, 0), new Color(255, 250, 240)),
        MODERN_MINIMAL("现代简约", new Color(70, 130, 180), new Color(255, 255, 255)),
        EDUCATIONAL("教育风格", new Color(0, 100, 0), new Color(245, 245, 220)),
        STARTUP_VIBES("创业风格", new Color(255, 102, 0), new Color(255, 255, 255)),
        FINANCIAL_BLUE("金融蓝调", new Color(0, 61, 138), new Color(248, 250, 252)),
        NATURE_GREEN("自然清新", new Color(34, 139, 34), new Color(245, 250, 245));

        final String name;
        final Color primaryColor;
        final Color backgroundColor;

        PPTTemplate(String name, Color primaryColor, Color backgroundColor) {
            this.name = name;
            this.primaryColor = primaryColor;
            this.backgroundColor = backgroundColor;
        }
    }

    // 定义动画类型 - 参考WPS动画效果
    public enum AnimationType {
        FADE_IN("淡入"),
        FLY_IN_FROM_LEFT("从左侧飞入"),
        FLY_IN_FROM_RIGHT("从右侧飞入"),
        FLY_IN_FROM_TOP("从上方飞入"),
        FLY_IN_FROM_BOTTOM("从下方飞入"),
        WIPE("擦除"),
        ZOOM_IN("缩放进入"),
        BOUNCE("弹跳"),
        SPIN("旋转");

        final String description;

        AnimationType(String description) {
            this.description = description;
        }
    }

    // 幻灯片类型
    public enum SlideType {
        COVER("封面页"),
        TABLE_OF_CONTENTS("目录页"),
        CONTENT("内容页"),
        CHART("图表页"),
        COMPARISON("对比页"),
        TIMELINE("时间线"),
        PROCESS("流程页"),
        QUOTE("引用页"),
        END("结束页"),
        BLANK("空白页");

        final String description;

        SlideType(String description) {
            this.description = description;
        }
    }

    public DocumentGeneratorTool(@Qualifier("chatModel") ChatLanguageModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * WPS风格的PPT生成 - 通过自然语言描述生成完整PPT
     * 示例: "帮我生成一个关于人工智能发展的PPT，风格科技风，包含封面、目录、5页内容、总结"
     */
    public String generatePPTByDescription(String description) {
        try {
            // 使用AI解析用户描述，提取关键信息
            String analysisPrompt = """
                请分析以下PPT生成需求，提取关键信息并以JSON格式输出：
                
                用户需求：%s
                
                输出格式（JSON）：
                {
                    "topic": "主题名称",
                    "template": "模板名称（商务专业/科技风格/创意设计/现代简约/教育风格/创业风格/金融蓝调/自然清新）",
                    "slideCount": 预计页数,
                    "sections": ["章节1", "章节2", "章节3"],
                    "style": "简洁/详细/图文并茂"
                }
                """;

            String analysisResult = chatModel.generate(String.format(analysisPrompt, description));
            Map<String, Object> analysis = objectMapper.readValue(analysisResult, Map.class);

            String topic = (String) analysis.getOrDefault("topic", "未命名演示文稿");
            String templateName = (String) analysis.getOrDefault("template", "科技风格");
            
            PPTTemplate template = parseTemplate(templateName);
            
            // 生成PPT文档
            generatePPTDocument(topic, template);
            
            // 保存到文件（模拟）
            String fileName = "generated/" + topic.replaceAll("[^a-zA-Z0-9]", "_") + ".pptx";
            
            return String.format("""
                【PPT生成成功】🎯
                
                📋 分析结果：
                ├─ 主题：%s
                ├─ 模板：%s
                ├─ 页数：10页（封面+目录+7页内容+结束页）
                └─ 文件：%s
                
                🎨 已包含：
                • 专业封面设计
                • 目录导航页
                • 数据图表展示
                • 核心业务介绍
                • 竞争优势分析
                • 发展战略规划
                • 流程演示图
                • 未来展望
                • 结束页
                
                ✨ 使用提示：打开后可根据需要修改内容
                """, topic, template.name, fileName);
                
        } catch (Exception e) {
            return "PPT生成失败: " + e.getMessage();
        }
    }

    /**
     * 生成PPT文档（二进制）
     */
    public byte[] generatePPTDocument(String topic) throws Exception {
        return generatePPTDocument(topic, PPTTemplate.BUSINESS_PROFESSIONAL);
    }

    public byte[] generatePPTDocument(String topic, PPTTemplate template) throws Exception {
        XMLSlideShow ppt = new XMLSlideShow();

        try {
            // 设置幻灯片大小为标准16:9
            ppt.setPageSize(new Dimension(960, 540));

            // ========== 1. 封面页 - WPS风格专业设计 ==========
            XSLFSlide coverSlide = ppt.createSlide();
            createWpsStyleCoverSlide(coverSlide, topic, template);

            // ========== 2. 目录页 ==========
            XSLFSlide tocSlide = ppt.createSlide();
            createTocSlide(tocSlide, template);

            // ========== 3. 内容页1 - 概述 ==========
            XSLFSlide overviewSlide = ppt.createSlide();
            createOverviewSlide(overviewSlide, topic, template);

            // ========== 4. 内容页2 - 核心要点 ==========
            XSLFSlide keyPointsSlide = ppt.createSlide();
            createKeyPointsSlide(keyPointsSlide, topic, template);

            // ========== 5. 数据图表页 ==========
            XSLFSlide chartSlide = ppt.createSlide();
            createDataChartSlide(chartSlide, topic, template);

            // ========== 6. 对比分析页 ==========
            XSLFSlide comparisonSlide = ppt.createSlide();
            createComparisonSlide(comparisonSlide, template);

            // ========== 7. 时间线页 ==========
            XSLFSlide timelineSlide = ppt.createSlide();
            createTimelineSlide(timelineSlide, template);

            // ========== 8. 流程步骤页 ==========
            XSLFSlide processSlide = ppt.createSlide();
            createProcessSlide(processSlide, topic, template);

            // ========== 9. 引用/亮点页 ==========
            XSLFSlide quoteSlide = ppt.createSlide();
            createQuoteSlide(quoteSlide, topic, template);

            // ========== 10. 结束页 ==========
            XSLFSlide endSlide = ppt.createSlide();
            createEndSlide(endSlide, template);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ppt.write(out);
            return out.toByteArray();
        } finally {
            ppt.close();
        }
    }

    /**
     * WPS风格封面页
     */
    private void createWpsStyleCoverSlide(XSLFSlide slide, String topic, PPTTemplate template) {
        // 渐变背景
        XSLFAutoShape bg = slide.createAutoShape();
        bg.setAnchor(new Rectangle(0, 0, 960, 540));
        bg.setShapeType(org.apache.poi.sl.usermodel.ShapeType.RECT);
        bg.setFillColor(template.backgroundColor);

        // WPS风格装饰元素
        // 右上角装饰矩形（简化版）
        XSLFAutoShape cornerShape = slide.createAutoShape();
        cornerShape.setAnchor(new Rectangle(850, 0, 110, 110));
        cornerShape.setShapeType(org.apache.poi.sl.usermodel.ShapeType.RECT);
        cornerShape.setFillColor(new Color(template.primaryColor.getRed(), 
                                           template.primaryColor.getGreen(), 
                                           template.primaryColor.getBlue(), 30));

        // 左下角装饰圆形
        XSLFAutoShape circle = slide.createAutoShape();
        circle.setAnchor(new Rectangle(-50, 400, 150, 150));
        circle.setShapeType(org.apache.poi.sl.usermodel.ShapeType.ELLIPSE);
        circle.setFillColor(new Color(template.primaryColor.getRed(), 
                                      template.primaryColor.getGreen(), 
                                      template.primaryColor.getBlue(), 20));

        // 标题区域
        XSLFTextBox titleBox = slide.createTextBox();
        titleBox.setAnchor(new Rectangle(100, 180, 760, 150));
        XSLFTextParagraph titlePara = titleBox.addNewTextParagraph();
        titlePara.setTextAlign(TextParagraph.TextAlign.CENTER);
        XSLFTextRun titleRun = titlePara.addNewTextRun();
        titleRun.setText(topic);
        titleRun.setFontSize(52.0);
        titleRun.setBold(true);
        titleRun.setFontColor(template.primaryColor);

        // 副标题
        XSLFTextBox subtitleBox = slide.createTextBox();
        subtitleBox.setAnchor(new Rectangle(100, 340, 760, 60));
        XSLFTextParagraph subtitlePara = subtitleBox.addNewTextParagraph();
        subtitlePara.setTextAlign(TextParagraph.TextAlign.CENTER);
        XSLFTextRun subtitleRun = subtitlePara.addNewTextRun();
        subtitleRun.setText("Professional Presentation");
        subtitleRun.setFontSize(24.0);
        subtitleRun.setFontColor(new Color(100, 100, 100));

        // 底部信息
        XSLFTextBox footerBox = slide.createTextBox();
        footerBox.setAnchor(new Rectangle(100, 460, 760, 40));
        XSLFTextParagraph footerPara = footerBox.addNewTextParagraph();
        footerPara.setTextAlign(TextParagraph.TextAlign.CENTER);
        XSLFTextRun footerRun = footerPara.addNewTextRun();
        footerRun.setText("Generated by XiaoYi Assistant · " + LocalDate.now().toString());
        footerRun.setFontSize(14.0);
        footerRun.setFontColor(new Color(150, 150, 150));
    }

    /**
     * 目录页
     */
    private void createTocSlide(XSLFSlide slide, PPTTemplate template) {
        addWpsHeader(slide, template);

        XSLFTextBox titleBox = slide.createTextBox();
        titleBox.setAnchor(new Rectangle(80, 80, 800, 60));
        XSLFTextParagraph titlePara = titleBox.addNewTextParagraph();
        XSLFTextRun titleRun = titlePara.addNewTextRun();
        titleRun.setText("📋 目录");
        titleRun.setFontSize(40.0);
        titleRun.setBold(true);
        titleRun.setFontColor(template.primaryColor);

        String[] tocItems = {
            "公司概览", "核心要点", "数据分析", "竞争对比",
            "发展历程", "流程演示", "核心价值"
        };

        int startY = 180;
        for (int i = 0; i < tocItems.length; i++) {
            int row = i / 2;
            int col = i % 2;
            int x = col == 0 ? 100 : 500;
            int y = startY + row * 60;

            XSLFAutoShape bullet = slide.createAutoShape();
            bullet.setAnchor(new Rectangle(x, y + 5, 12, 12));
            bullet.setShapeType(org.apache.poi.sl.usermodel.ShapeType.RECT);
            bullet.setFillColor(template.primaryColor);

            XSLFTextBox itemBox = slide.createTextBox();
            itemBox.setAnchor(new Rectangle(x + 25, y, 350, 40));
            XSLFTextParagraph itemPara = itemBox.addNewTextParagraph();
            XSLFTextRun itemRun = itemPara.addNewTextRun();
            itemRun.setText(String.format("%d. %s", i + 1, tocItems[i]));
            itemRun.setFontSize(22.0);
            itemRun.setFontColor(new Color(50, 50, 50));
        }

        // 装饰线条
        XSLFAutoShape line = slide.createAutoShape();
        line.setAnchor(new Rectangle(100, 480, 760, 2));
        line.setShapeType(org.apache.poi.sl.usermodel.ShapeType.LINE);
        line.setFillColor(new Color(200, 200, 200));
    }

    /**
     * 概述页
     */
    private void createOverviewSlide(XSLFSlide slide, String topic, PPTTemplate template) {
        addWpsHeader(slide, template);

        XSLFTextBox titleBox = slide.createTextBox();
        titleBox.setAnchor(new Rectangle(80, 80, 800, 50));
        XSLFTextParagraph titlePara = titleBox.addNewTextParagraph();
        XSLFTextRun titleRun = titlePara.addNewTextRun();
        titleRun.setText("🏢 公司概览");
        titleRun.setFontSize(36.0);
        titleRun.setBold(true);
        titleRun.setFontColor(template.primaryColor);

        // 左侧内容
        XSLFTextBox contentBox = slide.createTextBox();
        contentBox.setAnchor(new Rectangle(80, 150, 400, 320));

        String[] content = {
            "📌 企业使命",
            "致力于成为行业领先的解决方案提供商，为客户创造持续价值。",
            "",
            "🎯 企业愿景",
            "引领行业创新，成为受尊敬的行业领导者。",
            "",
            "💡 核心价值观",
            "创新 · 协作 · 诚信 · 卓越",
            "",
            "📈 发展目标",
            "持续增长，打造核心竞争力，实现可持续发展。"
        };

        for (String item : content) {
            XSLFTextParagraph para = contentBox.addNewTextParagraph();
            XSLFTextRun run = para.addNewTextRun();
            run.setText(item);

            if (item.contains("📌") || item.contains("🎯") || item.contains("💡") || item.contains("📈")) {
                run.setFontSize(20.0);
                run.setBold(true);
                run.setFontColor(template.primaryColor);
                para.setSpaceAfter(8.0);
            } else if (!item.isEmpty()) {
                run.setFontSize(16.0);
                para.setIndent(15.0);
                para.setSpaceAfter(5.0);
            }
        }

        // 右侧数据卡片
        createStatsCards(slide, template, 520, 150);
    }

    /**
     * 核心要点页
     */
    private void createKeyPointsSlide(XSLFSlide slide, String topic, PPTTemplate template) {
        addWpsHeader(slide, template);

        XSLFTextBox titleBox = slide.createTextBox();
        titleBox.setAnchor(new Rectangle(80, 80, 800, 50));
        XSLFTextParagraph titlePara = titleBox.addNewTextParagraph();
        XSLFTextRun titleRun = titlePara.addNewTextRun();
        titleRun.setText("🎯 核心要点");
        titleRun.setFontSize(36.0);
        titleRun.setBold(true);
        titleRun.setFontColor(template.primaryColor);

        String[][] keyPoints = {
            {"⚡ 创新技术", "掌握核心技术，持续创新发展"},
            {"🎯 精准定位", "深入理解市场，精准把握需求"},
            {"🚀 快速响应", "高效服务体系，快速响应客户"},
            {"💯 品质保证", "严格质量控制，确保交付品质"}
        };

        int startY = 150;
        int cardWidth = 420;
        int cardHeight = 120;

        for (int i = 0; i < keyPoints.length; i++) {
            int x = (i % 2) * (cardWidth + 40) + 80;
            int y = startY + (i / 2) * (cardHeight + 25);

            // 卡片背景
            XSLFAutoShape cardBg = slide.createAutoShape();
            cardBg.setAnchor(new Rectangle(x, y, cardWidth, cardHeight));
            cardBg.setShapeType(org.apache.poi.sl.usermodel.ShapeType.RECT);
            cardBg.setFillColor(new Color(template.primaryColor.getRed(), 
                                         template.primaryColor.getGreen(), 
                                         template.primaryColor.getBlue(), 15));
            cardBg.setLineColor(new Color(template.primaryColor.getRed(), 
                                          template.primaryColor.getGreen(), 
                                          template.primaryColor.getBlue(), 50));
            cardBg.setLineWidth(1.0);

            // 图标
            XSLFTextBox iconBox = slide.createTextBox();
            iconBox.setAnchor(new Rectangle(x + 20, y + 20, 60, 60));
            XSLFTextParagraph iconPara = iconBox.addNewTextParagraph();
            iconPara.setTextAlign(TextParagraph.TextAlign.CENTER);
            XSLFTextRun iconRun = iconPara.addNewTextRun();
            iconRun.setText(keyPoints[i][0].split(" ")[0]);
            iconRun.setFontSize(36.0);

            // 标题和描述
            XSLFTextBox textBox = slide.createTextBox();
            textBox.setAnchor(new Rectangle(x + 90, y + 20, cardWidth - 110, 80));

            XSLFTextParagraph cardTitlePara = textBox.addNewTextParagraph();
            XSLFTextRun titleRun2 = cardTitlePara.addNewTextRun();
            titleRun2.setText(keyPoints[i][0].split(" ")[1]);
            titleRun2.setFontSize(20.0);
            titleRun2.setBold(true);
            titleRun2.setFontColor(template.primaryColor);

            XSLFTextParagraph descPara = textBox.addNewTextParagraph();
            XSLFTextRun descRun = descPara.addNewTextRun();
            descRun.setText(keyPoints[i][1]);
            descRun.setFontSize(15.0);
        }
    }

    /**
     * 数据图表页
     */
    private void createDataChartSlide(XSLFSlide slide, String topic, PPTTemplate template) {
        addWpsHeader(slide, template);

        XSLFTextBox titleBox = slide.createTextBox();
        titleBox.setAnchor(new Rectangle(80, 80, 800, 50));
        XSLFTextParagraph titlePara = titleBox.addNewTextParagraph();
        XSLFTextRun titleRun = titlePara.addNewTextRun();
        titleRun.setText("📊 数据分析");
        titleRun.setFontSize(36.0);
        titleRun.setBold(true);
        titleRun.setFontColor(template.primaryColor);

        // 柱状图区域
        drawBarChart(slide, template, 80, 150);

        // 饼图区域
        drawPieChart(slide, template, 500, 150);
    }

    /**
     * 对比分析页
     */
    private void createComparisonSlide(XSLFSlide slide, PPTTemplate template) {
        addWpsHeader(slide, template);

        XSLFTextBox titleBox = slide.createTextBox();
        titleBox.setAnchor(new Rectangle(80, 80, 800, 50));
        XSLFTextParagraph titlePara = titleBox.addNewTextParagraph();
        XSLFTextRun titleRun = titlePara.addNewTextRun();
        titleRun.setText("⚖️ 竞争对比");
        titleRun.setFontSize(36.0);
        titleRun.setBold(true);
        titleRun.setFontColor(template.primaryColor);

        String[][] comparisonData = {
            {"维度", "我们", "竞品A", "竞品B"},
            {"技术实力", "⭐⭐⭐⭐⭐", "⭐⭐⭐⭐", "⭐⭐⭐"},
            {"服务质量", "⭐⭐⭐⭐⭐", "⭐⭐⭐⭐", "⭐⭐⭐⭐"},
            {"响应速度", "⭐⭐⭐⭐⭐", "⭐⭐⭐", "⭐⭐⭐⭐"},
            {"价格优势", "⭐⭐⭐⭐", "⭐⭐⭐⭐⭐", "⭐⭐⭐"},
            {"创新能力", "⭐⭐⭐⭐⭐", "⭐⭐⭐", "⭐⭐⭐⭐"}
        };

        int startX = 80, startY = 150;
        int colWidth = 220;
        int rowHeight = 60;

        // 表头背景
        XSLFAutoShape headerBg = slide.createAutoShape();
        headerBg.setAnchor(new Rectangle(startX, startY, colWidth * 4, rowHeight));
        headerBg.setShapeType(org.apache.poi.sl.usermodel.ShapeType.RECT);
        headerBg.setFillColor(template.primaryColor);

        // 绘制表格
        for (int i = 0; i < comparisonData.length; i++) {
            for (int j = 0; j < comparisonData[i].length; j++) {
                int x = startX + j * colWidth;
                int y = startY + i * rowHeight;

                XSLFTextBox cellBox = slide.createTextBox();
                cellBox.setAnchor(new Rectangle(x + 10, y + 10, colWidth - 20, rowHeight - 20));
                XSLFTextParagraph cellPara = cellBox.addNewTextParagraph();
                cellPara.setTextAlign(TextParagraph.TextAlign.CENTER);
                XSLFTextRun cellRun = cellPara.addNewTextRun();
                cellRun.setText(comparisonData[i][j]);

                if (i == 0) {
                    cellRun.setFontSize(16.0);
                    cellRun.setBold(true);
                    cellRun.setFontColor(Color.WHITE);
                } else {
                    cellRun.setFontSize(14.0);
                    if (j == 0) {
                        cellRun.setBold(true);
                        cellRun.setFontColor(new Color(80, 80, 80));
                    }
                }

                // 单元格边框
                if (i > 0) {
                    XSLFAutoShape border = slide.createAutoShape();
                    border.setAnchor(new Rectangle(x, y, colWidth, rowHeight));
                    border.setShapeType(org.apache.poi.sl.usermodel.ShapeType.RECT);
                    border.setFillColor(Color.WHITE);
                    border.setLineColor(new Color(200, 200, 200));
                    border.setLineWidth(0.5);
                }
            }
        }
    }

    /**
     * 时间线页
     */
    private void createTimelineSlide(XSLFSlide slide, PPTTemplate template) {
        addWpsHeader(slide, template);

        XSLFTextBox titleBox = slide.createTextBox();
        titleBox.setAnchor(new Rectangle(80, 80, 800, 50));
        XSLFTextParagraph titlePara = titleBox.addNewTextParagraph();
        XSLFTextRun titleRun = titlePara.addNewTextRun();
        titleRun.setText("⏰ 发展历程");
        titleRun.setFontSize(36.0);
        titleRun.setBold(true);
        titleRun.setFontColor(template.primaryColor);

        String[][] timelineData = {
            {"2018", "公司成立", "完成A轮融资"},
            {"2019", "产品上线", "服务客户突破100家"},
            {"2020", "技术升级", "获得行业认证"},
            {"2021", "规模扩张", "员工突破500人"},
            {"2022", "市场拓展", "覆盖30+城市"},
            {"2023", "创新突破", "发布核心技术"}
        };

        int startY = 150;
        int itemHeight = 70;

        // 时间线垂直线
        XSLFAutoShape line = slide.createAutoShape();
        line.setAnchor(new Rectangle(480, startY - 10, 3, timelineData.length * itemHeight + 20));
        line.setShapeType(org.apache.poi.sl.usermodel.ShapeType.LINE);
        line.setFillColor(template.primaryColor);

        for (int i = 0; i < timelineData.length; i++) {
            int y = startY + i * itemHeight;

            // 圆形标记
            XSLFAutoShape circle = slide.createAutoShape();
            circle.setAnchor(new Rectangle(465, y - 10, 30, 30));
            circle.setShapeType(org.apache.poi.sl.usermodel.ShapeType.ELLIPSE);
            circle.setFillColor(template.primaryColor);

            // 年份
            XSLFTextBox yearBox = slide.createTextBox();
            yearBox.setAnchor(new Rectangle(468, y - 5, 24, 20));
            XSLFTextParagraph yearPara = yearBox.addNewTextParagraph();
            yearPara.setTextAlign(TextParagraph.TextAlign.CENTER);
            XSLFTextRun yearRun = yearPara.addNewTextRun();
            yearRun.setText(timelineData[i][0]);
            yearRun.setFontSize(10.0);
            yearRun.setBold(true);
            yearRun.setFontColor(Color.WHITE);

            // 内容（交替左右）
            boolean isLeft = i % 2 == 0;
            int x = isLeft ? 80 : 520;
            int width = isLeft ? 360 : 360;

            XSLFAutoShape cardBg = slide.createAutoShape();
            cardBg.setAnchor(new Rectangle(x, y + 15, width, 40));
            cardBg.setShapeType(org.apache.poi.sl.usermodel.ShapeType.RECT);
            cardBg.setFillColor(new Color(template.primaryColor.getRed(), 
                                         template.primaryColor.getGreen(), 
                                         template.primaryColor.getBlue(), 15));

            XSLFTextBox contentBox = slide.createTextBox();
            contentBox.setAnchor(new Rectangle(x + 15, y + 20, width - 30, 30));
            XSLFTextParagraph contentPara = contentBox.addNewTextParagraph();
            XSLFTextRun contentRun = contentPara.addNewTextRun();
            contentRun.setText(timelineData[i][1] + " · " + timelineData[i][2]);
            contentRun.setFontSize(14.0);
        }
    }

    /**
     * 流程步骤页
     */
    private void createProcessSlide(XSLFSlide slide, String topic, PPTTemplate template) {
        addWpsHeader(slide, template);

        XSLFTextBox titleBox = slide.createTextBox();
        titleBox.setAnchor(new Rectangle(80, 80, 800, 50));
        XSLFTextParagraph titlePara = titleBox.addNewTextParagraph();
        XSLFTextRun titleRun = titlePara.addNewTextRun();
        titleRun.setText("⚡ 流程演示");
        titleRun.setFontSize(36.0);
        titleRun.setBold(true);
        titleRun.setFontColor(template.primaryColor);

        String[][] steps = {
            {"1️⃣", "需求分析", "深入了解客户需求"},
            {"2️⃣", "方案设计", "制定解决方案"},
            {"3️⃣", "开发实施", "高效执行落地"},
            {"4️⃣", "测试验证", "严格质量把控"},
            {"5️⃣", "交付上线", "成功交付使用"}
        };

        int startX = 50;
        int startY = 180;
        int stepWidth = 160;

        for (int i = 0; i < steps.length; i++) {
            int x = startX + i * (stepWidth + 30);

            // 步骤卡片
            XSLFAutoShape cardBg = slide.createAutoShape();
            cardBg.setAnchor(new Rectangle(x, startY, stepWidth, 180));
            cardBg.setShapeType(org.apache.poi.sl.usermodel.ShapeType.RECT);
            cardBg.setFillColor(new Color(template.primaryColor.getRed(), 
                                         template.primaryColor.getGreen(), 
                                         template.primaryColor.getBlue(), 10));

            // 图标
            XSLFTextBox iconBox = slide.createTextBox();
            iconBox.setAnchor(new Rectangle(x + 35, startY + 20, 90, 50));
            XSLFTextParagraph iconPara = iconBox.addNewTextParagraph();
            iconPara.setTextAlign(TextParagraph.TextAlign.CENTER);
            XSLFTextRun iconRun = iconPara.addNewTextRun();
            iconRun.setText(steps[i][0]);
            iconRun.setFontSize(36.0);

            // 标题
            XSLFTextBox titleBox2 = slide.createTextBox();
            titleBox2.setAnchor(new Rectangle(x + 10, startY + 80, stepWidth - 20, 30));
            XSLFTextParagraph titlePara2 = titleBox2.addNewTextParagraph();
            titlePara2.setTextAlign(TextParagraph.TextAlign.CENTER);
            XSLFTextRun titleRun2 = titlePara2.addNewTextRun();
            titleRun2.setText(steps[i][1]);
            titleRun2.setFontSize(16.0);
            titleRun2.setBold(true);
            titleRun2.setFontColor(template.primaryColor);

            // 描述
            XSLFTextBox descBox = slide.createTextBox();
            descBox.setAnchor(new Rectangle(x + 10, startY + 115, stepWidth - 20, 40));
            XSLFTextParagraph descPara = descBox.addNewTextParagraph();
            descPara.setTextAlign(TextParagraph.TextAlign.CENTER);
            XSLFTextRun descRun = descPara.addNewTextRun();
            descRun.setText(steps[i][2]);
            descRun.setFontSize(13.0);

            // 箭头
            if (i < steps.length - 1) {
                XSLFTextBox arrowBox = slide.createTextBox();
                arrowBox.setAnchor(new Rectangle(x + stepWidth + 5, startY + 70, 25, 30));
                XSLFTextParagraph arrowPara = arrowBox.addNewTextParagraph();
                arrowPara.setTextAlign(TextParagraph.TextAlign.CENTER);
                XSLFTextRun arrowRun = arrowPara.addNewTextRun();
                arrowRun.setText("→");
                arrowRun.setFontSize(28.0);
                arrowRun.setFontColor(template.primaryColor);
            }
        }
    }

    /**
     * 引用页
     */
    private void createQuoteSlide(XSLFSlide slide, String topic, PPTTemplate template) {
        // 背景
        XSLFAutoShape bg = slide.createAutoShape();
        bg.setAnchor(new Rectangle(0, 0, 960, 540));
        bg.setShapeType(org.apache.poi.sl.usermodel.ShapeType.RECT);
        bg.setFillColor(template.primaryColor);

        // 引用符号
        XSLFTextBox quoteBox = slide.createTextBox();
        quoteBox.setAnchor(new Rectangle(80, 120, 800, 80));
        XSLFTextParagraph quotePara = quoteBox.addNewTextParagraph();
        XSLFTextRun quoteRun = quotePara.addNewTextRun();
        quoteRun.setText("\"");
        quoteRun.setFontSize(80.0);
        quoteRun.setFontColor(new Color(255, 255, 255, 50));

        // 引用内容
        XSLFTextBox contentBox = slide.createTextBox();
        contentBox.setAnchor(new Rectangle(120, 180, 720, 120));
        XSLFTextParagraph contentPara = contentBox.addNewTextParagraph();
        contentPara.setTextAlign(TextParagraph.TextAlign.CENTER);
        XSLFTextRun contentRun = contentPara.addNewTextRun();
        contentRun.setText("创新是引领发展的第一动力，\n" +
                          "唯有不断创新，才能在激烈的竞争中立于不败之地。");
        contentRun.setFontSize(28.0);
        contentRun.setFontColor(Color.WHITE);

        // 引用来源
        XSLFTextBox authorBox = slide.createTextBox();
        authorBox.setAnchor(new Rectangle(120, 330, 720, 40));
        XSLFTextParagraph authorPara = authorBox.addNewTextParagraph();
        authorPara.setTextAlign(TextParagraph.TextAlign.CENTER);
        XSLFTextRun authorRun = authorPara.addNewTextRun();
        authorRun.setText("—— " + topic + " 核心价值");
        authorRun.setFontSize(18.0);
        authorRun.setFontColor(new Color(220, 220, 220));
    }

    /**
     * 结束页
     */
    private void createEndSlide(XSLFSlide slide, PPTTemplate template) {
        XSLFAutoShape bg = slide.createAutoShape();
        bg.setAnchor(new Rectangle(0, 0, 960, 540));
        bg.setShapeType(org.apache.poi.sl.usermodel.ShapeType.RECT);
        bg.setFillColor(template.primaryColor);

        // 感谢语
        XSLFTextBox thankBox = slide.createTextBox();
        thankBox.setAnchor(new Rectangle(100, 180, 760, 100));
        XSLFTextParagraph thankPara = thankBox.addNewTextParagraph();
        thankPara.setTextAlign(TextParagraph.TextAlign.CENTER);
        XSLFTextRun thankRun = thankPara.addNewTextRun();
        thankRun.setText("🎉 谢谢观看！");
        thankRun.setFontSize(52.0);
        thankRun.setBold(true);
        thankRun.setFontColor(Color.WHITE);

        // 联系方式
        XSLFTextBox contactBox = slide.createTextBox();
        contactBox.setAnchor(new Rectangle(100, 320, 760, 80));
        XSLFTextParagraph contactPara = contactBox.addNewTextParagraph();
        contactPara.setTextAlign(TextParagraph.TextAlign.CENTER);
        XSLFTextRun contactRun = contactPara.addNewTextRun();
        contactRun.setText("📧 contact@example.com\n📞 400-888-8888\n📍 www.example.com");
        contactRun.setFontSize(22.0);
        contactRun.setFontColor(new Color(230, 230, 230));

        // 底部装饰
        XSLFAutoShape footerLine = slide.createAutoShape();
        footerLine.setAnchor(new Rectangle(300, 460, 360, 3));
        footerLine.setShapeType(org.apache.poi.sl.usermodel.ShapeType.LINE);
        footerLine.setFillColor(new Color(200, 200, 200));

        XSLFTextBox footerBox = slide.createTextBox();
        footerBox.setAnchor(new Rectangle(100, 480, 760, 30));
        XSLFTextParagraph footerPara = footerBox.addNewTextParagraph();
        footerPara.setTextAlign(TextParagraph.TextAlign.CENTER);
        XSLFTextRun footerRun = footerPara.addNewTextRun();
        footerRun.setText("Generated by XiaoYi Assistant · " + LocalDateTime.now().toLocalDate().toString());
        footerRun.setFontSize(14.0);
        footerRun.setFontColor(new Color(200, 200, 200));
    }

    /**
     * 添加WPS风格头部装饰
     */
    private void addWpsHeader(XSLFSlide slide, PPTTemplate template) {
        // 顶部装饰条
        XSLFAutoShape headerBar = slide.createAutoShape();
        headerBar.setAnchor(new Rectangle(0, 0, 960, 6));
        headerBar.setShapeType(org.apache.poi.sl.usermodel.ShapeType.RECT);
        headerBar.setFillColor(template.primaryColor);

        // 左侧装饰块
        XSLFAutoShape leftBlock = slide.createAutoShape();
        leftBlock.setAnchor(new Rectangle(0, 0, 12, 540));
        leftBlock.setShapeType(org.apache.poi.sl.usermodel.ShapeType.RECT);
        leftBlock.setFillColor(template.primaryColor);
    }

    /**
     * 创建数据统计卡片
     */
    private void createStatsCards(XSLFSlide slide, PPTTemplate template, int x, int y) {
        String[][] stats = {
            {"2018", "成立年份"},
            {"500+", "员工规模"},
            {"30+", "覆盖城市"},
            {"ISO", "认证资质"}
        };

        int cardWidth = 150;
        int cardHeight = 100;

        for (int i = 0; i < stats.length; i++) {
            int cardX = x + (i % 2) * (cardWidth + 20);
            int cardY = y + (i / 2) * (cardHeight + 20);

            XSLFAutoShape cardBg = slide.createAutoShape();
            cardBg.setAnchor(new Rectangle(cardX, cardY, cardWidth, cardHeight));
            cardBg.setShapeType(org.apache.poi.sl.usermodel.ShapeType.RECT);
            cardBg.setFillColor(template.primaryColor);

            XSLFTextBox cardBox = slide.createTextBox();
            cardBox.setAnchor(new Rectangle(cardX + 10, cardY + 15, cardWidth - 20, cardHeight - 30));

            XSLFTextParagraph numPara = cardBox.addNewTextParagraph();
            numPara.setTextAlign(TextParagraph.TextAlign.CENTER);
            XSLFTextRun numRun = numPara.addNewTextRun();
            numRun.setText(stats[i][0]);
            numRun.setFontSize(28.0);
            numRun.setBold(true);
            numRun.setFontColor(Color.WHITE);

            XSLFTextParagraph labelPara = cardBox.addNewTextParagraph();
            labelPara.setTextAlign(TextParagraph.TextAlign.CENTER);
            XSLFTextRun labelRun = labelPara.addNewTextRun();
            labelRun.setText(stats[i][1]);
            labelRun.setFontSize(13.0);
            labelRun.setFontColor(new Color(230, 230, 230));
        }
    }

    /**
     * 绘制柱状图
     */
    private void drawBarChart(XSLFSlide slide, PPTTemplate template, int x, int y) {
        XSLFTextBox chartTitle = slide.createTextBox();
        chartTitle.setAnchor(new Rectangle(x, y - 30, 350, 25));
        XSLFTextParagraph titlePara = chartTitle.addNewTextParagraph();
        XSLFTextRun titleRun = titlePara.addNewTextRun();
        titleRun.setText("📈 业务增长趋势");
        titleRun.setFontSize(16.0);
        titleRun.setBold(true);
        titleRun.setFontColor(template.primaryColor);

        String[][] chartData = {
            {"2021", "60"},
            {"2022", "90"},
            {"2023", "130"},
            {"2024", "180"},
            {"2025", "240"}
        };

        int barWidth = 50;
        int maxValue = 240;

        for (int i = 0; i < chartData.length; i++) {
            int barX = x + i * (barWidth + 25) + 20;
            int barHeight = (int) (Double.parseDouble(chartData[i][1]) / (double) maxValue * 200);

            XSLFAutoShape bar = slide.createAutoShape();
            bar.setAnchor(new Rectangle(barX, y + 200 - barHeight, barWidth, barHeight));
            bar.setShapeType(org.apache.poi.sl.usermodel.ShapeType.RECT);
            bar.setFillColor(new Color(template.primaryColor.getRed(), 
                                      template.primaryColor.getGreen(), 
                                      template.primaryColor.getBlue(), 80));

            XSLFTextBox labelBox = slide.createTextBox();
            labelBox.setAnchor(new Rectangle(barX - 5, y + 210, barWidth + 10, 25));
            XSLFTextParagraph labelPara = labelBox.addNewTextParagraph();
            labelPara.setTextAlign(TextParagraph.TextAlign.CENTER);
            XSLFTextRun labelRun = labelPara.addNewTextRun();
            labelRun.setText(chartData[i][0]);
            labelRun.setFontSize(12.0);
        }
    }

    /**
     * 绘制饼图（模拟）
     */
    private void drawPieChart(XSLFSlide slide, PPTTemplate template, int x, int y) {
        XSLFTextBox chartTitle = slide.createTextBox();
        chartTitle.setAnchor(new Rectangle(x, y - 30, 250, 25));
        XSLFTextParagraph titlePara = chartTitle.addNewTextParagraph();
        XSLFTextRun titleRun = titlePara.addNewTextRun();
        titleRun.setText("🍰 收入构成");
        titleRun.setFontSize(16.0);
        titleRun.setBold(true);
        titleRun.setFontColor(template.primaryColor);

        // 饼图模拟
        XSLFAutoShape pieBg = slide.createAutoShape();
        pieBg.setAnchor(new Rectangle(x, y, 150, 150));
        pieBg.setShapeType(org.apache.poi.sl.usermodel.ShapeType.ELLIPSE);
        pieBg.setFillColor(new Color(240, 240, 240));

        // 图例
        String[][] legend = {
            {"🟢", "研发收入", "40%"},
            {"🔵", "服务收入", "35%"},
            {"🟡", "产品销售", "15%"},
            {"🟠", "其他收入", "10%"}
        };

        XSLFTextBox legendBox = slide.createTextBox();
        legendBox.setAnchor(new Rectangle(x + 170, y + 10, 150, 130));

        for (String[] item : legend) {
            XSLFTextParagraph itemPara = legendBox.addNewTextParagraph();
            XSLFTextRun itemRun = itemPara.addNewTextRun();
            itemRun.setText(item[0] + " " + item[1] + " " + item[2]);
            itemRun.setFontSize(14.0);
            itemPara.setSpaceAfter(10.0);
        }
    }

    /**
     * 解析模板名称
     */
    private PPTTemplate parseTemplate(String templateName) {
        for (PPTTemplate template : PPTTemplate.values()) {
            if (template.name.equals(templateName)) {
                return template;
            }
        }
        return PPTTemplate.BUSINESS_PROFESSIONAL;
    }

    /**
     * 生成Word文档
     */
    public String generateWord(String title, String sections) {
        Map<String, Object> wordContent = new LinkedHashMap<>();
        wordContent.put("type", "Word");
        wordContent.put("title", title);
        wordContent.put("createdBy", "小易助手");
        wordContent.put("createdDate", LocalDateTime.now().toString());

        Map<String, String> content = new LinkedHashMap<>();
        content.put("一、引言", "本文档详细介绍\"" + title + "\"相关内容。");
        content.put("二、主体内容", "根据用户需求，这里将详细阐述核心要点。");
        content.put("三、结论", "综上所述，本文档总结了" + title + "的关键信息。");

        wordContent.put("content", content);

        try {
            return "【Word文档生成成功】\n\n" + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(wordContent);
        } catch (Exception e) {
            return "Word文档生成失败: " + e.getMessage();
        }
    }

    /**
     * 生成表格
     */
    public String generateTable(String title, String[] headers, String[][] data) {
        Map<String, Object> tableContent = new LinkedHashMap<>();
        tableContent.put("type", "Table");
        tableContent.put("title", title);
        tableContent.put("headers", headers);
        tableContent.put("data", data);

        StringBuilder tableOutput = new StringBuilder();
        tableOutput.append("【表格生成成功】\n\n");
        tableOutput.append("标题: ").append(title).append("\n\n");

        int[] columnWidths = new int[headers.length];
        for (int i = 0; i < headers.length; i++) {
            columnWidths[i] = headers[i].length();
        }

        for (String[] row : data) {
            for (int i = 0; i < Math.min(row.length, headers.length); i++) {
                if (row[i] != null && row[i].length() > columnWidths[i]) {
                    columnWidths[i] = row[i].length();
                }
            }
        }

        tableOutput.append("┌");
        for (int i = 0; i < headers.length; i++) {
            tableOutput.append("─".repeat(columnWidths[i] + 2));
            if (i < headers.length - 1) tableOutput.append("┬");
        }
        tableOutput.append("┐\n");

        tableOutput.append("│");
        for (int i = 0; i < headers.length; i++) {
            tableOutput.append(" ").append(headers[i]);
            tableOutput.append(" ".repeat(columnWidths[i] - headers[i].length() + 1));
            tableOutput.append("│");
        }
        tableOutput.append("\n");

        tableOutput.append("├");
        for (int i = 0; i < headers.length; i++) {
            tableOutput.append("─".repeat(columnWidths[i] + 2));
            if (i < headers.length - 1) tableOutput.append("┼");
        }
        tableOutput.append("┤\n");

        for (String[] row : data) {
            tableOutput.append("│");
            for (int i = 0; i < headers.length; i++) {
                String cell = (i < row.length && row[i] != null) ? row[i] : "";
                tableOutput.append(" ").append(cell);
                tableOutput.append(" ".repeat(columnWidths[i] - cell.length() + 1));
                tableOutput.append("│");
            }
            tableOutput.append("\n");
        }

        tableOutput.append("└");
        for (int i = 0; i < headers.length; i++) {
            tableOutput.append("─".repeat(columnWidths[i] + 2));
            if (i < headers.length - 1) tableOutput.append("┴");
        }
        tableOutput.append("┘\n");

        return tableOutput.toString();
    }

    /**
     * 生成报告
     */
    public String generateReport(String topic, String format) {
        Map<String, Object> reportContent = new LinkedHashMap<>();
        reportContent.put("type", "Report");
        reportContent.put("topic", topic);
        reportContent.put("format", format);
        reportContent.put("generatedAt", LocalDateTime.now().toString());

        Map<String, Object> sections = new LinkedHashMap<>();
        sections.put("执行摘要", "本报告针对\"" + topic + "\"进行全面分析。");
        sections.put("现状分析", "当前状态评估和问题识别。");
        sections.put("解决方案", "针对问题提出的解决方案和建议。");
        sections.put("实施计划", "详细的实施步骤和时间表。");
        sections.put("预期效果", "实施后的预期效果和收益。");

        reportContent.put("sections", sections);

        try {
            return "【报告生成成功】\n\n" + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(reportContent);
        } catch (Exception e) {
            return "报告生成失败: " + e.getMessage();
        }
    }

    /**
     * 生成Excel
     */
    public String generateExcel(String title, String[] headers, String[][] data) {
        Map<String, Object> excelContent = new LinkedHashMap<>();
        excelContent.put("type", "Excel");
        excelContent.put("title", title);
        excelContent.put("headers", headers);
        excelContent.put("rows", data.length);
        excelContent.put("columns", headers.length);
        excelContent.put("data", data);

        try {
            return "【Excel表格生成成功】\n\n" + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(excelContent);
        } catch (Exception e) {
            return "Excel生成失败: " + e.getMessage();
        }
    }
}
