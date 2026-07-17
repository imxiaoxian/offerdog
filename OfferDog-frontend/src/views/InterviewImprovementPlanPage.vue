<script lang="ts" setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  CalendarOutlined,
  RiseOutlined,
  TrophyOutlined,
  BookOutlined,
  PlayCircleOutlined,
  AimOutlined,
} from '@ant-design/icons-vue'
import { interviewApi } from '@/services/interview'
import { useUserStore } from '@/stores/user'
import type { InterviewLearningPack, InterviewSession } from '@/types/interview'
import { ApiError } from '@/utils/request'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const sessions = ref<InterviewSession[]>([])
const sessionsLoading = ref(false)
const selectedSessionId = ref<string | undefined>(undefined)

const pack = ref<InterviewLearningPack | null>(null)
const packLoading = ref(false)
const packError = ref<string | null>(null)

const completedSessions = computed(() =>
  sessions.value
    .filter((s) => s.status === 'completed')
    .sort((a, b) => new Date(b.startTime).getTime() - new Date(a.startTime).getTime()),
)

async function loadSessions() {
  if (!userStore.currentUser?.id) return
  sessionsLoading.value = true
  try {
    const res = await interviewApi.getUserSessions(userStore.currentUser.id)
    sessions.value = res.data ?? []
  } catch (e) {
    console.error(e)
    message.error('加载面试记录失败')
  } finally {
    sessionsLoading.value = false
  }
}

async function loadPack(sid: string) {
  packLoading.value = true
  packError.value = null
  pack.value = null
  try {
    const res = await interviewApi.getLearningPack(sid)
    pack.value = res.data
  } catch (e) {
    const msg = e instanceof ApiError ? e.message : e instanceof Error ? e.message : '加载失败'
    packError.value = msg
    message.warning('请确认该场面试已生成评估报告')
  } finally {
    packLoading.value = false
  }
}

onMounted(async () => {
  if (!userStore.isLoggedIn) {
    message.warning('请先登录')
    router.push('/login')
    return
  }
  await loadSessions()
  const q = route.query.sessionId
  const fromQuery = typeof q === 'string' ? q : Array.isArray(q) ? q[0] : undefined
  if (fromQuery) {
    selectedSessionId.value = fromQuery
  } else if (completedSessions.value.length > 0) {
    selectedSessionId.value = completedSessions.value[0].sessionId
  }
})

watch(selectedSessionId, (sid) => {
  if (sid) void loadPack(sid)
})

watch(
  () => route.query.sessionId,
  (q) => {
    const s = typeof q === 'string' ? q : Array.isArray(q) ? q[0] : undefined
    if (s && s !== selectedSessionId.value) {
      selectedSessionId.value = s
    }
  },
)

function isReportPracticePlan(obj: unknown): obj is { focus_summary?: string; weekly_schedule?: string[]; drills?: string[] } {
  return obj != null && typeof obj === 'object'
}

function goReport() {
  if (!selectedSessionId.value) return
  router.push(`/interview/report/${selectedSessionId.value}`)
}

function goFullLearning() {
  if (!selectedSessionId.value) return
  router.push({ path: '/learning', query: { sessionId: selectedSessionId.value } })
}

function goMockInterview() {
  router.push('/ai-interviewer')
}
</script>

