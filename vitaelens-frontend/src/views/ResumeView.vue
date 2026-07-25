<template>
  <div class="page">
    <div class="toolbar">
      <el-upload
        :show-file-list="false"
        :before-upload="beforeUpload"
        :http-request="handleUpload"
        accept=".pdf,.docx,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
      >
        <el-button type="primary" :loading="uploading">上传简历</el-button>
      </el-upload>
      <el-button :loading="loading" @click="fetchList">刷新</el-button>
    </div>

    <el-table v-loading="loading" :data="list" empty-text="暂无简历，请先上传">
      <el-table-column prop="fileName" label="文件名" min-width="180" />
      <el-table-column prop="textLength" label="文本长度" width="120" />
      <el-table-column prop="createdAt" label="上传时间" min-width="180" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openPreview(row)">查看文本</el-button>
          <el-button link type="danger" @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-drawer v-model="previewVisible" title="解析文本" size="50%">
      <pre class="parsed-text">{{ previewText }}</pre>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox, type UploadRequestOptions } from 'element-plus'
import { deleteResume, listResumes, uploadResume } from '@/api/resume'
import type { ResumeResponse } from '@/types/api'

const list = ref<ResumeResponse[]>([])
const loading = ref(false)
const uploading = ref(false)
const previewVisible = ref(false)
const previewText = ref('')

const MAX_SIZE = 10 * 1024 * 1024

async function fetchList() {
  loading.value = true
  try {
    list.value = (await listResumes()) || []
  } finally {
    loading.value = false
  }
}

function beforeUpload(file: File) {
  const name = file.name.toLowerCase()
  const okType = name.endsWith('.pdf') || name.endsWith('.docx')
  if (!okType) {
    ElMessage.error('仅支持 PDF 或 DOCX 文件')
    return false
  }
  if (file.size > MAX_SIZE) {
    ElMessage.error('文件大小不能超过 10MB')
    return false
  }
  return true
}

async function handleUpload(options: UploadRequestOptions) {
  uploading.value = true
  try {
    await uploadResume(options.file as File)
    ElMessage.success('上传成功')
    await fetchList()
  } finally {
    uploading.value = false
  }
}

function openPreview(row: ResumeResponse) {
  previewText.value = row.parsedText || ''
  previewVisible.value = true
}

async function onDelete(row: ResumeResponse) {
  await ElMessageBox.confirm(`确认删除简历「${row.fileName}」？`, '删除确认', {
    type: 'warning',
  })
  await deleteResume(row.id)
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

.parsed-text {
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 13px;
  line-height: 1.6;
  margin: 0;
  color: #334155;
}
</style>
