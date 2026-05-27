package com.enterprise.knowledge.application.service;

import com.enterprise.knowledge.domain.ChatMessage;
import com.enterprise.knowledge.domain.ChatResponse;
import com.enterprise.knowledge.infrastructure.agent.IntentRecognizer;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 双引擎协同调度器 - 智能选择 RAG 或 Agent 模式
 */
@Service
public class DualEngineCoordinator {

    private final RagService ragService;
    private final AgentService agentService;
    private final IntentRecognizer intentRecognizer;

    public DualEngineCoordinator(RagService ragService,
                                 AgentService agentService,
                                 IntentRecognizer intentRecognizer) {
        this.ragService = ragService;
        this.agentService = agentService;
        this.intentRecognizer = intentRecognizer;
    }

    /**
     * 主问答入口 - 自动选择最优处理路径
     */
    public ChatResponse answer(String userQuery) {
        return answer(userQuery, null);
    }

    /**
     * 带对话历史的问答
     */
    public ChatResponse answer(String userQuery, List<ChatMessage> conversationHistory) {
        long startTime = System.currentTimeMillis();

        // 1. 意图识别
        IntentRecognizer.Intent intent = intentRecognizer.recognize(userQuery);

        // 2. 创建响应对象
        ChatResponse response = new ChatResponse();

        // 3. 根据意图路由到不同引擎
        EngineChoice engineChoice = selectEngine(intent, userQuery);

        switch (engineChoice) {
            case RAG:
                response = processWithRag(userQuery, intent);
                break;
            case AGENT:
                response = processWithAgent(userQuery, intent);
                break;
            case HYBRID:
                response = processHybrid(userQuery, intent);
                break;
            default:
                response = agentService.agentAnswer(userQuery);
        }

        // 4. 添加元数据
        addMetadata(response, engineChoice, System.currentTimeMillis() - startTime, intent);

        return response;
    }

    /**
     * 选择使用的引擎
     */
    private EngineChoice selectEngine(IntentRecognizer.Intent intent, String query) {
        switch (intent) {
            case FACT_QUERY:
                // 事实查询优先使用 RAG
                if (query.contains("公司") || query.contains("员工") || query.contains("制度")) {
                    return EngineChoice.RAG;
                }
                return EngineChoice.HYBRID;

            case TOOL_REQUIRED:
            case DATA_ANALYSIS:
            case DOCUMENT_GENERATION:
                // 文档生成、工具调用、数据分析直接使用 Agent
                return EngineChoice.AGENT;

            case CREATIVE_WRITING:
                // 创意写作可以先查知识库再创作
                return EngineChoice.HYBRID;

            default:
                return EngineChoice.AGENT;
        }
    }

    /**
     * RAG 模式处理
     */
    private ChatResponse processWithRag(String query, IntentRecognizer.Intent intent) {
        return ragService.ragAnswer(query);
    }

    /**
     * Agent 模式处理
     */
    private ChatResponse processWithAgent(String query, IntentRecognizer.Intent intent) {
        return agentService.agentAnswer(query);
    }

    /**
     * 混合模式：RAG + Agent
     * 先用 RAG 检索相关知识，再由 Agent 进行综合分析
     */
    private ChatResponse processHybrid(String query, IntentRecognizer.Intent intent) {
        try {
            // 1. 先通过 RAG 检索相关信息
            ChatResponse ragResult = ragService.ragAnswer(query);

            // 2. 将检索结果作为上下文交给 Agent 进一步处理
            String ragContext = buildHybridContext(ragResult);

            // 如果没有检索到内容，直接使用 Agent
            if (ragContext.isEmpty()) {
                return agentService.agentAnswer(query);
            }

            String enhancedPrompt = """
                    基于以下从企业内部知识库检索的信息：

                    %s

                    请结合你的分析能力，给出综合回答。
                    """;

            return agentService.agentAnswer(String.format(enhancedPrompt, ragContext));
        } catch (Exception e) {
            // 如果 RAG 出错，回退到 Agent
            return agentService.agentAnswer(query);
        }
    }

    /**
     * 构建混合模式上下文
     */
    private String buildHybridContext(ChatResponse ragResult) {
        StringBuilder context = new StringBuilder();

        if (ragResult != null && ragResult.getRetrievalResults() != null) {
            for (var result : ragResult.getRetrievalResults()) {
                if (result != null && result.getChunk() != null && result.getChunk().getContent() != null) {
                    context.append(result.getChunk().getContent()).append("\n");
                }
            }
        }

        return context.toString();
    }

    /**
     * 添加响应元数据
     */
    private void addMetadata(ChatResponse response, EngineChoice engineChoice,
                             long processingTime, IntentRecognizer.Intent intent) {
        Map<String, Object> metadata = Map.of(
                "engine", engineChoice.name(),
                "detectedIntent", intent.name(),
                "processingTimeMs", processingTime
        );
        response.setMetadata(metadata);
    }

    /**
     * 引擎选择枚举
     */
    private enum EngineChoice {
        RAG,      // 仅使用 RAG
        AGENT,    // 仅使用 Agent
        HYBRID    // RAG + Agent 混合模式
    }

    /**
     * 获取系统状态信息
     */
    public SystemStatus getStatus() {
        return new SystemStatus(true, "System ready", Instant.now());
    }

    /**
     * 系统状态
     */
    public static class SystemStatus {
        private boolean healthy;
        private String message;
        private Instant timestamp;

        public SystemStatus(boolean healthy, String message, Instant timestamp) {
            this.healthy = healthy;
            this.message = message;
            this.timestamp = timestamp;
        }

        public boolean isHealthy() { return healthy; }
        public String getMessage() { return message; }
        public Instant getTimestamp() { return timestamp; }
    }
}