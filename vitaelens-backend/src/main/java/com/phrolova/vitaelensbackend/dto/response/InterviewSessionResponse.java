package com.phrolova.vitaelensbackend.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InterviewSessionResponse {
    private Long id;
    private Long analysisTaskId;
    private Integer questionCount;
    private LocalDateTime createdAt;
}
