# 小易助手 - 云服务器部署指南

## 🚀 方法一：Docker 部署（推荐）

### 步骤 1：在本地打包项目

```bash
cd /Users/chenxi/Desktop/JAVA/backend
mvn clean package -DskipTests
```

### 步骤 2：准备部署文件

将以下文件上传到云服务器：
- `Dockerfile`
- `docker-compose.yml`
- 打包好的 jar 文件（`backend/target/enterprise-knowledge-system-1.0.0-SNAPSHOT.jar`）

### 步骤 3：更新 Dockerfile

我们已经更新了 Dockerfile 使用 JDK 21。

### 步骤 4：在云服务器上安装 Docker

```bash
# Ubuntu/Debian
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# 安装 Docker Compose
sudo apt-get install docker-compose-plugin
```

### 步骤 5：启动应用

```bash
# 创建目录
mkdir -p ~/xiaoyi-assistant
cd ~/xiaoyi-assistant

# 上传文件后，启动
docker-compose up -d

# 查看日志
docker-compose logs -f
```

---

## 📦 方法二：直接 jar 包部署

### 步骤 1：打包项目

```bash
cd backend
mvn clean package -DskipTests
```

### 步骤 2：上传 jar 包到服务器

```bash
scp target/enterprise-knowledge-system-1.0.0-SNAPSHOT.jar user@your-server:/path/to/deploy/
```

### 步骤 3：在服务器上安装 JDK 21

```bash
# Ubuntu/Debian
sudo apt-get update
sudo apt-get install openjdk-21-jdk

# 验证
java -version
```

### 步骤 4：运行应用

```bash
# 设置环境变量（API Key）
export QWEN_API_KEY="your-api-key-here"

# 运行
java -Xms512m -Xmx2g -jar enterprise-knowledge-system-1.0.0-SNAPSHOT.jar
```

### 步骤 5：使用 systemd 守护进程（生产推荐）

创建 `/etc/systemd/system/xiaoyi.service`：

```ini
[Unit]
Description=XiaoYi Assistant Service
After=network.target

[Service]
Type=simple
User=ubuntu
WorkingDirectory=/home/ubuntu/xiaoyi
Environment="QWEN_API_KEY=your-api-key-here"
ExecStart=/usr/bin/java -Xms512m -Xmx2g -jar /home/ubuntu/xiaoyi/enterprise-knowledge-system-1.0.0-SNAPSHOT.jar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

启动服务：

```bash
sudo systemctl daemon-reload
sudo systemctl enable xiaoyi
sudo systemctl start xiaoyi
sudo systemctl status xiaoyi
```

---

## 🌐 配置云服务器安全组

确保云服务器的安全组/防火墙开放以下端口：
- **8080** - 应用访问端口
- **80** / **443** - 如果配置了 Nginx 反向代理

---

## 🔒 使用 Nginx 反向代理（生产环境推荐）

### 安装 Nginx

```bash
sudo apt-get install nginx
```

### 配置 Nginx

创建 `/etc/nginx/sites-available/xiaoyi`：

```nginx
server {
    listen 80;
    server_name your-domain.com;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # WebSocket 支持
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
```

启用配置：

```bash
sudo ln -s /etc/nginx/sites-available/xiaoyi /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx
```

---

## ☁️ 主流云服务器推荐

| 云服务商 | 推荐配置 | 价格参考 |
|---------|---------|---------|
| 阿里云 | 2核4G | ~100元/月 |
| 腾讯云 | 2核4G | ~100元/月 |
| 华为云 | 2核4G | ~100元/月 |
| AWS EC2 | t3.medium | ~200元/月 |

---

## 📝 部署检查清单

- [ ] JDK 21 已安装
- [ ] API Key 已配置环境变量
- [ ] 防火墙/安全组已开放 8080 端口
- [ ] 应用已启动
- [ ] 访问 `http://your-server-ip:8080` 测试
- [ ] 配置日志轮转（可选）

---

## 🐛 常见问题

### 问题 1：内存不足

```bash
# 减小 JVM 内存参数
java -Xms256m -Xmx1g -jar app.jar
```

### 问题 2：端口被占用

```bash
# 检查端口占用
lsof -i :8080
# 修改 application.yml 中的端口
```

### 问题 3：API 调用失败

检查环境变量是否正确设置：
```bash
echo $QWEN_API_KEY
```
