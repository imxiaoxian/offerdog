<script lang="ts" setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  PlayCircleOutlined,
  FileTextOutlined,
  ClockCircleOutlined,
  TrophyOutlined,
  UserOutlined,
  MessageOutlined,
  LineChartOutlined,
} from '@ant-design/icons-vue'
import { interviewApi } from '@/services/interview'
import { ApiError } from '@/utils/request'
import { useUserStore } from '@/stores/user'
import {
  JOB_BUCKET_OPTIONS,
  type JobBucketKey,
  bucketForTemplateCategory,
} from '@/constants/jobCategoryBuckets'
import type { InterviewTemplate, InterviewSession, InterviewGrowthPoint } from '@/types/interview'

const router = useRouter()
const userStore = useUserStore()

const interviewSteps = [
  { title: '选择职位', content: '根据目标岗位和公司，选择匹配的面试场景。' },
  { title: '上传简历', content: '快速上传或粘贴简历，AI 自动理解你的背景。' },
  { title: '实时沟通', content: 'AI 面试官提问并即时评价，帮助发现弱点。' },
]

/** 当前用户可见的全部激活模板（分类筛选的数据源） */
const allTemplates = ref<InterviewTemplate[]>([])
/** 固定大类：全部 / 后端 / 技术管理 / 前端 / 算法 / DevOps */
const selectedBucket = ref<JobBucketKey>('all')

const filteredTemplates = computed(() => {
  if (selectedBucket.value === 'all') return allTemplates.value
  return allTemplates.value.filter((t) => {
    const b = bucketForTemplateCategory(t.category)
    return b !== null && b === selectedBucket.value
  })
})
const sessions = ref<InterviewSession[]>([])
const isLoadingTemplates = ref(true)
const isLoadingSessions = ref(true)
const isCreating = ref(false)

const growthPoints = ref<InterviewGrowthPoint[]>([])
const growthLoading = ref(false)

const GROWTH_W = 380
const GROWTH_H = 132

const growthPolylineOverall = computed(() => {
  const pts = growthPoints.value
  if (pts.length < 2) return ''
  const pad = 14
  return pts
    .map((p, i) => {
      const score = Math.min(10, Math.max(0, Number(p.overallScore)))
      const x = pad + (i / (pts.length - 1)) * (GROWTH_W - 2 * pad)
      const y = GROWTH_H - pad - (score / 10) * (GROWTH_H - 2 * pad)
      return `${x},${y}`
    })
    .join(' ')
})

function formatGrowthDate(raw: string | undefined) {
  if (!raw) return ''
  const d = new Date(raw)
  if (!Number.isNaN(d.getTime())) return d.toLocaleDateString('zh-CN', { month: 'numeric', day: 'numeric' })
  return raw.slice(0, 10)
}

const loadGrowthPoints = async () => {
  if (!userStore.currentUser?.id) return
  growthLoading.value = true
  try {
    const res = await interviewApi.getGrowthPoints(userStore.currentUser.id, 24)
    growthPoints.value = res.data ?? []
  } catch (e) {
    console.error('加载成长曲线失败', e)
    growthPoints.value = []
  } finally {
    growthLoading.value = false
  }
}

// 加载面试模板
onMounted(async () => {
  try {
    const res = await interviewApi.getTemplates()
    allTemplates.value = res.data.filter((t) => t.isActive)
  } catch (error) {
    console.error('加载模板失败:', error)
    message.error('加载面试模板失败')
  } finally {
    isLoadingTemplates.value = false
  }

  // 加载用户的面试历史
  if (userStore.currentUser?.id) {
    try {
      const res = await interviewApi.getUserSessions(userStore.currentUser.id)
      sessions.value = res.data
        .sort((a, b) => new Date(b.startTime).getTime() - new Date(a.startTime).getTime())
        .slice(0, 10) // 只显示最近10条
    } catch (error) {
      console.error('加载面试历史失败:', error)
    } finally {
      isLoadingSessions.value = false
    }
  } else {
    isLoadingSessions.value = false
  }

  void loadGrowthPoints()
})