<template>
  <div class="improve-page">
    <div class="page-head">
      <div class="head-row">
        <a-button type="link" class="back-link" @click="router.push('/ai-interviewer')">← AI 面试官</a-button>
        <a-space>
          <a-button v-if="selectedSessionId" @click="goReport"><TrophyOutlined /> 本场报告</a-button>
          <a-button v-if="selectedSessionId" type="primary" ghost @click="goFullLearning">
            <BookOutlined /> 完整学习包
          </a-button>
        </a-space>
      </div>
      <h1 class="page-title"><RiseOutlined /> 面试提升计划</h1>
      <p class="page-sub">
        基于单场面试报告，汇总<strong>薄弱点</strong>、报告中的<strong>练习安排</strong>与 AI 生成的<strong>7
        日强化计划</strong>，便于按周执行。
      </p>
    </div>

    <a-card class="pick-card" title="选择要制定计划的面试" :loading="sessionsLoading">
      <a-select
        v-model:value="selectedSessionId"
        style="width: 100%"
        placeholder="请选择已完成的面试（需已生成报告）"
        :options="
          completedSessions.map((s) => ({
            value: s.sessionId,
            label: `${new Date(s.startTime).toLocaleString('zh-CN')} · 完成 ${s.questionsCompleted}/${s.interviewPlan.total_questions} 题`,
          }))
        "
        show-search
        :filter-option="
          (input, option) =>
            (option?.label ?? '').toString().toLowerCase().includes(input.toLowerCase())
        "
      />
    </a-card>

    <a-spin :spinning="packLoading">
      <a-alert v-if="packError" type="warning" show-icon :message="packError" class="alert-block" />

      <template v-else-if="pack">
        <a-row :gutter="[16, 16]">
          <a-col :xs="24" :lg="12">
            <a-card title="薄弱点聚焦">
              <template #extra><AimOutlined /></template>
              <p class="muted">{{ pack.summaryReason }}</p>
              <a-space v-if="pack.weakPoints?.length" wrap>
                <a-tag v-for="(w, i) in pack.weakPoints" :key="i" color="orange">{{ w }}</a-tag>
              </a-space>
              <a-empty v-else description="本场未解析到具体薄弱点，可直接按下方周计划执行通用提升" />
            </a-card>
          </a-col>
          <a-col :xs="24" :lg="12">
            <a-card title="快速行动">
              <a-space direction="vertical" style="width: 100%">
                <a-button type="primary" block @click="goMockInterview">
                  <PlayCircleOutlined /> 再练一场模拟面试
                </a-button>
                <a-button block @click="goFullLearning">打开学习提升（知识点 / 推荐题 / 外链）</a-button>
              </a-space>
            </a-card>
          </a-col>
        </a-row>

        <a-card class="plan-card" title="报告中的练习计划">
          <template #extra><CalendarOutlined /></template>
          <template v-if="pack.reportPracticePlan && isReportPracticePlan(pack.reportPracticePlan)">
            <p v-if="pack.reportPracticePlan.focus_summary" class="goal-text">
              {{ pack.reportPracticePlan.focus_summary }}
            </p>
            <template v-if="pack.reportPracticePlan.weekly_schedule?.length">
              <h4>周安排</h4>
              <ol class="styled-list">
                <li v-for="(line, i) in pack.reportPracticePlan.weekly_schedule" :key="i">{{ line }}</li>
              </ol>
            </template>
            <template v-if="pack.reportPracticePlan.drills?.length">
              <h4>具体练习</h4>
              <ul class="styled-list">
                <li v-for="(d, i) in pack.reportPracticePlan.drills" :key="i">{{ d }}</li>
              </ul>
            </template>
          </template>
          <a-empty v-else description="该报告未包含 practice_plan 字段，可依赖下方 AI 7 日计划" />
        </a-card>

        <a-card class="plan-card" title="AI 个性化 7 日提升计划">
          <p class="goal-text">{{ pack.generatedPlan?.goal }}</p>
          <a-timeline>
            <a-timeline-item v-for="day in pack.generatedPlan?.days ?? []" :key="day.day" color="blue">
              <strong>第 {{ day.day }} 天 · {{ day.theme }}</strong>
              <ul class="task-list">
                <li v-for="(t, i) in day.tasks" :key="i">{{ t }}</li>
              </ul>
            </a-timeline-item>
          </a-timeline>
          <p class="foot-note">
            计划生成时间：{{ new Date(pack.generatedAt).toLocaleString('zh-CN') }} · 与「学习提升」页数据同源，可在一处执行、另一处查资源。
          </p>
        </a-card>
      </template>

      <a-empty v-else-if="!packLoading && !packError && !selectedSessionId" description="请先选择一场已完成的面试" />
    </a-spin>
  </div>
</template>

<style scoped>
.improve-page {
  padding: 0 16px 40px;
  max-width: 960px;
  margin: 0 auto;
}

.page-head {
  margin-bottom: 20px;
}

.head-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.back-link {
  padding-left: 0;
}

.page-title {
  margin: 8px 0 4px;
  font-size: 22px;
  font-weight: 600;
}

.page-sub {
  margin: 0;
  color: #595959;
  font-size: 14px;
  line-height: 1.6;
}

.pick-card {
  margin-bottom: 16px;
}

.alert-block {
  margin-bottom: 16px;
}

.plan-card {
  margin-top: 16px;
}

.goal-text {
  font-weight: 500;
  margin-bottom: 12px;
  color: #262626;
}

.styled-list {
  margin: 8px 0 16px;
  padding-left: 20px;
  line-height: 1.7;
}

.task-list {
  margin: 8px 0 0;
  padding-left: 18px;
  line-height: 1.65;
}

.muted {
  color: #8c8c8c;
  margin-bottom: 12px;
  line-height: 1.55;
}

.foot-note {
  margin-top: 16px;
  font-size: 12px;
  color: #8c8c8c;
}

h4 {
  margin: 12px 0 8px;
  font-size: 14px;
  color: #434343;
}
</style>
