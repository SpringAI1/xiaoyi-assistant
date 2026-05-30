package com.enterprise.knowledge.infrastructure.rag;

import com.enterprise.knowledge.domain.ChatResponse;
import com.enterprise.knowledge.domain.DocumentChunk;
import com.enterprise.knowledge.domain.SearchResult;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 企业级增强版RAG引擎
 * 支持混合检索、重排序、查询扩展等高级功能
 */
@Component
public class EnhancedRagEngine {

    private static final Logger logger = LoggerFactory.getLogger(EnhancedRagEngine.class);

    private final ChatLanguageModel chatModel;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final com.enterprise.knowledge.infrastructure.knowledgegraph.KnowledgeGraph knowledgeGraph;

    @Value("${knowledge.rag.max-results:5}")
    private int maxResults;

    @Value("${knowledge.rag.similarity-threshold:0.75}")
    private double similarityThreshold;

    @Value("${knowledge.rag.enable-hybrid-search:true}")
    private boolean enableHybridSearch;

    @Value("${knowledge.rag.enable-query-expansion:true}")
    private boolean enableQueryExpansion;

    @Value("${knowledge.rag.enable-reranking:true}")
    private boolean enableReranking;

    private final ExecutorService executorService = Executors.newFixedThreadPool(3);

    public EnhancedRagEngine(ChatLanguageModel chatModel,
                             EmbeddingModel embeddingModel,
                             EmbeddingStore<TextSegment> embeddingStore,
                             com.enterprise.knowledge.infrastructure.knowledgegraph.KnowledgeGraph knowledgeGraph) {
        this.chatModel = chatModel;
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.knowledgeGraph = knowledgeGraph;
    }

    /**
     * 增强版RAG回答
     */
    public ChatResponse enhancedRagAnswer(String query, String contextHistory) {
        long startTime = System.currentTimeMillis();
        logger.info("开始处理RAG查询: {}", query);

        try {
            // 1. 查询理解与扩展
            QueryUnderstanding understanding = understandQuery(query, contextHistory);

            // 2. 混合检索
            List<SearchResult> searchResults = hybridSearch(understanding);

            // 3. 结果重排序
            List<SearchResult> rankedResults = enableReranking ? 
                rerankResults(query, searchResults) : searchResults;

            // 4. 构建增强上下文
            String enhancedContext = buildEnhancedContext(rankedResults, understanding);

            // 5. 生成最终答案
            String answer = generateEnhancedAnswer(query, enhancedContext, contextHistory);

            // 6. 构建响应
            ChatResponse response = new ChatResponse();
            response.setContent(answer);
            response.setResponseType(ChatResponse.ResponseType.RAG_BASED);
            response.setRetrievalResults(rankedResults);
            response.setProcessingTime(System.currentTimeMillis() - startTime);

            logger.info("RAG查询完成，耗时: {}ms", System.currentTimeMillis() - startTime);
            return response;

        } catch (Exception e) {
            logger.error("RAG处理失败", e);
            ChatResponse errorResponse = new ChatResponse();
            errorResponse.setContent("抱歉，处理您的问题时出现了错误，请稍后再试。");
            errorResponse.setProcessingTime(System.currentTimeMillis() - startTime);
            return errorResponse;
        }
    }

    /**
     * 查询理解与扩展
     */
    private QueryUnderstanding understandQuery(String query, String contextHistory) {
        QueryUnderstanding understanding = new QueryUnderstanding();
        understanding.setOriginalQuery(query);

        // 分析查询类型
        understanding.setQueryType(classifyQueryType(query));

        // 提取关键词
        understanding.setKeywords(extractKeywords(query));

        // 查询扩展
        if (enableQueryExpansion) {
            understanding.setExpandedQueries(expandQuery(query));
        }

        return understanding;
    }

    /**
     * 混合检索策略
     */
    private List<SearchResult> hybridSearch(QueryUnderstanding understanding) {
        List<SearchResult> allResults = new ArrayList<>();
        
        String originalQuery = understanding.getOriginalQuery();
        List<String> queriesToSearch = new ArrayList<>();
        queriesToSearch.add(originalQuery);
        
        if (enableQueryExpansion && understanding.getExpandedQueries() != null) {
            queriesToSearch.addAll(understanding.getExpandedQueries());
        }

        // 并行执行多种检索策略
        if (enableHybridSearch) {
            try {
                List<Future<List<SearchResult>>> futures = new ArrayList<>();
                
                // 向量检索
                futures.add(executorService.submit(() -> vectorSearch(originalQuery)));
                
                // 关键词检索
                futures.add(executorService.submit(() -> keywordSearch(originalQuery)));
                
                // 知识图谱检索
                futures.add(executorService.submit(() -> knowledgeGraphSearch(originalQuery)));
                
                // 收集结果
                for (Future<List<SearchResult>> future : futures) {
                    try {
                        allResults.addAll(future.get(3, TimeUnit.SECONDS));
                    } catch (Exception e) {
                        logger.warn("检索任务失败", e);
                    }
                }
                
            } catch (Exception e) {
                logger.warn("并行检索失败，回退到单线程模式", e);
                allResults.addAll(vectorSearch(originalQuery));
                allResults.addAll(keywordSearch(originalQuery));
            }
        } else {
            // 简单向量检索
            allResults.addAll(vectorSearch(originalQuery));
        }

        // 去重与合并
        return deduplicateResults(allResults);
    }

