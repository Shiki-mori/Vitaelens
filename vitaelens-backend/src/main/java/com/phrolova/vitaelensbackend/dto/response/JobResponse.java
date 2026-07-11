package com.phrolova.vitaelensbackend.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class JobResponse {
    private Long id;
    private String title;
    private String content;
    private LocalDateTime createdAt;
}
