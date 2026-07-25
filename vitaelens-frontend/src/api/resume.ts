import { request } from '@/utils/request'
import type { ResumeResponse } from '@/types/api'

export function listResumes() {
  return request<ResumeResponse[]>({
    url: '/resumes',
    method: 'get',
  })
}

export function uploadResume(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request<ResumeResponse>({
    url: '/resumes/upload',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function deleteResume(id: number) {
  return request<null>({
    url: `/resumes/${id}`,
    method: 'delete',
  })
}
