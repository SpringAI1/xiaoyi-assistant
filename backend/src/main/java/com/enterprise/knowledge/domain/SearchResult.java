package com.enterprise.knowledge.domain;

import java.util.List;

/**
 * 检索结果
 */
public class SearchResult {
    private DocumentChunk chunk;
    private double similarity;
    private DocumentMetadata document;
    private double score;
    private String source;

    public SearchResult() {}

    public SearchResult(DocumentChunk chunk, double similarity) {
        this.chunk = chunk;
        this.similarity = similarity;
    }

    public DocumentChunk getChunk() { return chunk; }
    public void setChunk(DocumentChunk chunk) { this.chunk = chunk; }

    public double getSimilarity() { return similarity; }
    public void setSimilarity(double similarity) { this.similarity = similarity; }

    public DocumentMetadata getDocument() { return document; }
    public void setDocument(DocumentMetadata document) { this.document = document; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    /**
     * 检索结果列表
     */
    public static class SearchResults {
        private List<SearchResult> results;
        private long queryTimeMs;

        public SearchResults(List<SearchResult> results, long queryTimeMs) {
            this.results = results;
            this.queryTimeMs = queryTimeMs;
        }

        public List<SearchResult> getResults() { return results; }
        public long getQueryTimeMs() { return queryTimeMs; }
    }
}
