# Redis 缓存

缓存对象：分析任务的结果。因为AI调用成本高，响应慢，相同简历和岗位的重复分析应直接命中缓存。

## 当前阶段目标

- [ ] Redis 配置和序列化
- [ ] 分析结果缓存读写
- [ ] 缓存过期策略
- [ ] 缓存异常降级（Redis 挂了不影响主流程）
- [ ] 测试

## Redis依赖与配置

确认 `pom.xml` 中存在 Redis 依赖。

### 配置 RedisTemplate

Spring Boot 默认提供 `SpringRedisTemplate` ，对字符串友好。但是需要存储 Java 对象。  
配置一个使用 JSON 序列化的 `RedisTemplate`。

[RedisConfig.java](../vitaelens-backend/src/main/java/com/phrolova/vitaelensbackend/config/RedisConfig.java)

### RedisConnectionFactory

Spring Data Redis 提供的 Redis 连接工厂。  
链路： java程序 -> RedisTemplate ->RedisConnectionFactory -> Redis服务器

### RedisTemplate

Spring 操作 Redis 的核心类。

### key和value的序列化

Redis 本质只能保存 byte[]。  
默认情况，RedisTemplate 使用 JdkSerializationRedisSerializer 序列化。如：`user:1`被显示为`\xAC\xED\x00\x05t\x00\x06user:1`。  
所以 key 通常使用 StringRedisSerializer。

## 写缓存服务

封装Redis操作。

[CacheService.java](../vitaelens-backend/src/main/java/com/phrolova/vitaelensbackend/service/CacheService.java)  
[CacheServiceImpl.java](../vitaelens-backend/src/main/java/com/phrolova/vitaelensbackend/service/impl/CacheServiceImpl.java)

## 修改 AnalysisService 集成缓存