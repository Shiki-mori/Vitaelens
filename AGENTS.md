# AGENTS.md

## 项目概览

`VitaeLens` 是一个面向校招学生的 AI 简历优化与模拟面试系统。用户上传 PDF 或 DOCX 简历，输入目标岗位 JD，系统解析简历内容，调用大模型生成结构化分析结果，并进一步生成模拟面试题和回答反馈。项目以 Java 后端能力为核心，AI 能力作为业务场景和差异化入口。

项目的主要目标是实现一个具备工程完整性的 Spring Boot 后端项目，重点体现异步任务、结构化 AI 输出、缓存、限流、鉴权、文件解析、数据持久化和 Docker 部署能力。

当前仓库已基本跑通 V1–V2 主流程，并实现了 V3 的模拟面试最小闭环。下文描述以**实际代码为准**。

## 技术栈

后端采用：

- Java 17
- Spring Boot 3.5.15
- Spring Security
- JWT（jjwt）
- MyBatis-Plus
- MySQL 8.0
- Redis 7.2
- springdoc OpenAPI（Swagger UI）
- Apache PDFBox
- Apache POI
- Jackson
- Hibernate Validator
- Docker Compose

前端采用：

- Vue 3 3.5.38
- TypeScript
- Vite 7.3.1
- Element Plus
- Axios
- Pinia

部署采用：

- Docker Compose（`deploy/docker-compose.yml`）
- 前端容器内 Nginx（静态资源 + `/api` 反代）
- MySQL 8.0
- Redis 7.2
- Spring Boot fat jar（多阶段 Docker 构建）

## 仓库结构

```text
Vitaelens/
├── AGENTS.md
├── database/sql/schema.sql          -- 数据库建表脚本
├── deploy/
│   ├── docker-compose.yml           -- 一键部署编排
│   ├── docker-compose.yml.example
│   └── mysql-init/                  -- MySQL 初始化脚本挂载目录
├── vitaelens-backend/               -- Spring Boot 后端
└── vitaelens-frontend/              -- Vue 3 前端
```

## 后端项目结构

后端基础包名为 `com.phrolova.vitaelensbackend`，主类为 `VitaelensBackendApplication`。

当前按技术分层组织（非按领域分子包），结构如下：

```text
com.phrolova.vitaelensbackend/
├── VitaelensBackendApplication.java
├── config/              -- 配置类
│   ├── SecurityConfig.java
│   ├── CorsConfig.java
│   ├── RedisConfig.java
│   ├── AsyncConfig.java
│   ├── UploadConfig.java
│   ├── SwaggerConfig.java
│   ├── RateLimitAspect.java
│   ├── AdminInitializer.java
│   ├── MyMetaObjectHandler.java
│   └── properties/AdminProperties.java
├── common/              -- 公共类
│   ├── Result.java              -- 统一响应体
│   ├── ErrorCode.java           -- 错误码枚举
│   ├── RateLimit.java           -- 限流注解
│   ├── LimitType.java           -- 限流维度（USER / IP）
│   └── PageResult.java          -- 分页响应体（预留，暂未使用）
├── exception/           -- 异常处理
│   ├── BizException.java
│   └── GlobalExceptionHandler.java
├── auth/                -- 鉴权相关
│   ├── JwtUtil.java
│   ├── JwtFilter.java
│   └── UserContext.java
├── entity/              -- 数据库实体
├── mapper/              -- MyBatis Mapper
├── service/             -- 业务接口
│   └── impl/            -- 业务实现（含 CacheService）
├── controller/          -- 接口层
├── dto/
│   ├── request/         -- 请求 DTO
│   └── response/        -- 响应 DTO（替代独立 VO 包）
├── enums/
│   └── RoleEnum.java    -- 用户角色（user / admin）
├── ai/                  -- AI 调用封装
│   ├── AiClient.java
│   └── PromptTemplate.java
├── task/
│   └── AnalysisTaskExecutor.java  -- 预留占位，当前未使用
└── util/                -- 工具类
    ├── HashUtil.java
    ├── FileUtil.java
    ├── PdfParser.java
    └── DocxParser.java
```

