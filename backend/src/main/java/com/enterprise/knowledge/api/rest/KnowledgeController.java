package com.enterprise.knowledge.api.rest;

import com.enterprise.knowledge.application.service.AgentService;
import com.enterprise.knowledge.application.service.DualEngineCoordinator;
import com.enterprise.knowledge.application.service.RagService;
import com.enterprise.knowledge.domain.ChatMessage;
import com.enterprise.knowledge.domain.ChatResponse;
import com.enterprise.knowledge.domain.entity.Conversation;
import com.enterprise.knowledge.infrastructure.agent.tool.DocumentGeneratorTool;
import com.enterprise.knowledge.infrastructure.search.WebSearchService;
import com.enterprise.knowledge.service.AsyncDocumentService;
import com.enterprise.knowledge.service.ConversationMemoryService;
import com.enterprise.knowledge.service.RateLimitService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1")
public class KnowledgeController {
    private final DualEngineCoordinator coordinator;
    private final AsyncDocumentService documentService;
    private final RagService ragService;
    private final ChatLanguageModel chatModel;
    private final WebSearchService webSearchService;
    private final AgentService agentService;
    private final ConversationMemoryService memoryService;
    private final DocumentGeneratorTool documentGeneratorTool;
    private final RateLimitService rateLimitService;

    public KnowledgeController(DualEngineCoordinator coordinator,
                               AsyncDocumentService documentService,
                               RagService ragService,
                               ChatLanguageModel chatModel,
                               WebSearchService webSearchService,
                               AgentService agentService,
                               ConversationMemoryService memoryService,
                               DocumentGeneratorTool documentGeneratorTool,
                               RateLimitService rateLimitService) {
        this.coordinator = coordinator;
        this.documentService = documentService;
        this.ragService = ragService;
        this.chatModel = chatModel;
        this.webSearchService = webSearchService;
        this.agentService = agentService;
        this.memoryService = memoryService;
        this.documentGeneratorTool = documentGeneratorTool;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request,
                                             @RequestHeader(value = "X-Client-ID", defaultValue = "default") String clientId) {
        if (!rateLimitService.checkAndIncrement(clientId)) {
            ChatResponse response = new ChatResponse();
            response.setContent("请求过于频繁，请稍后再试。");
            response.setResponseType(ChatResponse.ResponseType.DIRECT_ANSWER);
            return ResponseEntity.status(429).body(response);
        }
        
        String searchMode = request.getSearchMode() != null ? request.getSearchMode() : "knowledge";

        if ("web".equals(searchMode)) {
            String searchResult = webSearchService.search(request.getQuery());
            ChatResponse response = new ChatResponse();
            response.setContent(searchResult);
            response.setResponseType(ChatResponse.ResponseType.WEB_SEARCH);
            response.setProcessingTime(0);
            return ResponseEntity.ok(response);
        } else if ("hybrid".equals(searchMode)) {
            ChatResponse ragResponse = coordinator.answer(request.getQuery());
            String webResult = webSearchService.search(request.getQuery());
            String combinedContent = ragResponse.getContent() + "\n\n" +
                "══════════════════════════════════\n\n" +
                "【联网补充信息】\n" + webResult;
            ChatResponse combinedResponse = new ChatResponse();
            combinedResponse.setContent(combinedContent);
            combinedResponse.setResponseType(ChatResponse.ResponseType.HYBRID);
            combinedResponse.setRetrievalResults(ragResponse.getRetrievalResults());
            combinedResponse.setProcessingTime(ragResponse.getProcessingTime());
            return ResponseEntity.ok(combinedResponse);
        } else {
            String sessionId = request.getSessionId();
            ChatResponse response = agentService.agentAnswer(request.getQuery(), sessionId);
            return ResponseEntity.ok(response);
        }
    }

