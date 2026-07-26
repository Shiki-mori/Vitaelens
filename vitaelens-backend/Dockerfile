# syntax=docker/dockerfile:1
# 构建阶段
FROM eclipse-temurin:17-jdk-alpine AS builder

#LABEL authors="phrolova"

# 设置工作目录
WORKDIR /app

# 复制 Maven Wrapper 与项目描述文件
COPY pom.xml .
COPY mvnw .
COPY .mvn ./.mvn

# 给予执行权限
RUN chmod +x mvnw

# 依赖层：pom 不变时可复用；cache 跨构建保留 .m2
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw dependency:go-offline -B

# 复制源码并打包（-DskipTests 跳过测试）
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw clean package -DskipTests

# 运行阶段
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 创建上传目录
RUN mkdir -p /app/uploads/resumes

# 从构建阶段复制到 Jar
COPY --from=builder /app/target/*.jar app.jar

# 暴露端口
EXPOSE 8080

# 设置环境变量；JVM参数可配
ENV JAVA_OPTS="-Xms256m -Xmx512m"

# 启动命令
# java $JAVA_OPTS -jar app.jar
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
