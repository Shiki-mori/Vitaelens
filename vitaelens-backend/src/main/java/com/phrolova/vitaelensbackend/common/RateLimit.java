package com.phrolova.vitaelensbackend.common;

import java.lang.annotation.*;

// 元注解
// 指定该注解只能作用于方法
@Target(ElementType.METHOD)
// 指定注解保留到运行时
@Retention(RetentionPolicy.RUNTIME)
// 该注解将出现在 JavaDoc 文档中
@Documented
public @interface RateLimit {

    /**
     * 限流维度
     */
    LimitType limitType() default LimitType.USER;

    /**
     * 时间窗口（秒）
     */
    int windowSeconds() default 60;

    /**
     * 窗口内最大请求次数
     */
    int maxRequests() default 3;

    /**
     * 限流提示信息
     */
    String message() default "请求过于频繁，请稍后再试";
}
