# 🚀 小易助手 - 企业级 Agent + RAG 双引擎智能问答系统

## 📋 项目简介

**小易助手**是一个生产级企业 AI 智能问答系统，采用 **Agent（智能代理）** + **RAG（检索增强生成）** 双引擎架构，内置企业级监控、对话记忆、知识库管理等功能，开箱即用。

### ✨ 核心特性

- ✅ **双引擎智能路由** - Agent + RAG 自动选择最优模式处理用户请求
- ✅ **增强Agent决策** - EnhancedAgentOrchestrator 实现智能路由、意图识别、多轮对话
- ✅ **增强RAG检索** - EnhancedRagEngine 实现混合检索、查询扩展、结果重排序
- ✅ **企业级监控** - SystemMonitor + ApiMonitoringAspect 实现调用追踪、性能监控、错误统计
- ✅ **技能系统** - 内置天气、搜索、翻译、文档生成、PPT制作等多种工具
- ✅ **数据持久化** - H2 Database + Spring Data JPA，应用重启不丢失数据
- ✅ **对话记忆** - 支持多轮对话上下文管理
- ✅ **分布式限流** - RateLimitService 实现 API 调用频率限制
- ✅ **异步处理** - AsyncDocumentService 处理文档上传
- ✅ **Docker部署** - 支持 Docker 容器化部署
- ✅ **前后端一体化** - 内置前端界面，开箱即用

## 🏗️ 技术架构

### 核心技术栈

| 技术类别 | 技术选型 |
|---------|---------|
| **后端框架** | Spring Boot 3.2.5 |
| **AI框架** | LangChain4j 0.34.0 |
| **数据库** | H2 Database (开发) / PostgreSQL + pgvector (生产) |
| **持久层** | Spring Data JPA |
| **前端** | 原生 JavaScript + CSS + HTML |
| **文档解析** | Apache PDFBox, Apache POI |
| **向量存储** | 内置 Embedding Store (支持扩展到 Elasticsearch/Pinecone) |
| **缓存** | 内存缓存 (支持扩展到 Redis) |
| **监控** | 自定义 AOP + SystemMonitor |
| **构建工具** | Maven |
| **JDK版本** | JDK 21 |

### 双引擎架构图

```
                    ┌─────────────────┐
                    │   用户请求      │
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │  Intent识别     │
                    │  & 路由决策     │
                    └────────┬────────┘
          ┌──────────────────┼──────────────────┐
          │                  │                  │
   ┌──────▼───────┐  ┌──────▼───────┐  ┌──────▼───────┐
   │  RAG引擎     │  │  Agent引擎   │  │  Hybrid混合  │
   │  (检索增强)  │  │  (智能代理)  │  │  (RAG+Agent) │
   └──────┬───────┘  └──────┬───────┘  └──────┬───────┘
          │                  │                  │
   ┌──────▼───────┐  ┌──────▼───────┐  ┌──────▼───────┐
   │ 文档检索     │  │ 工具调用     │  │ 综合处理     │
   │ 向量相似度   │  │ (天气/搜索)  │  │ 深度分析     │
   └──────┬───────┘  └──────┬───────┘  └──────┬───────┘
          │                  │                  │
          └──────────────────┼──────────────────┘
                             │
                    ┌────────▼────────┐
                    │  LLM生成回答     │
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │  返回用户结果    │
                    └─────────────────┘
```

## 🎯 主要功能模块

### 1. Enhanced Agent Orchestrator
- 智能意图识别（事实查询、工具调用、数据分析、创意写作等）
- 多轮对话上下文管理
- 音乐/天气等特殊场景优化
- 技能推荐与路由

### 2. Enhanced RAG Engine
- 混合检索（向量搜索 + 关键词匹配）
- 查询扩展（同义词、相关词扩展）
- 结果重排序（基于相关性和时效性）
- 文档分块与向量化

### 3. 企业级监控
- API 调用次数统计
- 响应时间监控
- 错误率追踪
- 系统状态 overview

### 4. 技能系统
- 🌤️ **天气查询** - 支持全国主要城市实时天气
- 🔍 **网络搜索** - 联网搜索最新信息
- 🌐 **网页抓取** - 网页内容提取和分析
-- 📊 **文档生成** - 生成 Word/PPT 文档

## 🚀 快速开始

