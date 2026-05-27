package com.enterprise.knowledge.infrastructure.observability;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ConversationTracer {

    private final Map<String, List<TraceRecord>> sessionTraces = new ConcurrentHashMap<>();
    private final int MAX_RECORDS_PER_SESSION = 100;

    public void recordQuery(String sessionId, String query, String intent, long processingTime) {
        TraceRecord record = new TraceRecord();
        record.type = "QUERY";
        record.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        record.content = query;
        record.intent = intent;
        record.processingTime = processingTime;
        
        addRecord(sessionId, record);
    }

    public void recordRetrieval(String sessionId, List<String> retrievedDocuments) {
        TraceRecord record = new TraceRecord();
        record.type = "RETRIEVAL";
        record.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        record.retrievedDocs = new ArrayList<>(retrievedDocuments);
        
        addRecord(sessionId, record);
    }

    public void recordSkillExecution(String sessionId, String skillName, String result, boolean success) {
        TraceRecord record = new TraceRecord();
        record.type = "SKILL";
        record.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        record.skillName = skillName;
        record.content = result;
        record.success = success;
        
        addRecord(sessionId, record);
    }

    public void recordUserFeedback(String sessionId, String feedback, int rating) {
        TraceRecord record = new TraceRecord();
        record.type = "FEEDBACK";
        record.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        record.content = feedback;
        record.rating = rating;
        
        addRecord(sessionId, record);
    }

    private void addRecord(String sessionId, TraceRecord record) {
        sessionTraces.computeIfAbsent(sessionId, k -> new ArrayList<>());
        List<TraceRecord> records = sessionTraces.get(sessionId);
        
        synchronized (records) {
            records.add(record);
            
            if (records.size() > MAX_RECORDS_PER_SESSION) {
                records.remove(0);
            }
        }
    }

    public List<TraceRecord> getTraceHistory(String sessionId) {
        return new ArrayList<>(sessionTraces.getOrDefault(sessionId, Collections.emptyList()));
    }

    public String generateTraceReport(String sessionId) {
        List<TraceRecord> records = getTraceHistory(sessionId);
        
        if (records.isEmpty()) {
            return "暂无追踪记录";
        }

        StringBuilder report = new StringBuilder();
        report.append("📊 会话追踪报告\n");
        report.append("═══════════════════════════\n\n");
        report.append(String.format("会话ID: %s\n", sessionId));
        report.append(String.format("记录数: %d\n\n", records.size()));

        for (TraceRecord record : records) {
            report.append(String.format("[%s] %s\n", record.timestamp, record.type));
            
            switch (record.type) {
                case "QUERY":
                    report.append(String.format("  问题: %s\n", record.content));
                    report.append(String.format("  意图: %s\n", record.intent));
                    report.append(String.format("  处理时间: %dms\n", record.processingTime));
                    break;
                    
                case "RETRIEVAL":
                    report.append(String.format("  检索文档数: %d\n", record.retrievedDocs.size()));
                    for (int i = 0; i < Math.min(3, record.retrievedDocs.size()); i++) {
                        String doc = record.retrievedDocs.get(i);
                        report.append(String.format("  📄 文档%d: %s\n", i + 1, 
                                doc.length() > 50 ? doc.substring(0, 50) + "..." : doc));
                    }
                    break;
                    
                case "SKILL":
                    report.append(String.format("  技能: %s\n", record.skillName));
                    report.append(String.format("  结果: %s\n", record.success ? "✅ 成功" : "❌ 失败"));
                    break;
                    
                case "FEEDBACK":
                    report.append(String.format("  反馈: %s\n", record.content));
                    report.append(String.format("  评分: %d/5\n", record.rating));
                    break;
            }
            
            report.append("\n");
        }

        return report.toString();
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalSessions = sessionTraces.size();
        int totalQueries = sessionTraces.values().stream()
                .mapToInt(list -> (int) list.stream()
                        .filter(r -> "QUERY".equals(r.type)).count()).sum();
        
        stats.put("totalSessions", totalSessions);
        stats.put("totalQueries", totalQueries);
        stats.put("activeSessions", sessionTraces.size());
        
        return stats;
    }

    public void clearSession(String sessionId) {
        sessionTraces.remove(sessionId);
    }

    public static class TraceRecord {
        public String type;
        public String timestamp;
        public String content;
        public String intent;
        public long processingTime;
        public List<String> retrievedDocs;
        public String skillName;
        public boolean success;
        public int rating;
    }
}
