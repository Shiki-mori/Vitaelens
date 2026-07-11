package com.phrolova.vitaelensbackend.controller;

import com.phrolova.vitaelensbackend.auth.UserContext;
import com.phrolova.vitaelensbackend.common.Result;
import com.phrolova.vitaelensbackend.dto.request.CreateJobRequest;
import com.phrolova.vitaelensbackend.dto.response.JobResponse;
import com.phrolova.vitaelensbackend.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @PostMapping
    public Result<JobResponse> create(@Valid @RequestBody CreateJobRequest request) {
        Long userId = UserContext.getUserId();
        return Result.success(jobService.createJob(request, userId));
    }

    @GetMapping
    public Result<List<JobResponse>> list() {
        Long userId = UserContext.getUserId();
        return Result.success(jobService.listJobs(userId));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        jobService.deleteJob(id, userId);
        return Result.success();
    }
}
