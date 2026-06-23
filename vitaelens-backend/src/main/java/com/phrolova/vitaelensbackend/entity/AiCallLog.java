package com.phrolova.vitaelensbackend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_call_log")
public class AiCallLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private String module;          // resume_analysis / interview_question / answer_feedback

    private Integer promptTokens;

    private Integer completionTokens;

    private Integer totalTokens;

    private Integer durationMs;

    private String status;          // SUCCESS / FAILED

    private String errorMessage;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}