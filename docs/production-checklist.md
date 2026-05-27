# 生产环境优化 checklist

## 🔴 P0 - 必须立即修复（阻止上线）

### 1. 数据持久化层实现

#### 1.1 数据库选型
```
推荐方案：PostgreSQL + pgvector 扩展
替代方案：Elasticsearch (已引入但配置简单)
不推荐：MySQL(8.0+ 支持 JSON 但向量检索弱)
```

#### 1.2 需要创建的表

```sql
-- 文档元数据表
CREATE TABLE documents (
    id VARCHAR(64) PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    file_name VARCHAR(500) NOT NULL,
    file_type VARCHAR(20) NOT NULL,
    file_size BIGINT,
    file_path VARCHAR(1000),
    status VARCHAR(20) DEFAULT 'processing', -- processing/done/failed
    version INT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    uploaded_by VARCHAR(100),
    metadata JSONB
);

-- 文档切片表
CREATE TABLE document_chunks (
    id VARCHAR(128) PRIMARY KEY,
    document_id VARCHAR(64) NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    chunk_number INT NOT NULL,
    content TEXT NOT NULL,
    embedding VECTOR(384), -- pgvector 向量字段
    word_count INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(document_id, chunk_number)
);

-- 创建向量相似度索引
CREATE INDEX ON document_chunks USING ivfflat (embedding vector_cosine_ops) 
    WITH (lists = 100);

-- 用户权限表（如果需要）
CREATE TABLE users (
    id VARCHAR(64) PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) DEFAULT 'user', -- admin/user/viewer
    email VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP
);

-- 文档权限表
CREATE TABLE document_permissions (
    id VARCHAR(64) PRIMARY KEY,
    document_id VARCHAR(64) REFERENCES documents(id) ON DELETE CASCADE,
    user_id VARCHAR(64) REFERENCES users(id),
    permission_level VARCHAR(20), -- read/write/admin
    UNIQUE(document_id, user_id)
);
```

#### 1.3 实体类改造

```java
// Document.java
@Entity
@Table(name = "documents")
public class Document {
    @Id
    private String id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(name = "file_name", nullable = false)
    private String fileName;
    
    @Column(name = "file_type", nullable = false)
    private String fileType;
    
    @Column(name = "file_size")
    private Long fileSize;
    
    @Column(name = "file_path")
    private String filePath;
    
    @Enumerated(EnumType.STRING)
    private DocumentStatus status;
    
    private Integer version;
    
    @Column(name = "uploaded_by")
    private String uploadedBy;
    
    @ElementCollection
    @MapKeyColumn(name = "key")
    @Column(name = "value")
    @CollectionTable(name = "document_metadata")
    private Map<String, String> metadata = new HashMap<>();
    
    // 关联查询
    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL)
    private List<DocumentChunk> chunks = new ArrayList<>();
}
```

### 2. 向量存储生产级方案

#### 2.1 Elasticsearch 配置（推荐）

```yaml
# application-prod.yml
spring:
  elasticsearch:
    uris: ${ES_URL:https://localhost:9200}
    username: ${ES_USERNAME:elastic}
    password: ${ES_PASSWORD}
    ssl:
      verification-mode: none # 内部网络可关闭
    
kai:
  vector-store:
    type: elasticsearch
    cluster-name: elastic-vector-search
    node-attributes: data_only: true
    
kai-es:
  index:
    mappings:
      embeddings:
        dimensions: 384
        similarity: cosine
```

#### 2.2 向量存储配置类

```java
@Configuration
@ConditionalOnProperty(name = "langchain.vector-store.type", havingValue = "elasticsearch")
public class ElasticsearchVectorConfig {
    
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore(
            RestClient restClient,
            EmbeddingModel embeddingModel) {
        return ElasticsearchEmbeddingStore.builder()
                .restClient(restClient)
                .indexName("knowledge-chunks")
                .dimensions(384)
                .similarity(CosineSimilarity)
                .build();
    }
}
```

### 3. 异步文档处理

```java
@Service
@Transactional
public class AsyncDocumentProcessor {
    
    private final ThreadPoolTaskExecutor taskExecutor;
    private final DocumentRepository documentRepository;
    
    @Async("documentProcessingExecutor")
    public CompletableFuture<DocumentMetadata> processDocument(
            MultipartFile file, String userId) {
        
        try {
            // 创建文档记录
            Document doc = createDocumentRecord(file, userId);
            
            // 解析内容
            String content = parseFileContent(file);
            
            // 分块处理
            List<String> chunks = splitIntoChunks(content);
            
            // 向量化并保存（批量处理）
            batchVectorizeAndSave(doc.getId(), chunks);
            
            // 更新状态
            doc.setStatus(DocumentStatus.DONE);
            
            return CompletableFuture.completedFuture(toMetadata(doc));
            
        } catch (Exception e) {
            log.error("文档处理失败", e);
            throw new DocumentProcessingException(e);
        }
    }
}

// 线程池配置
@Configuration
public class ThreadPoolConfig {
    
    @Bean("documentProcessingExecutor")
    public ThreadPoolTaskExecutor documentProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("doc-processor-");
        executor.setRejectedExecutionHandler(new CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
```

