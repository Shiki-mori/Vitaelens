import { request } from '@/utils/request'
import type { CreateJobRequest, JobResponse } from '@/types/api'

export function listJobs() {
  return request<JobResponse[]>({
    url: '/jobs',
    method: 'get',
  })
}

export function createJob(data: CreateJobRequest) {
  return request<JobResponse>({
    url: '/jobs',
    method: 'post',
    data,
  })
}

export function deleteJob(id: number) {
  return request<null>({
    url: `/jobs/${id}`,
    method: 'delete',
  })
}
