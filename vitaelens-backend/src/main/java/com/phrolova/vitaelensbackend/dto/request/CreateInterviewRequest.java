package com.phrolova.vitaelensbackend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateInterviewRequest {
    @NotNull(message = "分析任务ID不能为空")
    private Long analysisTaskId;
}
