# 简历上传及解析功能测试用例

## 功能名称

简历上传及解析功能测试

## 测试范围

| 模块 | 路径 | 覆盖内容 |
|------|------|----------|
| Controller | `ResumeController` | 上传、列表、删除接口；参数校验；异常响应 |
| Service | `ResumeServiceImpl` | 文件校验、保存、解析、入库、列表、删除 |
| Mapper | `ResumeMapper` | CRUD、按 userId 查询、逻辑删除 |
| Util | `FileUtil` | 扩展名提取、UUID 文件名生成 |
| Parser | `PdfParser` / `DocxParser` | PDF/DOCX 文本提取 |

**不在本次范围：** JWT 签发逻辑（auth 模块）、AI 分析、岗位 JD。

## 测试环境

| 项目 | 版本/说明 |
|------|-----------|
| Java | 17 |
| Spring Boot | 3.5.15 |
| MyBatis-Plus | 3.5.9 |
| 单元/Controller 测试数据库 | 无（Mock Mapper） |
| Mapper 集成测试数据库 | H2 内存库（MySQL 兼容模式） |
| 生产数据库 | MySQL 8.0.46 |
| 测试框架 | JUnit 5、Mockito、Spring Boot Test、MockMvc |
| 文件解析 | Apache PDFBox 3.0.1、Apache POI 5.2.5 |

## 测试用例表

| 编号 | 测试场景 | 输入 | 预期结果 | 测试类型 |
|------|----------|------|----------|----------|
| TC-001 | 正常上传 PDF 简历 | 有效 PDF 文件，`Content-Type: application/pdf`，扩展名 `.pdf` | `code=0`，返回 `ResumeResponse`，含 `id`、`fileName`、`parsedText`、`textLength` | Service 单元 / Controller |
| TC-002 | 正常上传 Word 简历 | 有效 DOCX 文件，`Content-Type: application/vnd.openxmlformats-officedocument.wordprocessingml.document`，扩展名 `.docx` | `code=0`，解析文本非空 | Service 单元 / Controller |
| TC-003 | 上传空文件 | 0 字节 MultipartFile | 抛出 `BizException(PARAM_ERROR)`，HTTP 响应 `code=400` | Service / Controller |
| TC-004 | 上传非法格式文件 | `text/plain` 或扩展名 `.txt` | `code=400`，提示文件类型或扩展名错误 | Service / Controller |
| TC-005 | 上传超大文件 | 文件 `size > 10MB` | `code=400`，提示文件大小超限 | Service / Controller |
| TC-006 | 文件解析失败 | 扩展名与内容不匹配（如 `.pdf` 但内容为乱码） | `code=5002`（FILE_ERROR） | Service 单元 |
| TC-007 | 解析结果为空 | 纯图片 PDF（无可提取文本） | `code=5002`，提示无法提取文本 | Service 单元 |
| TC-008 | 数据库存储失败 | Mock `resumeMapper.insert` 抛出异常 | 异常向上传播，Controller 层 `code=5000` | Service / Controller |
| TC-009 | 未登录访问 | 无 JWT / `UserContext` 为空 | Spring Security 返回 401（集成场景）；Controller 单测通过 Mock 隔离 | Controller / 集成 |
| TC-010 | 重复上传 | 同一用户连续上传两次相同文件 | 两次均成功，生成不同 `id` 和存储文件名（当前无去重） | Service 单元 |
| TC-011 | 查询简历列表 | `userId=1`，库中有 2 条记录 | 返回 2 条，按 `createdAt` 降序 | Service / Mapper |
| TC-012 | 查询空列表 | `userId=999`，无记录 | 返回空列表 | Service 单元 |
| TC-013 | 删除存在的简历 | 有效 `resumeId` + 匹配 `userId` | 逻辑删除成功，物理文件删除 | Service 单元 |
| TC-014 | 删除他人/不存在简历 | 错误 `resumeId` 或 `userId` 不匹配 | `code=404`，提示简历不存在 | Service / Controller |
| TC-015 | FileUtil 生成文件名 | 输入 `resume.pdf` | 返回 `{uuid}.pdf` 格式 | Util 单元 |
| TC-016 | FileUtil 非法扩展名 | 输入 `resume.exe` | 抛出 `IllegalArgumentException` | Util 单元 |
| TC-017 | PdfParser 提取文本 | 含文字的最小 PDF | 返回非空 trim 后文本 | Parser 单元 |
| TC-018 | DocxParser 提取文本 | 含段落的最小 DOCX | 返回非空文本 | Parser 单元 |
| TC-019 | Mapper 插入与查询 | H2 插入 Resume 实体 | `selectById` 可查到，`userId` 索引查询正确 | Mapper 集成 |
| TC-020 | Mapper 逻辑删除 | `deleteById` 后 | `selectById` 查不到（`@TableLogic`） | Mapper 集成 |
| TC-021 | Controller 上传成功 | Mock Service 返回响应 | HTTP 200，`code=0` | Controller |
| TC-022 | Controller 业务异常 | Mock Service 抛 `BizException` | HTTP 200，`code` 为对应错误码 | Controller |
| TC-023 | Controller 缺少 file 参数 | 不传 `file` 字段 | HTTP 400 或参数错误响应 | Controller |

## 已知业务行为说明

1. **用户不存在：** 当前 `uploadResume` 不校验 `user` 表是否存在，`userId` 来自 `UserContext`。若 `userId` 为 `null`，仍会尝试入库（可能触发外键约束，取决于 DB 配置）。TC-009 主要覆盖鉴权层。
2. **重复上传：** 当前允许同一用户多次上传相同文件，每次生成新 UUID 文件名和新记录。
3. **BizException 自定义消息：** `GlobalExceptionHandler` 目前只返回 `ErrorCode` 枚举默认消息，不返回 `BizException` 的自定义 `message`。测试预期以实际 Handler 行为为准。
4. **文件大小限制：** 代码为 10MB（`MAX_FILE_SIZE = 10 * 1024 * 1024`），与 AGENTS.md 建议的 5MB 不一致，测试以代码为准。
