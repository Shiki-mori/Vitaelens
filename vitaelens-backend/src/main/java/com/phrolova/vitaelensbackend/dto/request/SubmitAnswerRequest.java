package com.phrolova.vitaelensbackend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubmitAnswerRequest {
    @NotNull(message = "问题ID不能为空")
    private Long questionId;

    @NotNull(message = "回答内容不能为空")
    private String answer;
}
