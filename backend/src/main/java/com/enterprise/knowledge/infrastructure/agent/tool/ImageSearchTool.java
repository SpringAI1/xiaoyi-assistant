package com.enterprise.knowledge.infrastructure.agent.tool;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ImageSearchTool {

    public String searchImages(String query) {
        try {
            StringBuilder result = new StringBuilder();
            result.append("🖼️ 图片搜索结果：").append(query).append("\n\n");
            
            List<ImageResult> images = performImageSearch(query);
            
            result.append("找到 ").append(images.size()).append(" 张相关图片：\n\n");
            
            for (int i = 0; i < images.size(); i++) {
                ImageResult img = images.get(i);
                result.append("**图片 ").append(i + 1).append("**\n");
                result.append("📷 主题：").append(img.title).append("\n");
                result.append("📐 尺寸：").append(img.dimensions).append("\n");
                result.append("🎨 格式：").append(img.format).append("\n");
                result.append("📝 描述：").append(img.description).append("\n");
                result.append("⭐ 质量评分：").append(img.qualityScore).append("/10\n");
                result.append("\n");
            }
            
            result.append("💡 **智能推荐建议：**\n\n");
            result.append(generateRecommendations(query, images));
            
            return result.toString();
            
        } catch (Exception e) {
            return "图片搜索失败: " + e.getMessage();
        }
    }

    private List<ImageResult> performImageSearch(String query) {
        List<ImageResult> images = new ArrayList<>();
        
        if (query.contains("汽车") || query.contains("车")) {
            images.add(new ImageResult(
                "豪华轿车正面视图",
                "1920x1080",
                "JPEG",
                "高端轿车正面全身照，展现优雅流线型设计和品牌标志性前脸",
                9.2,
                "https://images.example.com/car1.jpg"
            ));
            images.add(new ImageResult(
                "汽车内饰驾驶舱",
                "2560x1440",
                "JPEG",
                "精致内饰布局，配备多功能方向盘和中控大屏",
                8.8,
                "https://images.example.com/car2.jpg"
            ));
            images.add(new ImageResult(
                "城市道路行驶中",
                "3840x2160",
                "PNG",
                "汽车在城市街道行驶场景，展现动态美感",
                9.5,
                "https://images.example.com/car3.jpg"
            ));
            images.add(new ImageResult(
                "汽车尾部设计",
                "1920x1080",
                "JPEG",
                "现代感十足的尾部设计，贯穿式尾灯组",
                8.6,
                "https://images.example.com/car4.jpg"
            ));
        } else if (query.contains("风景") || query.contains("自然")) {
            images.add(new ImageResult(
                "日出云海",
                "3840x2160",
                "JPEG",
                "高山之巅俯瞰云海日出，金色阳光洒满山谷",
                9.8,
                "https://images.example.com/scene1.jpg"
            ));
            images.add(new ImageResult(
                "森林溪流",
                "2560x1440",
                "PNG",
                "清澈溪水穿过翠绿森林，自然和谐之美",
                9.3,
                "https://images.example.com/scene2.jpg"
            ));
        } else if (query.contains("建筑") || query.contains("城市")) {
            images.add(new ImageResult(
                "现代摩天大楼",
                "3840x2160",
                "JPEG",
                "CBD核心区超高层建筑群，玻璃幕墙反射蓝天",
                9.1,
                "https://images.example.com/building1.jpg"
            ));
            images.add(new ImageResult(
                "历史文化建筑",
                "2560x1600",
                "JPEG",
                "传统中式古建筑群，飞檐翘角雕梁画栋",
                9.4,
                "https://images.example.com/building2.jpg"
            ));
        } else {
            images.add(new ImageResult(
                "通用主题图片",
                "1920x1080",
                "JPEG",
                "高质量通用主题配图，适用于多种场景",
                8.5,
                "https://images.example.com/generic1.jpg"
            ));
            images.add(new ImageResult(
                "创意设计素材",
                "2560x1440",
                "PNG",
                "现代简约风格设计素材，透明背景便于编辑",
                8.9,
                "https://images.example.com/design1.jpg"
            ));
            images.add(new ImageResult(
                "自然风光摄影",
                "3840x2160",
                "JPEG",
                "专业摄影师拍摄的自然风光作品，色彩鲜明层次丰富",
                9.0,
                "https://images.example.com/nature1.jpg"
            ));
        }
        
        return images;
    }

    private String generateRecommendations(String query, List<ImageResult> images) {
        StringBuilder rec = new StringBuilder();
        
        ImageResult bestMatch = images.stream()
                .max((a, b) -> Double.compare(a.qualityScore, b.qualityScore))
                .orElse(images.get(0));
        
        rec.append("根据您的搜索「").append(query).append("」，推荐以下方案：\n\n");
        
        rec.append("✨ **最佳选择**：").append(bestMatch.title).append("\n");
        rec.append("   理由：该图片质量评分最高（").append(bestMatch.qualityScore).append("/10），");
        rec.append("分辨率达到").append(bestMatch.dimensions).append("，");
        rec.append("适合用于：").append(bestMatch.format).append("格式\n\n");
        
        rec.append("📋 **使用场景建议**：\n");
        if (query.contains("汽车")) {
            rec.append("• 适合用于：产品展示、宣传册、网站配图\n");
            rec.append("• 推荐尺寸：1920x1080（网页）或 2560x1440（印刷）\n");
            rec.append("• 版权说明：建议获取正式授权或使用版权图片\n");
        } else if (query.contains("风景")) {
            rec.append("• 适合用于：壁纸、演示文稿、社交媒体配图\n");
            rec.append("• 推荐尺寸：3840x2160（4K高清）\n");
            rec.append("• 滤镜建议：可适当调整亮度或添加淡雅滤镜\n");
        } else {
            rec.append("• 适合用于：演示文稿、网站配图、创意设计\n");
            rec.append("• 推荐尺寸：根据具体用途选择合适分辨率\n");
            rec.append("• 编辑建议：PNG格式适合后期编辑\n");
        }
        
        return rec.toString();
    }

    private static class ImageResult {
        String title;
        String dimensions;
        String format;
        String description;
        double qualityScore;
        String url;

        ImageResult(String title, String dimensions, String format, 
                   String description, double qualityScore, String url) {
            this.title = title;
            this.dimensions = dimensions;
            this.format = format;
            this.description = description;
            this.qualityScore = qualityScore;
            this.url = url;
        }
    }
}
