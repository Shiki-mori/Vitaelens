package com.phrolova.vitaelensbackend.common;
//错误码枚举

import lombok.Getter;

@Getter
public enum ErrorCode {
    SUCCESS(0, "success"),
    PARAM_ERROR(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    RATE_LIMITED(429, "请求过于频繁，请稍后再试"),
    AI_ERROR(5001, "AI 分析服务异常"),
    FILE_ERROR(5002, "文件处理异常"),
    SYSTEM_ERROR(5000, "系统内部错误");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