AI 工具生成代码时，必须优先遵循以上结构。除非确有必要，不要引入新的顶层包。

## 后端分层

后端使用典型的 Controller、Service、Mapper 分层。当前实现按技术类型集中存放，而不是按领域拆分子模块目录。

各层职责如下：

| 层级 | 职责 |
|------------|-------------------------|
| Controller | 接收 HTTP 请求，做参数校验与限流注解，返回统一响应 |
| Service | 编排业务流程，处理事务，调用外部服务 |
| Mapper | 访问数据库，不写业务逻辑 |
| Entity | 对应数据库表结构 |
| DTO (request) | 接收前端请求参数 |
| DTO (response) | 返回前端展示数据 |
| Enum | 表示状态、类型、错误分类等固定值 |
| Client（`ai/`） | 封装大模型调用 |

Controller 不允许直接调用 Mapper。Mapper 不允许调用 Service。AI 调用、文件解析、Redis 操作必须封装在独立组件中，不允许散落在 Controller 中。

## 核心模块

### auth

负责注册、登录、JWT 签发和用户身份解析。

已实现能力：

- 用户注册
- 用户登录
- 密码 BCrypt 加密存储
- JWT 生成和校验
- 当前登录用户获取（`UserContext`）
- 启动时按配置初始化管理员账号（`AdminInitializer`）
- 登录接口 IP 维度限流

密码必须使用 BCrypt 或同等级别的哈希算法，不允许明文存储。

当前未实现：登出、token 黑名单、独立用户资料维护接口。

### resume

负责简历文件上传、解析和管理。

已实现能力：

- 上传 PDF / DOCX 简历
- 校验文件类型和大小（10 MB）
- 保存文件元信息
- 解析简历文本（`PdfParser` / `DocxParser`）
- 查询当前用户的简历列表
- 删除简历（含尽量删除本地文件）
- 上传接口用户维度限流

查询简历时必须带上当前登录用户 ID，禁止用户访问他人的简历。当前未提供按 ID 查询详情的独立接口；列表响应中包含解析文本摘要字段。

### job

负责岗位 JD 的创建和管理。

已实现能力：

- 创建岗位 JD
- 查询岗位 JD 列表
- 删除岗位 JD

岗位 JD 字段为：岗位名称（`title`）、岗位描述文本（`content`）、创建时间。当前 schema **不包含**公司名称字段，也未提供按 ID 查询详情接口。

### analysis

负责简历分析任务，是项目最核心的后端模块。

已实现能力：

- 创建简历分析任务
- 查询任务状态和结果
- 查询历史分析记录
- Redis + 数据库命中复用相同输入的分析结果
- 记录失败原因和 `retryCount`
- 分析创建接口用户维度限流
- 写入 `ai_call_log`（耗时与成功/失败状态）

分析任务设计为异步执行：创建接口写入 `PENDING` 任务后立即返回 `taskId`，由 `@Async("analysisExecutor")` 线程池执行分析流程，前端通过轮询查询任务状态。当前异步逻辑位于 `AnalysisServiceImpl`，`task/AnalysisTaskExecutor` 为预留空类。

任务状态使用字符串常量：

```text
PENDING
RUNNING
SUCCESS
FAILED
```

任务状态流转规则：

```text
PENDING -> RUNNING -> SUCCESS
PENDING -> RUNNING -> FAILED
FAILED 不自动回到 RUNNING（当前无手动重试接口）
```

缓存命中行为：

- Redis 命中：直接返回 `SUCCESS` 结果（可不带 `taskId`，不强制新建任务记录）
- 数据库已有相同 `inputHash` 的 SUCCESS 任务：回填 Redis 后返回该任务

