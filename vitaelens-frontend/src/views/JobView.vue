<template>
  <div class="page">
    <div class="toolbar">
      <el-button type="primary" @click="openCreate">新建岗位 JD</el-button>
      <el-button :loading="loading" @click="fetchList">刷新</el-button>
    </div>

    <el-table v-loading="loading" :data="list" empty-text="暂无岗位 JD">
      <el-table-column prop="title" label="岗位名称" min-width="160" />
      <el-table-column label="描述摘要" min-width="280">
        <template #default="{ row }">
          {{ summarize(row.content) }}
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" min-width="180" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">查看</el-button>
          <el-button link type="danger" @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="createVisible" title="新建岗位 JD" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="岗位名称" prop="title">
          <el-input v-model="form.title" maxlength="50" show-word-limit placeholder="例如：Java 后端开发" />
        </el-form-item>
        <el-form-item label="岗位描述" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="10"
            maxlength="5000"
            show-word-limit
            placeholder="粘贴完整 JD 文本"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="onCreate">创建</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" :title="detail?.title || '岗位详情'" size="50%">
      <pre class="detail-text">{{ detail?.content }}</pre>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createJob, deleteJob, listJobs } from '@/api/job'
import type { JobResponse } from '@/types/api'

const list = ref<JobResponse[]>([])
const loading = ref(false)
const creating = ref(false)
const createVisible = ref(false)
const detailVisible = ref(false)
const detail = ref<JobResponse | null>(null)
const formRef = ref<FormInstance>()

const form = reactive({
  title: '',
  content: '',
})

const rules: FormRules = {
  title: [
    { required: true, message: '请输入岗位名称', trigger: 'blur' },
    { max: 50, message: '最多 50 个字符', trigger: 'blur' },
  ],
  content: [
    { required: true, message: '请输入岗位描述', trigger: 'blur' },
    { max: 5000, message: '最多 5000 个字符', trigger: 'blur' },
  ],
}

function summarize(content: string) {
  if (!content) return ''
  return content.length > 80 ? `${content.slice(0, 80)}...` : content
}

async function fetchList() {
  loading.value = true
  try {
    list.value = (await listJobs()) || []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  form.title = ''
  form.content = ''
  createVisible.value = true
}

function openDetail(row: JobResponse) {
  detail.value = row
  detailVisible.value = true
}

async function onCreate() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  creating.value = true
  try {
    await createJob({ title: form.title, content: form.content })
    ElMessage.success('创建成功')
    createVisible.value = false
    await fetchList()
  } finally {
    creating.value = false
  }
}

async function onDelete(row: JobResponse) {
  await ElMessageBox.confirm(`确认删除岗位「${row.title}」？`, '删除确认', {
    type: 'warning',
  })
  await deleteJob(row.id)
  ElMessage.success('已删除')
  await fetchList()
}

onMounted(fetchList)
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.detail-text {
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 13px;
  line-height: 1.6;
  margin: 0;
  color: #334155;
}
</style>
