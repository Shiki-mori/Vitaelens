package com.phrolova.vitaelensbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.phrolova.vitaelensbackend.ai.AiClient;
import com.phrolova.vitaelensbackend.ai.PromptTemplate;
import com.phrolova.vitaelensbackend.common.ErrorCode;
import com.phrolova.vitaelensbackend.dto.request.CreateInterviewRequest;
import com.phrolova.vitaelensbackend.dto.request.SubmitAnswerRequest;
import com.phrolova.vitaelensbackend.dto.response.InterviewSessionDetailResponse;
import com.phrolova.vitaelensbackend.dto.response.InterviewSessionResponse;
import com.phrolova.vitaelensbackend.dto.response.QuestionResponse;
import com.phrolova.vitaelensbackend.entity.*;
import com.phrolova.vitaelensbackend.exception.BizException;
import com.phrolova.vitaelensbackend.mapper.*;
import com.phrolova.vitaelensbackend.service.InterviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewServiceImpl implements InterviewService {

    private final InterviewSessionMapper sessionMapper;
    private final InterviewQuestionMapper questionMapper;
    private final AnalysisTaskMapper taskMapper;
    private final ResumeMapper resumeMapper;
    private final JobDescriptionMapper jobDescriptionMapper;
    private final AiClient aiClient;
    private final ObjectMapper objectMapper;

    /**
     * 创建面试场次
     * @param request 创建面试场次请求
     * @param userId  用户ID
     * @return 面试场次信息
     */
    @Override
    public InterviewSessionResponse createSession(CreateInterviewRequest request, Long userId) {
        // 校验分析任务存在且完成
        AnalysisTask task = getTaskOrThrow(request.getAnalysisTaskId(), userId);
        if(!"SUCCESS".equals(task.getStatus())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "分析任务未完成，无法生成面试题");
        }

        // 获取简历和JD
        Resume resume = resumeMapper.selectById(task.getResumeId());
        JobDescription job = jobDescriptionMapper.selectById(task.getJdId());

        // 创建面试场次
        InterviewSession session = new InterviewSession();
        session.setUserId(userId);
        session.setAnalysisTaskId(task.getId());
        sessionMapper.insert(session);

        // 调用 AI 生成面试题
        String systemPrompt = PromptTemplate.getInterviewQuestionPrompt();
        String userMessage = PromptTemplate.buildInterviewQuestionMessage(
                resume.getParsedText(),
                job.getContent(),
                task.getResultJson()
        );

        String aiResponse = aiClient.chatJson(systemPrompt, userMessage);

        List<Map<String, String>> questions;
        try {
            questions = objectMapper.readValue(aiResponse, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("面试题 JSON 解析失败：{}", aiResponse);
            throw new BizException(ErrorCode.AI_ERROR, "面试题生成失败，请重试");
        }

        // 保存问题
        for (Map<String, String> q : questions) {
            InterviewQuestion question = new InterviewQuestion();
            question.setSessionId(session.getId());
            question.setQuestion(q.get("question"));
            questionMapper.insert(question);
        }

        log.info("面试场次创建成功：sessionId={}, 题目数={}", session.getId(), questions.size());

        return toSessionResponse(session, questions.size());
    }

    /**
     * 获取面试场次详情
     * @param sessionId  面试场次ID
     * @param userId  用户 ID
     * @return  面试场次详情
     */
    @Override
    public InterviewSessionDetailResponse getSessionDetail(Long sessionId, Long userId) {
        InterviewSession session = getSessionOrThrow(sessionId, userId);
        return buildDetailResponse(session);
    }

    /**
     * @param request  回答评价请求
     * @param userId  用户ID
     * @return 面试场次详情
     */
    @Override
    public InterviewSessionDetailResponse submitAnswer(SubmitAnswerRequest request, Long userId) {
        InterviewQuestion question = getQuestionOrThrow(request.getQuestionId());

        // 调用 AI 评价回答
        String systemPrompt = PromptTemplate.getAnswerFeedbackPrompt();
        String userMessage = PromptTemplate.buildAnswerFeedbackMessage(
                question.getQuestion(),
                request.getAnswer()
        );
        String aiResponse = aiClient.chatJson(systemPrompt, userMessage);

        Map<String, Object> feedback;
        try {
            feedback = objectMapper.readValue(aiResponse, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("回答评价 JSON 解析失败：{}", aiResponse);
            throw new BizException(ErrorCode.AI_ERROR, "回答评价失败，请重试");
        }

        // 保存回答和反馈
        question.setAnswer(request.getAnswer());
        question.setFeedbackJson(feedback);
        questionMapper.updateById(question);

        log.info("回答评价完成：questionId={}", request.getQuestionId());

        // 返回场次详情
        InterviewSession session = sessionMapper.selectById(question.getSessionId());
        return buildDetailResponse(session);
    }

    /**
     * 构建面试场次详情响应
     * @param session  面试场次
     * @return  面试场次详情响应
     */
    private InterviewSessionDetailResponse buildDetailResponse(InterviewSession session) {
        LambdaQueryWrapper<InterviewQuestion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewQuestion::getSessionId, session.getId())
                .orderByAsc(InterviewQuestion::getId);
        List<InterviewQuestion> questions = questionMapper.selectList(wrapper);

        List<QuestionResponse> questionResponses = questions.stream()
                .map(q -> {
                    QuestionResponse r = new QuestionResponse();
                    r.setId(q.getId());
                    r.setQuestion(q.getQuestion());
                    r.setAnswer(q.getAnswer());
                    r.setFeedbackJson(q.getFeedbackJson());
                    return r;
                }).toList();

        InterviewSessionDetailResponse response = new InterviewSessionDetailResponse();
        response.setId(session.getId());
        response.setAnalysisTaskId(session.getAnalysisTaskId());
        response.setCreatedAt(session.getCreatedAt());
        response.setQuestions(questionResponses);

        return response;
    }

    private AnalysisTask getTaskOrThrow(Long taskId, Long userId) {
        AnalysisTask task = taskMapper.selectById(taskId);
        if(task == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "分析任务不存在");
        }
        if(!task.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权限访问");
        }
        return task;
    }

    private InterviewSession getSessionOrThrow(Long sessionId, Long userId) {
        InterviewSession session = sessionMapper.selectById(sessionId);
        if(session == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "面试场次不存在");
        }
        if(!session.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权限访问");
        }
        return session;
    }

    private InterviewQuestion getQuestionOrThrow(Long questionId) {
        InterviewQuestion question = questionMapper.selectById(questionId);
        if(question == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "面试题不存在");
        }
        return question;
    }

    private InterviewSessionResponse toSessionResponse(InterviewSession session, int questionCount) {
        InterviewSessionResponse response = new InterviewSessionResponse();
        response.setId(session.getId());
        response.setAnalysisTaskId(session.getAnalysisTaskId());
        response.setQuestionCount(questionCount);
        response.setCreatedAt(session.getCreatedAt());
        return response;
    }
}
