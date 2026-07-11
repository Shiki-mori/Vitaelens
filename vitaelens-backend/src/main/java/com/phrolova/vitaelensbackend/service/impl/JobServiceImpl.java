package com.phrolova.vitaelensbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.phrolova.vitaelensbackend.common.ErrorCode;
import com.phrolova.vitaelensbackend.dto.request.CreateJobRequest;
import com.phrolova.vitaelensbackend.dto.response.JobResponse;
import com.phrolova.vitaelensbackend.entity.JobDescription;
import com.phrolova.vitaelensbackend.exception.BizException;
import com.phrolova.vitaelensbackend.mapper.JobDescriptionMapper;
import com.phrolova.vitaelensbackend.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {

    private final JobDescriptionMapper jobMapper;

    /**
     * 新建岗位描述
     * @param request 新建岗位描述请求
     * @param userId 用户ID
     */
    @Override
    public JobResponse createJob(CreateJobRequest request, Long userId) {
        JobDescription job = new JobDescription();
        job.setUserId(userId);
        job.setTitle(request.getTitle());
        job.setContent(request.getContent());
        jobMapper.insert(job);
        return toResponse(job);
    }

    /**
     * 根据用户ID获取其所有岗位，按时间顺序倒序返回
     * @param userId 用户ID
     */
    @Override
    public List<JobResponse> listJobs(Long userId) {
        LambdaQueryWrapper<JobDescription> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JobDescription::getUserId, userId)
                .orderByDesc(JobDescription::getCreatedAt);

        return jobMapper.selectList(wrapper).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 删除岗位描述
     * @param jobId 岗位ID
     * @param userId 用户ID
     */
    @Override
    public void deleteJob(Long jobId, Long userId) {
        LambdaQueryWrapper<JobDescription> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JobDescription::getId, jobId)
                .eq(JobDescription::getUserId, userId);
        JobDescription job = jobMapper.selectOne(wrapper);
        // 检查岗位是否存在
        if (job == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "岗位不存在");
        }
        jobMapper.deleteById(jobId);
    }

    private JobResponse toResponse(JobDescription job) {
        JobResponse response = new JobResponse();
        response.setId(job.getId());
        response.setTitle(job.getTitle());
        response.setContent(job.getContent());
        response.setCreatedAt(job.getCreatedAt());
        return response;
    }
}
