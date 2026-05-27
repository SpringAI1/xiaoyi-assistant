package com.enterprise.knowledge.application.service;

import com.enterprise.knowledge.domain.ChatMessage;
import com.enterprise.knowledge.domain.ChatResponse;
import com.enterprise.knowledge.infrastructure.agent.EnhancedAgentOrchestrator;
import com.enterprise.knowledge.infrastructure.agent.IntentRecognizer;
import com.enterprise.knowledge.infrastructure.agent.skill.Skill;
import com.enterprise.knowledge.infrastructure.agent.skill.SkillManager;
import com.enterprise.knowledge.infrastructure.agent.tool.CityExtractor;
import com.enterprise.knowledge.infrastructure.agent.tool.DocumentGeneratorTool;
import com.enterprise.knowledge.infrastructure.agent.tool.MediaGeneratorTool;
import com.enterprise.knowledge.infrastructure.agent.tool.WeatherTool;
import com.enterprise.knowledge.infrastructure.rag.EnhancedRagEngine;
import com.enterprise.knowledge.service.ConversationMemoryService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AgentService {

    private static final Logger logger = LoggerFactory.getLogger(AgentService.class);

    private final ChatLanguageModel chatModel;
    private final IntentRecognizer intentRecognizer;
    private final RagService ragService;
    private final EnhancedRagEngine enhancedRagEngine;
    private final EnhancedAgentOrchestrator orchestrator;
    private final DocumentGeneratorTool documentGeneratorTool;
    private final MediaGeneratorTool mediaGeneratorTool;
    private final WeatherTool weatherTool;
    private final CityExtractor cityExtractor;
    private final ConversationMemoryService memoryService;
    private final SkillManager skillManager;

    public AgentService(ChatLanguageModel chatModel,
                        IntentRecognizer intentRecognizer,
                        RagService ragService,
                        EnhancedRagEngine enhancedRagEngine,
                        EnhancedAgentOrchestrator orchestrator,
                        DocumentGeneratorTool documentGeneratorTool,
                        MediaGeneratorTool mediaGeneratorTool,
                        WeatherTool weatherTool,
                        CityExtractor cityExtractor,
                        ConversationMemoryService memoryService,
                        SkillManager skillManager) {
        this.chatModel = chatModel;
        this.intentRecognizer = intentRecognizer;
        this.ragService = ragService;
        this.enhancedRagEngine = enhancedRagEngine;
        this.orchestrator = orchestrator;
        this.documentGeneratorTool = documentGeneratorTool;
        this.mediaGeneratorTool = mediaGeneratorTool;
        this.weatherTool = weatherTool;
        this.cityExtractor = cityExtractor;
        this.memoryService = memoryService;
        this.skillManager = skillManager;
    }

    public ChatResponse agentAnswer(String userPrompt) {
        return agentAnswer(userPrompt, null);
    }

    public ChatResponse agentAnswer(String userPrompt, String sessionId) {
        long startTime = System.currentTimeMillis();
        logger.info("开始处理用户请求: {}", userPrompt);

        String historyPrompt = sessionId != null ? memoryService.buildHistoryPrompt(sessionId) : "";

        ChatResponse response;
        Skill usedSkill = null;
        
        try {
            // 使用增强版Agent决策引擎
            var decision = orchestrator.decideAction(userPrompt, sessionId);
            logger.info("Agent决策: {}, 置信度: {}", decision.getActionType(), decision.getConfidence());

            switch (decision.getActionType()) {
                case USE_SKILL:
                case CONTINUE_SKILL:
                    Skill skill = decision.getTargetSkill();
                    if (skill != null) {
                        usedSkill = skill;
                        String skillResult = skill.execute(userPrompt);
                        response = new ChatResponse();
                        response.setContent(skillResult);
                        response.setResponseType(ChatResponse.ResponseType.DIRECT_ANSWER);
                        logger.info("使用技能: {}", skill.getName());
                    } else {
                        response = handleDirectChat(userPrompt, historyPrompt);
                    }
                    break;

                case USE_RAG:
                    response = enhancedRagEngine.enhancedRagAnswer(userPrompt, historyPrompt);
                    logger.info("使用增强版RAG引擎");
                    break;

                case CLARIFY:
                    response = new ChatResponse();
                    response.setContent("您的问题有点简短，能否详细描述一下您的需求？");
                    response.setResponseType(ChatResponse.ResponseType.DIRECT_ANSWER);
                    break;

                case CHAT:
                default:
                    response = handleDirectChat(userPrompt, historyPrompt);
                    break;
            }
        } catch (Exception e) {
            logger.error("处理请求时出错", e);
            response = new ChatResponse();
            response.setContent("抱歉，处理您的问题时出现了错误，请稍后再试。");
            response.setProcessingTime(System.currentTimeMillis() - startTime);
            return response;
        }

        if (sessionId != null) {
            orchestrator.updateContext(sessionId, userPrompt, response.getContent(), usedSkill);
            memoryService.addMessage(sessionId, new ChatMessage(ChatMessage.Role.USER, userPrompt));
            memoryService.addMessage(sessionId, new ChatMessage(ChatMessage.Role.ASSISTANT, response.getContent()));
        }

        response.setProcessingTime(System.currentTimeMillis() - startTime);
        logger.info("请求处理完成，耗时: {}ms", System.currentTimeMillis() - startTime);
        return response;
    }

    private ChatResponse handleFactQuery(String query, String history) {
        String enhancedQuery = history.isEmpty() ? query : history + "\n\n当前问题: " + query;
        return ragService.ragAnswer(enhancedQuery);
    }

    private ChatResponse handleDirectChat(String prompt, String history) {
        String chatPrompt = (history.isEmpty() ? "" : history + "\n\n") + """
                作为小易助手，请回答以下问题：
                
                %s
                
                请给出准确、有用的回答。
                """.formatted(prompt);

        String answer = chatModel.generate(chatPrompt);

        ChatResponse response = new ChatResponse();
        response.setContent(answer);
        response.setResponseType(ChatResponse.ResponseType.DIRECT_ANSWER);
        return response;
    }

    private ChatResponse handleDocumentGeneration(String prompt) {
        String lowerPrompt = prompt.toLowerCase();
        
        if (lowerPrompt.contains("ppt") || lowerPrompt.contains("powerpoint") || 
            lowerPrompt.contains("演示文稿")) {
            
            String topic = "企业宣传";
            String cleanPrompt = prompt
                .replace("给我", "").replace("帮我", "").replace("生成", "")
                .replace("一个", "").replace("ppt", "").replace("PPT", "")
                .replace("演示文稿", "").replace("模板", "").replace("模版", "")
                .replace("的", "").trim();
            
            if (!cleanPrompt.isEmpty()) {
                topic = cleanPrompt;
            }
            
            String downloadUrl = "/api/v1/generate/ppt/download?topic=" + 
                java.net.URLEncoder.encode(topic, java.nio.charset.StandardCharsets.UTF_8);
            
            String answer = String.format("""
                    ✅ PPT生成成功！
                    
                    📊 PPT主题：%s
                    
                    📋 PPT包含以下页面：
                    1. 封面页
                    2. 目录页
                    3. 公司概览
                    4. 核心业务
                    5. 竞争优势
                    6. 发展战略
                    7. 未来展望
                    8. 结束页
                    
                    📥 下载链接：点击下面的链接下载PPT文件
                    %s
                    
                    💡 提示：您可以修改主题，例如"生成企业战略规划PPT"
                    """, topic, downloadUrl);
            
            ChatResponse response = new ChatResponse();
            response.setContent(answer);
            response.setResponseType(ChatResponse.ResponseType.DIRECT_ANSWER);
            return response;
        }
        
        String generationPrompt = """
                请分析用户需求，确定需要生成的文档类型（PPT、Word、Excel、报告等），并生成详细内容。
                
                用户需求：
                %s
                
                请按照以下格式输出：
                【文档类型】：识别用户需要生成的文档类型
                【内容大纲】：列出文档的主要章节和内容要点
                【详细内容】：根据大纲生成完整的文档内容
                
                请确保内容完整、结构清晰、格式规范。
                """.formatted(prompt);

        String answer = chatModel.generate(generationPrompt);

        ChatResponse response = new ChatResponse();
        response.setContent(answer);
        response.setResponseType(ChatResponse.ResponseType.DIRECT_ANSWER);
        return response;
    }

    private ChatResponse handleCreativeWriting(String prompt, String history) {
        String wordCountHint = "";
        int targetWordCount = 0;
        String lowerPrompt = prompt.toLowerCase();
        
        if (lowerPrompt.contains("字")) {
            try {
                String[] words = prompt.split("\\D+");
                for (String w : words) {
                    if (!w.isEmpty()) {
                        int num = Integer.parseInt(w);
                        if (num >= 50 && num <= 50000) {
                            targetWordCount = num;
                            wordCountHint = "（约" + targetWordCount + "字）";
                            break;
                        }
                    }
                }
            } catch (Exception e) {
            }
        }
        
        if (targetWordCount == 0) {
            targetWordCount = 500;
            wordCountHint = "（约500字）";
        }

        String writingPrompt = (history.isEmpty() ? "" : history + "\n\n") + """
                请根据用户的需求进行创意写作%s。
                
                用户需求：
                %s
                
                重要要求：
                1. 请确保生成的内容达到约%d字的长度
                2. 语言要流畅自然，内容要丰富饱满
                3. 结构清晰，层次分明
                4. 避免过于简短的回答
                5. 请充分展开内容，提供详细的描述
                """.formatted(wordCountHint, prompt, targetWordCount);

        String answer = chatModel.generate(writingPrompt);

        ChatResponse response = new ChatResponse();
        response.setContent(answer);
        response.setResponseType(ChatResponse.ResponseType.DIRECT_ANSWER);
        return response;
    }

    private ChatResponse handleToolRequired(String prompt) {
        String answer = chatModel.generate("""
                请分析用户的请求，确定是否需要使用工具。如果需要，请说明使用什么工具以及参数。
                
                用户请求：%s
                
                可用工具：计算器、公司制度查询、网络搜索
                """.formatted(prompt));

        ChatResponse response = new ChatResponse();
        response.setContent(answer);
        response.setResponseType(ChatResponse.ResponseType.TOOL_EXECUTION);
        return response;
    }

    private ChatResponse handleDataAnalysis(String prompt, String history) {
        String analysisPrompt = (history.isEmpty() ? "" : history + "\n\n") + """
                请对以下数据或问题进行深入分析：
                
                用户请求：
                %s
                
                请提供详细的分析报告，包括数据分析、趋势预测、建议方案等。
                """.formatted(prompt);

        String answer = chatModel.generate(analysisPrompt);

        ChatResponse response = new ChatResponse();
        response.setContent(answer);
        response.setResponseType(ChatResponse.ResponseType.DIRECT_ANSWER);
        return response;
    }

    private ChatResponse handleMediaGeneration(String prompt) {
        String answer = chatModel.generate("""
                请分析用户的多媒体生成需求，确定需要生成的类型（视频、音乐、音频、图片），并生成详细的内容描述。
                
                用户需求：
                %s
                
                请按照以下格式输出：
                【媒体类型】：识别用户需要生成的媒体类型
                【内容描述】：详细描述生成内容的主题、风格、时长等
                【生成方案】：提供具体的生成方案和步骤
                """.formatted(prompt));

        ChatResponse response = new ChatResponse();
        response.setContent(answer);
        response.setResponseType(ChatResponse.ResponseType.DIRECT_ANSWER);
        return response;
    }

    private ChatResponse handleWeatherQuery(String prompt) {
        String city = cityExtractor.extractCity(prompt);
        
        if (city == null || city.isEmpty()) {
            city = "北京";
        }
        
        String answer = weatherTool.getCurrentWeather(city);

        ChatResponse response = new ChatResponse();
        response.setContent(answer);
        response.setResponseType(ChatResponse.ResponseType.DIRECT_ANSWER);
        return response;
    }

    public String generatePPT(String topic) {
        return documentGeneratorTool.generatePPT(topic, "");
    }

    public byte[] generatePPTDocument(String topic) throws Exception {
        return documentGeneratorTool.generatePPTDocument(topic);
    }

    public String generateWord(String title) {
        return documentGeneratorTool.generateWord(title, "");
    }

    public String generateTable(String title, String[] headers, String[][] data) {
        return documentGeneratorTool.generateTable(title, headers, data);
    }

    public String generateVideo(String topic, String duration, String style) {
        return mediaGeneratorTool.generateVideo(topic, duration, style);
    }

    public String generateMusic(String genre, String mood, String duration) {
        return mediaGeneratorTool.generateMusic(genre, mood, duration);
    }

    public String getWeather(String city) {
        return weatherTool.getCurrentWeather(city);
    }
}
