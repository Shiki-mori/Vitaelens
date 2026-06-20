# AGENTS.md

## 项目概览

`VitaeLens` 是一个面向校招学生的 AI 简历优化与模拟面试系统。用户上传 PDF 或 DOCX 简历，输入目标岗位 JD，系统解析简历内容，调用大模型生成结构化分析结果，并进一步生成模拟面试题和回答反馈。项目以 Java 后端能力为核心，AI 能力作为业务场景和差异化入口。

项目的主要目标不是做一个简单的“AI 问答网页”，而是实现一个具备工程完整性的 Spring Boot 后端项目，重点体现异步任务、结构化 AI 输出、缓存、限流、鉴权、文件解析、数据持久化和 Docker 部署能力。

## 技术栈

后端采用：

- Java 17
- Spring Boot 3.5.15
- Spring Security
- JWT
- MyBatis-Plus
- MySQL 8.0
- Redis 7.2
- Knife4j 或 Swagger
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

部署采用：

- Docker Compose
- Nginx
- MySQL 8.0
- Redis 7.2
- Spring Boot fat jar 或 Docker 镜像

## 项目结构

推荐仓库结构如下：

```text
vitaelens/
├── vitaelens-backend/
│   ├── src/main/java/com/vitaelens/
│   │   ├── VitaeLensApplication.java
│   │   ├── auth/
│   │   ├── user/
│   │   ├── resume/
│   │   ├── job/
│   │   ├── analysis/
│   │   ├── interview/
│   │   ├── ai/
│   │   ├── security/
│   │   ├── common/
│   │   └── config/
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   ├── application-dev.yml
│   │   └── mapper/
│   ├── pom.xml
│   └── Dockerfile
├── vitaelens-frontend/
│   ├── src/
│   ├── package.json
│   └── Dockerfile
├── deploy/
│   ├── docker-compose.yml
│   ├── nginx.conf
│   └── mysql-init/
├── docs/
│   ├── api.md
│   ├── database.md
│   └── architecture.md
├── README.md
└── AGENTS.md
```

AI 工具生成代码时，必须优先遵循以上结构。除非确有必要，不要引入新的顶层目录。

## 后端分层

后端使用典型的 Controller、Service、Mapper 分层，但业务模块按领域组织，而不是按技术类型集中堆放。

每个业务模块建议包含：

```text
module/
├── controller/
├── service/
├── service/impl/
├── mapper/
├── entity/
├── dto/
├── vo/
└── enums/
```

各层职责如下：

| 层级 | 职责 |
|---|---|
| Controller | 接收 HTTP 请求，做参数校验，返回统一响应 |
| Service | 编排业务流程，处理事务，调用外部服务 |
| Mapper | 访问数据库，不写业务逻辑 |
| Entity | 对应数据库表结构 |
| DTO | 接收前端请求参数 |
| VO | 返回前端展示数据 |
| Enum | 表示状态、类型、错误分类等固定值 |
| Client | 封装第三方 API 或大模型调用 |

Controller 不允许直接调用 Mapper。Mapper 不允许调用 Service。AI 调用、文件解析、Redis 操作必须封装在独立组件中，不允许散落在 Controller 中。

## 核心模块

### auth

负责注册、登录、JWT 签发和用户身份解析。

主要能力：

- 用户注册
- 用户登录
- 密码加密存储
- JWT 生成和校验
- 当前登录用户获取

密码必须使用 BCrypt 或同等级别的哈希算法，不允许明文存储。

### user

负责用户基础信息维护。

第一版只需要保留最小能力：

- 查询当前用户信息
- 修改昵称或邮箱
- 逻辑删除用户，后续可选

### resume

负责简历文件上传、解析和管理。

主要能力：

- 上传 PDF 简历
- 上传 DOCX 简历
- 校验文件类型和大小
- 保存文件元信息
- 解析简历文本
- 查询当前用户的简历列表
- 删除简历

文件解析逻辑必须独立封装为 `ResumeParser` 或类似组件。PDF 使用 Apache PDFBox，DOCX 使用 Apache POI。解析失败时应返回明确错误，不应吞掉异常。

简历文件和解析后的文本都属于敏感数据。查询简历时必须带上当前登录用户 ID，禁止用户访问他人的简历。

### job

