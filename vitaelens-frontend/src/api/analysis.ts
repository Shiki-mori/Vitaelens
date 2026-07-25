import { request } from '@/utils/request'
import type { CreateAnalysisRequest, TaskResponse } from '@/types/api'

export function createAnalysisTask(data: CreateAnalysisRequest) {
  return request<TaskResponse>({
    url: '/analysis/tasks',
    method: 'post',
    data,
  })
}

export function getAnalysisTask(taskId: number) {
  return request<TaskResponse>({
    url: `/analysis/tasks/${taskId}`,
    method: 'get',
  })
}

export function listAnalysisTasks() {
  return request<TaskResponse[]>({
    url: '/analysis/tasks',
    method: 'get',
  })
}
