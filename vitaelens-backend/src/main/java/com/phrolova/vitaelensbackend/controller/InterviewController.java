package com.phrolova.vitaelensbackend.controller;

import com.phrolova.vitaelensbackend.auth.UserContext;
import com.phrolova.vitaelensbackend.common.LimitType;
import com.phrolova.vitaelensbackend.common.RateLimit;
import com.phrolova.vitaelensbackend.common.Result;
import com.phrolova.vitaelensbackend.dto.request.CreateInterviewRequest;
import com.phrolova.vitaelensbackend.dto.request.SubmitAnswerRequest;
import com.phrolova.vitaelensbackend.dto.response.InterviewSessionDetailResponse;
import com.phrolova.vitaelensbackend.dto.response.InterviewSessionResponse;
import com.phrolova.vitaelensbackend.service.InterviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    @PostMapping("/sessions")
    @RateLimit(limitType = LimitType.USER, message = "面试生成请求过于频繁，每分钟最多 3 次")
    public Result<InterviewSessionResponse> createSession(@Valid @RequestBody CreateInterviewRequest request) {
        Long userId = UserContext.getUserId();
        return Result.success(interviewService.createSession(request, userId));
    }

    @GetMapping("/sessions/{id}")
    public Result<InterviewSessionDetailResponse> getSessionDetail(@PathVariable("id") Long sessionId) {
        Long userId = UserContext.getUserId();
        return Result.success(interviewService.getSessionDetail(sessionId, userId));
    }

    @PostMapping("/questions/{id}/answer")
    @RateLimit(limitType = LimitType.USER, maxRequests = 10, message = "回答提交过于频繁，请稍后再试")
    public Result<InterviewSessionDetailResponse> submitAnswer(@PathVariable("id") Long id,
                                                               @Valid @RequestBody SubmitAnswerRequest request) {
        Long userId = UserContext.getUserId();
        request.setQuestionId(id);
        return Result.success(interviewService.submitAnswer(request, userId));
    }
}
