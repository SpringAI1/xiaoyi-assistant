package com.enterprise.knowledge.infrastructure.agent.skill;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class CalculatorSkill implements Skill {
    
    @Override
    public String getId() {
        return "calculator-skill";
    }
    
    @Override
    public String getName() {
        return "智能计算器";
    }
    
    @Override
    public String getDescription() {
        return "进行数学计算，支持加减乘除、平方、开方等";
    }
    
    @Override
    public List<String> getKeywords() {
        return Arrays.asList("计算", "算一下", "等于", "加", "减", "乘", "除", "平方", "开方", "math");
    }
    
    @Override
    public int getPriority() {
        return 8; // 高优先级
    }
    
    @Override
    public String execute(String input) {
        try {
            String expression = extractExpression(input);
            if (expression == null || expression.isEmpty()) {
                return "请提供需要计算的表达式，例如：'计算 100 + 200' 或 '2的平方'";
            }
            
            double result = evaluateExpression(expression);
            
            StringBuilder sb = new StringBuilder();
            sb.append("🧮 计算结果\n\n");
            sb.append("表达式：").append(expression).append("\n");
            sb.append("结果：").append(result).append("\n\n");
            sb.append("💡 提示：我支持加减乘除、平方(^)、开方(sqrt)等运算");
            
            return sb.toString();
            
        } catch (Exception e) {
            return "计算出错：" + e.getMessage() + "\n请尝试简单的表达式，如：100+200";
        }
    }
    
    private String extractExpression(String input) {
        String[] keywords = {"计算", "算一下", "等于", "计算一下"};
        for (String keyword : keywords) {
            if (input.contains(keyword)) {
                int idx = input.indexOf(keyword);
                return input.substring(idx + keyword.length()).trim();
            }
        }
        return input.trim();
    }
    
    private double evaluateExpression(String expr) {
        expr = expr.replaceAll("[^0-9+\\-*/().^sqrt]", "").trim();
        
        if (expr.contains("sqrt")) {
            String num = expr.replace("sqrt(", "").replace(")", "");
            return Math.sqrt(Double.parseDouble(num));
        }
        
        if (expr.contains("^")) {
            String[] parts = expr.split("\\^");
            return Math.pow(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]));
        }
        
        if (expr.contains("+")) {
            String[] parts = expr.split("\\+");
            return Double.parseDouble(parts[0]) + Double.parseDouble(parts[1]);
        }
        
        if (expr.contains("-")) {
            String[] parts = expr.split("-");
            return Double.parseDouble(parts[0]) - Double.parseDouble(parts[1]);
        }
        
        if (expr.contains("*")) {
            String[] parts = expr.split("\\*");
            return Double.parseDouble(parts[0]) * Double.parseDouble(parts[1]);
        }
        
        if (expr.contains("/")) {
            String[] parts = expr.split("/");
            return Double.parseDouble(parts[0]) / Double.parseDouble(parts[1]);
        }
        
        return Double.parseDouble(expr);
    }
}
