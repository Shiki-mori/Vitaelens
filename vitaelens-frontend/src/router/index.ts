import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { getToken } from '@/utils/auth'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { public: true },
  },
  {
    path: '/',
    component: () => import('@/layouts/AppLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'home',
        component: () => import('@/views/HomeView.vue'),
      },
      {
        path: 'resumes',
        name: 'resumes',
        component: () => import('@/views/ResumeView.vue'),
      },
      {
        path: 'jobs',
        name: 'jobs',
        component: () => import('@/views/JobView.vue'),
      },
      {
        path: 'analysis',
        name: 'analysis',
        component: () => import('@/views/AnalysisListView.vue'),
      },
      {
        path: 'analysis/:taskId',
        name: 'analysis-detail',
        component: () => import('@/views/AnalysisDetailView.vue'),
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  const loggedIn = !!getToken()
  if (to.meta.public) {
    if (loggedIn && to.path === '/login') {
      return { path: '/' }
    }
    return true
  }
  if (to.matched.some((record) => record.meta.requiresAuth) && !loggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