负责岗位 JD 的创建和管理。

主要能力：

- 创建岗位 JD
- 查询岗位 JD 列表
- 查询岗位 JD 详情
- 删除岗位 JD

岗位 JD 至少包含岗位名称、公司名称、岗位描述文本和创建时间。公司名称可以为空。

### analysis

负责简历分析任务，是项目最核心的后端模块。

主要能力：

- 创建简历分析任务
- 查询任务状态
- 查询分析结果
- 查询历史分析记录
- 缓存相同输入的分析结果
- 记录失败原因和重试次数

分析任务必须设计为异步任务。创建任务接口不应等待大模型完整返回，而是立即返回 `taskId`。后端使用线程池执行分析流程，前端通过轮询接口查询任务状态。

任务状态使用枚举：

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
FAILED 不自动回到 RUNNING，除非后续实现手动重试接口
```

### interview

负责模拟面试。

主要能力：

- 根据分析结果生成模拟面试题
- 创建面试场次
- 查询面试题列表
- 提交用户回答
- 调用 AI 评价回答
- 保存回答反馈

第一版可以只支持文本回答，不需要语音输入。面试题应根据简历内容、岗位 JD 和分析结果生成，而不是生成通用八股题。

### ai

负责大模型调用、Prompt 组装、结构化输出解析和调用日志记录。

主要能力：

- 封装 AI API 请求
- 管理 Prompt 模板
- 要求 AI 返回 JSON
- 解析 AI 返回结果
- 校验 JSON 字段完整性
- 失败重试
- 记录 token 用量和响应耗时，若模型接口支持

AI API Key 必须通过环境变量注入，不允许写入代码、配置文件或 README。

推荐配置：

```yaml
ai:
  provider: deepseek
  base-url: ${AI_BASE_URL}
  api-key: ${AI_API_KEY}
  model: ${AI_MODEL}
  timeout-seconds: 60
```

### security

负责安全相关的横切能力。

主要能力：

- JWT 过滤器
- 登录用户上下文
- 接口鉴权
- 密码加密
- CORS 配置
- 越权访问拦截

所有需要登录的接口都必须从安全上下文中获取当前用户 ID，不允许从前端传入 `userId` 后直接信任。

### common

存放通用组件。

建议包含：

- 统一响应体 `ApiResponse`
- 业务异常 `BusinessException`
- 错误码枚举 `ErrorCode`
- 全局异常处理器 `GlobalExceptionHandler`
- 分页返回对象
- 时间工具
- Hash 工具
- Redis Key 构造工具

不要把具体业务逻辑放入 `common`。

## 核心业务流程

### 简历分析流程

```text
用户上传简历
    -> 后端校验文件类型和大小
    -> 保存文件
    -> 解析简历文本
    -> 写入 resume 表

用户输入岗位 JD
    -> 写入 job_description 表

用户创建分析任务
    -> 后端计算 inputHash
    -> 查询 Redis 是否存在分析结果
    -> 命中缓存则创建 SUCCESS 任务并直接返回结果
    -> 未命中缓存则创建 PENDING 任务
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
    -> 根据策略重试
    -> 重试仍失败则任务状态改为 FAILED
    -> 保存 errorMessage
```

### 模拟面试流程

```text
用户选择一次分析结果
    -> 创建 interview_session
    -> 根据简历、JD、分析结果生成面试题
    -> 保存 interview_question
    -> 前端展示问题

用户提交回答
    -> 后端读取问题、简历摘要和岗位信息
    -> 调用 AI 生成评价
    -> 保存 answer 和 feedbackJson
    -> 返回评分、问题诊断和改进建议
