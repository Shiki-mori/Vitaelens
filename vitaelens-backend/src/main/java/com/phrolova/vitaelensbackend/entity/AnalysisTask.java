package com.phrolova.vitaelensbackend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("analysis_task")
public class AnalysisTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long resumeId;

    private Long jdId;

    private String inputHash;

    private String status;          // PENDING, PROCESSING, SUCCESS, FAILED 等

    private Integer score;

    private String resultJson;      // JSON 字段，存储为字符串

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