当前失败时会将 `retryCount` 加一并标记 `FAILED`，尚未实现自动多轮重试。

### interview

负责模拟面试（V3 最小闭环）。

已实现能力：

- 根据简历、岗位 JD、分析结果生成模拟面试题
- 创建面试场次
- 查询面试场次详情（含问题列表）
- 提交用户回答
- 调用 AI 评价回答
- 保存回答与反馈 JSON
- 创建场次 / 提交回答的用户维度限流

第一版只支持文本回答。面试题应根据简历内容、岗位 JD 和分析结果生成，而不是生成通用八股题。

当前未实现：面试场次列表接口、管理端、报告导出。

### ai

负责大模型调用、Prompt 组装、结构化输出解析和调用日志记录。

已实现能力：

- 封装 AI API 请求（`AiClient`，OpenAI 兼容 `/v1/chat/completions`）
- 管理 Prompt 模板（简历分析 / 面试出题 / 回答评价）
- 要求 AI 返回 JSON
- 解析 AI 返回结果
- 校验关键 JSON 字段（如 `overallScore`、`dimensionScores`）
- 分析任务路径记录调用耗时与状态到 `ai_call_log`

AI API Key 必须通过环境变量注入，不允许写入代码或提交到 Git。

配置示例：

```yaml
ai:
  provider: deepseek   # 生产配置中使用
  base-url: ${AI_BASE_URL}
  api-key: ${AI_API_KEY}
  model: ${AI_MODEL}
  timeout-seconds: 60
```

当前 token 用量字段在表结构中预留，业务侧尚未完整解析写入；面试相关 AI 调用暂未统一写入 `ai_call_log`。

### security

安全能力分布在 `auth/` 与 `config/`（无独立 `security` 包）。

已实现能力：

- JWT 过滤器
- 登录用户上下文
- 接口鉴权（`/api/auth/**` 与 Swagger 文档放行，其余 `/api/**` 需登录）
- 密码加密
- CORS 配置
- 越权访问拦截（查询附带当前用户 ID）

所有需要登录的接口都必须从安全上下文中获取当前用户 ID，不允许从前端传入 `userId` 后直接信任。

### common

存放通用组件。

当前包含：

- 统一响应体 `Result`
- 业务异常 `BizException`
- 错误码枚举 `ErrorCode`
- 全局异常处理器 `GlobalExceptionHandler`
- 限流注解 `RateLimit` / `LimitType`
- 分页返回对象 `PageResult`（预留）
- Hash 工具 `HashUtil`（位于 `util/`）

不要把具体业务逻辑放入 `common`。

## 核心业务流程

### 简历分析流程

```text
用户上传简历
    -> 后端校验文件类型和大小
    -> 保存文件
    -> 解析简历文本（PdfParser / DocxParser）
    -> 写入 resume 表

用户输入岗位 JD
    -> 写入 job_description 表

用户创建分析任务
    -> 后端计算 inputHash
    -> 查询 Redis 是否存在分析结果
    -> 命中 Redis 则直接返回 SUCCESS 结果
    -> 未命中则查询数据库是否已有相同 inputHash 的 SUCCESS 任务
    -> 命中数据库则回填 Redis 并返回该任务
    -> 均未命中则创建 PENDING 任务
    -> 返回 taskId
    -> 线程池异步执行分析
    -> 任务状态改为 RUNNING
    -> 调用 AI 接口
    -> 校验 JSON 结果
    -> 写入 analysis_task 表
    -> 写入 Redis 缓存
    -> 任务状态改为 SUCCESS
```

失败流程：

```text
AI 调用超时 / JSON 解析失败 / 字段缺失
    -> 记录错误日志
    -> 任务状态改为 FAILED
    -> retryCount + 1
    -> 保存 errorMessage
    -> 写入 ai_call_log
```

### 模拟面试流程

