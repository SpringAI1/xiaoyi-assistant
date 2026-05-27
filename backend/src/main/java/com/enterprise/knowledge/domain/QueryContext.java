package com.enterprise.knowledge.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * 查询上下文 - 用于构建 RAG 提示词
 */
public class QueryContext {
    private String query;
    private List<String> relevantChunks;
    private List<DocumentMetadata> documents;
    private long processingTimeMs;

    public QueryContext() {
        this.relevantChunks = new ArrayList<>();
        this.documents = new ArrayList<>();
    }

    public QueryContext(String query) {
        this();
        this.query = query;
    }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public List<String> getRelevantChunks() { return relevantChunks; }
    public void setRelevantChunks(List<String> relevantChunks) { this.relevantChunks = relevantChunks; }

    public void addRelevantChunk(String chunk) {
        this.relevantChunks.add(chunk);
    }

    public List<DocumentMetadata> getDocuments() { return documents; }
    public void setDocuments(List<DocumentMetadata> documents) { this.documents = documents; }

    public void addDocument(DocumentMetadata document) {
        this.documents.add(document);
    }

    public long getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(long processingTimeMs) { this.processingTimeMs = processingTimeMs; }

    /**
     * 构建 RAG 提示词上下文
     */
    public String buildRagContext() {
        StringBuilder context = new StringBuilder();
        context.append("根据以下相关文档内容回答问题：\n\n");

        for (int i = 0; i < relevantChunks.size(); i++) {
            DocumentMetadata doc = documents.get(i % documents.size());
            context.append(String.format("[来自：%s - %s 第%d页]\n",
                    doc.getTitle(), doc.getFileType(), i + 1));
            context.append(relevantChunks.get(i)).append("\n\n");
        }

        return context.toString();
    }
}
