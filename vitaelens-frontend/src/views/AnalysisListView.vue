<template>
  <div class="page">
    <el-card shadow="never" class="create-card">
      <template #header>
        <div class="card-header">创建分析任务</div>
      </template>
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="简历">
          <el-select
            v-model="form.resumeId"
            placeholder="选择简历"
            filterable
            style="width: 240px"
          >
            <el-option
              v-for="item in resumes"
              :key="item.id"
              :label="item.fileName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="岗位 JD">
          <el-select v-model="form.jdId" placeholder="选择岗位" filterable style="width: 240px">
            <el-option
              v-for="item in jobs"
              :key="item.id"
              :label="item.title"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="creating" @click="onCreate">开始分析</el-button>
          <el-button :loading="loading" @click="refreshAll">刷新</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-table v-loading="loading" :data="tasks" empty-text="暂无分析任务" class="task-table">
      <el-table-column prop="id" label="任务 ID" width="100" />
      <el-table-column label="简历" min-width="160">
        <template #default="{ row }">
          {{ resumeNameMap[row.resumeId] || row.resumeId }}
        </template>
      </el-table-column>
      <el-table-column label="岗位" min-width="140">
        <template #default="{ row }">
          {{ jobNameMap[row.jdId] || row.jdId }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="score" label="得分" width="90">
        <template #default="{ row }">
          {{ row.score ?? '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" min-width="180" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            :disabled="!row.id"
            @click="router.push(`/analysis/${row.id}`)"
          >
            查看
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createAnalysisTask, listAnalysisTasks } from '@/api/analysis'
import { listJobs } from '@/api/job'
import { listResumes } from '@/api/resume'
import type { JobResponse, ResumeResponse, TaskResponse } from '@/types/api'

const router = useRouter()
const resumes = ref<ResumeResponse[]>([])
const jobs = ref<JobResponse[]>([])
const tasks = ref<TaskResponse[]>([])
const loading = ref(false)
const creating = ref(false)

const form = reactive<{ resumeId?: number; jdId?: number }>({
  resumeId: undefined,
  jdId: undefined,
})

const resumeNameMap = computed(() =>
  Object.fromEntries(resumes.value.map((item) => [item.id, item.fileName])),
)
const jobNameMap = computed(() =>
  Object.fromEntries(jobs.value.map((item) => [item.id, item.title])),
)

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

async function refreshAll() {
  loading.value = true
  try {
    const [resumeList, jobList, taskList] = await Promise.all([
      listResumes(),
      listJobs(),
      listAnalysisTasks(),
    ])
    resumes.value = resumeList || []
    jobs.value = jobList || []
    tasks.value = taskList || []
  } finally {
    loading.value = false
  }
}

async function onCreate() {
  if (!form.resumeId || !form.jdId) {
    ElMessage.warning('请先选择简历和岗位 JD')
    return
  }
  creating.value = true
  try {
    const task = await createAnalysisTask({
      resumeId: form.resumeId,
      jdId: form.jdId,
    })

    if (task.status === 'SUCCESS' && task.resultJson) {
      ElMessage.success('命中缓存，已直接返回结果')
      if (task.id) {
        await router.push(`/analysis/${task.id}`)
      } else {
        sessionStorage.setItem('vitaelens_cached_task', JSON.stringify(task))
        await router.push({ name: 'analysis-detail', params: { taskId: 'cached' } })
      }
      return
    }

    if (!task.id) {
      ElMessage.error('未返回任务 ID，无法跟踪分析进度')
      return
    }

    ElMessage.success('分析任务已创建')
    await router.push(`/analysis/${task.id}`)
  } finally {
    creating.value = false
  }
}

onMounted(refreshAll)
</script>

<style scoped>
.create-card {
  margin-bottom: 16px;
}

.card-header {
  font-weight: 600;
}

.task-table {
  background: #fff;
}
</style>
