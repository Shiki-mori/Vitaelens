# VitaeLens

面向校招场景的 **AI 简历优化与模拟面试系统**。用户上传 PDF/DOCX 简历并填写目标岗位 JD 后，系统解析简历文本，调用大模型生成结构化匹配分析，并可基于分析结果生成模拟面试题与回答反馈。

项目以后端工程能力为主线：JWT 鉴权、文件解析、异步任务状态机、Redis 缓存与限流、结构化 AI 输出校验、Docker Compose 一键部署。

## 功能特性

- **账号与鉴权**：注册 / 登录、BCrypt 密码存储、JWT 无状态鉴权、用户数据隔离
- **简历管理**：PDF / DOCX 上传（10MB 限制）、PDFBox / POI 文本解析、列表查询与删除
- **岗位 JD**：创建、列表、删除目标岗位描述
- **简历分析**：按简历 + JD 创建分析任务；`PENDING → RUNNING → SUCCESS / FAILED` 状态流转；前端轮询查询结果
- **结果缓存**：相同输入（`inputHash`）命中 Redis / 历史成功任务时复用结果，降低重复调用成本
- **模拟面试**：基于分析结果生成面试题，提交文本回答并获取 AI 评价反馈
- **工程配套**：Redis 限流、统一响应与全局异常处理、springdoc OpenAPI、Docker Compose 部署

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Java 17、Spring Boot 3.5、Spring Security、JWT、MyBatis-Plus、Hibernate Validator |
| 数据 | MySQL 8.0、Redis 7.2 |
| 文件 / AI | Apache PDFBox、Apache POI、OpenAI 兼容 Chat Completions API |
| 前端 | Vue 3、TypeScript、Vite、Element Plus、Pinia、Axios |
| 部署 | Docker Compose、Nginx（前端容器内反代 `/api`） |

## 架构概览

```text
浏览器
  └─ frontend (Nginx 静态资源 + /api 反代)
       └─ backend (Spring Boot :8080)
            ├─ MySQL   用户 / 简历 / JD / 分析任务 / 面试记录 / AI 调用日志
            ├─ Redis   分析结果缓存 + 接口限流
            └─ AI Provider  结构化 JSON 分析 / 出题 / 回答评价
```

核心分析流程：

```text
上传简历 & 创建 JD
  → 创建分析任务（立即返回 taskId）
  → 线程池异步调用大模型
  → 校验 JSON 结构并落库
  → 写入 Redis 缓存
  → 前端轮询任务状态 / 查看结果
  →（可选）创建模拟面试场次并作答
```

## 仓库结构

```text
Vitaelens/
├── database/sql/schema.sql      # 建表脚本
├── deploy/                      # Docker Compose 与环境变量示例
├── vitaelens-backend/           # Spring Boot 后端
└── vitaelens-frontend/          # Vue 3 前端
```

## 快速开始

### 环境要求

- Docker / Docker Compose（推荐一键启动）
- 或本地：JDK 17、Maven、Node.js 22+、MySQL 8、Redis 7

### Docker Compose 一键启动

```bash
cd deploy
cp .env.example .env
cp docker-compose.yml.example docker-compose.yml
# 编辑 .env：填入 AI_BASE_URL、AI_API_KEY、AI_MODEL、JWT_SECRET 等
docker compose up -d --build
```

启动后默认访问：

| 服务 | 地址 |
|------|------|
| 前端 | http://localhost（示例 compose 映射为 `80:80`；本地改版可能为 `8088:80`） |
| 后端 API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui/index.html |

> 密钥、数据库密码、JWT Secret、AI API Key 均通过环境变量注入，请勿提交 `.env` 或含真实密钥的 `docker-compose.yml`。

### 本地开发（简要）

1. 启动 MySQL / Redis，执行 `database/sql/schema.sql`
2. 复制 `vitaelens-backend/src/main/resources/application.yml.example` 为 `application.yml` 并填写配置
3. 后端：`cd vitaelens-backend && mvn spring-boot:run`
4. 前端：`cd vitaelens-frontend && npm install && npm run dev`

## 主要接口

| 模块 | 接口 |
|------|------|
| 认证 | `POST /api/auth/register`、`POST /api/auth/login` |
| 简历 | `POST /api/resumes/upload`、`GET /api/resumes`、`DELETE /api/resumes/{id}` |
| 岗位 | `POST /api/jobs`、`GET /api/jobs`、`DELETE /api/jobs/{id}` |
| 分析 | `POST /api/analysis/tasks`、`GET /api/analysis/tasks`、`GET /api/analysis/tasks/{taskId}` |
| 面试 | `POST /api/interviews/sessions`、`GET /api/interviews/sessions/{id}`、`POST /api/interviews/questions/{id}/answer` |

更多细节见 Swagger 文档。

## 工程亮点

- **异步任务状态机**：创建分析接口不阻塞等待大模型，任务状态可查询、可落库，失败记录错误信息
- **缓存与限流**：Redis 缓存分析结果；对登录、上传、分析、面试等接口做用户 / IP 维度限流，控制成本与滥用
- **结构化 AI 输出**：Prompt 模板约束 JSON；服务端校验关键字段后再入库，避免原始文本直接作为业务结果
- **数据安全**：接口鉴权、按 `userId` 隔离查询；敏感配置环境变量注入；日志避免输出完整简历与 API Key
- **可部署性**：多阶段 Docker 构建 + Compose 编排 MySQL / Redis / Backend / Frontend

## 隐私说明

简历与岗位描述会发送至配置的第三方大模型服务商以完成分析与面试评价。请仅在个人学习 / 演示环境使用，并妥善保管 API Key；生产环境需自行评估数据合规要求。

## License

[MIT](./LICENSE)
