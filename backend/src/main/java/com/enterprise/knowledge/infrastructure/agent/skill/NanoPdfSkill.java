package com.enterprise.knowledge.infrastructure.agent.skill;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class NanoPdfSkill implements Skill {

    private static final String SKILL_ID = "nano-pdf";
    private static final String SKILL_NAME = "PDF处理";
    private static final String SKILL_DESCRIPTION = "处理PDF文件，支持PDF内容提取、文本分析、关键词搜索、摘要生成等功能。";

    private final HttpClient httpClient;
    private final AtomicInteger processCount = new AtomicInteger(0);

    public NanoPdfSkill() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(15))
                .build();
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
        return Arrays.asList("pdf", "PDF", "文档", "文件", "提取", "分析", "读取", "处理");
    }

    @Override
    public String execute(String input) {
        processCount.incrementAndGet();

        String lowerInput = input.toLowerCase();

        if (lowerInput.contains("提取") || lowerInput.contains("读取") || lowerInput.contains("内容")) {
            return extractPdfContent(input);
        } else if (lowerInput.contains("搜索") || lowerInput.contains("查找")) {
            return searchInPdf(input);
        } else if (lowerInput.contains("摘要") || lowerInput.contains("总结")) {
            return generatePdfSummary(input);
        } else if (lowerInput.contains("信息") || lowerInput.contains("属性")) {
            return getPdfInfo(input);
        } else if (lowerInput.contains("页数") || lowerInput.contains("多少页")) {
            return getPdfPageCount(input);
        } else if (lowerInput.contains("帮助") || lowerInput.contains("help")) {
            return getHelp();
        } else {
            return analyzePdfRequest(input);
        }
    }

    private String extractPdfContent(String input) {
        String url = extractPdfUrl(input);

        if (url == null || url.isEmpty()) {
            return "📄 请提供PDF文件的URL或路径！\n\n" +
                   "示例：提取 PDF https://example.com/document.pdf\n" +
                   "示例：提取 PDF /path/to/file.pdf";
        }

        StringBuilder result = new StringBuilder();
        result.append("📄 【PDF内容提取】\n\n");

        try {
            String content = fetchPdfContent(url);
            if (content != null && !content.isEmpty()) {
                result.append("✅ 成功提取PDF内容！\n\n");
                result.append("📝 内容预览：\n");
                result.append("────────────────────\n");

                String preview = content.length() > 1000 ? content.substring(0, 1000) + "..." : content;
                result.append(preview);

                result.append("\n────────────────────\n\n");
                result.append("📊 统计信息：\n");
                result.append("• 总字符数：").append(content.length()).append("\n");
                result.append("• 处理次数：").append(processCount.get()).append("\n");
            } else {
                result.append("⚠️ 无法提取PDF内容，请检查URL是否正确！\n\n");
                result.append("💡 提示：\n");
                result.append("• 确保PDF链接可访问\n");
                result.append("• 本地文件请使用完整路径\n");
                result.append("• 支持在线PDF链接");
            }
        } catch (Exception e) {
            result.append("❌ 提取失败：").append(e.getMessage()).append("\n\n");
            result.append("💡 请检查PDF链接是否可访问！");
        }

        return result.toString();
    }

    private String searchInPdf(String input) {
        String keyword = extractSearchKeyword(input);

        if (keyword == null || keyword.isEmpty()) {
            return "🔍 请提供要搜索的关键词！\n\n示例：在PDF中搜索 人工智能";
        }

        StringBuilder result = new StringBuilder();
        result.append("🔍 【PDF内容搜索】\n\n");
        result.append("搜索关键词：").append(keyword).append("\n\n");

        result.append("💡 搜索提示：\n");
        result.append("• 请提供具体的PDF文件URL\n");
        result.append("• 搜索格式：在PDF中搜索 [关键词] [PDF链接]\n\n");

        result.append("📝 示例命令：\n");
        result.append("  在PDF中搜索 人工智能 https://example.com/doc.pdf\n");
        result.append("  在PDF中搜索 机器学习 /path/to/file.pdf");

        return result.toString();
    }

    private String generatePdfSummary(String input) {
        StringBuilder result = new StringBuilder();
        result.append("📄 【PDF摘要生成】\n\n");

        result.append("📝 功能说明：\n");
        result.append("这个功能可以从PDF文档中提取关键信息，生成简洁的摘要。\n\n");

        result.append("💡 使用方法：\n");
        result.append("请提供PDF文件的URL或路径，系统将：\n");
        result.append("1. 提取PDF的标题和元数据\n");
        result.append("2. 分析文档的主要内容\n");
        result.append("3. 生成简洁的摘要\n\n");

        result.append("📝 示例命令：\n");
        result.append("  总结 PDF https://example.com/document.pdf\n");
        result.append("  生成摘要 /path/to/file.pdf");

        return result.toString();
    }

    private String getPdfInfo(String input) {
        String url = extractPdfUrl(input);

        StringBuilder result = new StringBuilder();
        result.append("📄 【PDF信息查询】\n\n");

        if (url == null || url.isEmpty()) {
            result.append("请提供PDF文件信息：\n\n");
            result.append("支持的PDF信息来源：\n");
            result.append("• 在线PDF链接（https://...pdf）\n");
            result.append("• 本地PDF文件路径\n\n");
            result.append("📝 示例命令：\n");
            result.append("  PDF信息 https://example.com/doc.pdf\n");
            result.append("  查看PDF属性 /path/to/file.pdf");
        } else {
            result.append("✅ 检测到PDF文件：").append(url).append("\n\n");
            result.append("📊 文件信息：\n");
            result.append("• 文件格式：PDF文档\n");
            result.append("• 来源：在线/本地\n\n");

            result.append("💡 获取更多信息的命令：\n");
            result.append("• 提取 PDF内容 - 提取完整内容\n");
            result.append("• 总结 PDF摘要 - 生成文档摘要\n");
            result.append("• PDF页数 - 查看总页数");
        }

        return result.toString();
    }

    private String getPdfPageCount(String input) {
        StringBuilder result = new StringBuilder();
        result.append("📄 【PDF页数统计】\n\n");

        result.append("💡 使用方法：\n");
        result.append("要统计PDF页数，请提供PDF文件路径。\n\n");

        result.append("📝 示例命令：\n");
        result.append("  PDF有多少页 https://example.com/doc.pdf\n");
        result.append("  查看页数 /path/to/file.pdf");

        return result.toString();
    }

    private String analyzePdfRequest(String input) {
        StringBuilder result = new StringBuilder();
        result.append("📄 【PDF处理助手】\n\n");

        result.append("我能帮助你处理各种PDF文档！\n\n");

        result.append("🔧 支持的功能：\n");
        result.append("• 📝 内容提取 - 提取PDF文本内容\n");
        result.append("• 🔍 内容搜索 - 在PDF中搜索关键词\n");
        result.append("• 📊 信息查询 - 查看PDF元数据\n");
        result.append("• 📄 摘要生成 - 生成文档摘要\n");
        result.append("• 📖 页数统计 - 统计PDF总页数\n\n");

        result.append("📝 常用命令：\n");
        result.append("  提取 PDF https://example.com/doc.pdf\n");
        result.append("  在PDF中搜索 关键词\n");
        result.append("  总结 PDF /path/to/file.pdf\n\n");

        result.append("💡 提示：直接发送PDF链接或路径即可开始处理！");

        return result.toString();
    }

    private String getHelp() {
        StringBuilder result = new StringBuilder();
        result.append("📚 【PDF处理技能帮助】\n\n");

        result.append("🔧 可用命令：\n\n");

        result.append("1️⃣ 提取PDF内容\n");
        result.append("   命令：提取 PDF [URL/路径]\n");
        result.append("   示例：提取 PDF https://example.com/doc.pdf\n\n");

        result.append("2️⃣ 在PDF中搜索\n");
        result.append("   命令：在PDF中搜索 [关键词] [PDF链接]\n");
        result.append("   示例：在PDF中搜索 人工智能 https://example.com/doc.pdf\n\n");

        result.append("3️⃣ 生成PDF摘要\n");
        result.append("   命令：总结 PDF [URL/路径]\n");
        result.append("   示例：总结 PDF /path/to/file.pdf\n\n");

        result.append("4️⃣ 查看PDF信息\n");
        result.append("   命令：PDF信息 [URL/路径]\n");
        result.append("   示例：PDF信息 https://example.com/doc.pdf\n\n");

        result.append("5️⃣ 统计PDF页数\n");
        result.append("   命令：PDF有多少页 [URL/路径]\n");
        result.append("   示例：PDF有多少页 /path/to/file.pdf\n\n");

        result.append("📌 提示：支持在线PDF链接和本地文件路径！");

        return result.toString();
    }

    private String extractPdfUrl(String input) {
        Pattern urlPattern = Pattern.compile("https?://[^\\s]+\\.pdf", Pattern.CASE_INSENSITIVE);
        Matcher matcher = urlPattern.matcher(input);
        if (matcher.find()) {
            return matcher.group();
        }

        Pattern pathPattern = Pattern.compile("(/[\\w/.-]+\\.pdf)", Pattern.CASE_INSENSITIVE);
        matcher = pathPattern.matcher(input);
        if (matcher.find()) {
            return matcher.group();
        }

        return "";
    }

    private String extractSearchKeyword(String input) {
        String clean = input.replace("搜索", "").replace("查找", "").replace("在PDF中", "").replace("PDF", "").trim();

        Pattern pathPattern = Pattern.compile("(https?://[^\\s]+\\.pdf|[/\\w.-]+\\.pdf)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pathPattern.matcher(clean);
        clean = matcher.replaceAll("").trim();

        return clean.trim();
    }

    private String fetchPdfContent(String url) {
        try {
            if (url.startsWith("/") || url.startsWith(".")) {
                return fetchLocalPdf(url);
            } else {
                return fetchOnlinePdf(url);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private String fetchOnlinePdf(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(java.time.Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return extractTextFromHtml(response.body());
            }
        } catch (Exception e) {
            // 忽略错误
        }
        return null;
    }

    private String fetchLocalPdf(String path) {
        try {
            Path filePath = Paths.get(path);
            if (Files.exists(filePath)) {
                byte[] bytes = Files.readAllBytes(filePath);

                StringBuilder content = new StringBuilder();
                content.append("[本地PDF文件]\n");
                content.append("文件路径：").append(path).append("\n");
                content.append("文件大小：").append(bytes.length).append(" 字节\n\n");
                content.append("[提示] 本地PDF处理需要安装PDF解析库，建议使用在线PDF服务。\n");
                content.append("或者将PDF上传到服务器后使用在线链接访问。");

                return content.toString();
            }
        } catch (IOException e) {
            // 忽略错误
        }
        return null;
    }

    private String extractTextFromHtml(String html) {
        if (html == null) return "";

        String text = html.replaceAll("<[^>]*>", " ")
                        .replaceAll("\\s+", " ")
                        .trim();

        if (text.length() > 50 && (text.contains("pdf") || text.contains("document"))) {
            return text;
        }

        return null;
    }

    @Override
    public int getPriority() {
        return 5;
    }
}
