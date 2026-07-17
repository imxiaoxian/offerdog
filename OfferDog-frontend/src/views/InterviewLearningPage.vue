<script lang="ts" setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  BookOutlined,
  LineChartOutlined,
  LinkOutlined,
  CalendarOutlined,
  FileSearchOutlined,
  TrophyOutlined,
  QuestionCircleOutlined,
} from '@ant-design/icons-vue'
import { interviewApi } from '@/services/interview'
import { useUserStore } from '@/stores/user'
import type { InterviewGrowthPoint, InterviewLearningPack, InterviewSession } from '@/types/interview'
import { ApiError } from '@/utils/request'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const CHART_W = 440
const CHART_H = 168
const PAD = 18

type DimKey = 'overallScore' | 'technicalKnowledge' | 'communication' | 'problemSolving'

const sessions = ref<InterviewSession[]>([])
const sessionsLoading = ref(false)
const selectedSessionId = ref<string | undefined>(undefined)

const growthPoints = ref<InterviewGrowthPoint[]>([])
const growthLoading = ref(false)

const pack = ref<InterviewLearningPack | null>(null)
const packLoading = ref(false)
const packError = ref<string | null>(null)

const completedSessions = computed(() =>
  sessions.value.filter((s) => s.status === 'completed').sort(
    (a, b) => new Date(b.startTime).getTime() - new Date(a.startTime).getTime(),
  ),
)

function valueAt(p: InterviewGrowthPoint, key: DimKey): number | null {
  if (key === 'overallScore') {
    const n = Number(p.overallScore)
    return Number.isFinite(n) ? n : null
  }
  const raw = p[key]
  if (raw == null || typeof raw !== 'number') return null
  return raw
}

function buildPoly(pts: InterviewGrowthPoint[], key: DimKey): string {
  const n = pts.length
  if (n < 2) return ''
  const coords: string[] = []
  for (let i = 0; i < n; i++) {
    const v = valueAt(pts[i], key)
    if (v == null) continue
    const score = Math.min(10, Math.max(0, v))
    const x = PAD + (i / (n - 1)) * (CHART_W - 2 * PAD)
    const y = CHART_H - PAD - (score / 10) * (CHART_H - 2 * PAD)
    coords.push(`${x},${y}`)
  }
  return coords.length >= 2 ? coords.join(' ') : ''
}

const polyOverall = computed(() => buildPoly(growthPoints.value, 'overallScore'))
const polyTech = computed(() => buildPoly(growthPoints.value, 'technicalKnowledge'))
const polyComm = computed(() => buildPoly(growthPoints.value, 'communication'))
const polyProb = computed(() => buildPoly(growthPoints.value, 'problemSolving'))

const hasChart = computed(() => growthPoints.value.length >= 2)

function formatShortDate(raw: string | undefined) {
  if (!raw) return ''
  const d = new Date(raw)
  if (!Number.isNaN(d.getTime())) return d.toLocaleDateString('zh-CN', { month: 'numeric', day: 'numeric' })
  return raw.slice(0, 10)
}

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

