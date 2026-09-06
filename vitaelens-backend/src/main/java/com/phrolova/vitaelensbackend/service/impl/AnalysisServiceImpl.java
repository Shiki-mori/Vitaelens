package com.phrolova.vitaelensbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.phrolova.vitaelensbackend.ai.AiClient;
import com.phrolova.vitaelensbackend.ai.PromptTemplate;
import com.phrolova.vitaelensbackend.common.ErrorCode;
import com.phrolova.vitaelensbackend.dto.request.CreateAnalysisRequest;
import com.phrolova.vitaelensbackend.dto.response.TaskResponse;
import com.phrolova.vitaelensbackend.entity.AiCallLog;
import com.phrolova.vitaelensbackend.entity.AnalysisTask;
import com.phrolova.vitaelensbackend.entity.JobDescription;
import com.phrolova.vitaelensbackend.entity.Resume;
import com.phrolova.vitaelensbackend.exception.BizException;
import com.phrolova.vitaelensbackend.mapper.AiCallLogMapper;
import com.phrolova.vitaelensbackend.mapper.AnalysisTaskMapper;
import com.phrolova.vitaelensbackend.mapper.JobDescriptionMapper;
import com.phrolova.vitaelensbackend.mapper.ResumeMapper;
import com.phrolova.vitaelensbackend.service.AnalysisService;
import com.phrolova.vitaelensbackend.service.CacheService;
import com.phrolova.vitaelensbackend.util.HashUtil;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisServiceImpl implements AnalysisService {

    private final AnalysisTaskMapper taskMapper;
    private final ResumeMapper resumeMapper;
    private final JobDescriptionMapper jobMapper;
    private final AiCallLogMapper aiCallLogMapper;
    private final AiClient aiClient;
    private final ObjectMapper objectMapper;

    @Autowired
    private CacheService cacheService;

    @Override
    public TaskResponse createTask(CreateAnalysisRequest request, Long userId) {

        // 校验简历存在
        Resume resume = getResumeOrThrow(request.getResumeId(), userId);

        // 校验jd存在
        JobDescription job = getJobOrThrow(request.getJdId(), userId);

        // 计算 hash ，用于缓存判断
        String inputHash = HashUtil.md5(resume.getParsedText() + "|" + job.getContent());

        // 先查Redis缓存
        Object cachedResult = cacheService.getAnalysisResult(inputHash);
        if (cachedResult != null) {
            log.info("Redis 缓存命中：userId={}, hash={}", userId, inputHash);
            return convertCachedToResponse(cachedResult, resume.getId(), job.getId());
        }

        // 检查数据库：是否有相同输入的成功任务
        LambdaQueryWrapper<AnalysisTask> cacheCheck = new LambdaQueryWrapper<>();
        cacheCheck.eq(AnalysisTask::getUserId, userId)
                .eq(AnalysisTask::getInputHash, inputHash)
                .eq(AnalysisTask::getStatus, "SUCCESS")
                .orderByDesc(AnalysisTask::getCreatedAt)
                .last("LIMIT 1");
        AnalysisTask cachedTask = taskMapper.selectOne(cacheCheck);
        if (cachedTask != null) {
            log.info("数据库缓存命中：userId={}, taskId={}, hash={}", userId, cachedTask.getId(), inputHash);
            // 回填 Redis
            cacheService.setAnalysisResult(inputHash, cachedTask.getResultJson());
            return toTaskResponse(cachedTask);
        }

        // 创建新任务
        AnalysisTask task = new AnalysisTask();
        task.setUserId(userId);
        task.setResumeId(resume.getId());
        task.setJdId(job.getId());
        task.setInputHash(inputHash);
        task.setStatus("PENDING");
        task.setRetryCount(0);
        taskMapper.insert(task);

        // 异步执行分析
        executeAnalysisAsync(task.getId());

        return toTaskResponse(task);
    }

    // 标记该方法为异步执行方法。"analysisExecutor"指定要使用的线程池Bean名称
    @Async("analysisExecutor")
    private void executeAnalysisAsync(Long taskId) {
        AnalysisTask task = taskMapper.selectById(taskId);
        if (task == null)
            return;

        long startTime = System.currentTimeMillis();

        try {
            // 更新状态为 RUNNING
            task.setStatus("RUNNING");
            task.setStartedAt(LocalDateTime.now());
            taskMapper.updateById(task);

            // 获取简历和 JD
            Resume resume = resumeMapper.selectById(task.getResumeId());
            JobDescription job = jobMapper.selectById(task.getJdId());

            // 构建 Prompt
            String systemPrompt = PromptTemplate.getResumeAnalysisSystemPrompt();
            String userMessage = PromptTemplate.buildResumeAnalysisUserMessage(
                    resume.getParsedText(),
                    job.getContent()
            );

            // 调用 AI
            String aiResponse = aiClient.chatJson(systemPrompt, userMessage);

            // 解析 JSON
            Map<String, Object> result;
            try {
                result = objectMapper.readValue(aiResponse, new TypeReference<>() {
                });
            } catch (Exception parseEx) {
                log.error("分析结果 JSON 解析失败: taskId={}, contentLength={}",
                        taskId, aiResponse == null ? 0 : aiResponse.length());
                throw new BizException(ErrorCode.AI_ERROR, "AI 返回结果无法解析，请重试");
            }

            // 校验必要字段
            validateResult(result);

            // 更新数据库
            task.setStatus("SUCCESS");
            task.setResultJson(result);
            task.setScore(((Number) result.getOrDefault("overallScore", 0)).intValue());
            task.setFinishedAt(LocalDateTime.now());
            taskMapper.updateById(task);

            // 写入 Redis 缓存
            cacheService.setAnalysisResult(task.getInputHash(), result);

            // 记录调用日志
            saveAiCallLog(task.getId(), "resume_analysis", "SUCCESS", null,
                    (int) (System.currentTimeMillis() - startTime));

            log.info("分析任务完成: taskId={}, score={}", taskId, task.getScore());
        } catch (Exception e) {
            log.error("分析任务失败: taskId={}", taskId, e);
            task.setStatus("FAILED");
            task.setErrorMessage(e.getMessage());
            task.setRetryCount(task.getRetryCount() + 1);
            task.setFinishedAt(LocalDateTime.now());
            taskMapper.updateById(task);

            saveAiCallLog(task.getId(), "resume_analysis", "FAILED", e.getMessage(),
                    (int) (System.currentTimeMillis() - startTime));
        }
    }

    private void validateResult(Map<String, Object> result) {
        if (!result.containsKey("overallScore") || !result.containsKey("dimensionScores")) {
            throw new BizException(ErrorCode.AI_ERROR, "AI 返回结果格式不完整，请重试");
        }
    }

    @Override
    public TaskResponse getTaskStatus(Long taskId, Long userId) {
        AnalysisTask task = taskMapper.selectById(taskId);
        if (task == null || !task.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.NOT_FOUND, "任务不存在");
        }
        return toTaskResponse(task);
    }

    @Override
    public List<TaskResponse> listTasks(Long userId) {
        LambdaQueryWrapper<AnalysisTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AnalysisTask::getUserId, userId)
                .orderByDesc(AnalysisTask::getCreatedAt);
        return taskMapper.selectList(wrapper).stream()
                .map(this::toTaskResponse)
                .collect(Collectors.toList());
    }

    private Resume getResumeOrThrow(@NotNull(message = "Resume ID cannot be null") Long resumeId, Long userId) {
        LambdaQueryWrapper<Resume> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Resume::getId, resumeId)
                .eq(Resume::getUserId, userId);

        Resume resume = resumeMapper.selectOne(wrapper);
        if (resume == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "简历不存在");
        }
        return resume;
    }

    private JobDescription getJobOrThrow(@NotNull(message = "JD ID cannot be null") Long jdId, Long userId) {
        LambdaQueryWrapper<JobDescription> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JobDescription::getId, jdId)
                .eq(JobDescription::getUserId, userId);

        JobDescription job = jobMapper.selectOne(wrapper);
        if (job == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "岗位不存在");
        }
        return job;
    }

    private void saveAiCallLog(Long taskId, String module, String status, String errorMessage, int durationMs) {
        AiCallLog log = new AiCallLog();
        log.setTaskId(taskId);
        log.setModule(module);
        log.setStatus(status);
        log.setErrorMessage(errorMessage);
        log.setDurationMs(durationMs);
        aiCallLogMapper.insert(log);
    }

    /**
     * 将缓存中的结果转换为 TaskResponse
     * @param cachedResult 缓存中的结果
     * @param resumeId 简历ID
     * @param jdId 岗位ID
     * @return TaskResponse
     */
    private TaskResponse convertCachedToResponse(Object cachedResult, Long resumeId, Long jdId) {
        TaskResponse response = new TaskResponse();
        response.setResumeId(resumeId);
        response.setJdId(jdId);
        response.setStatus("SUCCESS");

        if (cachedResult instanceof Map<?, ?> map) {
            Map<String, Object> resultMap = objectMapper.convertValue(
                    map,
                    new TypeReference<>() {
                    }
            );
            response.setResultJson(resultMap);

            Object score = map.get("overallScore");
            if (score instanceof Number) {
                response.setScore(((Number) score).intValue());
            }
        }

        response.setCreatedAt(LocalDateTime.now());
        response.setFinishedAt(LocalDateTime.now());
        return response;
    }

    private TaskResponse toTaskResponse(AnalysisTask task) {
        TaskResponse response = new TaskResponse();
//        BeanUtils.copyProperties(task, response);
        response.setId(task.getId());
        response.setResumeId(task.getResumeId());
        response.setJdId(task.getJdId());
        response.setStatus(task.getStatus());
        response.setScore(task.getScore());
        response.setResultJson(task.getResultJson());
        response.setErrorMessage(task.getErrorMessage());
        response.setCreatedAt(task.getCreatedAt());
        response.setFinishedAt(task.getFinishedAt());
        return response;
    }
}