// 开始面试
const startInterview = async (templateId: string) => {
  if (!userStore.isLoggedIn || !userStore.currentUser?.id) {
    message.warning('请先登录')
    router.push('/login')
    return
  }

  isCreating.value = true
  try {
    const res = await interviewApi.createSession({
      templateId,
      userId: userStore.currentUser.id,
    })
    const session = res.data
    message.success('面试会话创建成功')
    router.push(`/interview/room/${session.sessionId}`)
  } catch (error) {
    console.error('创建面试失败:', error)
    const detail = error instanceof ApiError ? error.message : ''
    message.error(detail ? `创建面试失败：${detail}` : '创建面试失败，请重试')
  } finally {
    isCreating.value = false
  }
}

const openConversation = (sessionId: string) => {
  router.push(`/interview/room/${sessionId}`)
}

// 继续面试
const continueInterview = (sessionId: string) => {
  openConversation(sessionId)
}

// 查看报告
const viewReport = (sessionId: string) => {
  router.push(`/interview/report/${sessionId}`)
}

// 格式化日期
const formatDate = (dateStr: string) => {
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

// 获取难度标签颜色
const getDifficultyColor = (level: string) => {
  const colors = {
    junior: 'green',
    mid: 'blue',
    senior: 'orange',
    high_senior: 'red',
  }
  return colors[level as keyof typeof colors] || 'default'
}

// 获取难度标签文本
const getDifficultyText = (level: string) => {
  const texts = {
    junior: '初级',
    mid: '中级',
    senior: '高级',
    high_senior: '资深',
  }
  return texts[level as keyof typeof texts] || level
}

// 获取状态标签颜色
const getStatusColor = (status: string) => {
  const colors = {
    in_progress: 'processing',
    completed: 'success',
    cancelled: 'default',
  }
  return colors[status as keyof typeof colors] || 'default'
}

// 获取状态文本
const getStatusText = (status: string) => {
  const texts = {
    in_progress: '进行中',
    completed: '已完成',
    cancelled: '已取消',
  }
  return texts[status as keyof typeof texts] || status
}
</script>

<template>
  <a-space direction="vertical" size="large" class="page-stack">
    <a-card title="offerDog 智能面试">
      <a-row :gutter="[24, 24]">
        <a-col :xs="24" :md="12">
          <a-typography-paragraph>
            offerDog
            面试官根据岗位能力模型自动生成问题，并结合语义理解给出评分和建议。通过持续练习，你可以在正式面试前找到提升方向。
          </a-typography-paragraph>
          <a-typography-title :level="5">面试流程</a-typography-title>
          <a-timeline>
            <a-timeline-item v-for="step in interviewSteps" :key="step.title">
              <strong>{{ step.title }}</strong>
              <div>{{ step.content }}</div>
            </a-timeline-item>
          </a-timeline>
        </a-col>
        <a-col :xs="24" :md="12">
          <a-card type="inner" title="评估提醒" :bordered="false">
            <p>· 根据岗位差异动态调整提问策略</p>
            <p>· 自动记录答题表现并生成报告</p>
            <p>· 支持语音/文本两种作答方式</p>
            <p>
              ·
              <a-button type="link" size="small" style="padding: 0" @click="router.push('/learning')">
                学习提升中心：知识片段、周计划与推荐题
              </a-button>
            </p>
            <p>
              ·
              <a-button type="link" size="small" style="padding: 0" @click="router.push('/improvement-plan')">
                面试提升计划：薄弱点 + 7 日执行表
              </a-button>
            </p>
          </a-card>
        </a-col>
      </a-row>
    </a-card>

    <!-- 面试模板选择 -->
    <a-card title="选择面试模板">
      <a-spin :spinning="isLoadingTemplates">
        <div v-if="!isLoadingTemplates && allTemplates.length > 0" class="category-toolbar">
          <span class="category-toolbar-label">岗位分类</span>
          <a-radio-group
            v-model:value="selectedBucket"
            button-style="solid"
            size="middle"
            class="category-radio-group"
          >
            <a-radio-button v-for="opt in JOB_BUCKET_OPTIONS" :key="opt.key" :value="opt.key">
              {{ opt.label }}
            </a-radio-button>
          </a-radio-group>
        </div>

        <a-row :gutter="[16, 16]">
          <a-col v-for="template in filteredTemplates" :key="template.id" :xs="24" :sm="12" :lg="8">
            <a-card hoverable class="template-card">
              <template #title>
                <div class="template-title">
                  <FileTextOutlined />
                  {{ template.templateName }}
                </div>
              </template>
              <template #extra>
                <a-tag :color="getDifficultyColor(template.difficultyLevel)">
                  {{ getDifficultyText(template.difficultyLevel) }}
                </a-tag>
              </template>

              <div class="template-content">
                <p class="template-desc">{{ template.description }}</p>
                <div class="template-info">
                  <div class="info-item">
                    <ClockCircleOutlined />
                    <span>约 {{ template.estimatedDurationMinutes }} 分钟</span>
                  </div>
                  <div class="info-item">
                    <UserOutlined />
                    <a-tag color="blue" class="category-tag">{{
                      template.category?.trim() || '未分类'
                    }}</a-tag>
                  </div>
                </div>
              </div>

              <template #actions>
                <a-button
                  type="primary"
                  block
                  :loading="isCreating"
                  @click="startInterview(template.id)"
                >
                  <template #icon><PlayCircleOutlined /></template>
                  开始面试
                </a-button>
              </template>
            </a-card>
          </a-col>
        </a-row>

        <a-empty
          v-if="!isLoadingTemplates && allTemplates.length === 0"
          description="暂无可用的面试模板"
        />
        <a-empty
          v-else-if="!isLoadingTemplates && filteredTemplates.length === 0"
          description="该分类下暂无模板，请切换分类"
        />
      </a-spin>
    </a-card>

    <!-- 能力成长曲线：依赖已完成的会话且已生成报告 -->
    <a-card v-if="userStore.isLoggedIn">
      <template #title>
        <span><LineChartOutlined /> 能力成长曲线</span>
      </template>
      <template #extra>
        <span class="growth-sub">按报告生成时间升序 · 仅统计已有报告的面试</span>
      </template>
      <a-spin :spinning="growthLoading">
        <a-empty
          v-if="!growthLoading && growthPoints.length === 0"
          description="暂无报告数据。完成面试并在报告页生成评估后，将显示总分变化趋势。"
        />
        <div v-else-if="growthPoints.length === 1" class="growth-single">
          <a-statistic
            title="最近一场总分"
            :value="growthPoints[0].overallScore"
            suffix="/10"
            :precision="1"
          />
          <a-button type="link" size="small" @click="viewReport(growthPoints[0].sessionId)">查看报告</a-button>
        </div>
        <div v-else-if="growthPoints.length > 1" class="growth-chart-block">
          <svg
            class="growth-svg"
            :viewBox="`0 0 ${GROWTH_W} ${GROWTH_H}`"
            preserveAspectRatio="none"
            role="img"
            aria-label="成长曲线"
          >
            <line
              x1="14"
              :y1="GROWTH_H / 2"
              :x2="GROWTH_W - 14"
              :y2="GROWTH_H / 2"
              stroke="#f0f0f0"
              stroke-width="1"
            />
            <polyline
              :points="growthPolylineOverall"
              fill="none"
              stroke="#1890ff"
              stroke-width="2.5"
            />
          </svg>
          <div class="growth-legend">
            <span><i class="dot blue" /> 总分（0–10）</span>
          </div>
          <div class="growth-axis">
            <span>{{ formatGrowthDate(growthPoints[0]?.reportCreatedAt) }}</span>
            <span>{{ formatGrowthDate(growthPoints[growthPoints.length - 1]?.reportCreatedAt) }}</span>
          </div>
        </div>
      </a-spin>
    </a-card>

    <!-- 近期面试记录 -->
    <a-card title="近期面试记录">
      <a-spin :spinning="isLoadingSessions">
        <a-list :data-source="sessions" bordered>
          <template #renderItem="{ item }">
            <a-list-item>
              <a-list-item-meta>
                <template #title> 面试会话 - {{ formatDate(item.startTime) }} </template>
                <template #description>
                  <a-space>
                    <span
                      >进度: {{ item.questionsCompleted }}/{{
                        item.interviewPlan.total_questions
                      }}</span
                    >
                    <a-divider type="vertical" />
                    <span>用时: {{ item.durationMinutes || 0 }} 分钟</span>
                  </a-space>
                </template>
              </a-list-item-meta>

              <template #actions>
                <a-tag :color="getStatusColor(item.status)">
                  {{ getStatusText(item.status) }}
                </a-tag>
                <a-button type="link" size="small" @click="openConversation(item.sessionId)">
                  <template #icon><MessageOutlined /></template>
                  查看对话
                </a-button>
                <a-button
                  v-if="item.status === 'in_progress'"
                  type="link"
                  size="small"
                  @click="continueInterview(item.sessionId)"
                >
                  继续面试
                </a-button>
                <a-button
                  v-if="item.status === 'completed'"
                  type="link"
                  size="small"
                  @click="viewReport(item.sessionId)"
                >
                  <template #icon><TrophyOutlined /></template>
                  查看报告
                </a-button>
              </template>
            </a-list-item>
          </template>
        </a-list>

        <a-empty
          v-if="!isLoadingSessions && sessions.length === 0"
          description="暂无面试记录，开始你的第一次AI面试吧！"
        />
      </a-spin>
    </a-card>
  </a-space>
</template>

<style scoped>
.page-stack {
  width: 100%;
}

.category-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  gap: 12px 16px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.category-toolbar-label {
  flex-shrink: 0;
  line-height: 32px;
  color: #595959;
  font-weight: 500;
}

.category-radio-group {
  flex: 1;
  min-width: 0;
}

.category-radio-group :deep(.ant-radio-button-wrapper) {
  margin-bottom: 8px;
}

.category-tag {
  margin: 0;
  border: none;
}

.template-card {
  height: 100%;
  transition: all 0.3s;
}

.template-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.template-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
}

