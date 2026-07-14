package com.phrolova.vitaelensbackend.service.impl;

import com.phrolova.vitaelensbackend.service.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheServiceImpl implements CacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String ANALYSIS_RESULT_PREFIX = "analysis:result:";
    // 设置缓存的数据在存储后 7天 过期失效
    private static final Duration CACHE_TTL = Duration.ofDays(7);

    @Override
    public void setAnalysisResult(String inputHash, Object result) {
        try {
            String key = ANALYSIS_RESULT_PREFIX + inputHash;
            redisTemplate.opsForValue().set(key, result, CACHE_TTL);
            log.info("缓存分析结果：hash={}, ttl={} days", inputHash, CACHE_TTL.toDays());
        }catch (Exception e) {
            log.warn("Redis 缓存写入失败，降级处理：{}", e.getMessage());
        }
    }

    @Override
    public Object getAnalysisResult(String inputHash) {
        try {
            String key = ANALYSIS_RESULT_PREFIX + inputHash;
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("Redis 缓存读取失败，降级处理：{}", e.getMessage());
            return null;
        }
    }

    @Override
    public void deleteAnalysisResult(String inputHash) {
        try {
            String key = ANALYSIS_RESULT_PREFIX + inputHash;
            redisTemplate.delete(key);
            log.info("删除缓存： hash={}", inputHash);
        } catch (Exception e) {
            log.warn("Redis 缓存删除失败，降级处理：{}", e.getMessage());
        }
    }
}
