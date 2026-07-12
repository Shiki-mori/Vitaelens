package com.phrolova.vitaelensbackend.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class TaskResponse {
    private Long id;
    private Long resumeId;
    private Long jdId;
    private String status;
    private Integer score;
    private Map<String, Object> resultJson;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime finishedAt;
}
