package com.phrolova.vitaelensbackend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateAnalysisRequest {

    @NotNull(message = "Resume ID cannot be null")
    private Long resumeId;

    @NotNull(message = "JD ID cannot be null")
    private Long jdId;
}