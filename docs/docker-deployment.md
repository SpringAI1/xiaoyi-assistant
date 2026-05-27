# Docker 部署指南

## 快速开始

### 1. 准备环境变量

```bash
cp .env.example .env
# 编辑 .env 文件，填入你的 API Key
```

### 2. 启动所有服务

```bash
docker-compose up -d
```

### 3. 查看日志

```bash
# 查看所有服务日志
docker-compose logs -f

# 查看应用日志
docker-compose logs -f app

# 查看数据库日志
docker-compose logs -f postgres
```

### 4. 停止服务

```bash
docker-compose down

# 清理数据卷（慎重！会删除所有数据）
docker-compose down -v
```

## 生产环境配置

### 修改 .env 中的敏感信息

```bash
# 强密码示例
DB_PASSWORD=YourSuperSecurePassword123!
REDIS_PASSWORD=YourRandomRedisPassword456!

# API Key
QWEN_API_KEY=sk-xxxxxxxxxxxxxxxxxxxxx
```

### JVM 内存调整

编辑 `Dockerfile` 或启动参数：

```bash
ENV JAVA_OPTS="-Xms1g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

### Elasticsearch 资源调整

编辑 `docker-compose.yml`:

```yaml
elasticsearch:
  environment:
    - "ES_JAVA_OPTS=-Xms2g -Xmx2g"
```

## 数据库迁移

首次启动时，Flyway 会自动执行 `V1__init.sql` 中的所有 SQL 语句。

如需手动更新数据库结构：

1. 创建新的迁移文件 `V2__add_feature_column.sql`
2. 重启容器或运行：

```bash
docker-compose exec app flyway migrate
```

## 健康检查

```bash
# 应用健康检查
curl http://localhost:8080/api/v1/health

# 数据库连接测试
docker-compose exec postgres pg_isready

# Redis 连接测试
docker-compose exec redis redis-cli ping
```

## 备份数据库

```bash
# 备份到本地
docker-compose exec postgres pg_dump -U knowledge knowledge_db > backup_$(date +%Y%m%d).sql

# 恢复数据库
cat backup.sql | docker-compose exec -T postgres psql -U knowledge knowledge_db
```

## 监控和调试

```bash
# 进入应用容器
docker-compose exec app sh

# 查看应用进程
ps aux

# 查看端口占用
netstat -tlnp
```
