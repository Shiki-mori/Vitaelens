<template>
  <div class="home">
    <section class="hero">
      <h2>用 AI 看清简历与岗位的匹配度</h2>
      <p>
        上传简历、录入目标岗位 JD，系统会异步完成结构化分析，给出评分、缺口与改写建议。
      </p>
    </section>

    <el-row :gutter="16">
      <el-col v-for="item in entries" :key="item.path" :xs="24" :sm="12" :lg="6">
        <div class="entry" @click="router.push(item.path)">
          <div class="entry-icon">
            <el-icon :size="28"><component :is="item.icon" /></el-icon>
          </div>
          <h3>{{ item.title }}</h3>
          <p>{{ item.desc }}</p>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { Collection, DataAnalysis, Document, Right } from '@element-plus/icons-vue'

const router = useRouter()

const entries = [
  {
    title: '简历管理',
    desc: '上传 PDF / DOCX，查看解析文本',
    path: '/resumes',
    icon: Document,
  },
  {
    title: '岗位 JD',
    desc: '创建并管理目标岗位描述',
    path: '/jobs',
    icon: Collection,
  },
  {
    title: '创建分析',
    desc: '选择简历与 JD，发起 AI 分析',
    path: '/analysis',
    icon: DataAnalysis,
  },
  {
    title: '查看历史',
    desc: '浏览历史任务与分析结果',
    path: '/analysis',
    icon: Right,
  },
]
</script>

<style scoped>
.hero {
  background: #fff;
  border-radius: 12px;
  padding: 28px 32px;
  margin-bottom: 20px;
  border: 1px solid #e5e7eb;
}

.hero h2 {
  margin: 0 0 10px;
  font-size: 24px;
}

.hero p {
  margin: 0;
  color: var(--vl-muted);
  line-height: 1.6;
  max-width: 720px;
}

.entry {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 16px;
  cursor: pointer;
  transition: border-color 0.2s, box-shadow 0.2s;
  min-height: 160px;
}

.entry:hover {
  border-color: #93c5fd;
  box-shadow: 0 8px 24px rgba(37, 99, 235, 0.08);
}

.entry-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  background: #eff6ff;
  color: var(--vl-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
}

.entry h3 {
  margin: 0 0 8px;
  font-size: 16px;
}

.entry p {
  margin: 0;
  color: var(--vl-muted);
  font-size: 13px;
  line-height: 1.5;
}
</style>
