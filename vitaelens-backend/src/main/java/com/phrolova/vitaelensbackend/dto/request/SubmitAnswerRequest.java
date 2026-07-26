package com.phrolova.vitaelensbackend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubmitAnswerRequest {
    /** 由路径参数注入，请求体可不传 */
    private Long questionId;

    @NotNull(message = "回答内容不能为空")
    private String answer;
}
