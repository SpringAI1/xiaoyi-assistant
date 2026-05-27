# 配置指南 - 企业知识问答系统

## 快速配置（5 分钟启动）

### 方式一：使用 Qwen（通义千问）推荐

1. **获取 API Key**
   - 访问 [阿里云百炼](https://bailian.console.aliyun.com/)
   - 创建 API Key

2. **配置环境变量**
```bash
export QWEN_API_KEY="sk-xxxxxxxxxxxxxxxx"
```

3. **修改配置文件** (application.yml)
```yaml
langchain:
  chat:
    provider: qwen
  embeddings:
    provider: local
```

4. **启动服务**
```bash
cd backend
mvn spring-boot:run
```

---

### 方式二：使用 OpenAI

```bash
export OPENAI_API_KEY="sk-xxxxxxxxxxxxxxxx"
```

```yaml
langchain:
  chat:
    provider: openai
  embeddings:
    provider: openai
```

---

### 方式三：本地部署（免费，需 GPU）

#### 1. 安装 Ollama
```bash
# macOS
brew install ollama

# Linux
curl -fsSL https://ollama.com/install.sh | sh
```

#### 2. 下载模型
```bash
# LLM 模型
ollama pull llama3.2

# Embedding 模型
ollama pull nomic-embed-text
```

#### 3. 启动 Ollama
```bash
ollama serve
```

#### 4. 配置文件
```yaml
langchain:
  chat:
    provider: ollama
  embeddings:
    provider: local
```

---

## 详细配置项

### AI 模型配置

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `langchain.chat.provider` | qwen | 聊天模型提供商 (qwen/openai/ollama) |
| `langchain.qwen.api-key` | - | 通义千问 API Key |
| `langchain.openai.api-key` | - | OpenAI API Key |
| `langchain.ollama.base-url` | localhost:11434 | Ollama 服务地址 |
| `langchain.ollama.model-name` | llama3.2 | Ollama 模型名称 |
| `langchain.embeddings.provider` | local | 嵌入模型提供商 (local/openai) |

### RAG 引擎配置

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `knowledge.rag.chunk-size` | 500 | 文档分块大小（字符数） |
| `knowledge.rag.chunk-overlap` | 100 | 块之间重叠部分 |
| `knowledge.rag.similarity-threshold` | 0.75 | 相似度阈值（0-1） |
| `knowledge.rag.max-results` | 5 | 每次检索返回的最大结果数 |

### Agent 引擎配置

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `knowledge.agent.max-tool-calls` | 5 | 最大工具调用次数 |
| `knowledge.agent.response-timeout` | 30 | 响应超时时间（秒） |

### 向量存储配置

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `knowledge.vector.dimensions` | 384 | 向量维度 |
| `knowledge.vector.similarity-metric` | COS | 相似性度量 (COS/EUCLIDEAN/DOT_PRODUCT) |

### Elasticsearch 配置（生产环境推荐）

```yaml
spring:
  elasticsearch:
    uris: http://localhost:9200
    username: elastic
    password: admin
```

### Redis 配置（缓存推荐）

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    password:
    database: 0
```

---

## 环境变量参考

```bash
# AI API Keys
QWEN_API_KEY=sk-your-qwen-api-key
OPENAI_API_KEY=sk-your-openai-api-key

# Server
SERVER_PORT=8080

# Database
ELASTICSEARCH_URI=http://localhost:9200
REDIS_HOST=localhost
REDIS_PORT=6379
```

---

## Docker 启动（生产环境）

```bash
# 构建镜像
docker build -t enterprise-knowledge-system .

# 运行容器
docker run -d \
  -p 8080:8080 \
  -e QWEN_API_KEY=your-key \
  enterprise-knowledge-system
```

---

## 常见问题

### Q: 如何切换不同的 AI 模型？
A: 修改 `application.yml` 中的 `langchain.chat.provider` 字段

### Q: 如何提高检索准确度？
A: 调整以下参数：
- 减小 `chunk-size` 到 300
- 增大 `similarity-threshold` 到 0.85
- 增加 `max-results` 到 10

### Q: 如何启用安全认证？
A: Spring Security 已内置，可在 `SecurityConfig.java` 中自定义规则

---

## 性能调优

### 对于大知识库
1. 使用 Elasticsearch 替代 InMemoryEmbeddingStore
2. 增加 JVM 堆内存 `-Xmx4g`
3. 启用 Redis 缓存常用查询

### 对于高并发场景
1. 添加 CDN 加速静态资源
2. 使用负载均衡器
3. 启用数据库连接池优化
