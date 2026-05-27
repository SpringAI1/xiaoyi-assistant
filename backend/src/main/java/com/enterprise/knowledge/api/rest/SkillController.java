package com.enterprise.knowledge.api.rest;

import com.enterprise.knowledge.infrastructure.agent.skill.Skill;
import com.enterprise.knowledge.infrastructure.agent.skill.SkillRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/skills")
public class SkillController {

    private final SkillRegistry skillRegistry;

    public SkillController(SkillRegistry skillRegistry) {
        this.skillRegistry = skillRegistry;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllSkills() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Skill> skills = skillRegistry.getAllSkills();
            
            List<Map<String, Object>> skillList = skills.stream()
                .map(skill -> {
                    Map<String, Object> skillMap = new HashMap<>();
                    skillMap.put("id", skill.getId());
                    skillMap.put("name", skill.getName());
                    skillMap.put("description", skill.getDescription());
                    skillMap.put("keywords", skill.getKeywords());
                    skillMap.put("priority", skill.getPriority());
                    skillMap.put("enabled", skill.isEnabled());
                    return skillMap;
                })
                .collect(Collectors.toList());

            response.put("status", "success");
            response.put("skills", skillList);
            response.put("total", skillList.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "获取技能列表失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/recommend")
    public ResponseEntity<Map<String, Object>> getRecommendSkills(
            @RequestParam(value = "query", required = false) String query) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Skill> skills = skillRegistry.getRecommendSkills(query);
            
            List<Map<String, Object>> skillList = skills.stream()
                .map(skill -> {
                    Map<String, Object> skillMap = new HashMap<>();
                    skillMap.put("id", skill.getId());
                    skillMap.put("name", skill.getName());
                    skillMap.put("description", skill.getDescription());
                    skillMap.put("keywords", skill.getKeywords());
                    skillMap.put("priority", skill.getPriority());
                    skillMap.put("enabled", skill.isEnabled());
                    return skillMap;
                })
                .collect(Collectors.toList());

            response.put("status", "success");
            response.put("skills", skillList);
            response.put("query", query);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "获取推荐技能失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/{skillId}/execute")
    public ResponseEntity<Map<String, Object>> executeSkill(
            @PathVariable String skillId,
            @RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String input = request.get("input");
            if (input == null) {
                input = request.getOrDefault("query", "");
            }
            
            Skill skill = skillRegistry.getSkill(skillId);
            if (skill == null) {
                response.put("status", "error");
                response.put("message", "技能不存在: " + skillId);
                return ResponseEntity.badRequest().body(response);
            }
            
            if (!skill.isEnabled()) {
                response.put("status", "error");
                response.put("message", "技能已禁用: " + skillId);
                return ResponseEntity.badRequest().body(response);
            }

            String result = skill.execute(input);
            
            response.put("status", "success");
            response.put("skillId", skillId);
            response.put("skillName", skill.getName());
            response.put("result", result);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "执行技能失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/{skillId}/toggle")
    public ResponseEntity<Map<String, Object>> toggleSkill(
            @PathVariable String skillId,
            @RequestParam(defaultValue = "true") boolean enabled) {
        Map<String, Object> response = new HashMap<>();
        try {
            Skill skill = skillRegistry.getSkill(skillId);
            if (skill == null) {
                response.put("status", "error");
                response.put("message", "技能不存在: " + skillId);
                return ResponseEntity.badRequest().body(response);
            }

            response.put("status", "success");
            response.put("skillId", skillId);
            response.put("enabled", enabled);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "切换技能状态失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
