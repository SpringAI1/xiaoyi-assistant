# 企业知识问答系统 - Agent + RAG 双引擎架构

## 项目概述
这是一个基于 Java 的企业级 AI 知识问答系统，采用 **Agent（智能代理）** + **RAG（检索增强生成）** 双引擎架构。

## 技术栈
- **后端框架**: Spring Boot 3.x
- **AI/LLM**: LangChain4j + OpenAI API / Qwen API
- **向量数据库**: ChromaDB / 或 Elasticsearch + 向量化
- **文档处理**: Apache PDFBox, Apache Tika
- **前端**: Vue.js + Element Plus (可选)

## 核心模块

### 1. RAG Engine (检索增强生成引擎)
- **Document Parser**: 解析 PDF、Word、TXT 等文档
- **Vector Embedder**: 文本向量化
- **Vector Store**: 向量存储与相似度检索
- **Retriever**: 根据问题检索相关文档片段

### 2. Agent Engine (智能代理引擎)
- **Intent Recognizer**: 意图识别（事实查询、数据分析、创意写作等）
- **Task Planner**: 任务规划与分解
- **Tool Executor**: 工具调用（搜索、计算、数据库查询等）
- **Memory Manager**: 对话上下文管理

### 3. Dual-Engine Coordinator (双引擎协调器)
- 根据问题类型自动选择 RAG 或 Agent 模式
- 支持混合模式：RAG 提供知识 + Agent 进行推理

## 目录结构
```
enterprise-knowledge-system/
├── backend/                 # Java 后端
│   ├── src/main/java/com/enterprise/knowledge/
│   │   ├── application/     # 应用层服务
│   │   ├── domain/          # 领域模型
│   │   ├── infrastructure/  # 基础设施（向量库、文档解析）
│   │   └── interface/       # REST API 接口
│   ├── pom.xml
│   └── application.yml
├── frontend/                # 前端界面（可选）
└── docs/                    # 文档
```
