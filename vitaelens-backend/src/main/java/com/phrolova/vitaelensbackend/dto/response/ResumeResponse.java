package com.phrolova.vitaelensbackend.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ResumeResponse {
    private Long id;
    private String fileName;
    private String parsedText;
    private Integer textLength;
    private LocalDateTime createdAt;
}