```text
用户选择一次分析结果
    -> 创建 interview_session
    -> 根据简历、JD、分析结果生成面试题
    -> 保存 interview_question
    -> 前端展示问题

用户提交回答
    -> 后端读取问题、简历与岗位信息
    -> 调用 AI 生成评价
    -> 保存 answer 和 feedbackJson
    -> 返回评分、问题诊断和改进建议
```

## 数据库设计

已实现以下表（见 `database/sql/schema.sql`）：

| 表名 | 说明 |
|----------------------|----------|
| `user` | 用户表（含 `role`、逻辑删除） |
| `resume` | 简历表 |
| `job_description` | 岗位 JD 表（`title` + `content`） |
| `analysis_task` | 简历分析任务表 |
| `interview_session` | 模拟面试场次表 |
| `interview_question` | 面试问题与回答表 |
| `ai_call_log` | AI 调用日志表 |

部署时由 `deploy/mysql-init` 挂载初始化。

## Redis 设计

Redis 主要用于分析结果缓存和接口限流。

实际 Key：

```text
analysis:result:{inputHash}
rate_limit:{user:{userId}|ip:{ip}}:{HTTP_METHOD:URI}
```

说明：

| Key | 用途 |
|---------------------------------------------|--------------------------------|
| `analysis:result:{inputHash}` | 缓存简历和 JD 的分析结果 |
| `rate_limit:{identifier}:{METHOD:URI}` | 基于 `@RateLimit` 的通用限流计数器 |

分析结果缓存过期时间为 7 天。限流 Key 的过期时间等于注解中的时间窗口（默认 60 秒）。

当前未实现 token 黑名单 Key。

## AI 输出格式

简历分析接口要求 AI 返回 JSON，不允许只返回自然语言。

推荐结构：

```json
{
  "overallScore": 78,
  "dimensionScores": {
    "technicalMatch": 80,
    "projectExperience": 75,
    "expressionQuality": 70,
    "jobMatch": 85
  },
  "strengths": [
    "项目经历与后端开发岗位有一定匹配度"
  ],
  "weaknesses": [
    "项目描述缺少量化结果和技术难点"
  ],
  "skillGaps": [
    "Redis",
    "消息队列",
    "Docker"
  ],
  "rewriteSuggestions": [
    {
      "original": "做了一个管理系统",
      "suggested": "基于 Spring Boot 独立开发后台管理系统，实现用户鉴权、数据统计与接口优化",
      "reason": "补充技术栈、职责和业务结果"
    }
  ],
  "interviewFocus": [
    "数据库表设计",
    "异步任务状态流转",
    "接口性能优化"
  ]
}
```

后端必须把 AI 返回内容反序列化为明确结构后再入库。当前至少校验 `overallScore` 与 `dimensionScores` 存在；不允许直接把未校验的原始文本作为正式分析结果返回给前端。

## 主要接口

### 认证

| 方法 | 路径 | 说明 |
|--------|----------------------|--------|
| `POST` | `/api/auth/register` | 注册 |
| `POST` | `/api/auth/login` | 登录 |

### 简历

| 方法 | 路径 | 说明 |
|----------|-----------------------|--------|
| `POST` | `/api/resumes/upload` | 上传简历 |
| `GET` | `/api/resumes` | 查询简历列表 |
| `DELETE` | `/api/resumes/{id}` | 删除简历 |

### 岗位 JD

| 方法 | 路径 | 说明 |
|----------|------------------|------------|
| `POST` | `/api/jobs` | 创建岗位 JD |
| `GET` | `/api/jobs` | 查询岗位 JD 列表 |
| `DELETE` | `/api/jobs/{id}` | 删除岗位 JD |

### 分析任务

| 方法 | 路径 | 说明 |
|--------|----------------------------|-----------|
| `POST` | `/api/analysis/tasks` | 创建分析任务 |
| `GET` | `/api/analysis/tasks/{taskId}` | 查询任务状态和结果 |
| `GET` | `/api/analysis/tasks` | 查询历史分析任务 |

