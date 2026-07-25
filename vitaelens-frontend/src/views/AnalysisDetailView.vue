<template>
  <div class="page" v-loading="loading">
    <div class="toolbar">
      <el-button @click="router.push('/analysis')">返回列表</el-button>
      <el-button v-if="canRefresh" :loading="loading" @click="loadTask">刷新状态</el-button>
    </div>

    <el-card v-if="task" shadow="never" class="status-card">
      <div class="status-row">
        <div>
          <div class="label">任务状态</div>
          <el-tag :type="statusType(task.status)">{{ statusLabel(task.status) }}</el-tag>
        </div>
        <div>
          <div class="label">综合得分</div>
          <div class="score">{{ task.score ?? result?.overallScore ?? '-' }}</div>
        </div>
        <div>
          <div class="label">任务 ID</div>
          <div>{{ task.id ?? '缓存结果' }}</div>
        </div>
        <div>
          <div class="label">完成时间</div>
          <div>{{ task.finishedAt || '-' }}</div>
        </div>
      </div>

      <el-alert
        v-if="isPending"
        class="mt"
        type="info"
        :closable="false"
        title="分析进行中，页面会自动刷新状态，请稍候…"
      />
      <el-alert
        v-if="task.status === 'FAILED'"
        class="mt"
        type="error"
        :closable="false"
        :title="task.errorMessage || '分析失败'"
      />
    </el-card>

    <template v-if="result">
      <el-row :gutter="16" class="mt">
        <el-col :xs="24" :md="12">
          <el-card shadow="never">
            <template #header>维度评分</template>
            <div v-if="dimensionEntries.length" class="dimension-list">
              <div v-for="item in dimensionEntries" :key="item.key" class="dimension-item">
                <div class="dimension-label">{{ item.label }}</div>
                <el-progress :percentage="clampScore(item.value)" :stroke-width="12" />
              </div>
            </div>
            <el-empty v-else description="暂无维度评分" :image-size="64" />
          </el-card>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-card shadow="never">
            <template #header>技能缺口</template>
            <div v-if="result.skillGaps?.length" class="tag-wrap">
              <el-tag v-for="gap in result.skillGaps" :key="gap" class="tag">{{ gap }}</el-tag>
            </div>
            <el-empty v-else description="暂无技能缺口" :image-size="64" />
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="16" class="mt">
        <el-col :xs="24" :md="12">
          <el-card shadow="never">
            <template #header>优势</template>
            <ul v-if="result.strengths?.length" class="text-list">
              <li v-for="(item, index) in result.strengths" :key="index">{{ item }}</li>
            </ul>
            <el-empty v-else description="暂无优势" :image-size="64" />
          </el-card>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-card shadow="never">
            <template #header>不足</template>
            <ul v-if="result.weaknesses?.length" class="text-list">
              <li v-for="(item, index) in result.weaknesses" :key="index">{{ item }}</li>
            </ul>
            <el-empty v-else description="暂无不足" :image-size="64" />
          </el-card>
        </el-col>
      </el-row>

      <el-card shadow="never" class="mt">
        <template #header>改写建议</template>
        <el-table
          v-if="result.rewriteSuggestions?.length"
          :data="result.rewriteSuggestions"
          empty-text="暂无改写建议"
        >
          <el-table-column prop="original" label="原文" min-width="180" />
          <el-table-column prop="suggested" label="建议改写" min-width="220" />
          <el-table-column prop="reason" label="原因" min-width="160" />
        </el-table>
        <el-empty v-else description="暂无改写建议" :image-size="64" />
      </el-card>

      <el-card shadow="never" class="mt">
        <template #header>面试关注点</template>
        <ul v-if="result.interviewFocus?.length" class="text-list">
          <li v-for="(item, index) in result.interviewFocus" :key="index">{{ item }}</li>
        </ul>
        <el-empty v-else description="暂无面试关注点" :image-size="64" />
      </el-card>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getAnalysisTask } from '@/api/analysis'