.template-content {
  min-height: 120px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.template-desc {
  flex: 1;
  color: #595959;
  line-height: 1.6;
  margin: 0;
}

.template-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-top: 8px;
  border-top: 1px solid #f0f0f0;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #8c8c8c;
  font-size: 14px;
}

@media (max-width: 768px) {
  .page-stack {
    padding: 12px 0;
  }

  .template-title {
    font-size: 14px;
  }

  .template-content {
    min-height: 100px;
  }

  .template-desc {
    font-size: 13px;
  }

  .info-item {
    font-size: 12px;
  }

  :deep(.ant-card-head) {
    padding: 12px 16px;
  }

  :deep(.ant-card-body) {
    padding: 16px;
  }
}

.growth-sub {
  font-size: 12px;
  color: #8c8c8c;
  font-weight: normal;
}

.growth-single {
  display: flex;
  align-items: center;
  gap: 16px;
}

.growth-chart-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.growth-svg {
  width: 100%;
  height: 140px;
  display: block;
}

.growth-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  font-size: 13px;
  color: #595959;
}

.growth-legend .dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-right: 6px;
  vertical-align: middle;
}

.growth-legend .dot.blue {
  background: #1890ff;
}

.growth-axis {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #8c8c8c;
}

@media (max-width: 480px) {
  .template-title {
    font-size: 13px;
  }

  .template-content {
    min-height: 80px;
  }

  .info-item {
    font-size: 11px;
  }
}
</style>
