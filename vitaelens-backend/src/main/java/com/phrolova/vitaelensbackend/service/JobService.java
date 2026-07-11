package com.phrolova.vitaelensbackend.service;

import com.phrolova.vitaelensbackend.dto.request.CreateJobRequest;
import com.phrolova.vitaelensbackend.dto.response.JobResponse;

import java.util.List;

public interface JobService {
    JobResponse createJob(CreateJobRequest request, Long userId);
    List<JobResponse> listJobs(Long userId);
    void deleteJob(Long jobId, Long userId);
}
