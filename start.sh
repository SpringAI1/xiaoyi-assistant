#!/bin/bash

# 企业知识问答系统 - 启动脚本

echo "========================================"
echo "   企业知识问答系统 (Agent + RAG)"
echo "========================================"
echo ""

# 检查 Java 版本
java_version=$(java -version 2>&1 | head -n 1)
echo "Java: $java_version"

# 切换到 backend 目录
cd "$(dirname "$0")/backend"

# 编译并运行
echo ""
echo "正在启动后端服务..."
echo ""

mvn spring-boot:run -Dspring-boot.run.profiles=default
