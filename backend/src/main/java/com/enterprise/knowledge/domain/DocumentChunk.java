package com.enterprise.knowledge.domain;

import java.util.List;

public class DocumentChunk {
    private String id;
    private String documentId;
    private int chunkNumber;
    private String content;
    private List<Float> embedding;
    private int startOffset;
    private int endOffset;

    public DocumentChunk() {}

    public DocumentChunk(String documentId, int chunkNumber, String content) {
        this.documentId = documentId;
        this.chunkNumber = chunkNumber;
        this.content = content;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public int getChunkNumber() { return chunkNumber; }
    public void setChunkNumber(int chunkNumber) { this.chunkNumber = chunkNumber; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public List<Float> getEmbedding() { return embedding; }
    public void setEmbedding(List<Float> embedding) { this.embedding = embedding; }

    public int getStartOffset() { return startOffset; }
    public void setStartOffset(int startOffset) { this.startOffset = startOffset; }

    public int getEndOffset() { return endOffset; }
    public void setEndOffset(int endOffset) { this.endOffset = endOffset; }
}