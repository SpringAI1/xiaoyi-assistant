# 小易助手 - 阿里云服务器部署详细指南

## ☁️ 第一步：准备阿里云 ECS 服务器

### 推荐配置
| 配置项 | 推荐值 | 说明 |
|-------|-------|------|
| 实例规格 | ecs.t6-c1m2.large (2核4G) | 性价比最高 |
| 操作系统 | Ubuntu 22.04 LTS | 稳定且易用 |
| 系统盘 | 40GB SSD | 足够使用 |
| 带宽 | 3Mbps+ | 保证访问速度 |

### 购买后获取信息
- 公网 IP 地址
- 登录用户名（通常是 root 或 ubuntu）
- 密码或 SSH 密钥

---

## 🔒 第二步：配置阿里云安全组

### 开放端口

1. 登录阿里云控制台 → ECS 实例
2. 找到你的实例 → 点击"安全组"
3. 点击"配置规则" → 手动添加

添加以下安全组规则：

| 端口范围 | 授权对象 | 协议类型 | 说明 |
|---------|---------|---------|------|
| 22 | 0.0.0.0/0 | TCP | SSH 远程登录 |
| 8080 | 0.0.0.0/0 | TCP | 应用访问 |
| 80 | 0.0.0.0/0 | TCP | HTTP（可选） |
| 443 | 0.0.0.0/0 | TCP | HTTPS（可选） |

---

## 📦 第三步：连接到服务器

### 使用 SSH 连接

```bash
# 如果是 root 用户
ssh root@你的服务器公网IP

# 如果是 ubuntu 用户（推荐）
ssh ubuntu@你的服务器公网IP
```

---

## 🛠️ 第四步：安装必要软件

### 更新系统包
```bash
sudo apt-get update
sudo apt-get upgrade -y
```

### 安装 JDK 21
```bash
# 安装 OpenJDK 21
sudo apt-get install -y openjdk-21-jdk

# 验证安装
java -version
```

### 安装 Docker（推荐）
```bash
# 下载 Docker 安装脚本
curl -fsSL https://get.docker.com -o get-docker.sh

# 执行安装
sudo sh get-docker.sh

# 将当前用户添加到 docker 组
sudo usermod -aG docker $USER

# 刷新组
newgrp docker

# 验证 Docker 安装
docker --version
docker compose version
```

---

## 🚀 第五步：部署应用（Docker 方式）

### 方法 A：使用 Git 拉取代码（推荐）

```bash
# 1. 克隆项目
cd ~
git clone https://github.com/SpringAI1/xiaoyi-assistant.git
cd xiaoyi-assistant
```

### 方法 B：本地上传文件

如果你在本地已经打包好了，可以使用 scp 上传：

```bash
# 在本地电脑执行（不是服务器上）
# 上传整个项目
scp -r /Users/chenxi/Desktop/JAVA ubuntu@你的服务器IP:~/xiaoyi-assistant
```

---

## 📦 第六步：打包并运行（在服务器上）

### 方式 1：使用 Docker（推荐）

```bash
# 进入项目目录
cd ~/xiaoyi-assistant

# 需要先在服务器上打包项目（如果本地没传 jar 包）
# 安装 Maven
sudo apt-get install -y maven

# 打包
cd backend
mvn clean package -DskipTests
cd ..

# 设置 API Key（重要！）
export QWEN_API_KEY="你的通义千问API_KEY"

# 使用简化版 Docker Compose 启动
docker compose -f docker-compose.simple.yml up -d --build

# 查看日志
docker compose -f docker-compose.simple.yml logs -f
```

### 方式 2：直接运行 jar 包

```bash
cd ~/xiaoyi-assistant/backend

# 设置 API Key
export QWEN_API_KEY="你的API_KEY"

# 后台运行（使用 nohup）
nohup java -Xms256m -Xmx1g -jar target/enterprise-knowledge-system-1.0.0-SNAPSHOT.jar > app.log 2>&1 &

# 查看日志
tail -f app.log

# 或者使用 systemd（生产环境推荐）
```

---

## ✅ 第七步：验证部署

### 检查服务状态

