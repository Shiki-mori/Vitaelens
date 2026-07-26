<template>
  <div class="page" v-loading="loading">
    <div class="toolbar">
      <el-button @click="router.push('/interviews')">返回</el-button>
      <el-button :loading="loading" @click="loadSession">刷新</el-button>
      <el-button
        v-if="session?.analysisTaskId"
        link
        type="primary"
        @click="router.push(`/analysis/${session.analysisTaskId}`)"
      >
        查看关联分析
      </el-button>
    </div>

    <el-card v-if="session" shadow="never" class="meta-card">
      <div class="meta-row">
        <div>
          <div class="label">场次 ID</div>
          <div>{{ session.id }}</div>
        </div>
        <div>
          <div class="label">分析任务</div>
          <div>#{{ session.analysisTaskId }}</div>
        </div>
        <div>
          <div class="label">题目数量</div>
          <div>{{ session.questions?.length || 0 }}</div>
        </div>
        <div>
          <div class="label">已作答</div>
          <div>{{ answeredCount }} / {{ session.questions?.length || 0 }}</div>
        </div>
      </div>
    </el-card>

    <el-empty v-if="!loading && (!session || !session.questions?.length)" description="暂无面试题" />

    <div v-for="(item, index) in session?.questions || []" :key="item.id" class="question-card">
      <el-card shadow="never">
        <template #header>
          <div class="q-header">
            <span>第 {{ index + 1 }} 题</span>
            <el-tag v-if="item.feedbackJson" type="success" size="small">已评价</el-tag>
            <el-tag v-else-if="item.answer" type="warning" size="small">已回答</el-tag>
            <el-tag v-else type="info" size="small">待回答</el-tag>
          </div>
        </template>

        <p class="question-text">{{ item.question }}</p>

        <el-input
          v-model="draftAnswers[item.id]"
          type="textarea"
          :rows="5"
          placeholder="请输入你的回答…"
          maxlength="5000"
          show-word-limit
        />

        <div class="actions">
          <el-button
            type="primary"
            :loading="submittingIds.has(item.id)"
            :disabled="!draftAnswers[item.id]?.trim()"
            @click="onSubmit(item.id)"
          >
            {{ item.feedbackJson ? '重新提交并评价' : '提交回答' }}
          </el-button>
        </div>

        <div v-if="item.feedbackJson" class="feedback">
          <div class="feedback-title">AI 评价</div>
          <div class="score-row">
            <div class="score-item">
              <div class="label">准确性</div>
              <el-progress
                :percentage="clampScore(item.feedbackJson.accuracyScore)"
                :stroke-width="10"
              />
            </div>
            <div class="score-item">
              <div class="label">完整性</div>
              <el-progress
                :percentage="clampScore(item.feedbackJson.completenessScore)"
                :stroke-width="10"
              />
            </div>
            <div class="score-item">
              <div class="label">表达清晰度</div>
              <el-progress
                :percentage="clampScore(item.feedbackJson.clarityScore)"
                :stroke-width="10"
              />
            </div>
          </div>
          <p v-if="item.feedbackJson.overallFeedback" class="overall">
            {{ item.feedbackJson.overallFeedback }}
          </p>
          <div v-if="item.feedbackJson.improvements?.length" class="block">
            <div class="block-title">改进建议</div>
            <ul>
              <li v-for="(tip, tipIndex) in item.feedbackJson.improvements" :key="tipIndex">
                {{ tip }}
              </li>
            </ul>
          </div>
          <el-alert
            v-if="item.feedbackJson.followUp"
            class="follow-up"
            type="info"
            :closable="false"
            :title="`可能的追问：${item.feedbackJson.followUp}`"
          />
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getInterviewSession, submitInterviewAnswer } from '@/api/interview'
import type { InterviewSessionDetailResponse } from '@/types/api'

const route = useRoute()
const router = useRouter()

const session = ref<InterviewSessionDetailResponse | null>(null)
const loading = ref(false)
const submittingIds = ref<Set<number>>(new Set())
const draftAnswers = reactive<Record<number, string>>({})

const answeredCount = computed(
  () => session.value?.questions?.filter((item) => !!item.answer || !!item.feedbackJson).length || 0,
)

function clampScore(value?: number) {
  if (typeof value !== 'number' || Number.isNaN(value)) return 0
  return Math.max(0, Math.min(100, Math.round(value)))
}

function syncDrafts(detail: InterviewSessionDetailResponse) {
  for (const question of detail.questions || []) {
    if (draftAnswers[question.id] === undefined) {
      draftAnswers[question.id] = question.answer || ''
    }
  }
}

async function loadSession() {
  const sessionId = Number(route.params.sessionId)
  if (!sessionId) {
    ElMessage.error('无效的面试场次')
    await router.replace('/interviews')
    return
  }

  loading.value = true
  try {
    const detail = await getInterviewSession(sessionId)
    session.value = detail
    syncDrafts(detail)
  } finally {
    loading.value = false
  }
}

async function onSubmit(questionId: number) {
  const answer = draftAnswers[questionId]?.trim()
  if (!answer) {
    ElMessage.warning('请先填写回答')
    return
  }

  submittingIds.value.add(questionId)
  submittingIds.value = new Set(submittingIds.value)
  try {
    const detail = await submitInterviewAnswer(questionId, answer)
    session.value = detail
    for (const question of detail.questions || []) {
      draftAnswers[question.id] = question.answer || draftAnswers[question.id] || ''
    }
    ElMessage.success('评价已生成')
  } finally {
    submittingIds.value.delete(questionId)
    submittingIds.value = new Set(submittingIds.value)
  }
}

watch(
  () => route.params.sessionId,
  async () => {
    await loadSession()
  },
)

onMounted(loadSession)
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
}

.meta-card {
  margin-bottom: 16px;
}

.meta-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.label {
  color: var(--vl-muted);
  font-size: 13px;
  margin-bottom: 6px;
}

.question-card {
  margin-bottom: 16px;
}

.q-header {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 600;
}

.question-text {
  margin: 0 0 14px;
  line-height: 1.7;
  color: #1f2937;
  white-space: pre-wrap;
}

.actions {
  margin-top: 12px;
}

.feedback {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #e5e7eb;
}

.feedback-title {
  font-weight: 600;
  margin-bottom: 12px;
}

.score-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 12px;
}

.overall {
  margin: 0 0 12px;
  line-height: 1.7;
  color: #334155;
}

.block-title {
  font-size: 13px;
  color: var(--vl-muted);
  margin-bottom: 6px;
}

.block ul {
  margin: 0;
  padding-left: 18px;
  line-height: 1.7;
  color: #334155;
}

.follow-up {
  margin-top: 12px;
}

@media (max-width: 768px) {
  .meta-row,
  .score-row {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
