import { request } from '@/utils/request'
import type {
  CreateInterviewRequest,
  InterviewSessionDetailResponse,
  InterviewSessionResponse,
} from '@/types/api'

/** 创建面试场次为同步 AI 调用，超时需更长 */
const AI_TIMEOUT_MS = 120000

export function createInterviewSession(data: CreateInterviewRequest) {
  return request<InterviewSessionResponse>({
    url: '/interviews/sessions',
    method: 'post',
    data,
    timeout: AI_TIMEOUT_MS,
  })
}

export function getInterviewSession(sessionId: number) {
  return request<InterviewSessionDetailResponse>({
    url: `/interviews/sessions/${sessionId}`,
    method: 'get',
  })
}

export function submitInterviewAnswer(questionId: number, answer: string) {
  return request<InterviewSessionDetailResponse>({
    url: `/interviews/questions/${questionId}/answer`,
    method: 'post',
    data: { questionId, answer },
    timeout: AI_TIMEOUT_MS,
  })
}