```bash
# Docker 方式
docker ps

# 查看应用日志
docker logs -f xiaoyi-assistant
```

### 测试访问

1. 在浏览器打开：`http://你的服务器公网IP:8080`
2. 应该能看到小易助手的前端界面
3. 测试健康检查：`http://你的服务器公网IP:8080/api/v1/health`

---

## 🔧 第八步：配置 Nginx 反向代理（可选但推荐）

### 安装 Nginx
```bash
sudo apt-get install -y nginx
```

### 配置 Nginx

创建配置文件：
```bash
sudo nano /etc/nginx/sites-available/xiaoyi
```

添加以下内容：
```nginx
server {
    listen 80;
    server_name 你的公网IP 或 域名;

    client_max_body_size 50M;

    location / {
        proxy_pass http://127.0.0.1:8080;
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

### 启用配置
```bash
# 创建软链接
sudo ln -s /etc/nginx/sites-available/xiaoyi /etc/nginx/sites-enabled/

# 删除默认配置（可选）
sudo rm /etc/nginx/sites-enabled/default

# 测试配置
sudo nginx -t

# 重启 Nginx
sudo systemctl restart nginx
```

现在你可以通过 `http://你的公网IP` 访问（不需要加8080端口）

---

## 🎯 第九步：配置 HTTPS（可选，使用阿里云 SSL）

### 1. 在阿里云申请免费 SSL 证书
- 进入阿里云控制台 → SSL 证书
- 申请免费证书（DV 证书）
- 下载证书（选择 Nginx 格式）

### 2. 上传证书到服务器

```bash
# 在服务器上创建目录
sudo mkdir -p /etc/nginx/ssl

# 使用 scp 上传证书文件（在本地执行）
scp /path/to/your-cert.pem ubuntu@你的IP:/etc/nginx/ssl/
scp /path/to/your-key.key ubuntu@你的IP:/etc/nginx/ssl/
```

### 3. 更新 Nginx 配置

修改 `/etc/nginx/sites-available/xiaoyi`：
```nginx
server {
    listen 443 ssl;
    server_name 你的域名;

    ssl_certificate /etc/nginx/ssl/cert.pem;
    ssl_certificate_key /etc/nginx/ssl/key.key;

    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;

    # 其他配置...
}

# HTTP 重定向到 HTTPS
server {
    listen 80;
    server_name 你的域名;
    return 301 https://$server_name$request_uri;
}
```

---

## 🔄 第十步：常用维护命令

### Docker 相关
```bash
# 启动服务
docker compose -f docker-compose.simple.yml up -d

# 停止服务
docker compose -f docker-compose.simple.yml stop

# 重启服务
docker compose -f docker-compose.simple.yml restart

# 查看日志
docker compose -f docker-compose.simple.yml logs -f

# 更新代码后重新构建
git pull
cd backend && mvn clean package -DskipTests && cd ..
docker compose -f docker-compose.simple.yml up -d --build
```

### 查看服务状态
```bash
# 检查端口是否监听
netstat -tlnp | grep 8080

# 检查进程
ps aux | grep java
```

---

## 💰 阿里云成本参考

| 配置 | 月费用（估算） |
|-----|--------------|
| 2核4G + 40G SSD | ~100-150元/月 |
| 1核2G + 40G SSD | ~60-80元/月 |
| 带宽 3Mbps | ~30元/月 |

新用户通常有 1-3 个月免费试用！

---

## 🐛 常见问题排查

### 问题 1：无法访问 8080 端口
- 检查阿里云安全组是否开放了 8080
- 检查服务器防火墙（`sudo ufw status`）
- 检查服务是否启动（`docker ps`）

### 问题 2：内存不足
- 减小 JVM 内存参数：`-Xms256m -Xmx512m`
- 升级服务器配置

### 问题 3：API 调用失败
- 检查环境变量 `echo $QWEN_API_KEY`
- 重新设置环境变量并重启服务

### 问题 4：Docker 权限问题
```bash
# 将用户加入 docker 组
sudo usermod -aG docker $USER
# 重新登录
```

---

## 📞 需要帮助？

查看主项目 README.md 或提交 Issue！
