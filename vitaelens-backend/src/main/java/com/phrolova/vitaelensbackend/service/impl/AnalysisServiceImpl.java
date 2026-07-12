package com.phrolova.vitaelensbackend.service.impl;

import com.phrolova.vitaelensbackend.dto.request.CreateAnalysisRequest;
import com.phrolova.vitaelensbackend.dto.response.TaskResponse;
import com.phrolova.vitaelensbackend.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisServiceImpl implements AnalysisService {
    @Override
    public TaskResponse createTask(CreateAnalysisRequest request, Long userId) {
        return null;
    }

    @Override
    public TaskResponse getTaskStatus(Long taskId, Long userId) {
        return null;
    }

    @Override
    public List<TaskResponse> listTasks(Long userId) {
        return List.of();
    }
}
