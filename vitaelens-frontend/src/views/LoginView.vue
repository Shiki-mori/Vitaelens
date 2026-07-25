<template>
  <div class="login-page">
    <div class="login-card">
      <h1 class="brand">VitaeLens</h1>
      <p class="subtitle">AI 简历优化与岗位匹配分析</p>

      <el-tabs v-model="activeTab" stretch>
        <el-tab-pane label="登录" name="login">
          <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" label-position="top">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="loginForm.username" placeholder="请输入用户名" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input
                v-model="loginForm.password"
                type="password"
                show-password
                placeholder="请输入密码"
                @keyup.enter="onLogin"
              />
            </el-form-item>
            <el-button type="primary" class="submit-btn" :loading="loginLoading" @click="onLogin">
              登录
            </el-button>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="注册" name="register">
          <el-form
            ref="registerFormRef"
            :model="registerForm"
            :rules="registerRules"
            label-position="top"
          >
            <el-form-item label="用户名" prop="username">
              <el-input v-model="registerForm.username" placeholder="2-50 个字符" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input
                v-model="registerForm.password"
                type="password"
                show-password
                placeholder="至少 6 位"
              />
            </el-form-item>
            <el-form-item label="邮箱（可选）" prop="email">
              <el-input v-model="registerForm.email" placeholder="可选" />
            </el-form-item>
            <el-button
              type="primary"
              class="submit-btn"
              :loading="registerLoading"
              @click="onRegister"
            >
              注册
            </el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const activeTab = ref('login')
const loginLoading = ref(false)
const registerLoading = ref(false)
const loginFormRef = ref<FormInstance>()
const registerFormRef = ref<FormInstance>()

const loginForm = reactive({
  username: '',
  password: '',
})

const registerForm = reactive({
  username: '',
  password: '',
  email: '',
})

const loginRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const registerRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 50, message: '用户名长度为 2-50 个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' },
  ],
  email: [
    {
      type: 'email',
      message: '邮箱格式不正确',
      trigger: 'blur',
    },
  ],
}

async function onLogin() {
  const valid = await loginFormRef.value?.validate().catch(() => false)
  if (!valid) return
  loginLoading.value = true
  try {
    await auth.login({
      username: loginForm.username,
      password: loginForm.password,
    })
    ElMessage.success('登录成功')
    const redirect = (route.query.redirect as string) || '/'
    await router.replace(redirect)
  } finally {
    loginLoading.value = false
  }
}

async function onRegister() {
  const valid = await registerFormRef.value?.validate().catch(() => false)
  if (!valid) return
  registerLoading.value = true
  try {
    await auth.register({
      username: registerForm.username,
      password: registerForm.password,
      email: registerForm.email || undefined,
    })
    ElMessage.success('注册成功，请登录')
    activeTab.value = 'login'
    loginForm.username = registerForm.username
    loginForm.password = ''
  } finally {
    registerLoading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    radial-gradient(circle at top left, rgba(37, 99, 235, 0.12), transparent 40%),
    linear-gradient(160deg, #eef2ff 0%, #f8fafc 45%, #e2e8f0 100%);
  padding: 24px;
}

.login-card {
  width: 100%;
  max-width: 420px;
  background: #fff;
  border-radius: 12px;
  padding: 32px 28px 28px;
  box-shadow: 0 12px 40px rgba(15, 23, 42, 0.08);
}

.brand {
  margin: 0;
  text-align: center;
  font-size: 32px;
  font-weight: 700;
  color: #0f172a;
}

.subtitle {
  margin: 8px 0 24px;
  text-align: center;
  color: var(--vl-muted);
  font-size: 14px;
}

.submit-btn {
  width: 100%;
  margin-top: 8px;
}
</style>
