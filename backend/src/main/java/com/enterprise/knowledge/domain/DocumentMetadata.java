package com.enterprise.knowledge.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文档元数据
 */
public class DocumentMetadata {
    private String id;
    private String title;
    private String filePath;
    private String fileType; // PDF, DOCX, TXT, etc.
    private Instant uploadTime;
    private int version;
    private int chunkCount;
    private Map<String, String> customMetadata;
    private List<String> embeddingIds; // 跟踪该文档的所有向量ID

    public DocumentMetadata() {
        this.customMetadata = new HashMap<>();
        this.embeddingIds = new ArrayList<>();
    }

    public DocumentMetadata(String id, String title, String filePath, String fileType) {
        this();
        this.id = id;
        this.title = title;
        this.filePath = filePath;
        this.fileType = fileType;
        this.uploadTime = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public Instant getUploadTime() { return uploadTime; }
    public void setUploadTime(Instant uploadTime) { this.uploadTime = uploadTime; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public int getChunkCount() { return chunkCount; }
    public void setChunkCount(int chunkCount) { this.chunkCount = chunkCount; }

    public Map<String, String> getCustomMetadata() { return customMetadata; }
    public void setCustomMetadata(Map<String, String> customMetadata) { this.customMetadata = customMetadata; }

    public void addCustomMetadata(String key, String value) {
        this.customMetadata.put(key, value);
    }

    public String getCustomMetadata(String key) {
        return this.customMetadata.get(key);
    }

    public List<String> getEmbeddingIds() {
        return embeddingIds;
    }

    public void setEmbeddingIds(List<String> embeddingIds) {
        this.embeddingIds = embeddingIds;
    }

    public void addEmbeddingId(String embeddingId) {
        this.embeddingIds.add(embeddingId);
    }
}
