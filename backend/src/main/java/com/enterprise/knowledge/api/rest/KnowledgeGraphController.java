package com.enterprise.knowledge.api.rest;

import com.enterprise.knowledge.infrastructure.knowledgegraph.KnowledgeGraph;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/knowledge-graph")
public class KnowledgeGraphController {

    private final KnowledgeGraph knowledgeGraph;

    public KnowledgeGraphController(KnowledgeGraph knowledgeGraph) {
        this.knowledgeGraph = knowledgeGraph;
    }

    @GetMapping("/search")
    public Map<String, Object> search(@RequestParam String query) {
        List<KnowledgeGraph.KnowledgeNode> results = knowledgeGraph.search(query);
        
        Map<String, Object> response = new HashMap<>();
        response.put("query", query);
        response.put("resultCount", results.size());
        
        if (!results.isEmpty()) {
            KnowledgeGraph.KnowledgeNode topResult = results.get(0);
            response.put("topResult", Map.of(
                    "id", topResult.id,
                    "type", topResult.type,
                    "description", topResult.description
            ));
        }
        
        return response;
    }

    @GetMapping("/insights")
    public String getInsights(@RequestParam String domain) {
        return knowledgeGraph.generateInsights(domain);
    }

    @GetMapping("/statistics")
    public Map<String, Object> getStatistics() {
        return knowledgeGraph.getStatistics();
    }

    @PostMapping("/entity")
    public Map<String, Object> addEntity(@RequestParam String entity,
                                         @RequestParam String type,
                                         @RequestParam String description) {
        knowledgeGraph.addEntity(entity, type, description);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "实体已添加: " + entity);
        return response;
    }
}