### 前置要求
- JDK 21+
- Maven 3.6+

### AI API 配置

配置你的AI API Key（编辑 `backend/src/main/resources/application.yml`）:

```yaml
langchain:
  chat:
    provider: qwen  # 可选: qwen, openai, ollama
    qwen:
      api-key: ${QWEN_API_KEY:your-qwen-api-key}
```

### 启动服务

1. **启动后端**
```bash
cd backend
mvn clean compile spring-boot:run
```

2. **访问应用**
```
浏览器打开：http://localhost:8080
```

3. **其他接口**
- 健康检查：http://localhost:8080/api/v1/health
- 系统状态：http://localhost:8080/api/v1/system/status
- 前端界面：http://localhost:8080

## 📊 API 接口文档

### 聊天对话接口
```
POST /api/v1/chat
Content-Type: application/json

请求体:
{
  "query": "你的问题",
  "sessionId": "对话会话ID",
  "searchMode": "knowledge"  // knowledge | web | hybrid
}

响应:
{
  "content": "回答内容",
  "responseType": "RAG_BASED | DIRECT_ANSWER | HYBRID",
  "retrievalResults": [...],
  "processingTime": 1234
}
```

### 系统状态接口
```
GET /api/v1/system/status

响应:
{
  "healthy": true,
  "uptimeSeconds": 3600,
  "totalApiCalls": 1250,
  "errorRate": 0.5,
  "avgResponseTime": 800
}
```

### 技能管理接口
```
GET /api/v1/skills - 获取所有可用技能
```

## 📁 项目结构

```
JAVA/
├── backend/                          # 后端项目
│   ├── src/main/java/com/enterprise/knowledge/
│   │   ├── MainApplication.java      # 主入口
│   │   ├── api/rest/                 # REST API控制器
│   │   ├── application/service/      # 应用服务层
│   │   │   ├── AgentService.java     # Agent服务
│   │   │   ├── RagService.java       # RAG服务
│   │   │   └── DualEngineCoordinator.java  # 双引擎协调器
│   │   ├── domain/                   # 领域模型
│   │   ├── infrastructure/           # 基础设施
│   │   │   ├── agent/                # Agent模块
│   │   │   │   ├── EnhancedAgentOrchestrator.java
│   │   │   │   ├── IntentRecognizer.java
│   │   │   │   └── skill/            # 技能系统
│   │   │   ├── rag/                  # RAG模块
│   │   │   │   └── EnhancedRagEngine.java
│   │   │   ├── monitor/              # 监控模块
│   │   │   │   ├── SystemMonitor.java
│   │   │   │   └── ApiMonitoringAspect.java
│   │   │   └── document/             # 文档解析
│   │   ├── repository/               # 数据仓库
│   │   └── service/                  # 业务服务
│   └── src/main/resources/
│       └── application.yml           # 应用配置
├── docs/                             # 项目文档
├── frontend/                         # 前端（独立）
├── README.md                         # 项目介绍
└── docker-compose.yml                # Docker部署配置
```

## 🎯 应用场景

- 🏢 企业内部知识库问答
- 📚 文档管理与查询
- 🤖 智能客服助手
- 📊 数据分析与报告
- 📝 内容生成与创作
- 💼 业务流程自动化

## 🛠️ 扩展开发

### 添加新技能

创建类实现 `Skill` 接口：

```java
@Component
public class MySkill implements Skill {
    @Override
    public String getId() { return "my-skill"; }
    
    @Override
    public String getName() { return "我的技能"; }
    
    @Override
    public String execute(String input) {
        // 技能逻辑
        return "结果";
    }
}
```

### 添加新监控指标

在 `SystemMonitor` 中添加自定义指标：

```java
public void recordCustomMetric(String name, long value) {
    // 记录指标
}
```

## 🐳 Docker部署

```bash
# 构建镜像
docker build -t xiaoyi-assistant .

# 启动容器
docker run -p 8080:8080 -e QWEN_API_KEY=your-key xiaoyi-assistant
```

或者使用 Docker Compose：

```bash
docker-compose up -d
```

## 📄 License

MIT License

## 🤝 贡献

欢迎提交 Issue 和 PR！

---

**项目名称**: 小易助手 (XiaoYi Assistant)
**版本**: 2.0.0
**更新日期**: 2026-05-28
