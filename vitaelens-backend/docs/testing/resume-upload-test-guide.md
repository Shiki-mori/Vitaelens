# 简历上传及解析功能 — 测试说明

## 1. 如何运行测试

### IDEA 中运行

1. 打开 Maven 面板，展开 `vitaelens-backend` → `Lifecycle`，双击 `test` 运行全部测试。
2. 运行单个测试类：打开测试文件（如 `ResumeControllerTest.java`），点击类名或方法左侧的绿色运行按钮。
3. 运行 resume 模块全部测试：在项目视图中右键 `src/test/java/com/phrolova/vitaelensbackend`，选择 **Run Tests in ...**。

### Maven 命令

在项目根目录下的 `vitaelens-backend` 模块执行：

```bash
# 运行全部测试
mvn test

# 仅运行简历相关测试
mvn test -Dtest="Resume*Test,FileUtilTest,PdfParserTest,DocxParserTest"

# 运行单个测试类
mvn test -Dtest=ResumeServiceImplTest

# 运行单个测试方法
mvn test -Dtest=ResumeServiceImplTest#uploadResume_pdf_success
```

## 2. 如何准备测试环境

### 单元测试 / Controller 测试（默认）

- **无需 MySQL、Redis**，不启动完整 Spring 容器（Controller 使用 `@WebMvcTest` 切片，Service 使用纯 Mockito）。
- **无需手动准备上传目录**，Service 测试使用 JUnit `@TempDir` 临时目录。
- **无需真实简历文件**，测试工具类 `ResumeTestFixtures` 在运行时动态生成最小 PDF/DOCX。

### Mapper 集成测试

- 使用 **H2 内存数据库**（`test` profile），无需安装 MySQL。
- 配置文件：`src/test/resources/application-test.yml`
- 建表脚本：`src/test/resources/sql/schema-h2.sql`（仅 `resume` 表）
- 测试类标注 `@ActiveProfiles("test")` 和 `@Transactional`，每条测试结束后自动回滚。

### 配置文件说明

| 文件 | 作用 |
|------|------|
| `src/test/resources/application-test.yml` | 测试专用：H2 数据源、禁用 Docker Compose / Redis / Security 自动配置 |
| `src/main/resources/application.yml` | 主配置，单元测试不加载 |

**为何需要 `application-test.yml`：** 主配置依赖 Docker Compose 启动 MySQL，不适合自动化测试。H2 内存库使 Mapper 测试可独立运行。

### 测试文件准备

测试资源由 `ResumeTestFixtures` 程序化生成，位于：

```
src/test/java/.../support/ResumeTestFixtures.java
```

如需使用真实简历样本，可放入：

```
src/test/resources/fixtures/sample-resume.pdf
src/test/resources/fixtures/sample-resume.docx
```

并在测试中通过 `ClassPathResource` 加载。

## 3. 如何查看测试结果

### Maven 输出

```
[INFO] Tests run: 30, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### IDEA

- **Run** 窗口显示每个测试方法绿/红状态。
- 失败时可展开查看断言差异和堆栈。

### Surefire 报告

```
vitaelens-backend/target/surefire-reports/
```

每个测试类对应一个 XML 和 TXT 报告。

## 4. 如何新增测试案例

### 新增 Service 层用例

1. 打开 `ResumeServiceImplTest.java`。
2. 添加 `@Test` 方法，命名建议：`方法名_场景_预期`（如 `uploadResume_emptyFile_throwsBizException`）。
3. 使用 `@TempDir Path tempDir` 提供隔离的上传目录。
4. Mock `ResumeMapper` 的返回值或异常。
5. 使用 `ResumeTestFixtures` 构造 `MockMultipartFile`。

示例：

```java
@Test
void uploadResume_invalidExtension_throwsBizException(@TempDir Path tempDir) {
    ResumeServiceImpl service = createService(tempDir);
    MockMultipartFile file = ResumeTestFixtures.invalidExtensionFile();

    BizException ex = assertThrows(BizException.class,
            () -> service.uploadResume(file, 1L));

    assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
    verify(resumeMapper, never()).insert(any());
}
```

### 新增 Controller 层用例

1. 打开 `ResumeControllerTest.java`。
2. 在 `@BeforeEach` 中已通过 `UserContext.setUserId(1L)` 模拟登录。
3. Mock `ResumeService` 行为，用 `MockMvc` 发起请求并断言 JSON。

### 新增 Mapper 层用例

1. 打开 `ResumeMapperTest.java`。
2. 确保类上有 `@ActiveProfiles("test")` 和 `@Transactional`。
3. 直接调用 `resumeMapper.insert/selectList/deleteById` 并断言。

### 新增 Parser/Util 用例

1. 在对应 `*Test.java` 中添加测试。
2. Parser 测试可使用 `@TempDir` 写入临时文件后调用静态方法。

## 5. 测试分层建议

| 层级 | 注解 | 依赖 Mock | 速度 |
|------|------|-----------|------|
| Util/Parser | 无 / `@TempDir` | 无 | 最快 |
| Service | `@ExtendWith(MockitoExtension.class)` | Mapper | 快 |
| Controller | `@WebMvcTest` | Service | 快 |
| Mapper | `@SpringBootTest` + `@ActiveProfiles("test")` | H2 内存库 | 中等 |

优先保持 Service 和 Controller 测试不依赖外部基础设施，Mapper 测试仅在验证持久化行为时使用 H2。

## 6. 可测试性改进建议（不修改业务代码的前提下供后续参考）

| 问题 | 影响 | 建议 |
|------|------|------|
| `PdfParser`/`DocxParser`/`FileUtil` 为静态方法 | Service 层难以 Mock 解析失败场景 | 抽取 `ResumeParser` 接口，注入实现类 |
| `GlobalExceptionHandler` 忽略自定义 message | 前端无法拿到「文件不能为空」等细节 | Handler 中优先使用 `e.getMessage()` |
| `uploadResume` 不校验用户存在 | 「用户不存在」场景无法测 | 上传前查询 `UserMapper` |
| 文件写入与 DB 插入无补偿 | 解析或入库失败可能残留文件 | 失败时删除已保存文件 |
