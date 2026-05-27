package com.enterprise.knowledge.infrastructure.agent.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.sl.usermodel.TextParagraph;
import org.apache.poi.xslf.usermodel.*;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.List;

@Component
public class DocumentGeneratorTool {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WebSearchTool webSearchTool;
    
    // 定义模板样式
    public enum PPTTemplate {
        BUSINESS_PROFESSIONAL("商务专业", new Color(0, 51, 102), new Color(255, 255, 255)),
        TECH_INNOVATION("科技风格", new Color(0, 128, 128), new Color(240, 248, 255)),
        CREATIVE_DESIGN("创意设计", new Color(139, 0, 0), new Color(255, 250, 240)),
        MODERN_MINIMAL("现代简约", new Color(70, 130, 180), new Color(255, 255, 255)),
        EDUCATIONAL("教育风格", new Color(0, 100, 0), new Color(245, 245, 220));
        
        final String name;
        final Color primaryColor;
        final Color backgroundColor;
        
        PPTTemplate(String name, Color primaryColor, Color backgroundColor) {
            this.name = name;
            this.primaryColor = primaryColor;
            this.backgroundColor = backgroundColor;
        }
    }
    
    // 定义动画类型
    public enum AnimationType {
        FADE_IN,
        FLY_IN_FROM_LEFT,
        FLY_IN_FROM_RIGHT,
        FLY_IN_FROM_TOP,
        FLY_IN_FROM_BOTTOM,
        WIPE,
        ZOOM_IN
    }

    public DocumentGeneratorTool(WebSearchTool webSearchTool) {
        this.webSearchTool = webSearchTool;
    }

    public String generatePPT(String topic, String structure) {
        Map<String, Object> pptContent = new LinkedHashMap<>();
        pptContent.put("type", "PPT");
        pptContent.put("topic", topic);
        
        Map<String, Object> slides = new LinkedHashMap<>();
        
        slides.put("封面页", Map.of(
            "title", topic,
            "subtitle", "专业演示文稿",
            "author", "小易助手",
            "date", java.time.LocalDate.now().toString()
        ));

        slides.put("目录页", Map.of(
            "title", "目录",
            "contents", new String[]{"概述", "主要内容", "详细分析", "总结展望"}
        ));

        slides.put("概述页", Map.of(
            "title", "概述",
            "content", "本节介绍" + topic + "的基本概念和背景信息。"
        ));

        slides.put("内容页1", Map.of(
            "title", "核心要点",
            "points", new String[]{"要点一：核心价值", "要点二：关键特性", "要点三：应用场景"}
        ));

        slides.put("内容页2", Map.of(
            "title", "详细分析",
            "content", "深入分析" + topic + "的实施策略和最佳实践。"
        ));

        slides.put("总结页", Map.of(
            "title", "总结与展望",
            "summary", "总结" + topic + "的主要内容和未来发展方向。"
        ));

        pptContent.put("slides", slides);

        try {
            return "【PPT生成成功】\n\n" + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(pptContent);
        } catch (Exception e) {
            return "PPT生成失败: " + e.getMessage();
        }
    }

    public byte[] generatePPTDocument(String topic) throws Exception {
        return generatePPTDocument(topic, PPTTemplate.BUSINESS_PROFESSIONAL);
    }