### 4. 缓存层设计

```java
@Service
public class CachedQueryService {
    
    private final CacheManager cacheManager;
    private final RagService ragService;
    
    @Cacheable(value = "answers", key = "#query + '#tenantId'", unless = "#result == null")
    public ChatResponse getAnswerWithCache(String query, String tenantId) {
        // 生成查询哈希作为缓存键
        String queryHash = DigestUtils.sha256Hex(query);
        return ragService.answer(query);
    }
    
    @CacheEvict(value = "answers", key = "#query + '#tenantId'")
    public void evictAnswerCache(String query, String tenantId) {
        // 删除相关缓存
    }
}
```

### 5. 统一的异常处理

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(DocumentProcessingException.class)
    public ResponseEntity<ErrorResponse> handleDocumentError(
            DocumentProcessingException ex) {
        return ResponseEntity.badRequest()
            .body(ErrorResponse.of(ex.getCode(), ex.getMessage()));
    }
    
    @ExceptionHandler(AIModelException.class)
    public ResponseEntity<ErrorResponse> handleAIError(AIModelException ex) {
        log.warn("AI 模型调用失败", ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(ErrorResponse.of(ex.getCode(), "AI 服务暂时不可用"));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericError(Exception ex) {
        log.error("系统异常", ex);
        return ResponseEntity.internalServerError()
            .body(ErrorResponse.of("SYS_ERROR", "系统内部错误"));
    }
}

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponse {
    private String code;
    private String message;
    private long timestamp;
    private String path;
}
```

### 6. 限流和防护

```java
@Configuration
@EnableRateLimiter
public class RateLimitConfig {
    
    @Bean
    public KeyResolver apiKeyResolver() {
        return exchange -> Mono.just(
            extractApiKey(exchange.getRequest())
        );
    }
    
    @Bean
    public RedisRateLimiter redisRateLimiter() {
        // 默认限流：每秒 10 次请求
        return new RedisRateLimiter(10, 1, TimeUnit.SECONDS);
    }
}
```

---

## 🟡 P1 - 强烈建议（影响用户体验）

### 7. 文档类型支持增强
- [ ] OCR 识别（PDF 图片扫描件）
- [ ] Excel 表格结构化提取
- [ ] 图片/图表内容分析（多模态模型）

### 8. 知识库管理功能
- [ ] 文档版本控制
- [ ] 批量操作（导入/导出/删除）
- [ ] 全文搜索能力
- [ ] 标签分类系统

### 9. 对话历史与上下文
- [ ] 会话持久化
- [ ] 多轮对话记忆
- [ ] 会话导出/分享功能

### 10. 前端优化
- [ ] Markdown 渲染优化
- [ ] 引用来源展示
- [ ] 搜索结果高亮
- [ ] WebSocket 实时通知

---

## 🟢 P2 - 锦上添花（提升品质）

### 11. 监控和可观测性
- [ ] Prometheus + Grafana 监控面板
- [ ] 链路追踪 (Jaeger/Zipkin)
- [ ] 业务指标统计
- [ ] 性能分析报告

### 12. 测试覆盖
- [ ] 单元测试覆盖率 > 80%
- [ ] 集成测试
- [ ] 压力测试脚本
- [ ] 自动化测试流水线

### 13. 运维工具
- [ ] 健康检查端点完善
- [ ] 配置热更新
- [ ] 优雅停机处理
- [ ] 备份恢复脚本

### 14. 国际化支持
- [ ] i18n 配置
- [ ] 多语言模型支持

---

## 📊 生产部署架构建议

```
                    ┌─────────────┐
                    │   Nginx     │
                    │  (负载均衡)  │
                    └──────┬──────┘
                           │
           ┌───────────────┼───────────────┐
           │               │               │
    ┌──────▼──────┐ ┌──────▼──────┐ ┌──────▼──────┐
    │   Service A │ │   Service B │ │   Service C │
    │    Pod 1    │ │    Pod 2    │ │    Pod 3    │
    └──────┬──────┘ └──────┬──────┘ └──────┬──────┘
           │               │               │
           └───────────────┴───────┬───────┘
                                   │
         ┌─────────────────────────┼─────────────────────────┐
         │                         │                         │
   ┌─────▼─────┐           ┌──────▼──────┐          ┌──────▼──────┐
   │ PostgreSQL│           │  Redis Cache│          │  Object Store│
   │ + pgvector│           │             │          │ (S3/MinIO)   │
   └───────────┘           └─────────────┘          └──────────────┘
```
