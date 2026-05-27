package com.enterprise.knowledge.infrastructure.knowledgegraph;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class KnowledgeGraph {

    private final Map<String, KnowledgeNode> nodes = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> relationships = new ConcurrentHashMap<>();

    public void addEntity(String entity, String type, String description) {
        KnowledgeNode node = new KnowledgeNode();
        node.id = entity;
        node.type = type;
        node.description = description;
        node.createdAt = System.currentTimeMillis();
        nodes.put(entity, node);
    }

    public void addRelationship(String from, String to, String relationType) {
        relationships.computeIfAbsent(from, k -> new HashSet<>()).add(to);
        
        String edgeKey = from + "->" + to;
        KnowledgeEdge edge = new KnowledgeEdge();
        edge.from = from;
        edge.to = to;
        edge.type = relationType;
        edge.weight = 1.0;
    }

    public List<KnowledgeNode> search(String query) {
        List<KnowledgeNode> results = new ArrayList<>();
        String lowerQuery = query.toLowerCase();
        
        for (KnowledgeNode node : nodes.values()) {
            if (node.id.toLowerCase().contains(lowerQuery) ||
                node.description.toLowerCase().contains(lowerQuery) ||
                node.type.toLowerCase().contains(lowerQuery)) {
                results.add(node);
            }
        }
        
        results.sort((a, b) -> {
            int scoreA = calculateRelevance(a, lowerQuery);
            int scoreB = calculateRelevance(b, lowerQuery);
            return Integer.compare(scoreB, scoreA);
        });
        
        return results;
    }

    private int calculateRelevance(KnowledgeNode node, String query) {
        int score = 0;
        if (node.id.toLowerCase().contains(query)) score += 10;
        if (node.description.toLowerCase().contains(query)) score += 5;
        if (node.type.toLowerCase().contains(query)) score += 3;
        score += node.accessCount;
        return score;
    }

    public void incrementAccess(String entity) {
        KnowledgeNode node = nodes.get(entity);
        if (node != null) {
            node.lastAccessedAt = System.currentTimeMillis();
            node.accessCount++;
        }
    }

    public List<KnowledgeNode> getRelatedEntities(String entity, int limit) {
        List<KnowledgeNode> related = new ArrayList<>();
        Set<String> relatedIds = relationships.getOrDefault(entity, Collections.emptySet());
        
        for (String id : relatedIds) {
            KnowledgeNode node = nodes.get(id);
            if (node != null) {
                related.add(node);
            }
            if (related.size() >= limit) break;
        }
        
        return related;
    }

    public String generateInsights(String domain) {
        List<KnowledgeNode> domainNodes = search(domain);
        
        if (domainNodes.isEmpty()) {
            return "暂无相关知识图谱信息";
        }

        StringBuilder insights = new StringBuilder();
        insights.append("🧠 知识图谱分析：").append(domain).append("\n\n");
        
        insights.append("📊 **基本信息**\n");
        insights.append("- 相关实体数量：").append(domainNodes.size()).append("\n");
        
        insights.append("\n🔗 **关系网络**\n");
        int relationCount = relationships.values().stream()
                .mapToInt(Set::size).sum();
        insights.append("- 关联关系数量：").append(relationCount).append("\n");
        
        insights.append("\n💡 **智能建议**\n");
        if (!domainNodes.isEmpty()) {
            KnowledgeNode topNode = domainNodes.get(0);
            insights.append("- 最相关实体：").append(topNode.id).append("\n");
            insights.append("- 类型：").append(topNode.type).append("\n");
            insights.append("- 描述：").append(topNode.description).append("\n");
        }
        
        return insights.toString();
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalEntities", nodes.size());
        stats.put("totalRelationships", 
                relationships.values().stream().mapToInt(Set::size).sum());
        
        Map<String, Long> typeDistribution = new HashMap<>();
        for (KnowledgeNode node : nodes.values()) {
            typeDistribution.merge(node.type, 1L, Long::sum);
        }
        stats.put("typeDistribution", typeDistribution);
        
        return stats;
    }

    public void initializeDefaultKnowledge() {
        addEntity("人工智能", "技术领域", "AI技术，包括机器学习、深度学习、自然语言处理等");
        addEntity("小易助手", "产品", "智能助手系统，具备问答、搜索、文件处理等多种能力");
        addEntity("知识图谱", "技术", "知识表示和推理的技术，用于构建实体关系网络");
        addEntity("RAG", "技术", "检索增强生成，结合检索系统和生成模型的技术");
        
        addRelationship("小易助手", "人工智能", "应用");
        addRelationship("小易助手", "知识图谱", "使用");
        addRelationship("小易助手", "RAG", "基于");
        addRelationship("人工智能", "知识图谱", "包含");
    }

    public static class KnowledgeNode {
        public String id;
        public String type;
        public String description;
        public long createdAt;
        public long lastAccessedAt;
        public int accessCount;
    }

    public static class KnowledgeEdge {
        public String from;
        public String to;
        public String type;
        public double weight;
    }
}
