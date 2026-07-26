package com.phrolova.vitaelensbackend.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class InterviewSessionDetailResponse {
    private Long id;
    private Long analysisTaskId;
    private LocalDateTime createdAt;
    private List<QuestionResponse> questions;
}
