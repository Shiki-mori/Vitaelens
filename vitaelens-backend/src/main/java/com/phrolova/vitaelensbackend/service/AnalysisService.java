package com.phrolova.vitaelensbackend.service;

import com.phrolova.vitaelensbackend.dto.request.CreateAnalysisRequest;
import com.phrolova.vitaelensbackend.dto.response.TaskResponse;

import java.util.List;

public interface AnalysisService {
    TaskResponse createTask(CreateAnalysisRequest request, Long userId);
    TaskResponse getTaskStatus(Long taskId, Long userId);
    List<TaskResponse> listTasks(Long userId);
}
