package com.phrolova.vitaelensbackend.controller;

import com.phrolova.vitaelensbackend.auth.UserContext;
import com.phrolova.vitaelensbackend.common.Result;
import com.phrolova.vitaelensbackend.dto.request.CreateAnalysisRequest;
import com.phrolova.vitaelensbackend.dto.response.TaskResponse;
import com.phrolova.vitaelensbackend.service.AnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    @PostMapping("/tasks")
    public Result<TaskResponse> createTask(@Valid @RequestBody CreateAnalysisRequest request) {
        Long userId = UserContext.getUserId();
        return Result.success(analysisService.createTask(request, userId));
    }

    @GetMapping("/tasks/{taskId}")
    public Result<TaskResponse> getTaskStatus(@PathVariable Long taskId) {
        Long userId = UserContext.getUserId();
        return Result.success(analysisService.getTaskStatus(taskId, userId));
    }

    @GetMapping("/tasks")
    public Result<List<TaskResponse>> listTasks() {
        Long userId = UserContext.getUserId();
        return Result.success(analysisService.listTasks(userId));
    }

}
