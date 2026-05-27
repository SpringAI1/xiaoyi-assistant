package com.enterprise.knowledge.infrastructure.agent.skill;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class CodeGenerationSkill implements Skill {
    
    @Override
    public String getId() {
        return "code-generation-skill";
    }
    
    @Override
    public String getName() {
        return "代码生成器";
    }
    
    @Override
    public String getDescription() {
        return "生成简单的代码片段，支持基础代码示例";
    }
    
    @Override
    public List<String> getKeywords() {
        return Arrays.asList("代码", "code", "生成代码", "写代码", "编程", "程序", "function");
    }
    
    @Override
    public int getPriority() {
        return 7;
    }
    
    @Override
    public String execute(String input) {
        try {
            String language = detectLanguage(input);
            String code = generateCode(input, language);
            
            StringBuilder sb = new StringBuilder();
            sb.append("💻 代码生成\n\n");
            sb.append("语言：").append(language).append("\n\n");
            sb.append("```").append(language).append("\n");
            sb.append(code).append("\n");
            sb.append("```\n\n");
            sb.append("💡 提示：这是基础示例代码，可根据需求调整");
            
            return sb.toString();
            
        } catch (Exception e) {
            return "代码生成出错：" + e.getMessage();
        }
    }
    
    private String detectLanguage(String input) {
        if (input.contains("java") || input.contains("Java")) return "java";
        if (input.contains("python") || input.contains("Python")) return "python";
        if (input.contains("javascript") || input.contains("js")) return "javascript";
        if (input.contains("html")) return "html";
        if (input.contains("css")) return "css";
        return "java";
    }
    
    private String generateCode(String input, String language) {
        String lowerInput = input.toLowerCase();
        
        if (lowerInput.contains("hello") || lowerInput.contains("你好")) {
            return generateHelloWorld(language);
        }
        
        if (lowerInput.contains("排序") || lowerInput.contains("sort")) {
            return generateSortCode(language);
        }
        
        if (lowerInput.contains("循环") || lowerInput.contains("loop")) {
            return generateLoopCode(language);
        }
        
        if (lowerInput.contains("函数") || lowerInput.contains("function")) {
            return generateFunctionCode(language);
        }
        
        return generateHelloWorld(language);
    }
    
    private String generateHelloWorld(String language) {
        switch (language) {
            case "java":
                return "public class HelloWorld {\n    public static void main(String[] args) {\n        System.out.println(\"Hello, World!\");\n    }\n}";
            case "python":
                return "print(\"Hello, World!\")";
            case "javascript":
                return "console.log(\"Hello, World!\");";
            case "html":
                return "<!DOCTYPE html>\n<html>\n<body>\n    <h1>Hello, World!</h1>\n</body>\n</html>";
            default:
                return "echo \"Hello, World!\"";
        }
    }
    
    private String generateSortCode(String language) {
        switch (language) {
            case "java":
                return "import java.util.Arrays;\n\npublic class SortExample {\n    public static void main(String[] args) {\n        int[] arr = {5, 2, 8, 1, 9};\n        Arrays.sort(arr);\n        System.out.println(Arrays.toString(arr));\n    }\n}";
            case "python":
                return "arr = [5, 2, 8, 1, 9]\narr.sort()\nprint(arr)";
            default:
                return generateHelloWorld(language);
        }
    }
    
    private String generateLoopCode(String language) {
        switch (language) {
            case "java":
                return "for (int i = 1; i <= 10; i++) {\n    System.out.println(\"Number: \" + i);\n}";
            case "python":
                return "for i in range(1, 11):\n    print(f\"Number: {i}\")";
            default:
                return generateHelloWorld(language);
        }
    }
    
    private String generateFunctionCode(String language) {
        switch (language) {
            case "java":
                return "public int add(int a, int b) {\n    return a + b;\n}\n\n// 使用\nint result = add(5, 3);";
            case "python":
                return "def add(a, b):\n    return a + b\n\n# 使用\nresult = add(5, 3)";
            default:
                return generateHelloWorld(language);
        }
    }
}
