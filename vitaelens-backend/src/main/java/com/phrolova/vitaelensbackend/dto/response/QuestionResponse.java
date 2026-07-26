package com.phrolova.vitaelensbackend.dto.response;

import lombok.Data;

import java.util.Map;

@Data
public class QuestionResponse {
    private Long id;
    private String question;
    private String answer;
    private Map<String, Object> feedbackJson;
}