    @PostMapping("/chat/session")
    public ResponseEntity<Map<String, Object>> createSession(@RequestParam(value = "userId", required = false) String userId) {
        Map<String, Object> response = new HashMap<>();
        try {
            String sessionId = memoryService.createSession(userId);
            response.put("status", "success");
            response.put("sessionId", sessionId);
            response.put("message", "会话已创建");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "创建会话失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/chat/sessions")
    public ResponseEntity<Map<String, Object>> getSessions(@RequestParam(value = "userId", required = false) String userId) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Conversation> sessions = memoryService.getUserSessions(userId);
            response.put("status", "success");
            response.put("sessions", sessions);
            response.put("count", sessions.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "获取会话列表失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/chat/session/{sessionId}")
    public ResponseEntity<Map<String, Object>> getSessionHistory(@PathVariable String sessionId) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (!memoryService.hasSession(sessionId)) {
                response.put("status", "not_found");
                response.put("message", "会话不存在");
                return ResponseEntity.notFound().build();
            }
            
            List<ChatMessage> history = memoryService.getHistory(sessionId);
            response.put("status", "success");
            response.put("sessionId", sessionId);
            response.put("history", history);
            response.put("count", history.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "获取会话历史失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/chat/session/{sessionId}")
    public ResponseEntity<Map<String, Object>> clearSession(@PathVariable String sessionId) {
        Map<String, Object> response = new HashMap<>();
        try {
            memoryService.clearSession(sessionId);
            response.put("status", "success");
            response.put("message", "会话已清除");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "清除会话失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/chat/sessions")
    public ResponseEntity<Map<String, Object>> clearAllSessions() {
        Map<String, Object> response = new HashMap<>();
        try {
            memoryService.clearAllSessions();
            response.put("status", "success");
            response.put("message", "所有会话已清除");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "清除会话失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@RequestParam("query") String query,
                                  @RequestParam(value = "sessionId", required = false) String sessionId) {
        SseEmitter emitter = new SseEmitter(30000L);

        CompletableFuture.runAsync(() -> {
            try {
                ChatResponse response = agentService.agentAnswer(query, sessionId);

                String content = response.getContent();
                int chunkSize = 50;

                for (int i = 0; i < content.length(); i += chunkSize) {
                    int end = Math.min(i + chunkSize, content.length());
                    emitter.send(SseEmitter.event()
                        .name("message")
                        .data(content.substring(i, end)));
                    Thread.sleep(50);
                }

                emitter.send(SseEmitter.event()
                    .name("metadata")
                    .data(Map.of(
                        "engine", response.getMetadata() != null ? response.getMetadata().get("engine") : "UNKNOWN",
                        "processingTimeMs", response.getProcessingTime()
                    )));

                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "UP");
        status.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(status);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        try {
            var systemStatus = coordinator.getStatus();
            var docStats = ragService.getDocumentStats();

            status.put("status", systemStatus.isHealthy() ? "UP" : "DOWN");
            status.put("message", systemStatus.getMessage());
            status.put("timestamp", System.currentTimeMillis());
            status.put("documents", Map.of(
                "total", docStats.getTotalDocuments(),
                "totalChunks", docStats.getTotalChunks()
            ));
            status.put("engine", Map.of(
                "ragEnabled", true,
                "agentEnabled", true,
                "hybridEnabled", true,
                "webSearchEnabled", true,
                "memoryEnabled", true
            ));
            status.put("activeSessions", memoryService.getSessionCount());

            return ResponseEntity.ok(status);
        } catch (Exception e) {
            status.put("status", "ERROR");
            status.put("message", "获取系统状态失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(status);
        }
    }

    @PostMapping("/generate/ppt")
    public ResponseEntity<Map<String, Object>> generatePPT(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String topic = request.get("topic");
            String result = agentService.generatePPT(topic != null ? topic : "企业知识管理");
            response.put("status", "success");
            response.put("content", result);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "生成PPT失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/generate/ppt/download")
    public ResponseEntity<byte[]> downloadPPT(
            @RequestParam("topic") String topic,
            @RequestParam(value = "template", required = false) String templateParam) {
        try {
            byte[] pptBytes;
            var template = templateParam != null ? 
                parseTemplate(templateParam) : 
                com.enterprise.knowledge.infrastructure.agent.tool.DocumentGeneratorTool.PPTTemplate.BUSINESS_PROFESSIONAL;
                
            pptBytes = documentGeneratorTool.generatePPTDocument(topic, template);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(new MediaType("application", "vnd.openxmlformats-officedocument.presentationml.presentation"));
            headers.setContentDispositionFormData("attachment", 
                (topic != null ? topic.replaceAll("\\s+", "_") : "presentation") + ".pptx");
            headers.setContentLength(pptBytes.length);

            return new ResponseEntity<>(pptBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    private com.enterprise.knowledge.infrastructure.agent.tool.DocumentGeneratorTool.PPTTemplate parseTemplate(String template) {
        return switch(template.toLowerCase()) {
            case "tech" -> com.enterprise.knowledge.infrastructure.agent.tool.DocumentGeneratorTool.PPTTemplate.TECH_INNOVATION;
            case "creative" -> com.enterprise.knowledge.infrastructure.agent.tool.DocumentGeneratorTool.PPTTemplate.CREATIVE_DESIGN;
            case "minimal" -> com.enterprise.knowledge.infrastructure.agent.tool.DocumentGeneratorTool.PPTTemplate.MODERN_MINIMAL;
            case "education" -> com.enterprise.knowledge.infrastructure.agent.tool.DocumentGeneratorTool.PPTTemplate.EDUCATIONAL;
            default -> com.enterprise.knowledge.infrastructure.agent.tool.DocumentGeneratorTool.PPTTemplate.BUSINESS_PROFESSIONAL;
        };
    }

    @PostMapping("/generate/word")
    public ResponseEntity<Map<String, Object>> generateWord(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String title = request.get("title");
            String result = agentService.generateWord(title != null ? title : "企业报告");
            response.put("status", "success");
            response.put("content", result);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "生成Word文档失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/generate/video")
    public ResponseEntity<Map<String, Object>> generateVideo(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String topic = request.get("topic");
            String duration = request.get("duration");
            String style = request.get("style");
            String result = agentService.generateVideo(
                topic != null ? topic : "企业介绍",
                duration,
                style
            );
            response.put("status", "success");
            response.put("content", result);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "生成视频脚本失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/generate/music")
    public ResponseEntity<Map<String, Object>> generateMusic(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String genre = request.get("genre");
            String mood = request.get("mood");
            String duration = request.get("duration");
            String result = agentService.generateMusic(genre, mood, duration);
            response.put("status", "success");
            response.put("content", result);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "生成音乐失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/weather")
    public ResponseEntity<Map<String, Object>> getWeather(@RequestParam(value = "city", defaultValue = "北京") String city) {
        Map<String, Object> response = new HashMap<>();
        try {
            String result = agentService.getWeather(city);
            response.put("status", "success");
            response.put("content", result);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "获取天气失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping(value = "/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadDocument(
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "userId", defaultValue = "user1") String userId) {

        Map<String, Object> response = new HashMap<>();
        try {
            documentService.processDocument(file, userId);
            response.put("status", "processing");
            response.put("message", "文档已开始处理");
            response.put("filename", file.getOriginalFilename());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "文档上传失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping(value = "/chat/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ChatResponse> chatWithDocument(
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "query", required = false) String query,
        @RequestParam(value = "sessionId", required = false) String sessionId) {
        
        try {
            String documentContent = documentService.parseDocumentContent(file);
            
            String enhancedQuery = "";
            if (documentContent != null && !documentContent.isEmpty()) {
                enhancedQuery = "请根据以下文档内容回答问题：\n\n" + 
                              "【文档内容】\n" + documentContent + "\n\n" +
                              "【用户问题】\n" + (query != null ? query : "请总结这份文档的内容");
            } else {
                enhancedQuery = query != null ? query : "文档解析失败，请重试";
            }
            
            ChatResponse response = agentService.agentAnswer(enhancedQuery, sessionId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            ChatResponse response = new ChatResponse();
            response.setContent("处理文档时发生错误: " + e.getMessage());
            response.setResponseType(ChatResponse.ResponseType.DIRECT_ANSWER);
            return ResponseEntity.ok(response);
        }
    }

    @GetMapping("/documents")
    public ResponseEntity<Map<String, Object>> listDocuments() {
        Map<String, Object> response = new HashMap<>();
        try {
            var stats = ragService.getDocumentStats();
            response.put("status", "success");
            response.put("totalDocuments", stats.getTotalDocuments());
            response.put("totalChunks", stats.getTotalChunks());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "获取文档列表失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/documents/{docId}")
    public ResponseEntity<Map<String, Object>> deleteDocument(@PathVariable String docId) {
        Map<String, Object> response = new HashMap<>();
        try {
            ragService.deleteDocument(docId);
            response.put("status", "success");
            response.put("message", "文档已删除");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "删除文档失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/documents/{docId}")
    public ResponseEntity<Map<String, Object>> getDocument(@PathVariable String docId) {
        Map<String, Object> response = new HashMap<>();
        try {
            var metadata = ragService.getDocumentById(docId);
            if (metadata != null) {
                response.put("status", "success");
                response.put("document", metadata);
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "not_found");
                response.put("message", "文档不存在");
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "获取文档失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    public static class ChatRequest {
        private String query;
        private String searchMode;
        private String sessionId;

        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
        public String getSearchMode() { return searchMode; }
        public void setSearchMode(String searchMode) { this.searchMode = searchMode; }
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    }
}