import type { TaskResponse } from '@/types/api'

const route = useRoute()
const router = useRouter()

const task = ref<TaskResponse | null>(null)
const loading = ref(false)
let timer: ReturnType<typeof setInterval> | null = null
let pollCount = 0
const MAX_POLL = 90

const dimensionLabelMap: Record<string, string> = {
  technicalMatch: '技术匹配',
  projectExperience: '项目经历',
  expressionQuality: '表达质量',
  jobMatch: '岗位匹配',
}

const result = computed(() => task.value?.resultJson || null)

const isPending = computed(
  () => task.value?.status === 'PENDING' || task.value?.status === 'RUNNING',
)

const canRefresh = computed(() => {
  const id = route.params.taskId
  return id !== 'cached' && !!id
})

const dimensionEntries = computed(() => {
  const scores = result.value?.dimensionScores
  if (!scores) return []
  return Object.entries(scores)
    .filter(([, value]) => typeof value === 'number')
    .map(([key, value]) => ({
      key,
      label: dimensionLabelMap[key] || key,
      value: value as number,
    }))
})

function statusLabel(status: string) {
  const map: Record<string, string> = {
    PENDING: '等待中',
    RUNNING: '分析中',
    SUCCESS: '成功',
    FAILED: '失败',
  }
  return map[status] || status
}

function statusType(status: string) {
  const map: Record<string, 'info' | 'warning' | 'success' | 'danger'> = {
    PENDING: 'info',
    RUNNING: 'warning',
    SUCCESS: 'success',
    FAILED: 'danger',
  }
  return map[status] || 'info'
}

function clampScore(value: number) {
  if (Number.isNaN(value)) return 0
  return Math.max(0, Math.min(100, Math.round(value)))
}

function stopPolling() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

function startPolling() {
  stopPolling()
  pollCount = 0
  timer = setInterval(async () => {
    pollCount += 1
    if (pollCount > MAX_POLL) {
      stopPolling()
      ElMessage.warning('分析耗时较长，请稍后手动刷新')
      return
    }
    await loadTask(false)
  }, 2000)
}

async function loadCachedTask() {
  const raw = sessionStorage.getItem('vitaelens_cached_task')
  if (!raw) {
    ElMessage.error('缓存结果不存在，请重新创建分析')
    await router.replace('/analysis')
    return
  }
  task.value = JSON.parse(raw) as TaskResponse
}

async function loadTask(showLoading = true) {
  const taskIdParam = String(route.params.taskId)
  if (taskIdParam === 'cached') {
    await loadCachedTask()
    return
  }

  const taskId = Number(taskIdParam)
  if (!taskId) {
    ElMessage.error('无效的任务 ID')
    await router.replace('/analysis')
    return
  }

  if (showLoading) loading.value = true
  try {
    task.value = await getAnalysisTask(taskId)
    if (task.value.status === 'SUCCESS' || task.value.status === 'FAILED') {
      stopPolling()
    } else if (!timer) {
      startPolling()
    }
  } finally {
    if (showLoading) loading.value = false
  }
}

watch(
  () => route.params.taskId,
  async () => {
    stopPolling()
    await loadTask()
  },
)

onMounted(async () => {
  await loadTask()
})

onBeforeUnmount(() => {
  stopPolling()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.status-card {
  margin-bottom: 8px;
}

.status-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.label {
  color: var(--vl-muted);
  font-size: 13px;
  margin-bottom: 6px;
}

.score {
  font-size: 28px;
  font-weight: 700;
  color: var(--vl-primary);
}

.mt {
  margin-top: 16px;
}

.dimension-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.dimension-label {
  margin-bottom: 6px;
  font-size: 13px;
}

.tag-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag {
  margin: 0;
}

.text-list {
  margin: 0;
  padding-left: 18px;
  line-height: 1.7;
  color: #334155;
}

@media (max-width: 768px) {
  .status-row {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
