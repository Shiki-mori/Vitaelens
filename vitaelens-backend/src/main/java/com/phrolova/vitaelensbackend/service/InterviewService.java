package com.phrolova.vitaelensbackend.service;

import com.phrolova.vitaelensbackend.dto.request.CreateInterviewRequest;
import com.phrolova.vitaelensbackend.dto.request.SubmitAnswerRequest;
import com.phrolova.vitaelensbackend.dto.response.InterviewSessionDetailResponse;
import com.phrolova.vitaelensbackend.dto.response.InterviewSessionResponse;

public interface InterviewService {
    InterviewSessionResponse createSession(CreateInterviewRequest request, Long userId);
    InterviewSessionDetailResponse getSessionDetail(Long sessionId, Long userId);
    InterviewSessionDetailResponse submitAnswer(SubmitAnswerRequest request, Long userId);
}
