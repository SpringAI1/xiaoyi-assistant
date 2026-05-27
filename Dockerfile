# 使用 JDK 21 基础镜像
FROM eclipse-temurin:21-jdk-alpine

# 设置工作目录
WORKDIR /app

# 复制 application 包
COPY backend/target/enterprise-knowledge-system-1.0.0-SNAPSHOT.jar app.jar

# 创建数据目录
RUN mkdir -p /app/data

# 设置 JVM 参数
ENV JAVA_OPTS="-Xms512m -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# 设置 AI API Key 环境变量（可以在运行时覆盖）
ENV QWEN_API_KEY=""

# 暴露端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/v1/health || exit 1

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
