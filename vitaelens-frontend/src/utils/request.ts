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
  // #region agent log
    fetch('http://127.0.0.1:7741/ingest/3ca358cf-e2c8-4ba3-9852-df7aac4b4abe',{method:'POST',headers:{'Content-Type':'application/json','X-Debug-Session-Id':'2cebf6'},body:JSON.stringify({sessionId:'2cebf6',runId:'post-fix',hypothesisId:'A',location:'request.ts:request',message:'outgoing request',data:{url:config.url,method:config.method,baseURL:config.baseURL,hasToken:!!token,isAuthPath:String(config.url||'').includes('/auth/')},timestamp:Date.now()})}).catch(()=>{});
  // #endregion
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
    // #region agent log
    fetch('http://127.0.0.1:7741/ingest/3ca358cf-e2c8-4ba3-9852-df7aac4b4abe',{method:'POST',headers:{'Content-Type':'application/json','X-Debug-Session-Id':'2cebf6'},body:JSON.stringify({sessionId:'2cebf6',runId:'post-fix',hypothesisId:'D',location:'request.ts:response-success',message:'response ok path',data:{url:response.config?.url,status:response.status,code:payload?.code,msg:payload?.message,dataType:typeof response.data,hasCode:typeof payload?.code==='number'},timestamp:Date.now()})}).catch(()=>{});
    // #endregion
    if (payload && typeof payload.code === 'number') {
      if (payload.code === 0) {
        return response
      }
      if (payload.code === 401) {
        // #region agent log
        fetch('http://127.0.0.1:7741/ingest/3ca358cf-e2c8-4ba3-9852-df7aac4b4abe',{method:'POST',headers:{'Content-Type':'application/json','X-Debug-Session-Id':'2cebf6'},body:JSON.stringify({sessionId:'2cebf6',runId:'post-fix',hypothesisId:'E',location:'request.ts:biz-401',message:'business code 401',data:{url:response.config?.url,code:payload.code,msg:payload.message},timestamp:Date.now()})}).catch(()=>{});
        // #endregion
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
    const dataSnippet = typeof error.response?.data === 'string'
      ? String(error.response.data).slice(0, 200)
      : error.response?.data
    // #region agent log
    fetch('http://127.0.0.1:7741/ingest/3ca358cf-e2c8-4ba3-9852-df7aac4b4abe',{method:'POST',headers:{'Content-Type':'application/json','X-Debug-Session-Id':'2cebf6'},body:JSON.stringify({sessionId:'2cebf6',runId:'post-fix',hypothesisId:'A',location:'request.ts:response-error',message:'response error path',data:{url:error.config?.url,method:error.config?.method,status,message,code:error.response?.data?.code,willShowUnauthorized:status===401||status===403,dataSnippet},timestamp:Date.now()})}).catch(()=>{});
    // #endregion
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
