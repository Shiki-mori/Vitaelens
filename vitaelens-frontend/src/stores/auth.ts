import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { login as loginApi, register as registerApi } from '@/api/auth'
import type { LoginRequest, RegisterRequest } from '@/types/api'
import {
  USER_ID_KEY,
  USERNAME_KEY,
  clearAuthStorage,
  getToken,
  setToken,
} from '@/utils/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(getToken())
  const userId = ref<number | null>(
    localStorage.getItem(USER_ID_KEY) ? Number(localStorage.getItem(USER_ID_KEY)) : null,
  )
  const username = ref<string | null>(localStorage.getItem(USERNAME_KEY))

  const isLoggedIn = computed(() => !!token.value)

  async function login(payload: LoginRequest) {
    const data = await loginApi(payload)
    token.value = data.token
    userId.value = data.userId
    username.value = data.username
    setToken(data.token)
    localStorage.setItem(USER_ID_KEY, String(data.userId))
    localStorage.setItem(USERNAME_KEY, data.username)
  }

  async function register(payload: RegisterRequest) {
    await registerApi(payload)
  }

  function logout() {
    token.value = null
    userId.value = null
    username.value = null
    clearAuthStorage()
  }

  return {
    token,
    userId,
    username,
    isLoggedIn,
    login,
    register,
    logout,
  }
})
