><localhost:8080/swagger-ui/index.html>

使用springdoc

在`pom.xml`中添加`swagger`相关依赖:

```xml
<dependency>
    <groupId>com.github.xiaoymin</groupId>
    <artifactId>knife4j-spring-boot-starter</artifactId>
    <version>${knife4j}</version>
</dependency>
```

版本：

```xml
<properties>
    <java.version>17</java.version>
    <knife4j>3.0.2</knife4j>
</properties>
```

引入spring doc依赖：

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.9</version>
</dependency>
```

启用SpringDoc OpenAPI（Swagger UI）  
作用是生成openAPI文档。

新建一个swagger配置类：[SwaggerConfig.java](../vitaelens-backend/src/main/java/com/phrolova/vitaelensbackend/config/SwaggerConfig.java)。

在[SecurityConfig.java](../vitaelens-backend/src/main/java/com/phrolova/vitaelensbackend/config/SecurityConfig.java)中放行swagger-ui等资源。

启动spring boot后，  
Swagger UI:<localhost:8080/swagger-ui/index.html>  
OpenAPI JSON 原始数据:<localhost:8080/v3/api-docs>

访问Swagger UI界面，显示：

```text
Unable to render this definition The provided definition does not specify a valid version field.   

Please indicate a valid Swagger or OpenAPI version field. Supported version fields are swagger: "2.0" and those that match openapi: 3.x.y (for example, openapi: 3.1.0).
```

JSON数据返回：

```text
{"code":5000,"message":"系统内部错误","data":null}
```

说明`/v3/api-docs`没有进入SpringDoc，被编写的全局异常处理接管。  
但是swagger不会主动抛出系统错误，说明`/v3/api-docs`在执行过程中发生了异常，被`@RestControllerAdvice`捕获，被统一包装成`SYSTEM_ERROR(5000)`。

经过排查，knife4j-spring-boot-starter 3.0.2 是 旧版 Knife4j，它是为 Springfox（Swagger 2） 或早期 Spring Boot 设计的，不适用于 Spring Boot 3 + SpringDoc 2.x。

而现在使用 `Spring Boot 3.5.x + SpringDoc 2.5.0`。它们依赖新版的 swagger-core-jakarta。

一部分类来自旧版 Knife4j ，一部分类来自新版 SpringDoc。

最终运行时发生冲突。

删除knife4j相关依赖，仅保留springdoc，问题解决。

# 使用api注解

>Springdoc是Spring Boot下实现swagger API描述规范 的 自动生成工具。  

>Swagger UI 将生成的Open API文档 进行可视化。

SpringDoc 3.x 对应的注解：

```java
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
```

使用`@Tag`+`@Operation`。

## @Tag

Controller层使用 `@Tag` ，对模块分类。如：

```java
@RestController
@RequestMapping("/api/user")
@Tag(name = "用户模块", description = "用户相关接口")
public class UserController {
}
```

## @Operation

API方法使用`@Operation`，如：

```java
@Operation(
    summary = "创建用户",
    description = "根据用户名和密码创建新用户"
)
@PostMapping("/create")
public UserVO create(@RequestBody UserDTO dto) {
    return userService.create(dto);
}
```

`summary`：接口描述  
`description`：详细说明

## @Parameter

query/path/header/cookie/body参数使用`@Parameter`，如：

```java
@Operation(summary = "根据ID查询用户")
@GetMapping("/{id}")
public UserVO getUser(
        @Parameter(description = "用户ID") 
        @PathVariable Long id
) {
    return userService.getById(id);
}
```

## @Schema

字段使用`Schema`，如：

```java
@Data
public class UserDTO {

    @Schema(description = "用户名", example = "alice")
    private String username;

    @Schema(description = "密码", example = "123456")
    private String password;
}
```