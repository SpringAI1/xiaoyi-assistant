# 技术文档 - Agent + RAG 双引擎架构详解

## 一、系统架构

### 1.1 架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                           前端 (Vue.js)                          │
│                         /api/v1/chat                             │
└───────────────────────────┬─────────────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────────────┐
│                     REST API Layer                               │
│                   KnowledgeController                            │
└───────────────────────────┬─────────────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────────────┐
│                 DualEngineCoordinator (协调器)                   │
│                    意图识别 + 路由决策                            │
├─────────────────────────┬─────────────────────┬─────────────────┤
│                         │                     │                 │
┌────────▼────────┐  ┌─────▼─────┐    ┌────────▼────────┐        │
│  RagService     │  │AgentService│    │IntentRecognizer │        │
│  (RAG 引擎服务)   │  │(Agent 服务) │    │  (意图识别器)   │        │
└────────┬────────┘  └─────┬─────┘    └─────────────────┘        │
         │                 │                                      │
    ┌────▼────┐      ┌─────▼────┐                                │
    │Document │      │TaskPlanner│                               │
    │VectorStore│    │ToolExecutor│                              │
    └──────────┘      └──────────┘                                │
```

### 1.2 核心组件说明

| 组件 | 职责 |
|------|------|
| DualEngineCoordinator | 双引擎调度器，根据问题类型选择最优处理路径 |
| RagService | RAG 服务，处理文档索引和基于知识的问答 |
| AgentService | Agent 服务，处理工具调用和任务规划 |
| IntentRecognizer | 意图识别器，分析用户问题的语义类型 |
| DocumentVectorStore | 向量存储，保存文档的向量化表示 |
| TaskPlanner | 任务规划器，分解复杂任务并执行多步操作 |
| ToolExecutor | 工具执行器，调用内置工具完成具体任务 |

## 二、RAG 引擎实现

### 2.1 文档处理流程

```
上传文档
    │
    ▼
┌───────────────┐
│  文档解析      │ ← PDFBox, Tika, POI
└───────┬───────┘
        │
        ▼
┌───────────────┐
│ 文本分块       │ ← 按固定大小分割，保留上下文重叠
└───────┬───────┘
        │
        ▼
┌───────────────┐
│  向量化       │ ← Embedding Model
└───────┬───────┘
        │
        ▼
┌───────────────┐
│  向量存储      │ ← Elasticsearch/Pinecone/In-Memory
└───────────────┘
```

### 2.2 检索流程

```
用户提问
    │
    ▼
┌───────────────┐
│ 问题向量化     │
└───────┬───────┘
        │
        ▼
┌───────────────┐
│ 相似度检索     │ ← Cosine Similarity
│ (Top-K)       │
└───────┬───────┘
        │
        ▼
┌───────────────┐
│ 阈值过滤       │ ← 低于阈值的结果被丢弃
└───────┬───────┘
        │
        ▼
┌───────────────┐
│ 构建 RAG Prompt│
│ + LLM 生成     │
└───────────────┘
```

## 三、Agent 引擎实现

### 3.1 意图识别策略

| 意图类型 | 特征关键词 | 处理模式 |
|---------|-----------|---------|
| FACT_QUERY | 政策，规定，制度，流程，员工，公司 | RAG |
| TOOL_REQUIRED | 计算，搜索，查询，获取 | Agent+Tools |
| DATA_ANALYSIS | 分析，统计，数据，趋势，对比 | Agent+Tools |
| CREATIVE_WRITING | 写，创作，设计，构思 | Hybrid |
| GENERAL_CHAT | - | Agent |

### 3.2 工具定义

```java
@Tool("描述")
public String toolName(@P("参数描述") ArgType param) {
    // 工具实现
}
```

可用工具：
- `searchKnowledgeBase`: 搜索知识库
- `calculate`: 数学计算
- `getWeather`: 天气查询
- `getCurrentTime`: 时间获取
- `fetchUrlContent`: URL 内容获取

### 3.3 多轮工具调用

```
User Input → LLM → Detect Tools → Execute Tools → 
    ↑                                     ↓
    └──────────── Return Results ◄────────┘
                        │
                        ▼
                  Final Answer
```

## 四、配置文件

### application.yml

```yaml
knowledge:
  rag:
    chunk-size: 500           # 分块大小
    chunk-overlap: 100        # 重叠大小
    similarity-threshold: 0.75 # 相似度阈值
    max-results: 5            # 最大返回数

  agent:
    max-tool-calls: 5         # 最大工具调用次数
    response-timeout: 30      # 超时时间（秒）

  vector:
    dimensions: 384           # 向量维度
    similarity-metric: COS    # 相似性度量
```

## 五、扩展指南

### 添加新的文档格式

在 `DocumentParser.java` 中添加解析逻辑：

```java
case "docx":
    return parseDocx(filePath);
```

### 添加新工具

在 `BuiltInTools.java` 中添加：

```java
@Tool("发送内部通知")
public String sendNotification(@P("接收人") String recipient,
                               @P("消息内容") String message) {
    // 实现通知逻辑
}
```

### 自定义向量存储

实现 `EmbeddingStore<TextSegment>` 接口或使用 LangChain4j 提供的存储实现。
