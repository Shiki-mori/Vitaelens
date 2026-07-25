package com.phrolova.vitaelensbackend.config;

import com.phrolova.vitaelensbackend.auth.UserContext;
import com.phrolova.vitaelensbackend.common.ErrorCode;
import com.phrolova.vitaelensbackend.common.LimitType;
import com.phrolova.vitaelensbackend.common.RateLimit;
import com.phrolova.vitaelensbackend.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final StringRedisTemplate stringRedisTemplate;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint point, RateLimit rateLimit) throws Throwable {
        String key = buildKey(rateLimit.limitType());

        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count == null) {
            // Redis 异常，放行
            return point.proceed();
        }

        if (count == 1) {
            stringRedisTemplate.expire(key, rateLimit.windowSeconds(), TimeUnit.SECONDS);
        }

        if (count > rateLimit.maxRequests()) {
            log.warn("限流触发：key={}, count={}, limit={}",
                    key, count, rateLimit.maxRequests());
            throw new BizException(ErrorCode.RATE_LIMITED, rateLimit.message());
        }

        return point.proceed();
    }

    private String buildKey(LimitType limitType) {
        String identifier;
        if (limitType == LimitType.USER) {
            Long userId = UserContext.getUserId();
            if (userId == null) {
                // 未登录用户按IP限流
                identifier = "ip: " + getClientIp();
            } else {
                identifier = "user: " + userId;
            }
        } else {
            identifier = "ip: " + getClientIp();
        }
        return "rate_limit:" + identifier + ":" + getMethodKey();
    }

    private String getClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null)
            return "unknown";

        HttpServletRequest request = attributes.getRequest();
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private String getMethodKey() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null)
            return "unknown";

        HttpServletRequest request = attributes.getRequest();
        return request.getMethod() + ":" + request.getRequestURI();
    }
}
