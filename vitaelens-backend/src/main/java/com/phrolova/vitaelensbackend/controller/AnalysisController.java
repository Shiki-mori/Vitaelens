package com.phrolova.vitaelensbackend.controller;

import com.phrolova.vitaelensbackend.auth.UserContext;
import com.phrolova.vitaelensbackend.common.LimitType;
import com.phrolova.vitaelensbackend.common.RateLimit;
import com.phrolova.vitaelensbackend.common.Result;
import com.phrolova.vitaelensbackend.dto.request.CreateAnalysisRequest;
import com.phrolova.vitaelensbackend.dto.response.TaskResponse;
import com.phrolova.vitaelensbackend.service.AnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "AI 分析模块", description = "AI 分析相关接口")
@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    /**
     * 创建任务
     *
     * @param request 创建任务请求参数
     * @return 创建响应参数
     */
    @Operation(summary = "创建分析任务")
    @PostMapping("/tasks")
    @RateLimit(limitType = LimitType.USER, windowSeconds = 60, maxRequests = 3, message = "简历分析请求过于频繁，每分钟最多 3 次")
    public Result<TaskResponse> createTask(@Valid @RequestBody CreateAnalysisRequest request) {
        Long userId = UserContext.getUserId();
        return Result.success(analysisService.createTask(request, userId));
    }

    /**
     * 获取任务状态
     *
     * @param taskId 任务ID
     * @return 任务状态响应参数
     */
    @Operation(summary = "获取任务状态")
    @GetMapping("/tasks/{taskId}")
    public Result<TaskResponse> getTaskStatus(@PathVariable Long taskId) {
        Long userId = UserContext.getUserId();
        return Result.success(analysisService.getTaskStatus(taskId, userId));
    }

    /**
     * 获取任务列表
     *
     * @return 任务列表响应参数
     */
    @Operation(summary = "获取任务列表")
    @GetMapping("/tasks")
    public Result<List<TaskResponse>> listTasks() {
        Long userId = UserContext.getUserId();
        return Result.success(analysisService.listTasks(userId));
    }

}