### 模拟面试

| 方法 | 路径 | 说明 |
|--------|-----------------------------------------|-----------|
| `POST` | `/api/interviews/sessions` | 创建面试场次 |
| `GET` | `/api/interviews/sessions/{id}` | 查询面试场次详情 |
| `POST` | `/api/interviews/questions/{id}/answer` | 提交回答并获取反馈 |

接口文档：`/swagger-ui/**`、`/v3/api-docs/**`（开发/调试用途，已放行鉴权）。

## 统一响应格式

所有接口使用统一响应格式（`Result`）：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

错误响应示例：

```json
{
  "code": 401,
  "message": "未登录或登录已过期",
  "data": null
}
```

Controller 返回值不应直接暴露 Entity，必须通过 `dto/response` 返回前端需要的数据。

## 异常处理

必须实现全局异常处理器。

至少处理：

- 参数校验异常
- 业务异常（`BizException`）
- 文件上传异常
- JWT 异常
- AI 调用异常
- 数据库异常
- 未知异常

未知异常返回通用错误信息，不应把堆栈、SQL、密钥、文件路径暴露给前端。

## 文件上传约束

简历上传规则：

- 只允许 PDF 和 DOCX
- 单文件大小限制为 10 MB
- 文件名必须重新生成，不能直接使用用户上传文件名作为存储文件名
- 文件路径不应暴露给前端
- 删除简历时，需要删除数据库记录，并尽量删除对应文件

存储路径通过配置控制：

```yaml
vitaelens:
  upload-dir: ./uploads/resumes
```

部署环境：

```yaml
vitaelens:
  upload-dir: /app/uploads/resumes
```

## 安全与隐私

简历数据包含手机号、邮箱、学校、项目经历等敏感信息。所有 AI 工具生成代码时必须遵守以下原则：

- 不允许未登录访问简历、JD、分析任务和面试记录
- 查询数据时必须附带当前登录用户 ID 条件
- 不允许从前端传入 `userId` 后直接作为可信身份
- 不允许在日志中打印完整简历文本
- 不允许在日志中打印 AI API Key
- 不允许把用户上传文件的真实路径返回给前端
- 可以对手机号、邮箱做脱敏展示
- 如接入第三方模型，应在 README 中说明用户数据会被发送到模型服务商

## 限流策略

AI 与敏感接口使用 Redis + `@RateLimit` 实现限流。当前规则：

| 接口 | 限制 |
|--------|----------------|
| 登录 | 每个 IP 每分钟最多 10 次 |
| 简历上传 | 每个用户每分钟最多 5 次 |
| 创建分析任务 | 每个用户每分钟最多 3 次 |
| 创建面试场次 | 每个用户每分钟最多 3 次 |
| 提交面试回答 | 每个用户每分钟最多 10 次 |

超过限制时返回明确错误码和错误信息。

## 日志要求

必须记录：

- 用户登录失败
- 文件解析失败
- 分析任务状态变化
- AI 调用成功或失败
- AI 调用耗时
- JSON 解析失败
- 限流拦截

禁止记录：

- 明文密码
- JWT 完整内容
- AI API Key
- 完整简历文本
- 用户上传文件真实绝对路径

## 事务边界

推荐事务边界：

- 用户注册需要事务
- 简历元信息保存和解析结果更新可以在同一业务流程中处理，但文件写入本身不属于数据库事务
- 创建分析任务需要事务
- 异步任务更新状态和结果需要事务
- 创建面试场次和批量保存问题需要事务
- 提交回答和保存反馈需要事务

调用外部 AI 接口时，不应长时间持有数据库事务。

## 前端页面

当前前端路由与页面：

