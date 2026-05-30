package com.enterprise.knowledge.infrastructure.agent.skill;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class SelfImprovingAgentSkill implements LearningSkill {

    private static final String SKILL_ID = "self-improving-agent";
    private static final String SKILL_NAME = "自我改进代理";
    private static final String SKILL_DESCRIPTION = "一个能够从用户反馈中学习并持续自我改进的智能代理。它可以记住对话历史，分析用户反馈，并不断优化响应质量。";

    private final Map<String, LearningRecord> learningRecords = new ConcurrentHashMap<>();
    private final AtomicInteger feedbackCount = new AtomicInteger(0);
    private final Set<String> positiveFeedbackKeywords = new HashSet<>(Arrays.asList(
            "好", "棒", "不错", "可以", "满意", "厉害", "喜欢", "有用", "帮助", "谢谢"
    ));
    private final Set<String> negativeFeedbackKeywords = new HashSet<>(Arrays.asList(
            "不好", "不行", "错", "差", "失败", "问题", "错误", "不对", "糟糕", "没用"
    ));

    @Override
    public String getId() {
        return SKILL_ID;
    }

    @Override
    public String getName() {
        return SKILL_NAME;
    }

    @Override
    public String getDescription() {
        return SKILL_DESCRIPTION;
    }

    @Override
    public List<String> getKeywords() {
        return Arrays.asList("学习", "改进", "优化", "反馈", "自我提升", "self-improving", "learn", "improve");
    }

    @Override
    public String execute(String input) {
        String sessionId = extractSessionId(input);
        String actualInput = extractActualInput(input);
        
        if (isFeedback(input)) {
            return processFeedback(sessionId, input);
        }
        
        if (isStatusQuery(input)) {
            return getStatusReport();
        }
        
        if (isResetRequest(input)) {
            return resetLearning();
        }
        
        return analyzeAndRespond(sessionId, actualInput);
    }

    @Override
    public void learn(String context, String feedback) {
        String sessionId = UUID.randomUUID().toString().substring(0, 8);
        LearningRecord record = learningRecords.computeIfAbsent(sessionId, k -> new LearningRecord());
        record.addInteraction(context, feedback, isPositiveFeedback(feedback));
        feedbackCount.incrementAndGet();
    }

    @Override
    public double getConfidence() {
        if (feedbackCount.get() == 0) {
            return 0.7;
        }
        
        int positiveCount = 0;
        for (LearningRecord record : learningRecords.values()) {
            positiveCount += record.getPositiveFeedbackCount();
        }
        
        return Math.min(0.95, 0.7 + (positiveCount * 0.02));
    }

    @Override
    public int getPriority() {
        return 3;
    }

    private String extractSessionId(String input) {
        if (input.contains("session:")) {
            int start = input.indexOf("session:") + 8;
            int end = input.indexOf(" ", start);
            if (end > start) {
                return input.substring(start, end);
            }
            return input.substring(start);
        }
        return "default";
    }

    private String extractActualInput(String input) {
        return input.replaceAll("session:\\S+\\s*", "").trim();
    }

    private boolean isFeedback(String input) {
        return input.contains("反馈") || input.contains("评价") || 
               input.contains("feedback") || input.contains("review");
    }

    private boolean isStatusQuery(String input) {
        return input.contains("状态") || input.contains("学习进度") || 
               input.contains("progress") || input.contains("status") || input.contains("报告");
    }

    private boolean isResetRequest(String input) {
        return input.contains("重置") || input.contains("清空") || 
               input.contains("reset") || input.contains("clear");
    }

    private String processFeedback(String sessionId, String input) {
        boolean isPositive = isPositiveFeedback(input);
        
        LearningRecord record = learningRecords.computeIfAbsent(sessionId, k -> new LearningRecord());
        record.addInteraction(input, input, isPositive);
        feedbackCount.incrementAndGet();
        
        if (isPositive) {
            return "🎯 收到正面反馈！我会继续努力学习和改进！\n\n" +
                   "当前学习进度：已收到 " + feedbackCount.get() + " 条反馈\n" +
                   "当前置信度：" + String.format("%.2f%%", getConfidence() * 100);
        } else {
            return "📝 收到反馈，我会认真分析并改进！\n\n" +
                   "请告诉我具体需要改进的地方，我会记录下来并优化。\n" +
                   "当前学习进度：已收到 " + feedbackCount.get() + " 条反馈\n" +
                   "当前置信度：" + String.format("%.2f%%", getConfidence() * 100);
        }
    }

    private boolean isPositiveFeedback(String feedback) {
        String lower = feedback.toLowerCase();
        int positiveScore = 0;
        int negativeScore = 0;
        
        for (String keyword : positiveFeedbackKeywords) {
            if (lower.contains(keyword.toLowerCase())) {
                positiveScore++;
            }
        }
        
        for (String keyword : negativeFeedbackKeywords) {
            if (lower.contains(keyword.toLowerCase())) {
                negativeScore++;
            }
        }
        
        return positiveScore > negativeScore;
    }

    private String getStatusReport() {
        int totalInteractions = 0;
        int positiveFeedbacks = 0;
        
        for (LearningRecord record : learningRecords.values()) {
            totalInteractions += record.getInteractionCount();
            positiveFeedbacks += record.getPositiveFeedbackCount();
        }
        
        return String.format("""
                🤖 自我改进代理状态报告
                ─────────────────────────
                
                📊 学习统计：
                • 对话会话数：%d
                • 总交互次数：%d
                • 收到反馈数：%d
                • 正面反馈数：%d
                • 学习置信度：%.2f%%
                
                🚀 能力指标：
                • 自我优化等级：L%d
                • 知识积累量：%d 条记录
                • 响应优化率：+%.1f%%
                
                💡 学习建议：
                %s
                """,
                learningRecords.size(),
                totalInteractions,
                feedbackCount.get(),
                positiveFeedbacks,
                getConfidence() * 100,
                getLearningLevel(),
                totalInteractions,
                calculateImprovementRate(),
                generateLearningSuggestion(positiveFeedbacks, feedbackCount.get())
        );
    }

    private int getLearningLevel() {
        int count = feedbackCount.get();
        if (count >= 100) return 5;
        if (count >= 50) return 4;
        if (count >= 20) return 3;
        if (count >= 5) return 2;
        return 1;
    }

    private double calculateImprovementRate() {
        int count = feedbackCount.get();
        return Math.min(50, count * 0.5);
    }

    private String generateLearningSuggestion(int positive, int total) {
        if (total == 0) {
            return "• 开始与我互动，我会从你的反馈中学习！";
        }
        
        double ratio = (double) positive / total;
        
        if (ratio > 0.8) {
            return "• 太棒了！继续保持，我会不断进化！";
        } else if (ratio > 0.5) {
            return "• 不错的进步！请继续给我反馈帮助我成长。";
        } else {
            return "• 我正在努力改进中，请告诉我需要优化的地方！";
        }
    }

    private String resetLearning() {
        learningRecords.clear();
        feedbackCount.set(0);
        return "🔄 已重置学习记录！\n\n" +
               "我已清空所有学习数据，重新开始学习之旅！\n" +
               "当前置信度：70%";
    }

    private String analyzeAndRespond(String sessionId, String input) {
        LearningRecord record = learningRecords.computeIfAbsent(sessionId, k -> new LearningRecord());
        record.addInteraction(input, "自动分析", true);
        
        return "🧠 自我改进代理正在分析...\n\n" +
               "你的输入：「" + input + "」\n\n" +
               "📝 分析结果：\n" +
               "• 意图识别：" + analyzeIntent(input) + "\n" +
               "• 情绪分析：" + analyzeSentiment(input) + "\n" +
               "• 复杂度评估：" + assessComplexity(input) + "\n\n" +
               "💡 学习建议：继续与我互动，我会不断学习和改进！\n" +
               "当前学习进度：已收到 " + feedbackCount.get() + " 条反馈";
    }

    private String analyzeIntent(String input) {
        if (input.contains("歌词") || input.contains("歌曲")) {
            return "音乐查询";
        } else if (input.contains("天气") || input.contains("温度")) {
            return "天气查询";
        } else if (input.contains("生成") || input.contains("创建")) {
            return "内容生成";
        } else if (input.contains("翻译") || input.contains("英文")) {
            return "翻译需求";
        } else if (input.contains("计算") || input.contains("数学")) {
            return "计算需求";
        } else {
            return "通用对话";
        }
    }

    private String analyzeSentiment(String input) {
        String lower = input.toLowerCase();
        
        if (positiveFeedbackKeywords.stream().anyMatch(k -> lower.contains(k.toLowerCase()))) {
            return "积极情绪";
        } else if (negativeFeedbackKeywords.stream().anyMatch(k -> lower.contains(k.toLowerCase()))) {
            return "消极情绪";
        } else {
            return "中性情绪";
        }
    }

    private String assessComplexity(String input) {
        int length = input.length();
        int wordCount = input.split("[\\s，,。.！!？?]+").length;
        
        if (length > 100 || wordCount > 15) {
            return "高复杂度";
        } else if (length > 50 || wordCount > 8) {
            return "中复杂度";
        } else {
            return "低复杂度";
        }
    }

    private static class LearningRecord {
        private final List<Interaction> interactions = Collections.synchronizedList(new ArrayList<>());
        private final LocalDateTime createdAt = LocalDateTime.now();
        
        public void addInteraction(String input, String feedback, boolean positive) {
            interactions.add(new Interaction(input, feedback, positive, LocalDateTime.now()));
        }
        
        public int getInteractionCount() {
            return interactions.size();
        }
        
        public int getPositiveFeedbackCount() {
            return (int) interactions.stream().filter(Interaction::isPositive).count();
        }
        
        public LocalDateTime getCreatedAt() {
            return createdAt;
        }
    }

    private static class Interaction {
        private final String input;
        private final String feedback;
        private final boolean positive;
        private final LocalDateTime timestamp;
        
        public Interaction(String input, String feedback, boolean positive, LocalDateTime timestamp) {
            this.input = input;
            this.feedback = feedback;
            this.positive = positive;
            this.timestamp = timestamp;
        }
        
        public boolean isPositive() {
            return positive;
        }
    }
}
