import { request } from '@/utils/request'
import type { LoginRequest, LoginResponse, RegisterRequest } from '@/types/api'

export function login(data: LoginRequest) {
  return request<LoginResponse>({
    url: '/auth/login',
    method: 'post',
    data,
  })
}

export function register(data: RegisterRequest) {
  return request<null>({
    url: '/auth/register',
    method: 'post',
    data,
  })
}