    /**
     * 向量检索
     */
    private List<SearchResult> vectorSearch(String query) {
        List<SearchResult> results = new ArrayList<>();
        
        try {
            var embedding = embeddingModel.embed(query).content();
            var matches = embeddingStore.findRelevant(embedding, maxResults / 2, similarityThreshold);
            
            for (var match : matches) {
                SearchResult result = new SearchResult();
                DocumentChunk chunk = new DocumentChunk();
                chunk.setContent(match.embedded().text());
                result.setChunk(chunk);
                result.setScore(match.score());
                result.setSource("vector_search");
                results.add(result);
            }
        } catch (Exception e) {
            logger.warn("向量检索失败", e);
        }
        
        return results;
    }

    /**
     * 关键词检索
     */
    private List<SearchResult> keywordSearch(String query) {
        List<SearchResult> results = new ArrayList<>();
        String[] keywords = query.toLowerCase().split("\\s+");
        
        try {
            // 简单的关键词匹配实现
            var embedding = embeddingModel.embed(query).content();
            var matches = embeddingStore.findRelevant(embedding, maxResults / 2, 0.6);
            
            for (var match : matches) {
                String content = match.embedded().text().toLowerCase();
                boolean hasMatch = false;
                
                for (String keyword : keywords) {
                    if (content.contains(keyword)) {
                        hasMatch = true;
                        break;
                    }
                }
                
                if (hasMatch) {
                    SearchResult result = new SearchResult();
                    DocumentChunk chunk = new DocumentChunk();
                    chunk.setContent(match.embedded().text());
                    result.setChunk(chunk);
                    result.setScore(match.score() * 0.9); // 关键词搜索分数稍低
                    result.setSource("keyword_search");
                    results.add(result);
                }
            }
        } catch (Exception e) {
            logger.warn("关键词检索失败", e);
        }
        
        return results;
    }

    /**
     * 知识图谱检索
     */
    private List<SearchResult> knowledgeGraphSearch(String query) {
        List<SearchResult> results = new ArrayList<>();
        
        try {
            var kgNodes = knowledgeGraph.search(query);
            for (var node : kgNodes) {
                SearchResult result = new SearchResult();
                DocumentChunk chunk = new DocumentChunk();
                chunk.setContent(String.format("【%s】%s\n类型：%s", 
                    node.id, node.description, node.type));
                result.setChunk(chunk);
                result.setScore(0.75);
                result.setSource("knowledge_graph");
                results.add(result);
            }
        } catch (Exception e) {
            logger.warn("知识图谱检索失败", e);
        }
        
        return results;
    }

    /**
     * 结果重排序
     */
    private List<SearchResult> rerankResults(String query, List<SearchResult> results) {
        if (results.size() <= 1) {
            return results;
        }
        
        // 基于多种因素重排序
        return results.stream()
            .sorted((a, b) -> {
                double scoreA = calculateCombinedScore(query, a);
                double scoreB = calculateCombinedScore(query, b);
                return Double.compare(scoreB, scoreA);
            })
            .limit(maxResults)
            .collect(Collectors.toList());
    }

    /**
     * 计算组合分数
     */
    private double calculateCombinedScore(String query, SearchResult result) {
        double baseScore = result.getScore();
        
        // 来源权重
        double sourceWeight = switch (result.getSource()) {
            case "vector_search" -> 1.0;
            case "knowledge_graph" -> 0.95;
            case "keyword_search" -> 0.85;
            default -> 0.7;
        };
        
        // 内容长度奖励
        String content = result.getChunk().getContent();
        double lengthBonus = Math.min(1.0, content.length() / 500.0) * 0.1;
        
        return baseScore * sourceWeight + lengthBonus;
    }

    /**
     * 去重结果
     */
    private List<SearchResult> deduplicateResults(List<SearchResult> results) {
        Map<String, SearchResult> uniqueResults = new LinkedHashMap<>();
        
        for (SearchResult result : results) {
            String contentHash = String.valueOf(result.getChunk().getContent().hashCode());
            if (!uniqueResults.containsKey(contentHash)) {
                uniqueResults.put(contentHash, result);
            } else {
                // 保留分数较高的
                SearchResult existing = uniqueResults.get(contentHash);
                if (result.getScore() > existing.getScore()) {
                    uniqueResults.put(contentHash, result);
                }
            }
        }
        
        return new ArrayList<>(uniqueResults.values());
    }

