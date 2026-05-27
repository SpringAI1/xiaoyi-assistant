# 项目结构总览

```
enterprise-knowledge-system/
│
├── backend/                                    # Java 后端
│   ├── src/main/java/com/enterprise/knowledge/
│   │   │
│   │   ├── MainApplication.java               # 启动类
│   │   │
│   │   ├── config/                            # 配置类
│   │   │   └── LangChainConfig.java          # LangChain4j 配置
│   │   │
│   │   ├── domain/                            # 领域模型
│   │   │   ├── DocumentMetadata.java         # 文档元数据
│   │   │   ├── DocumentChunk.java            # 文档切片
│   │   │   ├── ChatMessage.java              # 对话消息
│   │   │   ├── ChatResponse.java             # 问答响应
│   │   │   ├── SearchResult.java             # 检索结果
│   │   │   └── QueryContext.java             # 查询上下文
│   │   │
│   │   ├── infrastructure/                    # 基础设施层
│   │   │   ├── document/
│   │   │   │   └── DocumentParser.java       # 文档解析器
│   │   │   ├── vector/
│   │   │   │   └── DocumentVectorStore.java  # 向量存储
│   │   │   ├── rag/
│   │   │   │   └── RagRetriever.java         # RAG 检索器
│   │   │   └── agent/
│   │   │       ├── IntentRecognizer.java     # 意图识别器
│   │   │       ├── TaskPlanner.java          # 任务规划器
│   │   │       ├── ToolExecutor.java         # 工具执行器
│   │   │       └── tool/
│   │   │           └── BuiltInTools.java     # 内置工具集
│   │   │
│   │   ├── application/service/               # 应用服务层
│   │   │   ├── RagService.java               # RAG 服务
│   │   │   ├── AgentService.java             # Agent 服务
│   │   │   └── DualEngineCoordinator.java    # 双引擎协调器
│   │   │
│   │   └── interface/rest/                    # REST API 接口
│   │       ├── KnowledgeController.java      # 主控制器
│   │       └── MessageDto.java               # 消息传输对象
│   │
│   ├── src/main/resources/
│   │   └── application.yml                    # 应用配置
│   │
│   ├── pom.xml                                # Maven 配置
│   └── mvnw                                   # Maven Wrapper
│
├── frontend/                                  # 前端界面
│   ├── index.html                             # 主页面
│   └── assets/
│       ├── css/
│       │   └── main.css                       # 样式文件
│       └── js/
│           └── main.js                        # Vue 应用逻辑
│
├── docs/                                      # 文档
│   ├── tech-architecture.md                   # 技术架构文档
│   └── ...
│
├── project-overview.md                        # 项目概述
├── README.md                                  # 项目说明
└── start.sh                                   # 启动脚本
```

## 关键文件说明

| 文件 | 说明 |
|------|------|
| `MainApplication.java` | Spring Boot 启动入口 |
| `LangChainConfig.java` | AI 模型和向量存储配置 |
| `RagService.java` | RAG 核心服务 - 处理文档和检索 |
| `AgentService.java` | Agent 核心服务 - 处理智能问答 |
| `DualEngineCoordinator.java` | 双引擎调度器 - 路由决策 |
| `KnowledgeController.java` | REST API 接口 |
| `index.html` | 前端聊天界面 |