    public byte[] generatePPTDocument(String topic, PPTTemplate template) throws Exception {
        XMLSlideShow ppt = new XMLSlideShow();
        
        try {
            List<String> topicInfo = webSearchTool.getTopicInformation(topic);
            
            // 设置幻灯片母版和主题
            applyTemplateTheme(ppt, template);
            
            // ========== 1. 封面页 - 专业设计 ==========
            XSLFSlide coverSlide = ppt.createSlide();
            createCoverSlide(coverSlide, topic, template);
            
            // ========== 2. 目录页 ==========
            XSLFSlide tocSlide = ppt.createSlide();
            createTocSlide(tocSlide, template);

            // ========== 3. 公司概览页 ==========
            XSLFSlide overviewSlide = ppt.createSlide();
            createOverviewSlide(overviewSlide, topic, template);

            // ========== 4. 数据图表页 ==========
            XSLFSlide chartSlide = ppt.createSlide();
            createChartSlide(chartSlide, topic, template);

            // ========== 5. 核心业务页 ==========
            XSLFSlide businessSlide = ppt.createSlide();
            createBusinessSlide(businessSlide, topic, template);

            // ========== 6. 竞争优势页 ==========
            XSLFSlide advantageSlide = ppt.createSlide();
            createAdvantageSlide(advantageSlide, topic, template);

            // ========== 7. 发展战略页 ==========
            XSLFSlide strategySlide = ppt.createSlide();
            createStrategySlide(strategySlide, topic, template);

            // ========== 8. 流程演示页 ==========
            XSLFSlide processSlide = ppt.createSlide();
            createProcessSlide(processSlide, topic, template);

            // ========== 9. 未来展望页 ==========
            XSLFSlide futureSlide = ppt.createSlide();
            createFutureSlide(futureSlide, topic, template);

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
    
    private void applyTemplateTheme(XMLSlideShow ppt, PPTTemplate template) {
        // 设置默认字体和样式
        // 注意：Apache POI对PPT主题支持有限，主要通过手动设置颜色实现
    }
    
    private void createCoverSlide(XSLFSlide slide, String topic, PPTTemplate template) {
        // 添加背景
        XSLFAutoShape bg = slide.createAutoShape();
        bg.setAnchor(new Rectangle(0, 0, 720, 540));
        bg.setShapeType(org.apache.poi.sl.usermodel.ShapeType.RECT);
        bg.setFillColor(template.backgroundColor);
        
        // 装饰性左侧色块
        XSLFAutoShape accentBar = slide.createAutoShape();
        accentBar.setAnchor(new Rectangle(0, 0, 40, 540));
        accentBar.setShapeType(org.apache.poi.sl.usermodel.ShapeType.RECT);
        accentBar.setFillColor(template.primaryColor);
        
        // 标题
        XSLFTextBox titleBox = slide.createTextBox();
        titleBox.setAnchor(new Rectangle(70, 140, 600, 120));
        XSLFTextParagraph titlePara = titleBox.addNewTextParagraph();
        titlePara.setTextAlign(TextParagraph.TextAlign.CENTER);
        XSLFTextRun titleRun = titlePara.addNewTextRun();
        titleRun.setText(topic);
        titleRun.setFontSize(48.0);
        titleRun.setBold(true);
        titleRun.setFontColor(template.primaryColor);

        // 副标题
        XSLFTextBox subtitleBox = slide.createTextBox();
        subtitleBox.setAnchor(new Rectangle(70, 280, 600, 60));
        XSLFTextParagraph subtitlePara = subtitleBox.addNewTextParagraph();
        subtitlePara.setTextAlign(TextParagraph.TextAlign.CENTER);
        XSLFTextRun subtitleRun = subtitlePara.addNewTextRun();
        subtitleRun.setText("专业演示文稿 | " + template.name);
        subtitleRun.setFontSize(24.0);
        subtitleRun.setFontColor(new Color(80, 80, 80));

        // 日期和作者
        XSLFTextBox infoBox = slide.createTextBox();
        infoBox.setAnchor(new Rectangle(70, 400, 600, 40));
        XSLFTextParagraph infoPara = infoBox.addNewTextParagraph();
        infoPara.setTextAlign(TextParagraph.TextAlign.CENTER);
        XSLFTextRun infoRun = infoPara.addNewTextRun();
        infoRun.setText("小易助手 · " + java.time.LocalDate.now().toString());
        infoRun.setFontSize(16.0);
        infoRun.setFontColor(new Color(120, 120, 120));
    }
    
    private void createTocSlide(XSLFSlide slide, PPTTemplate template) {
        addDecorativeHeader(slide, template);
        addProfessionalTitle(slide, "目录", template);
        
        String[] tocItems = {
            "📋 公司概览",
            "📊 数据分析", 
            "🎯 核心业务",
            "💡 竞争优势",
            "🚀 发展战略",
            "⚡ 流程演示",
            "✨ 未来展望"
        };
        
        // 两列布局
        int leftX = 80, rightX = 400;
        int startY = 130;
        
        for (int i = 0; i < tocItems.length; i++) {
            int x = (i < 4) ? leftX : rightX;
            int y = startY + ((i < 4) ? i : i - 4) * 50;
            
            XSLFTextBox itemBox = slide.createTextBox();
            itemBox.setAnchor(new Rectangle(x, y, 280, 40));
            
            XSLFTextParagraph itemPara = itemBox.addNewTextParagraph();
            XSLFTextRun itemRun = itemPara.addNewTextRun();
            itemRun.setText((i + 1) + ". " + tocItems[i]);
            itemRun.setFontSize(18.0);
            itemRun.setBold(true);
            itemRun.setFontColor(template.primaryColor);
        }
    }
    
    private void createOverviewSlide(XSLFSlide slide, String topic, PPTTemplate template) {
        addDecorativeHeader(slide, template);
        addProfessionalTitle(slide, "公司概览", template);
        
        // 左侧内容 - 使命愿景
        XSLFTextBox leftBox = slide.createTextBox();
        leftBox.setAnchor(new Rectangle(50, 120, 300, 380));
        
        String[] overviewItems = {
            "📌 企业使命",
            "致力于成为行业领先的解决方案提供商，" + 
            "为客户创造价值，为社会贡献力量。",
            "",
            "🎯 企业愿景",
            "引领行业创新，成为受尊敬的行业领导者，" +
            "持续推动技术进步。",
            "",
            "💡 核心价值观",
            "创新 · 协作 · 诚信 · 卓越"
        };
        
        for (String item : overviewItems) {
            XSLFTextParagraph para = leftBox.addNewTextParagraph();
            XSLFTextRun run = para.addNewTextRun();
            run.setText(item);
            
            if (item.contains("📌") || item.contains("🎯") || item.contains("💡")) {
                run.setFontSize(20.0);
                run.setBold(true);
                run.setFontColor(template.primaryColor);
            } else if (!item.isEmpty()) {
                run.setFontSize(14.0);
                para.setIndent(10.0);
            }
        }
        
        // 右侧数据卡片
        int cardX = 370, cardY = 130;
        int cardWidth = 150, cardHeight = 90;
        
        String[][] stats = {
            {"2018", "成立年份"},
            {"500+", "员工规模"},
            {"30+", "覆盖城市"},
            {"ISO", "认证资质"}
        };
        
        for (int i = 0; i < stats.length; i++) {
            int x = cardX + (i % 2) * (cardWidth + 20);
            int y = cardY + (i / 2) * (cardHeight + 20);
            
            // 卡片背景
            XSLFAutoShape cardBg = slide.createAutoShape();
            cardBg.setAnchor(new Rectangle(x, y, cardWidth, cardHeight));
            cardBg.setShapeType(org.apache.poi.sl.usermodel.ShapeType.RECT);
            cardBg.setFillColor(template.primaryColor);
            
            // 卡片内容
            XSLFTextBox cardBox = slide.createTextBox();
            cardBox.setAnchor(new Rectangle(x + 10, y + 15, cardWidth - 20, cardHeight - 30));
            
            XSLFTextParagraph numPara = cardBox.addNewTextParagraph();
            numPara.setTextAlign(TextParagraph.TextAlign.CENTER);
            XSLFTextRun numRun = numPara.addNewTextRun();
            numRun.setText(stats[i][0]);
            numRun.setFontSize(24.0);
            numRun.setBold(true);
            numRun.setFontColor(Color.WHITE);
            
            XSLFTextParagraph labelPara = cardBox.addNewTextParagraph();
            labelPara.setTextAlign(TextParagraph.TextAlign.CENTER);
            XSLFTextRun labelRun = labelPara.addNewTextRun();
            labelRun.setText(stats[i][1]);
            labelRun.setFontSize(12.0);
            labelRun.setFontColor(new Color(230, 230, 230));
        }
    }
    
    private void createChartSlide(XSLFSlide slide, String topic, PPTTemplate template) {
        addDecorativeHeader(slide, template);
        addProfessionalTitle(slide, "数据概览", template);
        
        // 创建模拟数据图表（使用文本可视化）
        XSLFTextBox chartTitle = slide.createTextBox();
        chartTitle.setAnchor(new Rectangle(50, 110, 620, 30));
        XSLFTextParagraph chartTitlePara = chartTitle.addNewTextParagraph();
        XSLFTextRun chartTitleRun = chartTitlePara.addNewTextRun();
        chartTitleRun.setText("📊 业务增长趋势");
        chartTitleRun.setFontSize(16.0);
        chartTitleRun.setBold(true);
        chartTitleRun.setFontColor(template.primaryColor);
        
        // 模拟柱状图
        String[][] chartData = {
            {"2021", "50", "███"},
            {"2022", "80", "█████"},
            {"2023", "120", "████████"},
            {"2024", "180", "███████████"},
            {"2025", "240", "██████████████"}
        };
        
        int chartX = 80, chartY = 150;
        
        for (int i = 0; i < chartData.length; i++) {
            int y = chartY + i * 50;
            
            // 年份
            XSLFTextBox yearBox = slide.createTextBox();
            yearBox.setAnchor(new Rectangle(chartX, y, 60, 30));
            XSLFTextParagraph yearPara = yearBox.addNewTextParagraph();
            XSLFTextRun yearRun = yearPara.addNewTextRun();
            yearRun.setText(chartData[i][0]);
            yearRun.setFontSize(14.0);
            yearRun.setBold(true);
            
            // 数值
            XSLFTextBox valueBox = slide.createTextBox();
            valueBox.setAnchor(new Rectangle(chartX + 70, y, 50, 30));
            XSLFTextParagraph valuePara = valueBox.addNewTextParagraph();
            XSLFTextRun valueRun = valuePara.addNewTextRun();
            valueRun.setText(chartData[i][1] + "M");
            valueRun.setFontSize(14.0);
            valueRun.setFontColor(template.primaryColor);
            
            // 模拟柱子
            XSLFTextBox barBox = slide.createTextBox();
            barBox.setAnchor(new Rectangle(chartX + 130, y, 400, 30));
            XSLFTextParagraph barPara = barBox.addNewTextParagraph();
            XSLFTextRun barRun = barPara.addNewTextRun();
            barRun.setText(chartData[i][2]);
            barRun.setFontSize(24.0);
            barRun.setFontColor(template.primaryColor);
        }
        
        // 右侧饼图模拟
        XSLFTextBox legendBox = slide.createTextBox();
        legendBox.setAnchor(new Rectangle(420, 180, 250, 300));
        
        String[] legend = {
            "🟢 研发收入  40%",
            "🔵 服务收入  35%",
            "🟡 产品销售  15%",
            "🟠 其他收入  10%"
        };
        
        XSLFTextParagraph legendTitle = legendBox.addNewTextParagraph();
        XSLFTextRun legendTitleRun = legendTitle.addNewTextRun();
        legendTitleRun.setText("收入构成");
        legendTitleRun.setFontSize(16.0);
        legendTitleRun.setBold(true);
        legendTitle.setSpaceAfter(20.0);
        
        for (String item : legend) {
            XSLFTextParagraph itemPara = legendBox.addNewTextParagraph();
            XSLFTextRun itemRun = itemPara.addNewTextRun();
            itemRun.setText(item);
            itemRun.setFontSize(14.0);
            itemPara.setSpaceAfter(10.0);
        }
    }
    
    private void createBusinessSlide(XSLFSlide slide, String topic, PPTTemplate template) {
        addDecorativeHeader(slide, template);
        addProfessionalTitle(slide, "核心业务", template);
        
        String[][] businessCards = {
            {"🤖 AI研发服务", "自然语言处理、计算机视觉、机器学习、深度学习"},
            {"☁️ 云计算解决方案", "私有云部署、混合云架构、云安全、容器化"},
            {"📊 数据分析服务", "大数据分析、商业智能、数据可视化、数据挖掘"},
            {"💼 技术咨询服务", "架构设计、技术评估、实施指导、培训"}
        };
        
        int startX = 50;
        int startY = 130;
        int cardWidth = 300;
        int cardHeight = 160;
        
        for (int i = 0; i < businessCards.length; i++) {
            int x = startX + (i % 2) * (cardWidth + 20);
            int y = startY + (i / 2) * (cardHeight + 20);
            
            // 卡片背景
            XSLFAutoShape cardBg = slide.createAutoShape();
            cardBg.setAnchor(new Rectangle(x, y, cardWidth, cardHeight));
            cardBg.setShapeType(org.apache.poi.sl.usermodel.ShapeType.RECT);
            cardBg.setFillColor(new Color(template.primaryColor.getRed(), 
                                         template.primaryColor.getGreen(), 
                                         template.primaryColor.getBlue(), 30));
            
            // 卡片内容
            XSLFTextBox cardBox = slide.createTextBox();
            cardBox.setAnchor(new Rectangle(x + 15, y + 15, cardWidth - 30, cardHeight - 30));
            
            XSLFTextParagraph cardTitlePara = cardBox.addNewTextParagraph();
            XSLFTextRun cardTitleRun = cardTitlePara.addNewTextRun();
            cardTitleRun.setText(businessCards[i][0]);
            cardTitleRun.setFontSize(20.0);
            cardTitleRun.setBold(true);
            cardTitleRun.setFontColor(template.primaryColor);
            cardTitlePara.setSpaceAfter(10.0);
            
            XSLFTextParagraph descPara = cardBox.addNewTextParagraph();
            XSLFTextRun descRun = descPara.addNewTextRun();
            descRun.setText(businessCards[i][1]);
            descRun.setFontSize(14.0);
            descPara.setIndent(10.0);
        }
    }
    
    private void createAdvantageSlide(XSLFSlide slide, String topic, PPTTemplate template) {
        addDecorativeHeader(slide, template);
        addProfessionalTitle(slide, "竞争优势", template);
        
        String[][] advantages = {
            {"🎯", "技术领先", "拥有核心技术专利50+项，技术团队占比70%以上"},
            {"🚀", "快速响应", "7x24小时技术支持，平均响应时间小于1小时"},
            {"💼", "行业经验", "服务超过1000家企业客户，覆盖多个行业"},
            {"📈", "持续创新", "每年研发投入占营收15%以上，保持技术领先"}
        };
        
        int startY = 130;
        int itemHeight = 90;
        
        for (int i = 0; i < advantages.length; i++) {
            int y = startY + i * itemHeight;
            
            // 图标
            XSLFTextBox iconBox = slide.createTextBox();
            iconBox.setAnchor(new Rectangle(50, y, 50, 50));
            XSLFTextParagraph iconPara = iconBox.addNewTextParagraph();
            iconPara.setTextAlign(TextParagraph.TextAlign.CENTER);
            XSLFTextRun iconRun = iconPara.addNewTextRun();
            iconRun.setText(advantages[i][0]);
            iconRun.setFontSize(32.0);
            
            // 内容
            XSLFTextBox contentBox = slide.createTextBox();
            contentBox.setAnchor(new Rectangle(120, y, 550, itemHeight - 10));
            
            XSLFTextParagraph titlePara = contentBox.addNewTextParagraph();
            XSLFTextRun titleRun = titlePara.addNewTextRun();
            titleRun.setText(advantages[i][1]);
            titleRun.setFontSize(20.0);
            titleRun.setBold(true);
            titleRun.setFontColor(template.primaryColor);
            
            XSLFTextParagraph descPara = contentBox.addNewTextParagraph();
            XSLFTextRun descRun = descPara.addNewTextRun();
            descRun.setText(advantages[i][2]);
            descRun.setFontSize(14.0);
            descPara.setIndent(5.0);
        }
    }
    
    private void createStrategySlide(XSLFSlide slide, String topic, PPTTemplate template) {
        addDecorativeHeader(slide, template);
        addProfessionalTitle(slide, "发展战略", template);
        
        String[][] strategies = {
            {"📊", "短期目标（1-2年）", "• 深耕核心业务，提升服务质量\n• 拓展区域市场，增加市场份额"},
            {"📈", "中期目标（3-5年）", "• 布局新兴技术领域\n• 打造行业标杆案例\n• 建立技术标准"},
            {"🎯", "长期目标（5年+）", "• 成为行业领导者\n• 构建生态合作伙伴体系\n• 全球化布局"}
        };
        
        int startY = 130;
        
        for (int i = 0; i < strategies.length; i++) {
            int x = 80 + i * 200;
            
            // 目标卡片
            XSLFAutoShape cardBg = slide.createAutoShape();
            cardBg.setAnchor(new Rectangle(x, startY, 180, 350));
            cardBg.setShapeType(org.apache.poi.sl.usermodel.ShapeType.RECT);
            cardBg.setFillColor(new Color(template.primaryColor.getRed(), 
                                         template.primaryColor.getGreen(), 
                                         template.primaryColor.getBlue(), 20));
            
            XSLFTextBox cardBox = slide.createTextBox();
            cardBox.setAnchor(new Rectangle(x + 15, startY + 15, 150, 320));
            
            // 图标
            XSLFTextParagraph iconPara = cardBox.addNewTextParagraph();
            iconPara.setTextAlign(TextParagraph.TextAlign.CENTER);
            XSLFTextRun iconRun = iconPara.addNewTextRun();
            iconRun.setText(strategies[i][0]);
            iconRun.setFontSize(40.0);
            iconPara.setSpaceAfter(15.0);
            
            // 标题
            XSLFTextParagraph titlePara = cardBox.addNewTextParagraph();
            titlePara.setTextAlign(TextParagraph.TextAlign.CENTER);
            XSLFTextRun titleRun = titlePara.addNewTextRun();
            titleRun.setText(strategies[i][1]);
            titleRun.setFontSize(16.0);
            titleRun.setBold(true);
            titleRun.setFontColor(template.primaryColor);
            titlePara.setSpaceAfter(20.0);
            
            // 内容
            String[] items = strategies[i][2].split("\n");
            for (String item : items) {
                XSLFTextParagraph itemPara = cardBox.addNewTextParagraph();
                XSLFTextRun itemRun = itemPara.addNewTextRun();
                itemRun.setText(item);
                itemRun.setFontSize(12.0);
                itemPara.setSpaceAfter(8.0);
            }
        }
    }
    
    private void createProcessSlide(XSLFSlide slide, String topic, PPTTemplate template) {
        addDecorativeHeader(slide, template);
        addProfessionalTitle(slide, "流程演示", template);
        
        String[][] processSteps = {
            {"1", "需求分析", "深入了解客户需求，制定解决方案"},
            {"2", "方案设计", "基于AI技术，设计最佳实施路径"},
            {"3", "开发实施", "快速迭代开发，保证交付质量"},
            {"4", "测试验证", "严格测试流程，确保系统稳定"},
            {"5", "上线运营", "平滑上线，持续优化改进"}
        };
        
        int startY = 180;
        
        for (int i = 0; i < processSteps.length; i++) {
            int x = 50 + i * 130;
            
            // 圆形步骤标记 - 使用RECT替代，避免编译错误
            XSLFAutoShape circle = slide.createAutoShape();
            circle.setAnchor(new Rectangle(x, startY, 60, 60));
            circle.setShapeType(org.apache.poi.sl.usermodel.ShapeType.RECT);
            circle.setFillColor(template.primaryColor);
            
            XSLFTextBox numBox = slide.createTextBox();
            numBox.setAnchor(new Rectangle(x, startY + 10, 60, 40));
            XSLFTextParagraph numPara = numBox.addNewTextParagraph();
            numPara.setTextAlign(TextParagraph.TextAlign.CENTER);
            XSLFTextRun numRun = numPara.addNewTextRun();
            numRun.setText(processSteps[i][0]);
            numRun.setFontSize(28.0);
            numRun.setBold(true);
            numRun.setFontColor(Color.WHITE);
            
            // 步骤标题
            XSLFTextBox titleBox = slide.createTextBox();
            titleBox.setAnchor(new Rectangle(x - 10, startY + 75, 80, 30));
            XSLFTextParagraph titlePara = titleBox.addNewTextParagraph();
            titlePara.setTextAlign(TextParagraph.TextAlign.CENTER);
            XSLFTextRun titleRun = titlePara.addNewTextRun();
            titleRun.setText(processSteps[i][1]);
            titleRun.setFontSize(14.0);
            titleRun.setBold(true);
            
            // 步骤描述
            XSLFTextBox descBox = slide.createTextBox();
            descBox.setAnchor(new Rectangle(x - 15, startY + 105, 90, 60));
            XSLFTextParagraph descPara = descBox.addNewTextParagraph();
            descPara.setTextAlign(TextParagraph.TextAlign.CENTER);
            XSLFTextRun descRun = descPara.addNewTextRun();
            descRun.setText(processSteps[i][2]);
            descRun.setFontSize(10.0);
            
            // 箭头（除了最后一个）
            if (i < processSteps.length - 1) {
                XSLFTextBox arrowBox = slide.createTextBox();
                arrowBox.setAnchor(new Rectangle(x + 65, startY + 20, 30, 20));
                XSLFTextParagraph arrowPara = arrowBox.addNewTextParagraph();
                arrowPara.setTextAlign(TextParagraph.TextAlign.CENTER);
                XSLFTextRun arrowRun = arrowPara.addNewTextRun();
                arrowRun.setText("→");
                arrowRun.setFontSize(24.0);
                arrowRun.setFontColor(template.primaryColor);
            }
        }
    }
    
    private void createFutureSlide(XSLFSlide slide, String topic, PPTTemplate template) {
        addDecorativeHeader(slide, template);
        addProfessionalTitle(slide, "未来展望", template);
        
        XSLFTextBox futureBox = slide.createTextBox();
        futureBox.setAnchor(new Rectangle(60, 130, 600, 350));
        
        XSLFTextParagraph futurePara1 = futureBox.addNewTextParagraph();
        futurePara1.setTextAlign(TextParagraph.TextAlign.CENTER);
        XSLFTextRun futureRun1 = futurePara1.addNewTextRun();
        futureRun1.setText("🚀 展望未来");
        futureRun1.setFontSize(36.0);
        futureRun1.setBold(true);
        futureRun1.setFontColor(template.primaryColor);
        futurePara1.setSpaceAfter(30.0);
        
        XSLFTextParagraph futurePara2 = futureBox.addNewTextParagraph();
        futurePara2.setTextAlign(TextParagraph.TextAlign.CENTER);
        XSLFTextRun futureRun2 = futurePara2.addNewTextRun();
        futureRun2.setText("在数字化转型的浪潮中，我们将继续秉持创新精神，\n" +
            "以技术驱动发展，以服务创造价值，\n" +
            "与客户携手共创美好未来！");
        futureRun2.setFontSize(20.0);
        futurePara2.setSpaceAfter(40.0);
        
        XSLFTextParagraph futurePara3 = futureBox.addNewTextParagraph();
        futurePara3.setTextAlign(TextParagraph.TextAlign.CENTER);
        XSLFTextRun futureRun3 = futurePara3.addNewTextRun();
        futureRun3.setText("✨ 期待与您合作 ✨");
        futureRun3.setFontSize(28.0);
        futureRun3.setBold(true);
    }
    
    private void createEndSlide(XSLFSlide slide, PPTTemplate template) {
        // 背景
        XSLFAutoShape bg = slide.createAutoShape();
        bg.setAnchor(new Rectangle(0, 0, 720, 540));
        bg.setShapeType(org.apache.poi.sl.usermodel.ShapeType.RECT);
        bg.setFillColor(template.primaryColor);
        
        // 感谢语
        XSLFTextBox endBox = slide.createTextBox();
        endBox.setAnchor(new Rectangle(50, 160, 620, 120));
        XSLFTextParagraph endPara = endBox.addNewTextParagraph();
        endPara.setTextAlign(TextParagraph.TextAlign.CENTER);
        XSLFTextRun endRun = endPara.addNewTextRun();
        endRun.setText("谢谢观看！");
        endRun.setFontSize(56.0);
        endRun.setBold(true);
        endRun.setFontColor(Color.WHITE);
        
        // 联系方式
        XSLFTextBox contactBox = slide.createTextBox();
        contactBox.setAnchor(new Rectangle(50, 320, 620, 100));
        XSLFTextParagraph contactPara = contactBox.addNewTextParagraph();
        contactPara.setTextAlign(TextParagraph.TextAlign.CENTER);
        XSLFTextRun contactRun = contactPara.addNewTextRun();
        contactRun.setText("联系方式：contact@example.com\n电话：400-888-8888");
        contactRun.setFontSize(20.0);
        contactRun.setFontColor(new Color(240, 240, 240));
    }
    
    private void addDecorativeHeader(XSLFSlide slide, PPTTemplate template) {
        // 添加顶部装饰条
        XSLFAutoShape headerBar = slide.createAutoShape();
        headerBar.setAnchor(new Rectangle(0, 0, 720, 8));
        headerBar.setShapeType(org.apache.poi.sl.usermodel.ShapeType.RECT);
        headerBar.setFillColor(template.primaryColor);
        
        // 添加左侧装饰条
        XSLFAutoShape leftBar = slide.createAutoShape();
        leftBar.setAnchor(new Rectangle(0, 0, 15, 540));
        leftBar.setShapeType(org.apache.poi.sl.usermodel.ShapeType.RECT);
        leftBar.setFillColor(template.primaryColor);
    }
    
    private void addProfessionalTitle(XSLFSlide slide, String title, PPTTemplate template) {
        XSLFTextBox titleBox = slide.createTextBox();
        titleBox.setAnchor(new Rectangle(50, 30, 620, 60));
        XSLFTextParagraph titlePara = titleBox.addNewTextParagraph();
        XSLFTextRun titleRun = titlePara.addNewTextRun();
        titleRun.setText(title);
        titleRun.setFontSize(36.0);
        titleRun.setBold(true);
        titleRun.setFontColor(template.primaryColor);
    }
    
    private void addProfessionalTitle(XSLFSlide slide, String title) {
        addProfessionalTitle(slide, title, PPTTemplate.BUSINESS_PROFESSIONAL);
    }

    private void addTitleToSlide(XSLFSlide slide, String title) {
        XSLFTextBox titleBox = slide.createTextBox();
        titleBox.setAnchor(new Rectangle(50, 40, 600, 50));
        XSLFTextParagraph titlePara = titleBox.addNewTextParagraph();
        XSLFTextRun titleRun = titlePara.addNewTextRun();
        titleRun.setText(title);
        titleRun.setFontSize(32.0);
        titleRun.setBold(true);
    }

    public String generateWord(String title, String sections) {
        Map<String, Object> wordContent = new LinkedHashMap<>();
        wordContent.put("type", "Word");
        wordContent.put("title", title);
        wordContent.put("createdBy", "小易助手");
        wordContent.put("createdDate", java.time.LocalDateTime.now().toString());

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

    public String generateReport(String topic, String format) {
        Map<String, Object> reportContent = new LinkedHashMap<>();
        reportContent.put("type", "Report");
        reportContent.put("topic", topic);
        reportContent.put("format", format);
        reportContent.put("generatedAt", java.time.LocalDateTime.now().toString());

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
