# 企业知识问答系统 - 生产部署手册

## 📋 目录

1. [架构概览](#架构概览)
2. [前置准备](#前置准备)
3. [快速部署](#快速部署)
4. [配置详解](#配置详解)
5. [性能调优](#性能调优)
6. [监控告警](#监控告警)
7. [日常运维](#日常运维)
8. [故障排查](#故障排查)

---

## 架构概览

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   Nginx      │     │    App       │     │    Vue.js    │
│ (负载均衡)   │────▶│ (Spring Boot)│◀────│  (前端)      │
└──────────────┘     └──────┬───────┘     └──────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        ▼                   ▼                   ▼
   ┌──────────┐      ┌──────────────┐    ┌──────────┐
   │ Postgres │      │    Redis     │    │   AI     │
   │ +pgvector│      │   (缓存)     │    │  Provider│
   └──────────┘      └──────────────┘    └──────────┘
```

### 组件说明

| 组件 | 用途 | 推荐配置 |
|------|------|---------|
| Nginx | API 网关/负载均衡 | 1 CPU, 2GB RAM |
| Spring Boot | 应用服务 | 2-4 CPU, 4-8GB RAM |
| PostgreSQL | 数据库 + 向量存储 | 4 CPU, 8GB+ RAM |
| Redis | 缓存 + 分布式锁 | 2 CPU, 4GB RAM |
| AI Provider | LLM 推理服务 | 按供应商计费 |

---

## 前置准备

### 1. 系统要求

- **操作系统**: Linux (Ubuntu 20.04+, CentOS 8+)
- **内存**: 至少 8GB (推荐 16GB+)
- **磁盘**: 至少 50GB SSD
- **JDK**: 17+ (Docker 镜像已内置)

### 2. AI API Key 获取

#### Qwen (通义千问) - 推荐
1. 访问 https://bailian.console.aliyun.com/
2. 创建 API Key
3. 复制 key 到 `.env` 文件

#### OpenAI
1. 访问 https://platform.openai.com/
2. 创建 API Key
3. 复制 key 到 `.env` 文件

#### Ollama (本地免费)
```bash
# macOS
brew install ollama
ollama pull llama3.2

# Linux
curl -fsSL https://ollama.com/install.sh | sh
ollama pull llama3.2
```

---

## 快速部署

### Docker Compose 方式（推荐）

```bash
# 1. 克隆项目
cd /path/to/project

# 2. 准备环境变量
cp .env.example .env
vim .env  # 编辑配置

# 3. 启动所有服务
docker-compose up -d

# 4. 查看日志
docker-compose logs -f app

# 等待服务就绪（约 2-3 分钟）
# 健康检查通过后即可使用
```

### 单机安装方式

```bash
# 1. 安装依赖
sudo apt-get update
sudo apt-get install -y openjdk-17-jdk postgresql redis-server maven

# 2. 初始化数据库
psql -U postgres -c "CREATE EXTENSION vector;"
psql -U postgres -c "CREATE DATABASE knowledge_db;"
./mvnw flyway:migrate

# 3. 启动应用
java -Xmx2g -jar target/enterprise-knowledge-system-1.0.0-SNAPSHOT.jar
```

---

## 配置详解

### 应用配置 (application.yml)

```yaml
server:
  port: 8080
  
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/knowledge_db
    username: knowledge
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      
  jpa:
    hibernate:
      ddl-auto: none
    show-sql: false
    
redis:
  host: localhost
  port: 6379
  password: ${REDIS_PASSWORD}
  
langchain:
  chat:
    provider: qwen  # qwen | openai | ollama
  embeddings:
    provider: local
```

### Docker Compose 配置

```yaml
services:
  app:
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 2G
    
    environment:
      - SPRING_DATASOURCE_MAX_POOL_SIZE=20
      - JAVA_OPTS=-Xms1g -Xmx2g
```

---

## 性能调优

### 1. JVM 调优

```bash
# G1 GC 优化
JAVA_OPTS="-Xms2g -Xmx4g \
           -XX:+UseG1GC \
           -XX:MaxGCPauseMillis=200 \
           -XX:InitiatingHeapOccupancyPercent=35 \
           -XX:G1ReservePercent=15"
```

### 2. 数据库连接池

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20    # 并发量高时可增加
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

### 3. 向量检索优化

Elasticsearch 配置：
```yaml
spring:
  elasticsearch:
    connection-timeout: 5s
    socket-timeout: 30s
```

---

## 监控告警

### Prometheus + Grafana 监控

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'knowledge-app'
    static_configs:
      - targets: ['app:8080']
    metrics_path: '/actuator/prometheus'
```

### 关键指标

| 指标 | 说明 | 告警阈值 |
|------|------|---------|
| HTTP_5xx_Rate | 错误请求率 | > 1% |
| Response_Time_P99 | P99 响应时间 | > 3s |
| DB_Connection_Pool | 数据库连接池 | > 80% |
| Cache_Hit_Ratio | 缓存命中率 | < 70% |
| Vector_Query_Time | 向量查询时间 | > 500ms |

---

## 日常运维

### 备份策略

```bash
# 每日自动备份脚本
#!/bin/bash
DATE=$(date +%Y%m%d)
BACKUP_DIR="/backup/postgres"
mkdir -p $BACKUP_DIR
docker-compose exec -T postgres pg_dump -U knowledge knowledge_db | gzip > $BACKUP_DIR/backup_$DATE.sql.gz
find $BACKUP_DIR -name "*.sql.gz" -mtime +7 -delete
```

### 日志轮转

```yaml
# application.yml
logging:
  file:
    name: /var/log/knowledge/app.log
    max-size: 100MB
    max-history: 30
```

### 优雅停机

```bash
# 发送 SIGTERM 让应用优雅关闭
docker-compose stop

# 或手动触发
curl -X POST http://localhost:8080/actuator/shutdown
```

---

## 故障排查

### 常见问题

#### 1. 应用无法启动

```bash
# 查看日志
docker-compose logs app

# 检查数据库连接
docker-compose exec postgres pg_isready -U knowledge

# 检查 Redis 连接
docker-compose exec redis redis-cli ping
```

#### 2. 向量检索慢

```sql
-- 添加索引
CREATE INDEX ON document_chunks USING hnsw (embedding vector_cosine_ops);

-- 分析表
ANALYZE document_chunks;
```

#### 3. 缓存命中率低

```bash
# 检查 Redis 内存
docker-compose exec redis redis-cli INFO memory

# 清理过期键
docker-compose exec redis redis-cli DEBUG SLEEP 1  # 仅测试用
```

#### 4. API 限流触发

```bash
# 查看限流计数器
docker-compose exec redis redis-cli GET ratelimit:query:user_xxx

# 重置限流
curl -X POST http://localhost:8080/admin/ratelimit/reset?userId=user_xxx
```

---

## 升级指南

### 从 v0.x 升级到 v1.0

```bash
# 1. 备份数据
docker-compose exec postgres pg_dump -U knowledge knowledge_db > backup_before_upgrade.sql

# 2. 停止服务
docker-compose down

# 3. 拉取新代码
git pull

# 4. 运行迁移
docker-compose run --rm app flyway migrate

# 5. 重启服务
docker-compose up -d
```

---

## 联系支持

- **问题反馈**: GitHub Issues
- **技术支持**: support@enterprise.local
- **文档**: docs/README.md

---

**最后更新**: 2026-05-27
**版本**: 1.0.0