    /**
     * 构建增强上下文
     */
    private String buildEnhancedContext(List<SearchResult> results, QueryUnderstanding understanding) {
        StringBuilder context = new StringBuilder();
        
        if (results.isEmpty()) {
            return "[知识库中未找到直接相关的信息]";
        }
        
        context.append("【检索到的相关信息】\n\n");
        
        for (int i = 0; i < results.size(); i++) {
            SearchResult result = results.get(i);
            context.append(String.format("【资料 %d】 (来源: %s, 相关度: %.2f)\n", 
                i + 1, result.getSource(), result.getScore()));
            context.append(result.getChunk().getContent());
            context.append("\n\n");
        }
        
        if (understanding.getKeywords() != null && !understanding.getKeywords().isEmpty()) {
            context.append("【检索关键词】\n");
            context.append(String.join(", ", understanding.getKeywords()));
            context.append("\n\n");
        }
        
        return context.toString();
    }

    /**
     * 生成增强版答案
     */
    private String generateEnhancedAnswer(String query, String context, String history) {
        String prompt = """
            你是一个专业的企业知识助手，请根据提供的信息回答用户的问题。
            
            【对话历史】
            %s
            
            【参考信息】
            %s
            
            【用户问题】
            %s
            
            【回答要求】
            1. 请基于参考信息回答问题，如果参考信息不足，请说明
            2. 回答要专业、准确、有条理
            3. 重要信息可以用项目符号或数字列表突出显示
            4. 可以适当结合对话历史理解用户意图
            5. 请用简洁清晰的语言表达
            """.formatted(
                history != null ? history : "无历史对话",
                context,
                query
            );
        
        return chatModel.generate(prompt);
    }

    /**
     * 分类查询类型
     */
    private QueryType classifyQueryType(String query) {
        String lowerQuery = query.toLowerCase();
        
        if (lowerQuery.contains("如何") || lowerQuery.contains("怎么")) {
            return QueryType.HOW_TO;
        } else if (lowerQuery.contains("什么") || lowerQuery.contains("是")) {
            return QueryType.FACTUAL;
        } else if (lowerQuery.contains("为什么") || lowerQuery.contains("原因")) {
            return QueryType.EXPLANATION;
        } else if (lowerQuery.contains("比较") || lowerQuery.contains("区别")) {
            return QueryType.COMPARISON;
        } else {
            return QueryType.GENERAL;
        }
    }

    /**
     * 提取关键词
     */
    private List<String> extractKeywords(String query) {
        List<String> keywords = new ArrayList<>();
        
        // 简单的关键词提取
        String[] words = query.split("[\\s，。？！,?.!]+");
        Set<String> stopWords = new HashSet<>(Arrays.asList(
            "的", "了", "在", "是", "我", "有", "和", "就", "不", "人", "都", "一", "一个", "上", "也", "很", "到", "说", "要", "去", "你", "会", "着", "没有", "看", "好", "自己", "这"
        ));
        
        for (String word : words) {
            if (word.length() >= 2 && !stopWords.contains(word)) {
                keywords.add(word);
            }
        }
        
        return keywords;
    }

    /**
     * 查询扩展
     */
    private List<String> expandQuery(String query) {
        List<String> expanded = new ArrayList<>();
        expanded.add(query);
        
        // 简单的同义词扩展
        Map<String, String> synonyms = Map.of(
            "怎么", "如何",
            "什么是", "介绍",
            "为什么", "原因",
            "帮助", "教程",
            "使用", "操作"
        );
        
        for (Map.Entry<String, String> entry : synonyms.entrySet()) {
            if (query.contains(entry.getKey())) {
                expanded.add(query.replace(entry.getKey(), entry.getValue()));
            }
        }
        
        return expanded;
    }

    // ==================== 内部类 ====================

    private static class QueryUnderstanding {
        private String originalQuery;
        private QueryType queryType;
        private List<String> keywords;
        private List<String> expandedQueries;
        private boolean isComplex;

        public String getOriginalQuery() { return originalQuery; }
        public void setOriginalQuery(String query) { this.originalQuery = query; }

        public QueryType getQueryType() { return queryType; }
        public void setQueryType(QueryType type) { this.queryType = type; }

        public List<String> getKeywords() { return keywords; }
        public void setKeywords(List<String> words) { this.keywords = words; }

        public List<String> getExpandedQueries() { return expandedQueries; }
        public void setExpandedQueries(List<String> queries) { this.expandedQueries = queries; }

        public boolean isComplex() { return isComplex; }
        public void setComplex(boolean complex) { this.isComplex = complex; }
    }

    private enum QueryType {
        FACTUAL,      // 事实查询
        HOW_TO,       // 操作指南
        EXPLANATION,  // 解释性
        COMPARISON,   // 比较查询
        GENERAL       // 通用查询
    }
}
