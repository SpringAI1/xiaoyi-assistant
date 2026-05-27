package com.enterprise.knowledge.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class ChatResponse {
    private String id;
    private String content;
    private Instant timestamp;
    private ResponseType responseType;
    private List<SearchResult> retrievalResults;
    private Map<String, Object> metadata;
    private long processingTime;

    public enum ResponseType {
        DIRECT_ANSWER,
        RAG_BASED,
        WEB_SEARCH,
        HYBRID,
        TOOL_EXECUTION,
        ERROR
    }

    public ChatResponse() {
        this.timestamp = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public ResponseType getResponseType() { return responseType; }
    public void setResponseType(ResponseType responseType) { this.responseType = responseType; }

    public List<SearchResult> getRetrievalResults() { return retrievalResults; }
    public void setRetrievalResults(List<SearchResult> retrievalResults) {
        this.retrievalResults = retrievalResults;
    }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    public long getProcessingTime() { return processingTime; }
    public void setProcessingTime(long processingTime) { this.processingTime = processingTime; }
}