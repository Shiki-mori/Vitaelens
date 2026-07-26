package com.phrolova.vitaelensbackend.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@TableName(value = "interview_question", autoResultMap = true)
public class InterviewQuestion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;

    private String question;

    private String answer;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> feedbackJson;    // JSON 字段

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}