package com.phrolova.vitaelensbackend.controller;

import com.phrolova.vitaelensbackend.auth.UserContext;
import com.phrolova.vitaelensbackend.common.LimitType;
import com.phrolova.vitaelensbackend.common.RateLimit;
import com.phrolova.vitaelensbackend.common.Result;
import com.phrolova.vitaelensbackend.dto.response.ResumeResponse;
import com.phrolova.vitaelensbackend.service.ResumeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "简历模块", description = "简历管理相关接口")
@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    /**
     * Upload resume
     * @param file 简历文件
     * @return 简历上传响应参数
     */
    @Operation(summary = "简历上传")
    @PostMapping("/upload")
    @RateLimit(limitType = LimitType.USER, windowSeconds = 60, maxRequests = 5, message = "文件上传过于频繁，每分钟最多 5 次")
    public Result<ResumeResponse> upload(@RequestParam("file") MultipartFile file) {
        Long userId = UserContext.getUserId();
        ResumeResponse response = resumeService.uploadResume(file, userId);
        return Result.success(response);
    }

    /**
     * List resumes
     * @return 简历列表
     */
    @Operation(summary = "简历列表查询")
    @GetMapping
    public Result<List<ResumeResponse>> list() {
        Long userId = UserContext.getUserId();
        List<ResumeResponse> responses = resumeService.listResumes(userId);
        return Result.success(responses);
    }

    /**
     * Delete resume
     * @param id 简历ID
     */
    @Operation(summary = "简历删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        resumeService.deleteResume(id, userId);
        return Result.success();
    }
}
