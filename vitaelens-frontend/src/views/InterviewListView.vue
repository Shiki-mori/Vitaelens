<template>
  <div class="page">
    <el-card shadow="never" class="create-card">
      <template #header>
        <div class="card-header">开始模拟面试</div>
      </template>
      <p class="hint">
        请选择一次已成功的简历分析结果。系统将根据简历、岗位 JD 和分析结论生成针对性面试题（可能需要数十秒）。
      </p>
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="分析任务">
          <el-select
            v-model="selectedTaskId"
            placeholder="选择 SUCCESS 状态的分析任务"
            filterable
            style="width: 360px"
          >
            <el-option
              v-for="item in successTasks"
              :key="item.id"
              :label="taskLabel(item)"
              :value="item.id!"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="creating" @click="onCreate">生成面试题</el-button>
          <el-button :loading="loading" @click="fetchTasks">刷新</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-empty
      v-if="!loading && successTasks.length === 0"
      description="暂无可用的成功分析任务，请先完成简历分析"
    >
      <el-button type="primary" @click="router.push('/analysis')">去分析</el-button>
    </el-empty>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listAnalysisTasks } from '@/api/analysis'
import { createInterviewSession } from '@/api/interview'
import type { TaskResponse } from '@/types/api'

const router = useRouter()
const route = useRoute()

const tasks = ref<TaskResponse[]>([])
const loading = ref(false)
const creating = ref(false)
const selectedTaskId = ref<number>()

const successTasks = computed(() =>
  tasks.value.filter((item) => item.status === 'SUCCESS' && item.id != null),
)

function taskLabel(task: TaskResponse) {
  const score = task.score ?? task.resultJson?.overallScore ?? '-'
  return `任务 #${task.id} · 得分 ${score} · ${task.createdAt || ''}`
}

async function fetchTasks() {
  loading.value = true
  try {
    tasks.value = (await listAnalysisTasks()) || []
    const queryTaskId = Number(route.query.analysisTaskId)
    if (queryTaskId && successTasks.value.some((item) => item.id === queryTaskId)) {
      selectedTaskId.value = queryTaskId
    } else if (!selectedTaskId.value && successTasks.value.length > 0) {
      selectedTaskId.value = successTasks.value[0].id
    }
  } finally {
    loading.value = false
  }
}

async function onCreate() {
  if (!selectedTaskId.value) {
    ElMessage.warning('请先选择分析任务')
    return
  }
  creating.value = true
  try {
    const session = await createInterviewSession({ analysisTaskId: selectedTaskId.value })
    ElMessage.success(`已生成 ${session.questionCount} 道面试题`)
    await router.push(`/interviews/${session.id}`)
  } finally {
    creating.value = false
  }
}

onMounted(fetchTasks)
</script>

<style scoped>
.create-card {
  margin-bottom: 16px;
}

.card-header {
  font-weight: 600;
}

.hint {
  margin: 0 0 16px;
  color: var(--vl-muted);
  font-size: 13px;
  line-height: 1.6;
}
</style>
