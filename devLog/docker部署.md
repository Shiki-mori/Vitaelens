# docker部署

## 后端Dockerfile

[Dockerfile](../vitaelens-backend/Dockerfile)

## 前端Dockerfile

[Dockerfile](../vitaelens-frontend/Dockerfile)

## 前端Nginx配置

[nginx.conf](../vitaelens-frontend/nginx.conf)

## 生产环境 docker-compose.yml

[docker-compose.yml](../deploy/docker-compose.yml)

## 生产环境 application-prod.yml

[application-prod.yml](../vitaelens-backend/src/main/resources/application-prod.yml)

## 环境变量文件

[.env](../deploy/.env)

# 构建并启动

在目录`/deploy/`执行：

```bash
docker compose up -d --build
```

等待构建完成，查看状态：

```bash
docker compose ps
docker compose logs -f backend
```

# 验证部署

访问：  
前端：<http://localhost>  
后端API：<http://localhost/api/auth/login>

# 停止与清理

```bash
# 停止
docker compose down
# 停止并清理数据卷
docker compose down -v
```

# 为docker配置代理

shell使用本地代理。但`dockerd`是systemd服务，没有继承代理，直连无法连接。

参考 [docker配置.md](./docker配置.md)。

