package com.enterprise.knowledge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 小易助手 - 启动类
 * Agent + RAG 双引擎架构
 */
@SpringBootApplication
public class MainApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(MainApplication.class, args);

        System.out.println("\n");
        System.out.println("╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║   小易助手 (Agent + RAG 双引擎)                             ║");
        System.out.println("╚═══════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("应用已启动！访问地址：");
        System.out.println("  - 前端界面：http://localhost:8080");
        System.out.println("  - API 健康检查：http://localhost:8080/api/v1/health");
        System.out.println("  - H2 数据库控制台：http://localhost:8080/h2-console");
        System.out.println();
        System.out.println("请按 Ctrl+C 停止服务");
        System.out.println();
    }

    /**
     * 配置 CORS（跨域支持）
     */
    WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("http://localhost:3000", "http://localhost:8080", "*")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}