```

## 数据库设计

第一版至少包含以下表：

| 表名 | 说明 |
|---|---|
| `user` | 用户表 |
| `resume` | 简历表 |
| `job_description` | 岗位 JD 表 |
| `analysis_task` | 简历分析任务表 |
| `interview_session` | 模拟面试场次表 |
| `interview_question` | 面试问题与回答表 |
| `ai_call_log` | AI 调用日志表 |

### user

核心字段：

- `id`
- `username`
- `email`
- `password_hash`
- `created_at`
- `updated_at`
- `deleted`

### resume

核心字段：

- `id`
- `user_id`
- `file_name`
- `file_type`
- `file_path`
- `file_size`
- `parsed_text`
- `created_at`
- `updated_at`
- `deleted`

### job_description

核心字段：

- `id`
- `user_id`
- `title`
- `company_name`
- `content`
- `created_at`
- `updated_at`
- `deleted`

### analysis_task

核心字段：

- `id`
- `user_id`
- `resume_id`
- `jd_id`
- `input_hash`
- `status`
- `score`
- `result_json`
- `error_message`
- `retry_count`
- `started_at`
- `finished_at`
- `created_at`
- `updated_at`
- `deleted`

### interview_session

核心字段：

- `id`
- `user_id`
- `analysis_task_id`
- `title`
- `created_at`
- `updated_at`
- `deleted`

### interview_question

核心字段：

- `id`
- `session_id`
- `question_type`
- `question`
- `reference_points`
- `answer`
- `feedback_json`
- `score`
- `created_at`
- `updated_at`
- `deleted`

### ai_call_log

核心字段：

- `id`
- `user_id`
- `biz_type`
- `biz_id`
- `provider`
- `model`
- `prompt_hash`
- `success`
- `error_message`
- `latency_ms`
- `prompt_tokens`
- `completion_tokens`
- `created_at`

## Redis 设计

Redis 主要用于缓存和限流。

推荐 Key：

```text
analysis:result:{inputHash}
rate:analysis:user:{userId}
rate:interview:user:{userId}
auth:blacklist:{tokenId}
```

说明：

| Key | 用途 |
|---|---|
| `analysis:result:{inputHash}` | 缓存简历和 JD 的分析结果 |
| `rate:analysis:user:{userId}` | 限制用户创建分析任务频率 |
| `rate:interview:user:{userId}` | 限制用户提交面试回答频率 |
| `auth:blacklist:{tokenId}` | 可选，用于登出后的 token 黑名单 |

分析结果缓存建议设置过期时间，例如 7 天。限流 Key 根据业务要求设置 60 秒或 1 小时过期时间。

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

后端必须把 AI 返回内容反序列化为明确的 Java 对象，再保存为 JSON 字符串。不允许直接把未校验的原始文本作为正式分析结果返回给前端。

## 主要接口

### 认证

| 方法 | 路径 | 说明 |
|---|---|---|
| `POST` | `/api/auth/register` | 注册 |
| `POST` | `/api/auth/login` | 登录 |
| `GET` | `/api/users/me` | 查询当前用户 |

### 简历

| 方法 | 路径 | 说明 |
|---|---|---|
| `POST` | `/api/resumes/upload` | 上传简历 |
| `GET` | `/api/resumes` | 查询简历列表 |
| `GET` | `/api/resumes/{id}` | 查询简历详情 |
| `DELETE` | `/api/resumes/{id}` | 删除简历 |

### 岗位 JD

| 方法 | 路径 | 说明 |
|---|---|---|
| `POST` | `/api/jobs` | 创建岗位 JD |
| `GET` | `/api/jobs` | 查询岗位 JD 列表 |
| `GET` | `/api/jobs/{id}` | 查询岗位 JD 详情 |
| `DELETE` | `/api/jobs/{id}` | 删除岗位 JD |

### 分析任务

| 方法 | 路径 | 说明 |
|---|---|---|
| `POST` | `/api/analysis/tasks` | 创建分析任务 |
| `GET` | `/api/analysis/tasks/{id}` | 查询任务状态和结果 |
| `GET` | `/api/analysis/tasks` | 查询历史分析任务 |

### 模拟面试

| 方法 | 路径 | 说明 |
|---|---|---|
| `POST` | `/api/interviews/sessions` | 创建面试场次 |
| `GET` | `/api/interviews/sessions/{id}` | 查询面试场次 |
| `POST` | `/api/interviews/questions/{id}/answer` | 提交回答并获取反馈 |

## 统一响应格式

所有接口使用统一响应格式：

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
  "code": 40001,
  "message": "未登录或登录已过期",
  "data": null
}
```

Controller 返回值不应直接暴露 Entity。必须通过 VO 返回前端需要的数据。

## 异常处理

必须实现全局异常处理器。

至少处理：

- 参数校验异常
- 业务异常
- 文件上传异常
- JWT 异常
- AI 调用异常
- 数据库异常
- 未知异常