| 路由 | 页面 | 说明 |
|--------------------------|-------------------------|----------------|
| `/login` | `LoginView` | 登录和注册 |
| `/` | `HomeView` | 项目介绍和入口 |
| `/resumes` | `ResumeView` | 上传、查看、删除简历 |
| `/jobs` | `JobView` | 创建和管理目标岗位 |
| `/analysis` | `AnalysisListView` | 创建分析任务，展示任务列表 |
| `/analysis/:taskId` | `AnalysisDetailView` | 展示评分、问题、建议和技能缺口 |
| `/interviews` | `InterviewListView` | 从成功分析任务创建面试场次 |
| `/interviews/:sessionId` | `InterviewSessionView` | 展示问题、提交回答、查看反馈 |

前端不承担核心业务判断。鉴权、数据隔离、任务状态流转和限流必须由后端保证。

## 部署架构

项目可通过 `deploy/docker-compose.yml` 一键启动。

实际服务：

```text
frontend   -- Nginx 静态资源 + /api 反代
backend    -- Spring Boot
mysql
redis
```

请求路径：

```text
浏览器
  -> frontend 容器 Nginx
  -> 前端静态页面
  -> /api 反向代理到 backend:8080
  -> MySQL / Redis / AI Provider
```

生产环境中，AI API Key、数据库密码、JWT 密钥通过环境变量或 `deploy/.env` 传入，`.env` 不应提交到 Git。

## 实现状态

### 已完成（对应原 V1 / V2 / V3 最小集）

- 用户注册登录与 JWT 鉴权
- 简历上传与 PDF/DOCX 文本解析
- 岗位 JD 创建与列表管理
- 异步分析任务与状态机（PENDING / RUNNING / SUCCESS / FAILED）
- Redis 分析结果缓存
- Redis 通用限流
- AI JSON 结构化输出校验
- 全局异常处理
- Docker Compose 部署
- springdoc OpenAPI 接口文档
- 模拟面试题生成与回答评价
- AI 调用日志（分析任务路径，含耗时）

### 未实现 / 预留

- 独立用户资料接口（如查询/修改当前用户）
- 简历 / JD 按 ID 详情接口
- 岗位公司名称字段
- 分析任务手动重试与自动多轮重试
- 面试场次列表接口
- Token 用量完整统计
- 简单管理端
- 简历优化前后版本对比
- 导出 Markdown 或 PDF 报告
- Token 黑名单登出
- `PageResult` 分页能力
- `AnalysisTaskExecutor` 独立执行器封装

## AI 编码约束

后续 AI 工具生成代码时必须遵守以下规则：

1. 不要跳过鉴权和用户数据隔离。
2. 不要把业务逻辑写在 Controller。
3. 不要直接返回 Entity。
4. 不要把 API Key、JWT 密钥、数据库密码写死在代码中。
5. 不要在日志中输出完整简历文本。
6. 不要让 AI 原始输出直接入库，必须先解析和校验。
7. 不要在调用外部 AI 接口时持有数据库事务。
8. 不要为赶进度引入过多复杂中间件。
9. 不要把未规划功能提前塞进主流程，避免项目失控。
10. 不要生成与本文件架构明显冲突的目录和模块。

## 包名

后端基础包名：

```text
com.phrolova.vitaelensbackend
```

包名应保持稳定，不要随意更换。

## 简历包装方向

项目完成后，简历中应突出后端工程能力，而不是只写“接入大模型”。

推荐描述方向（需与最终完成内容一致）：

```text
基于 Spring Boot 3 + Vue 3 开发 AI 简历优化与模拟面试系统，支持 PDF/DOCX 简历解析、岗位 JD 匹配、AI 评分、优化建议生成、模拟面试题生成与回答反馈。后端使用 MySQL 存储用户、简历、岗位与分析任务数据，设计异步任务状态机处理大模型调用，避免长时间阻塞主请求流程；基于 Redis 实现分析结果缓存与用户维度限流，降低重复调用成本并防止高频请求；通过 Prompt 模板和 JSON 结构化校验约束大模型输出，提升结果解析稳定性。
```

实际简历描述必须与最终完成内容一致。没有完成的功能不要写入简历。
