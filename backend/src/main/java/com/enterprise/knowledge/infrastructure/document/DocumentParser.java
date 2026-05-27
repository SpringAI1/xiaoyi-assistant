package com.enterprise.knowledge.infrastructure.document;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.sl.usermodel.SlideShow;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
public class DocumentParser {

    public String parse(MultipartFile file) throws Exception {
        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";

        if (fileName.endsWith(".pdf")) {
            return parsePdf(file.getInputStream());
        } else if (fileName.endsWith(".pptx")) {
            return parsePptx(file.getInputStream());
        } else if (fileName.endsWith(".ppt")) {
            return parsePpt(file.getInputStream());
        } else if (fileName.endsWith(".docx")) {
            return parseDocx(file.getInputStream());
        } else if (fileName.endsWith(".doc")) {
            return "[DOC 文件需要使用 Microsoft Word 打开查看]";
        } else if (fileName.endsWith(".txt") || fileName.endsWith(".md")) {
            return new String(file.getBytes());
        } else if (fileName.endsWith(".xlsx")) {
            return parseXlsx(file.getInputStream());
        } else if (fileName.endsWith(".xls")) {
            return parseXls(file.getInputStream());
        } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || 
                   fileName.endsWith(".png") || fileName.endsWith(".gif") || 
                   fileName.endsWith(".bmp")) {
            return parseImage(file.getInputStream(), fileName);
        } else {
            return "[不支持的文件格式: " + fileName + "]";
        }
    }

    public String parse(Path filePath, String fileType) throws Exception {
        String type = fileType.toLowerCase();
        if ("pdf".equals(type)) {
            return parsePdf(java.nio.file.Files.newInputStream(filePath));
        } else if ("pptx".equals(type)) {
            return parsePptx(java.nio.file.Files.newInputStream(filePath));
        } else if ("ppt".equals(type)) {
            return parsePpt(java.nio.file.Files.newInputStream(filePath));
        } else if ("docx".equals(type)) {
            return parseDocx(java.nio.file.Files.newInputStream(filePath));
        } else if ("doc".equals(type)) {
            return "[DOC 文件需要使用 Microsoft Word 打开查看]";
        } else if ("txt".equals(type) || "md".equals(type)) {
            return new String(java.nio.file.Files.readAllBytes(filePath));
        } else if ("xlsx".equals(type)) {
            return parseXlsx(java.nio.file.Files.newInputStream(filePath));
        } else if ("xls".equals(type)) {
            return parseXls(java.nio.file.Files.newInputStream(filePath));
        } else if ("jpg".equals(type) || "jpeg".equals(type) || 
                   "png".equals(type) || "gif".equals(type) || 
                   "bmp".equals(type)) {
            return parseImage(java.nio.file.Files.newInputStream(filePath), type);
        } else {
            return "[不支持的文件格式: " + type + "]";
        }
    }

    private String parsePdf(InputStream inputStream) {
        try {
            PDDocument document = Loader.loadPDF(inputStream.readAllBytes());
            String text;
            try {
                PDFTextStripper stripper = new PDFTextStripper();
                text = stripper.getText(document);
            } finally {
                document.close();
            }
            return text;
        } catch (Exception e) {
            return "[PDF 解析失败: " + e.getMessage() + "]";
        }
    }

    private String parsePptx(InputStream inputStream) {
        StringBuilder text = new StringBuilder();
        try (XMLSlideShow ppt = new XMLSlideShow(inputStream)) {
            List<XSLFSlide> slides = ppt.getSlides();
            for (int i = 0; i < slides.size(); i++) {
                XSLFSlide slide = slides.get(i);
                text.append("=== 第 ").append(i + 1).append(" 页 ===\n");
                slide.getShapes().forEach(shape -> {
                    if (shape instanceof org.apache.poi.xslf.usermodel.XSLFTextShape) {
                        org.apache.poi.xslf.usermodel.XSLFTextShape textShape = 
                            (org.apache.poi.xslf.usermodel.XSLFTextShape) shape;
                        text.append(textShape.getText()).append("\n");
                    }
                });
                text.append("\n");
            }
        } catch (Exception e) {
            return "[PPTX 解析失败: " + e.getMessage() + "]";
        }
        return text.toString();
    }

    private String parsePpt(InputStream inputStream) {
        StringBuilder text = new StringBuilder();
        try (HSLFSlideShow ppt = new HSLFSlideShow(inputStream)) {
            var slides = ppt.getSlides();
            for (int i = 0; i < slides.size(); i++) {
                var slide = slides.get(i);
                text.append("=== 第 ").append(i + 1).append(" 页 ===\n");
                slide.getShapes().forEach(shape -> {
                    if (shape instanceof org.apache.poi.hslf.usermodel.HSLFTextShape) {
                        org.apache.poi.hslf.usermodel.HSLFTextShape textShape = 
                            (org.apache.poi.hslf.usermodel.HSLFTextShape) shape;
                        text.append(textShape.getText()).append("\n");
                    }
                });
                text.append("\n");
            }
        } catch (Exception e) {
            return "[PPT 解析失败: " + e.getMessage() + "]";
        }
        return text.toString();
    }

    private String parseDocx(InputStream inputStream) {
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            StringBuilder text = new StringBuilder();
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                text.append(paragraph.getText()).append("\n");
            }
            return text.toString();
        } catch (Exception e) {
            return "[DOCX 解析失败: " + e.getMessage() + "]";
        }
    }

    private String parseXlsx(InputStream inputStream) {
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            return parseExcel(workbook);
        } catch (Exception e) {
            return "[XLSX 解析失败: " + e.getMessage() + "]";
        }
    }

    private String parseXls(InputStream inputStream) {
        try (Workbook workbook = new HSSFWorkbook(inputStream)) {
            return parseExcel(workbook);
        } catch (Exception e) {
            return "[XLS 解析失败: " + e.getMessage() + "]";
        }
    }

    private String parseExcel(Workbook workbook) {
        StringBuilder text = new StringBuilder();

        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            text.append("=== Sheet: ").append(sheet.getSheetName()).append(" ===\n");

            for (Row row : sheet) {
                for (Cell cell : row) {
                    text.append(getCellValueAsString(cell)).append("\t");
                }
                text.append("\n");
            }
            text.append("\n");
        }

        return text.toString();
    }

    private String parseImage(InputStream inputStream, String format) {
        try {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                return "[无法解析图片文件]";
            }
            
            StringBuilder sb = new StringBuilder();
            sb.append("=== 图片信息 ===\n");
            sb.append("格式: ").append(format).append("\n");
            sb.append("宽度: ").append(image.getWidth()).append(" 像素\n");
            sb.append("高度: ").append(image.getHeight()).append(" 像素\n");
            sb.append("颜色模式: ").append(image.getColorModel().getColorSpace().getType()).append("\n");
            sb.append("\n");
            sb.append("[注意: 本系统目前需要通过OCR插件才能提取图片中的文字]\n");
            sb.append("[您可以: 1. 将图片中的文字复制出来上传为文本文件 2. 或者使用专门的OCR工具]\n");
            
            return sb.toString();
        } catch (Exception e) {
            return "[图片解析失败: " + e.getMessage() + "]";
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getDateCellValue().toString();
                } else {
                    yield String.valueOf(cell.getNumericCellValue());
                }
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield cell.getStringCellValue();
                } catch (Exception e) {
                    yield String.valueOf(cell.getNumericCellValue());
                }
            }
            default -> "";
        };
    }

    public List<String> chunkText(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();

        if (text == null || text.isEmpty()) {
            return chunks;
        }

        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());

            int lastNewline = text.lastIndexOf('\n', end);
            if (lastNewline > start + overlap) {
                end = lastNewline;
            }

            chunks.add(text.substring(start, end).trim());
            start = end - overlap;

            if (overlap > 0 && start >= end) {
                break;
            }
        }

        return chunks;
    }

    public boolean isSupportedFileType(String filename) {
        if (filename == null) return false;
        String lower = filename.toLowerCase();
        return lower.endsWith(".pdf") ||
               lower.endsWith(".pptx") || lower.endsWith(".ppt") ||
               lower.endsWith(".docx") || lower.endsWith(".doc") ||
               lower.endsWith(".txt") || lower.endsWith(".md") ||
               lower.endsWith(".xlsx") || lower.endsWith(".xls") ||
               lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
               lower.endsWith(".png") || lower.endsWith(".gif") ||
               lower.endsWith(".bmp");
    }

    public String getFileTypeDescription(String filename) {
        if (filename == null) return "未知";
        String lower = filename.toLowerCase();
        if (lower.endsWith(".pdf")) return "PDF 文档";
        if (lower.endsWith(".pptx") || lower.endsWith(".ppt")) return "PowerPoint 文档";
        if (lower.endsWith(".docx") || lower.endsWith(".doc")) return "Word 文档";
        if (lower.endsWith(".txt")) return "文本文件";
        if (lower.endsWith(".md")) return "Markdown 文档";
        if (lower.endsWith(".xlsx") || lower.endsWith(".xls")) return "Excel 电子表格";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || 
            lower.endsWith(".png") || lower.endsWith(".gif") || 
            lower.endsWith(".bmp")) return "图片文件";
        return "未知格式";
    }
}
