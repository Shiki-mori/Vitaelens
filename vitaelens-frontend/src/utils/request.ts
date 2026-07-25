import axios, { type AxiosInstance, type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { Result } from '@/types/api'
import { clearAuthStorage, getToken } from '@/utils/auth'

const service: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 60000,
})

service.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

let redirecting = false

async function handleUnauthorized(): Promise<void> {
  clearAuthStorage()
  if (redirecting) return
  const router = (await import('@/router')).default
  if (router.currentRoute.value.path === '/login') return
  redirecting = true
  try {
    await router.push({
      path: '/login',
      query: { redirect: router.currentRoute.value.fullPath },
    })
  } finally {
    redirecting = false
  }
}

service.interceptors.response.use(
  (response) => {
    const payload = response.data as Result<unknown>
    if (payload && typeof payload.code === 'number') {
      if (payload.code === 0) {
        return response
      }
      if (payload.code === 401) {
        ElMessage.error(payload.message || '未登录或登录已过期')
        void handleUnauthorized()
        return Promise.reject(new Error(payload.message))
      }
      ElMessage.error(payload.message || '请求失败')
      return Promise.reject(new Error(payload.message || '请求失败'))
    }
    return response
  },
  (error) => {
    const status = error.response?.status as number | undefined
    const message = error.response?.data?.message as string | undefined
    if (status === 401 || status === 403) {
      ElMessage.error(message || '未登录或登录已过期')
      void handleUnauthorized()
    } else if (status === 429) {
      ElMessage.error(message || '请求过于频繁，请稍后再试')
    } else {
      ElMessage.error(message || error.message || '网络异常')
    }
    return Promise.reject(error)
  },
)

export async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await service.request<Result<T>>(config)
  return response.data.data
}

export default service