async function loadGrowth() {
  if (!userStore.currentUser?.id) return
  growthLoading.value = true
  try {
    const res = await interviewApi.getGrowthPoints(userStore.currentUser.id, 30)
    growthPoints.value = res.data ?? []
  } catch (e) {
    console.error(e)
    growthPoints.value = []
  } finally {
    growthLoading.value = false
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
    message.warning(msg.includes('报告') ? msg : '请确认该场面试已生成报告后再试')
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
  await loadGrowth()

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

function goReport() {
  if (!selectedSessionId.value) return
  router.push(`/interview/report/${selectedSessionId.value}`)
}

function isReportPracticePlan(obj: unknown): obj is { focus_summary?: string; weekly_schedule?: string[]; drills?: string[] } {
  return obj != null && typeof obj === 'object'
}
</script>

<template>
  <div class="learning-page">
    <div class="page-head">
      <div class="head-row">
        <a-button type="link" class="back-link" @click="router.push('/ai-interviewer')">← 返回 AI 面试官</a-button>
        <a-space>
          <a-button
            type="link"
            @click="
              router.push({
                path: '/improvement-plan',
                query: selectedSessionId ? { sessionId: selectedSessionId } : {},
              })
            "
          >
            面试提升计划
          </a-button>
          <a-button v-if="selectedSessionId" type="link" @click="goReport">
            <TrophyOutlined /> 查看该场报告
          </a-button>
        </a-space>
      </div>
      <h1 class="page-title">学习提升中心</h1>
      <p class="page-sub">知识库片段、精选外链、个性化 7 日计划与推荐面试题</p>
    </div>

    <a-row :gutter="[16, 16]">
      <a-col :xs="24" :lg="10">
        <a-card title="能力成长曲线" :loading="growthLoading">
          <template #extra><LineChartOutlined /></template>
          <p class="hint">按已生成报告的时间升序；纵轴为 0–10 分。</p>
          <a-empty v-if="!growthLoading && growthPoints.length === 0" description="暂无报告数据，完成面试并生成报告后将显示曲线" />
          <div v-else-if="growthPoints.length === 1" class="single-stat">
            <a-statistic title="总分" :value="growthPoints[0].overallScore" suffix="/10" :precision="1" />
          </div>
          <div v-else-if="hasChart" class="chart-wrap">
            <svg
              class="chart-svg"
              :viewBox="`0 0 ${CHART_W} ${CHART_H}`"
              preserveAspectRatio="xMidYMid meet"
            >
              <text :x="4" :y="PAD" class="axis-label">10</text>
              <text :x="4" :y="CHART_H / 2" class="axis-label">5</text>
              <text :x="4" :y="CHART_H - 4" class="axis-label">0</text>
              <line
                :x1="PAD"
                :y1="CHART_H / 2"
                :x2="CHART_W - PAD"
                :y2="CHART_H / 2"
                class="grid"
              />
              <polyline v-if="polyTech" :points="polyTech" class="line tech" />
              <polyline v-if="polyComm" :points="polyComm" class="line comm" />
              <polyline v-if="polyProb" :points="polyProb" class="line prob" />
              <polyline v-if="polyOverall" :points="polyOverall" class="line overall" />
            </svg>
            <div class="legend">
              <span><i class="swatch overall" />总分</span>
              <span><i class="swatch tech" />技术知识</span>
              <span><i class="swatch comm" />沟通表达</span>
              <span><i class="swatch prob" />问题解决</span>
            </div>
            <div class="axis-foot">
              <span>{{ formatShortDate(growthPoints[0]?.reportCreatedAt) }}</span>
              <span>{{ formatShortDate(growthPoints[growthPoints.length - 1]?.reportCreatedAt) }}</span>
            </div>
          </div>
          <a-empty v-else description="至少需要两场已生成报告的面试以绘制折线" />
        </a-card>
      </a-col>

      <a-col :xs="24" :lg="14">
        <a-card title="选择面试场次" :loading="sessionsLoading">
          <a-select
            v-model:value="selectedSessionId"
            style="width: 100%"
            placeholder="请选择已完成的面试（需已生成报告）"
            :options="
              completedSessions.map((s) => ({
                value: s.sessionId,
                label: `${new Date(s.startTime).toLocaleString('zh-CN')} · 进度 ${s.questionsCompleted}/${s.interviewPlan.total_questions}`,
              }))
            "
            show-search
            :filter-option="
              (input, option) =>
                (option?.label ?? '').toString().toLowerCase().includes(input.toLowerCase())
            "
          />
          <p class="hint">学习包会读取该场次的评估报告，并检索知识库与题库。</p>
        </a-card>
      </a-col>
    </a-row>

    <a-card class="pack-card" :loading="packLoading">
      <template #title><BookOutlined /> 智能学习包</template>
      <a-alert v-if="packError" type="error" show-icon :message="packError" style="margin-bottom: 16px" />
      <template v-else-if="pack">
        <p class="summary">{{ pack.summaryReason }}</p>
        <a-tabs>
          <a-tab-pane key="kb">
            <template #tab><span><FileSearchOutlined /> 技术知识点（知识库）</span></template>
            <a-list v-if="pack.knowledgeSnippets?.length" :data-source="pack.knowledgeSnippets" item-layout="vertical">
              <template #renderItem="{ item }">
                <a-list-item>
                  <a-list-item-meta :title="item.title">
                    <template #description>
                      <a-space wrap>
                        <a-tag v-if="item.kbCategory">{{ item.kbCategory }}</a-tag>
                        <a-tag v-if="item.matchedWeakPoint" color="blue">关联：{{ item.matchedWeakPoint }}</a-tag>
                      </a-space>
                      <div class="excerpt">{{ item.excerpt }}</div>
                    </template>
                  </a-list-item-meta>
                </a-list-item>
              </template>
            </a-list>
            <a-empty v-else description="暂无匹配片段，可先导入知识库向量或换一场面试" />
          </a-tab-pane>

          <a-tab-pane key="bank">
            <template #tab><span><QuestionCircleOutlined /> 面试题库（向量检索）</span></template>
            <a-list
              v-if="pack.questionSnippets?.length"
              :data-source="pack.questionSnippets"
              item-layout="vertical"
            >
              <template #renderItem="{ item }">
                <a-list-item>
                  <a-list-item-meta :title="item.title">
                    <template #description>
                      <a-space wrap>
                        <a-tag v-if="item.kbCategory">{{ item.kbCategory }}</a-tag>
                        <a-tag v-if="item.matchedWeakPoint" color="blue">关联：{{ item.matchedWeakPoint }}</a-tag>
                      </a-space>
                      <div class="excerpt">{{ item.excerpt }}</div>
                      <div v-if="item.sourceHint" class="muted small-hint">{{ item.sourceHint }}</div>
                    </template>
                  </a-list-item-meta>
                </a-list-item>
              </template>
            </a-list>
            <a-empty
              v-else
              description="暂无题库向量命中，请在管理端为题库执行向量化索引，或确认库中已有题目"
            />
          </a-tab-pane>

          <a-tab-pane key="links">
            <template #tab><span><LinkOutlined /> 精选学习资源</span></template>
            <a-list :data-source="pack.externalLinks" item-layout="vertical">
              <template #renderItem="{ item }">
                <a-list-item>
                  <a :href="item.url" target="_blank" rel="noopener noreferrer">{{ item.title }}</a>
                  <div class="link-desc">{{ item.description }}</div>
                  <a-tag>{{ item.topicTag }}</a-tag>
                </a-list-item>
              </template>
            </a-list>
          </a-tab-pane>

          <a-tab-pane key="plan">
            <template #tab><span><CalendarOutlined /> 个性化练习计划</span></template>
            <div v-if="pack.reportPracticePlan && isReportPracticePlan(pack.reportPracticePlan)" class="report-plan">
              <h4>报告内练习计划（生成报告时写入）</h4>
              <p v-if="pack.reportPracticePlan.focus_summary">
                {{ pack.reportPracticePlan.focus_summary }}
              </p>
              <template v-if="pack.reportPracticePlan.weekly_schedule?.length">
                <strong>周安排</strong>
                <ol>
                  <li v-for="(line, i) in pack.reportPracticePlan.weekly_schedule" :key="i">{{ line }}</li>
                </ol>
              </template>
              <template v-if="pack.reportPracticePlan.drills?.length">
                <strong>具体练习</strong>
                <ul>
                  <li v-for="(d, i) in pack.reportPracticePlan.drills" :key="i">{{ d }}</li>
                </ul>
              </template>
            </div>
            <a-divider v-if="pack.reportPracticePlan && isReportPracticePlan(pack.reportPracticePlan)" />
            <h4>AI 生成 7 日计划（针对本场薄弱点）</h4>
            <p class="goal">{{ pack.generatedPlan?.goal }}</p>
            <a-timeline>
              <a-timeline-item v-for="day in pack.generatedPlan?.days ?? []" :key="day.day" color="blue">
                <strong>第 {{ day.day }} 天 · {{ day.theme }}</strong>
                <ul class="task-list">
                  <li v-for="(t, i) in day.tasks" :key="i">{{ t }}</li>
                </ul>
              </a-timeline-item>
            </a-timeline>
          </a-tab-pane>

          <a-tab-pane key="questions">
            <template #tab><span>推荐面试题</span></template>
            <a-list
              v-if="pack.recommendedQuestions?.length"
              :data-source="pack.recommendedQuestions"
              item-layout="vertical"
            >
              <template #renderItem="{ item }">
                <a-list-item>
                  <a-list-item-meta>
                    <template #title>
                      <a-space>
                        <a-tag>{{ item.difficulty }}</a-tag>
                        <span v-for="(t, ti) in item.tags?.slice(0, 5)" :key="ti">{{ t }}</span>
                      </a-space>
                    </template>
                    <template #description>
                      <div>{{ item.content }}</div>
                      <div v-if="item.matchedPoint" class="muted">薄弱点关联：{{ item.matchedPoint }}</div>
                    </template>
                  </a-list-item-meta>
                </a-list-item>
              </template>
            </a-list>
            <a-empty v-else description="当前报告未解析到薄弱点或题库无匹配" />
          </a-tab-pane>
        </a-tabs>
        <p class="generated-at">学习包生成时间：{{ new Date(pack.generatedAt).toLocaleString('zh-CN') }}</p>
      </template>
      <a-empty v-else-if="!packLoading && !packError" description="请选择一场已完成的面试" />
    </a-card>
  </div>
</template>

<style scoped>
.learning-page {
  padding: 0 16px 32px;
  max-width: 1200px;
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
  color: #8c8c8c;
  font-size: 14px;
}

.hint {
  color: #8c8c8c;
  font-size: 13px;
  margin: 0 0 12px;
}

.summary {
  color: #262626;
  margin-bottom: 16px;
  line-height: 1.6;
}

.chart-wrap {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.chart-svg {
  width: 100%;
  height: 200px;
  display: block;
}

.axis-label {
  fill: #8c8c8c;
  font-size: 10px;
}

.grid {
  stroke: #f0f0f0;
  stroke-width: 1;
}

.line {
  fill: none;
  stroke-width: 2.2;
  stroke-linejoin: round;
  stroke-linecap: round;
}

.line.overall {
  stroke: #1890ff;
}

.line.tech {
  stroke: #52c41a;
  stroke-dasharray: 5 4;
}

.line.comm {
  stroke: #722ed1;
  stroke-dasharray: 3 3;
}

.line.prob {
  stroke: #fa8c16;
  stroke-dasharray: 6 3;
}

.legend {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 16px;
  font-size: 13px;
  color: #595959;
}

.swatch {
  display: inline-block;
  width: 12px;
  height: 3px;
  margin-right: 6px;
  vertical-align: middle;
}

.swatch.overall {
  background: #1890ff;
}

.swatch.tech {
  background: #52c41a;
}

.swatch.comm {
  background: #722ed1;
}

.swatch.prob {
  background: #fa8c16;
}

.axis-foot {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #8c8c8c;
}

.single-stat {
  padding: 8px 0;
}

.pack-card {
  margin-top: 8px;
}

.excerpt {
  margin-top: 8px;
  color: #434343;
  white-space: pre-wrap;
  line-height: 1.55;
}

.link-desc {
  color: #595959;
  margin: 6px 0;
  font-size: 13px;
}

.goal {
  font-weight: 600;
  margin-bottom: 12px;
}

.task-list {
  margin: 6px 0 0;
  padding-left: 18px;
}

.report-plan {
  margin-bottom: 8px;
}

.generated-at {
  margin-top: 16px;
  font-size: 12px;
  color: #8c8c8c;
}

.small-hint {
  font-size: 12px;
  margin-top: 6px;
  color: #8c8c8c;
}

.muted {
  margin-top: 6px;
  color: #8c8c8c;
  font-size: 13px;
}
</style>
