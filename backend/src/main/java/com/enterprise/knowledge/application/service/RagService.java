package com.enterprise.knowledge.application.service;

import com.enterprise.knowledge.domain.ChatResponse;
import com.enterprise.knowledge.domain.DocumentChunk;
import com.enterprise.knowledge.domain.DocumentMetadata;
import com.enterprise.knowledge.domain.SearchResult;
import com.enterprise.knowledge.infrastructure.rag.RagRetriever;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class RagService {

    private final ChatLanguageModel chatModel;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final RagRetriever ragRetriever;
    private final com.enterprise.knowledge.infrastructure.knowledgegraph.KnowledgeGraph knowledgeGraph;

    @Value("${knowledge.rag.chunk-size:500}")
    private int chunkSize;

    @Value("${knowledge.rag.chunk-overlap:100}")
    private int chunkOverlap;

    @Value("${knowledge.rag.max-results:5}")
    private int maxResults;

    @Value("${knowledge.rag.similarity-threshold:0.75}")
    private double similarityThreshold;

    private final Map<String, DocumentMetadata> documentStore = new ConcurrentHashMap<>();

    public RagService(ChatLanguageModel chatModel,
                      EmbeddingModel embeddingModel,
                      EmbeddingStore<TextSegment> embeddingStore,
                      RagRetriever ragRetriever,
                      com.enterprise.knowledge.infrastructure.knowledgegraph.KnowledgeGraph knowledgeGraph) {
        this.chatModel = chatModel;
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.ragRetriever = ragRetriever;
        this.knowledgeGraph = knowledgeGraph;
        initializeDefaultKnowledge();
    }
    
    private void initializeDefaultKnowledge() {
        knowledgeGraph.addEntity("人工智能", "技术领域", "AI技术，包括机器学习、深度学习、自然语言处理等");
        knowledgeGraph.addEntity("小易助手", "产品", "智能助手系统，具备问答、搜索、文件处理等多种能力");
        knowledgeGraph.addEntity("知识图谱", "技术", "知识表示和推理的技术，用于构建实体关系网络");
        knowledgeGraph.addEntity("RAG", "技术", "检索增强生成，结合检索系统和生成模型的技术");
        knowledgeGraph.addEntity("向量数据库", "技术", "存储向量嵌入并支持相似度搜索的数据库");
        
        knowledgeGraph.addRelationship("小易助手", "人工智能", "应用");
        knowledgeGraph.addRelationship("小易助手", "知识图谱", "使用");
        knowledgeGraph.addRelationship("小易助手", "RAG", "基于");
        knowledgeGraph.addRelationship("RAG", "向量数据库", "使用");
        knowledgeGraph.addRelationship("人工智能", "知识图谱", "包含");
    }

    public DocumentMetadata uploadDocument(MultipartFile file, String userId) throws Exception {
        String filename = file.getOriginalFilename();
        String id = "doc_" + System.currentTimeMillis();

        DocumentMetadata metadata = new DocumentMetadata(
            id,
            extractTitle(filename),
            filename,
            extractFileType(filename)
        );

        documentStore.put(id, metadata);
        return metadata;
    }

    public void addDocumentChunks(String documentId, String content) {
        DocumentMetadata metadata = documentStore.get(documentId);
        if (metadata == null) {
            throw new IllegalArgumentException("Document not found: " + documentId);
        }

        List<String> chunks = chunkText(content, chunkSize, chunkOverlap);
        
        for (String chunkContent : chunks) {
            TextSegment segment = TextSegment.from(chunkContent);
            segment.metadata().put("documentId", documentId);
            Embedding embedding = embeddingModel.embed(segment).content();
            String embeddingId = embeddingStore.add(embedding, segment);
            metadata.addEmbeddingId(embeddingId);
        }
        
        metadata.setChunkCount(chunks.size());
    }

    public DocumentStats getDocumentStats() {
        long totalChunks = documentStore.size() > 0 ?
            documentStore.values().stream().mapToInt(DocumentMetadata::getChunkCount).sum() : 0;
        return new DocumentStats(documentStore.size(), (int) totalChunks);
    }

    public DocumentMetadata getDocumentById(String docId) {
        return documentStore.get(docId);
    }

    public ChatResponse ragAnswer(String query) {
        long startTime = System.currentTimeMillis();

        List<SearchResult> retrievalResults = retrieveRelevantChunks(query);

        String context = buildContext(retrievalResults);
        String answer = generateAnswer(query, context);

        ChatResponse response = new ChatResponse();
        response.setContent(answer);
        response.setResponseType(ChatResponse.ResponseType.RAG_BASED);
        response.setRetrievalResults(retrievalResults);
        response.setProcessingTime(System.currentTimeMillis() - startTime);

        return response;
    }

    public List<SearchResult> retrieveRelevantChunks(String query) {
        List<SearchResult> vectorResults = new ArrayList<>();
        List<SearchResult> keywordResults = new ArrayList<>();
        List<SearchResult> kgResults = new ArrayList<>();
        
        List<String> segments = ragRetriever.retrieve(query, maxResults / 3);
        for (String segmentText : segments) {
            SearchResult result = new SearchResult();
            DocumentChunk chunk = new DocumentChunk();
            chunk.setContent(segmentText);
            result.setChunk(chunk);
            result.setScore(0.8);
            result.setSource("vector_search");
            vectorResults.add(result);
        }
        
        List<String> keywordMatches = keywordSearch(query, maxResults / 3);
        for (String segmentText : keywordMatches) {
            SearchResult result = new SearchResult();
            DocumentChunk chunk = new DocumentChunk();
            chunk.setContent(segmentText);
            result.setChunk(chunk);
            result.setScore(0.6);
            result.setSource("keyword_search");
            keywordResults.add(result);
        }
        
        var kgNodes = knowledgeGraph.search(query);
        for (var node : kgNodes) {
            SearchResult result = new SearchResult();
            DocumentChunk chunk = new DocumentChunk();
            chunk.setContent(String.format("【%s】%s\n类型：%s", node.id, node.description, node.type));
            result.setChunk(chunk);
            result.setScore(0.7);
            result.setSource("knowledge_graph");
            kgResults.add(result);
        }
        
        List<SearchResult> mergedResults = new ArrayList<>();
        mergedResults.addAll(vectorResults);
        mergedResults.addAll(kgResults);
        
        for (SearchResult kr : keywordResults) {
            boolean exists = false;
            for (SearchResult existing : mergedResults) {
                if (existing.getChunk().getContent().equals(kr.getChunk().getContent())) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                mergedResults.add(kr);
            }
        }
        
        return mergedResults.stream()
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(maxResults)
                .collect(Collectors.toList());
    }
    
    private List<String> keywordSearch(String query, int limit) {
        List<String> results = new ArrayList<>();
        String lowerQuery = query.toLowerCase();
        String[] keywords = lowerQuery.split("\\s+");
        
        for (DocumentMetadata metadata : documentStore.values()) {
            if (metadata.getEmbeddingIds() == null) continue;
            
            for (String embeddingId : metadata.getEmbeddingIds()) {
                try {
                    var matchOpt = embeddingStore.findRelevant(
                        embeddingModel.embed(query).content(),
                        1
                    );
                    
                    if (!matchOpt.isEmpty()) {
                        String content = matchOpt.get(0).embedded().text();
                        boolean match = true;
                        for (String keyword : keywords) {
                            if (!content.toLowerCase().contains(keyword)) {
                                match = false;
                                break;
                            }
                        }
                        if (match && !results.contains(content)) {
                            results.add(content);
                            if (results.size() >= limit) break;
                        }
                    }
                } catch (Exception e) {
                    continue;
                }
            }
            if (results.size() >= limit) break;
        }
        
        return results;
    }

    private String buildContext(List<SearchResult> results) {
        StringBuilder context = new StringBuilder();

        if (results.isEmpty()) {
            return "[知识库中未找到相关信息]";
        }

        for (int i = 0; i < results.size(); i++) {
            context.append(String.format("【参考资料 %d】\n", i + 1));
            context.append(results.get(i).getChunk().getContent());
            context.append("\n\n");
        }

        return context.toString();
    }

    private String generateAnswer(String query, String context) {
        String prompt = """
                请根据以下参考信息回答问题。如果参考信息不足以回答问题，请说明。

                【参考信息】
                %s

                【问题】
                %s

                【回答】
                """.formatted(context, query);

        return chatModel.generate(prompt);
    }

    public void deleteDocument(String docId) {
        DocumentMetadata metadata = documentStore.remove(docId);
        if (metadata != null && metadata.getEmbeddingIds() != null) {
            for (String embeddingId : metadata.getEmbeddingIds()) {
                try {
                    embeddingStore.remove(embeddingId);
                } catch (Exception e) {
                    System.err.println("Failed to remove embedding: " + embeddingId);
                }
            }
        }
    }

    private List<String> chunkText(String content, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();

        if (content == null || content.isEmpty()) {
            return chunks;
        }

        int start = 0;
        while (start < content.length()) {
            int end = Math.min(start + chunkSize, content.length());
            chunks.add(content.substring(start, end));
            start = end - overlap;

            if (overlap > 0 && start >= end) {
                break;
            }
        }

        return chunks;
    }

    private String extractTitle(String filename) {
        if (filename == null) return "Untitled";
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(0, lastDot) : filename;
    }

    private String extractFileType(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1).toLowerCase() : "txt";
    }

    public static class DocumentStats {
        private final int totalDocuments;
        private final int totalChunks;

        public DocumentStats(int totalDocuments, int totalChunks) {
            this.totalDocuments = totalDocuments;
            this.totalChunks = totalChunks;
        }

        public int getTotalDocuments() { return totalDocuments; }
        public int getTotalChunks() { return totalChunks; }
    }
}