未知异常返回通用错误信息，不应把堆栈、SQL、密钥、文件路径暴露给前端。

## 文件上传约束

简历上传规则：

- 只允许 PDF 和 DOCX
- 单文件大小建议限制为 5 MB
- 文件名必须重新生成，不能直接使用用户上传文件名作为存储文件名
- 文件路径不应暴露给前端
- 删除简历时，需要删除数据库记录，并尽量删除对应文件

推荐存储路径通过配置控制：

```yaml
vitaelens:
  upload-dir: ./uploads/resumes
```

部署环境可以改为：

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
- 后续如接入第三方模型，应在 README 中说明用户数据会被发送到模型服务商

## 限流策略

AI 相关接口必须有限流设计，因为它们涉及外部接口成本。

建议第一版规则：

| 接口 | 限制 |
|---|---|
| 创建分析任务 | 每个用户每分钟最多 3 次 |
| 创建面试场次 | 每个用户每分钟最多 3 次 |
| 提交面试回答 | 每个用户每分钟最多 5 次 |

限流可基于 Redis 计数器实现。超过限制时返回明确错误码和错误信息。

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

第一版页面控制在较小范围：

| 页面 | 说明 |
|---|---|
| 登录页 | 登录和注册 |
| 首页 | 项目介绍和入口 |
| 简历管理页 | 上传、查看、删除简历 |
| 岗位 JD 页 | 创建和管理目标岗位 |
| 分析任务页 | 创建分析任务，展示任务状态 |
| 分析结果页 | 展示评分、问题、建议和技能缺口 |
| 模拟面试页 | 展示问题、提交回答、查看反馈 |

前端不承担核心业务判断。鉴权、数据隔离、任务状态流转和限流必须由后端保证。

## 部署架构

部署目标是让项目可以通过 Docker Compose 一键启动。

推荐服务：

```text
nginx
frontend
backend
mysql
redis
```

请求路径：

```text
浏览器
  -> Nginx
  -> 前端静态页面
  -> /api 反向代理到 Spring Boot
  -> MySQL / Redis / AI Provider
```

生产环境中，AI API Key 通过环境变量传入。数据库密码、JWT 密钥也必须通过环境变量或 `.env` 文件传入，`.env` 不应提交到 Git。

## 开发优先级

### V1

目标是跑通主流程。

范围：

- 用户注册登录
- JWT 鉴权
- 简历上传
- PDF/DOCX 文本解析
- 岗位 JD 创建
- 同步或简化版 AI 分析
- 分析结果保存
- 历史记录查询

### V2

目标是体现后端工程能力。

范围：

- 异步分析任务
- 任务状态机
- Redis 缓存
- Redis 限流
- AI JSON 结构化输出校验
- 失败重试
- 全局异常处理
- Docker Compose 部署
- Knife4j 接口文档

### V3

目标是提升项目差异化。

范围：

- 模拟面试题生成
- 回答评价
- AI 调用日志
- Token 和耗时统计
- 简单管理端
- 简历优化前后版本对比
- 导出 Markdown 或 PDF 报告

如果时间不足，优先完成 V1 和 V2。V3 可以只做模拟面试的最小版本。

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
9. 不要把 V3 功能提前塞进 V1，避免项目失控。
10. 不要生成与本文件架构明显冲突的目录和模块。

## 推荐包名

推荐后端基础包名：

```text
com.vitaelens
```

如果项目名称后续变更，可以保留包名不变。包名稳定比展示名称更重要。

## 简历包装方向

项目完成后，简历中应突出后端工程能力，而不是只写“接入大模型”。

推荐描述方向：

```text
基于 Spring Boot 3 + Vue 3 开发 AI 简历优化与模拟面试系统，支持 PDF/DOCX 简历解析、岗位 JD 匹配、AI 评分、优化建议生成、模拟面试题生成与回答反馈。后端使用 MySQL 存储用户、简历、岗位与分析任务数据，设计异步任务状态机处理大模型调用，避免长时间阻塞主请求流程；基于 Redis 实现分析结果缓存与用户维度限流，降低重复调用成本并防止高频请求；通过 Prompt 模板和 JSON 结构化校验约束大模型输出，提升结果解析稳定性。
```

实际简历描述必须与最终完成内容一致。没有完成的功能不要写入简历。
