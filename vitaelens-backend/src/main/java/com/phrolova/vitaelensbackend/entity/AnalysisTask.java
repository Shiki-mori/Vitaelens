package com.phrolova.vitaelensbackend.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@TableName(value = "analysis_task", autoResultMap = true)
public class AnalysisTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long resumeId;

    private Long jdId;

    private String inputHash;

    private String status;          // PENDING, PROCESSING, SUCCESS, FAILED 等

    private Integer score;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> resultJson;      // JSON 字段，存储为字符串

    private String errorMessage;

    private Integer retryCount;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}