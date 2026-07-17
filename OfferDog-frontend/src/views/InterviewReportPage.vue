<script lang="ts" setup>
import { onMounted, onUnmounted, ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  TrophyOutlined,
  FileTextOutlined,
  BarChartOutlined,
  BulbOutlined,
  RiseOutlined,
  BookOutlined,
  CalendarOutlined,
} from '@ant-design/icons-vue'
import { interviewApi } from '@/services/interview'
import { invalidateInterviewSessionCaches } from '@/utils/interviewSessionCache'
import type { InterviewReport, ReportGenerationEvent, QuestionRecommendationRsp } from '@/types/interview'
import { WS_BASE_URL } from '@/config/env'
import { Client, type IMessage } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { ApiError } from '@/utils/request'

const route = useRoute()
const router = useRouter()

const sessionId = route.params.sessionId as string
const report = ref<InterviewReport | null>(null)
const isLoading = ref(true)
const activeTab = ref('overview')
const reportError = ref<string | null>(null)

type GenerationState =
  | 'connecting'
  | 'waiting'
  | 'queued'
  | 'processing'
  | 'success'
  | 'failed'
  | 'disconnected'

const wsClient = ref<Client | null>(null)
const wsConnected = ref(false)
const latestEvent = ref<ReportGenerationEvent | null>(null)
const generationState = ref<GenerationState>('connecting')
const generationHint = ref('正在与AI报告服务建立连接...')
const lastStatusTimestamp = ref<number | null>(null)

const questionRec = ref<QuestionRecommendationRsp | null>(null)
const recLoading = ref(false)
const recError = ref<string | null>(null)

const statusTextMap: Record<string, string> = {
  QUEUED: '报告任务已提交，等待处理',
  PROCESSING: 'AI正在生成面试报告',
  SUCCESS: '报告生成完成',
  FAILED: '报告生成失败',
}

const generationProgress = computed(() => {
  switch (generationState.value) {
    case 'success':
      return 100
    case 'processing':
      return 80
    case 'queued':
      return 45
    case 'waiting':
      return 25
    case 'disconnected':
      return 35
    case 'failed':
      return 0
    default:
      return 10
  }
})

const latestStatusText = computed(() => {
  if (latestEvent.value) {
    return statusTextMap[latestEvent.value.status] ?? '等待服务器状态'
  }
  return generationState.value === 'failed' ? '生成失败' : '等待服务器状态'
})

const statusTimeText = computed(() => {
  if (!lastStatusTimestamp.value) return ''
  return new Date(lastStatusTimestamp.value).toLocaleString('zh-CN')
})

const isReportPendingError = (error: unknown) => {
  const keywords = ['生成中', '不存在', '未找到', '处理中']

  if (error instanceof ApiError) {
    if (['REPORT_GENERATING', 'REPORT_NOT_READY'].includes(error.code ?? '')) {
      return true
    }
    return keywords.some((keyword) => (error.message ?? '').includes(keyword))
  }

  if (error instanceof Error) {
    return keywords.some((keyword) => error.message.includes(keyword))
  }

  return false
}

const handleReportStatus = (event: ReportGenerationEvent) => {
  latestEvent.value = event
  lastStatusTimestamp.value = event.timestamp ?? Date.now()

  const displayMessage = event.message ?? statusTextMap[event.status] ?? '报告状态更新'
  generationHint.value = displayMessage

  switch (event.status) {
    case 'QUEUED':
      generationState.value = 'queued'
      break
    case 'PROCESSING':
      generationState.value = 'processing'
      break
    case 'SUCCESS':
      generationState.value = 'success'
      invalidateInterviewSessionCaches(sessionId)
      void loadReport({ bypassCache: true })
      break
    case 'FAILED':
      generationState.value = 'failed'
      reportError.value = displayMessage
      isLoading.value = false
      message.error(displayMessage)
      break
  }
}

const connectWebSocket = () => {
  if (wsClient.value) return

  generationState.value = 'connecting'
  generationHint.value = '正在与AI报告服务建立连接...'

  const socket = new SockJS(`${WS_BASE_URL}/ws`)
  const client = new Client({
    webSocketFactory: () => socket as unknown as WebSocket,
    reconnectDelay: 5000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000,
    debug: import.meta.env.DEV ? (msg: string) => console.debug('[Report WS]', msg) : undefined,
    onConnect: () => {
      wsConnected.value = true
      generationState.value = report.value ? 'success' : 'waiting'
      generationHint.value = '实时通道已建立，等待服务器状态...'

      client.subscribe(`/topic/interview/report/${sessionId}`, (frame: IMessage) => {
        if (!frame.body) return
        try {
          const payload = JSON.parse(frame.body) as ReportGenerationEvent
          handleReportStatus(payload)
        } catch (error) {
          console.error('解析报告状态失败:', error)
        }
      })
    },
    onStompError: (frame) => {
      console.error('WebSocket STOMP错误:', frame.headers['message'])
      wsConnected.value = false
      generationState.value = 'disconnected'
      generationHint.value = '报告状态通道异常，稍后将自动重试'
    },
    onWebSocketError: (event) => {
      console.error('WebSocket错误:', event)
      wsConnected.value = false
      generationState.value = 'disconnected'
      generationHint.value = '连接报告状态通道失败，稍后将自动重试'
    },
    onDisconnect: () => {
      wsConnected.value = false
      if (!report.value) {
        generationState.value = 'disconnected'
        generationHint.value = '连接已断开，正在准备重连...'
      }
    },
  })

  wsClient.value = client
  client.activate()
}

const disconnectWebSocket = () => {
  if (wsClient.value) {
    wsClient.value.deactivate()
    wsClient.value = null
  }
}

const reconnectWebSocket = () => {
  disconnectWebSocket()
  connectWebSocket()
}

const triggerReportGeneration = async (withFeedback = false) => {
  try {
    await interviewApi.generateReport(sessionId)
    if (withFeedback) {
      message.success('已重新提交报告生成任务')
    }
  } catch (error) {
    const errMsg = error instanceof Error ? error.message : '触发报告生成失败'
    if (withFeedback) {
      message.error(errMsg)
    } else {
      console.error('触发报告生成失败:', error)
    }
  }
}

const retryGeneration = async () => {
  invalidateInterviewSessionCaches(sessionId)
  reportError.value = null
  isLoading.value = true
  generationState.value = 'queued'
  generationHint.value = '已重新提交报告任务，等待服务器状态...'
  latestEvent.value = null
  lastStatusTimestamp.value = null

  if (!wsClient.value) {
    connectWebSocket()
  }

  await triggerReportGeneration(true)
}

const fetchQuestionRecommendations = async () => {
  recLoading.value = true
  recError.value = null
  try {
    const res = await interviewApi.getQuestionRecommendations(sessionId)
    questionRec.value = res.data
  } catch (e) {
    const msg = e instanceof Error ? e.message : '加载题库推荐失败'
    recError.value = msg
    questionRec.value = null
  } finally {
    recLoading.value = false
  }
}

const loadReport = async (options?: { silent?: boolean; bypassCache?: boolean }) => {
  const silent = options?.silent ?? false

  try {
    const res = await interviewApi.getReport(sessionId, { bypassCache: options?.bypassCache })
    report.value = res.data
    console.log('📊 面试报告:', report.value)
    isLoading.value = false
    reportError.value = null
    generationState.value = 'success'
    generationHint.value = '报告已生成'
    disconnectWebSocket()
    void fetchQuestionRecommendations()
  } catch (error) {
    console.error('获取报告失败:', error)

    if (isReportPendingError(error)) {
      if (!silent) {
        message.loading('报告生成中，请稍候...', 3)
      }
      return
    }

    const errMsg = error instanceof Error ? error.message : '获取面试报告失败'
    reportError.value = errMsg
    isLoading.value = false
    generationState.value = 'failed'
    generationHint.value = errMsg
    message.error(errMsg)
  }
}

onMounted(() => {
  connectWebSocket()
  triggerReportGeneration()
  loadReport({ silent: true })
})

onUnmounted(() => {
  disconnectWebSocket()
})

// 计算评分等级
const scoreLevel = computed(() => {
  if (!report.value) return ''
  const score = report.value.overallScore
  if (score >= 9) return 'excellent'
  if (score >= 8) return 'good'
  if (score >= 7) return 'average'
  return 'poor'
})

// 评分等级颜色
const scoreLevelColor = computed(() => {
  const colors = {
    excellent: '#52c41a',
    good: '#1890ff',
    average: '#faad14',
    poor: '#f5222d',
  }
  return colors[scoreLevel.value as keyof typeof colors] || '#8c8c8c'
})

// 维度评分数据
const dimensionScores = computed(() => {
  if (!report.value?.reportContent?.dimension_scores) return []
  const dimensions = report.value.reportContent.dimension_scores
  return [
    { name: '技术知识', score: dimensions.technical_knowledge, key: 'technical_knowledge' },
    { name: '问题解决', score: dimensions.problem_solving, key: 'problem_solving' },
    { name: '系统设计', score: dimensions.system_design, key: 'system_design' },
    { name: '沟通表达', score: dimensions.communication, key: 'communication' },
    { name: '实战经验', score: dimensions.practical_experience, key: 'practical_experience' },
    { name: '学习能力', score: dimensions.learning_ability, key: 'learning_ability' },
  ]
})

// 技能评分数据
const skillScores = computed(() => {
  if (!report.value?.reportContent?.skill_assessment) return []
  return Object.entries(report.value.reportContent.skill_assessment).map(([skill, score]) => ({
    skill,
    score,
  }))
})

// 格式化分数
const formatScore = (score: number) => {
  return score.toFixed(1)
}

// 返回面试列表
const backToList = () => {
  router.push('/ai-interviewer')
}

const openLearningPack = () => {
  router.push({ path: '/learning', query: { sessionId } })
}

const openImprovementPlan = () => {
  router.push({ path: '/improvement-plan', query: { sessionId } })
}
</script>

<template>
  <div class="interview-report">
    <!-- 加载中状态 -->
    <div v-if="isLoading" class="loading-container">
      <a-spin size="large">
        <template #tip>
          <div class="loading-content">
            <h2>正在生成面试报告...</h2>
            <p class="loading-desc">{{ generationHint }}</p>
            <div class="loading-status">
              <a-tag :color="wsConnected ? 'blue' : 'default'">
                {{ wsConnected ? '实时通道已建立' : '等待连接...' }}
              </a-tag>
              <a-tag v-if="latestEvent" color="green">{{ latestStatusText }}</a-tag>
            </div>
            <a-progress
              :percent="generationProgress"
              :show-info="true"
              status="active"
              stroke-color="#1890ff"
            />
            <p class="loading-tip" v-if="statusTimeText">最新状态：{{ statusTimeText }}</p>
            <a-alert
              v-if="generationState === 'disconnected'"
              type="warning"
              show-icon
              message="实时连接已断开，将自动尝试重连。"
            />
            <div class="loading-actions">
              <a-button v-if="generationState === 'disconnected'" @click="reconnectWebSocket">
                立即重连
              </a-button>
            </div>
          </div>
        </template>
      </a-spin>
    </div>

    <!-- 报告内容 -->
    <div v-else-if="report" class="report-content">
      <!-- 报告头部 -->
      <div class="report-header">
        <div class="header-left">
          <h1 class="report-title">
            <FileTextOutlined class="title-icon" />
            面试评估报告
          </h1>
          <p class="report-meta">
            生成时间: {{ new Date(report.createdAt).toLocaleString('zh-CN') }}
            <a-divider type="vertical" />
            AI模型: {{ report.aiModel }}
            <a-divider type="vertical" />
            Token使用: {{ report.generationTokensUsed }}
          </p>
        </div>
        <div class="header-right">
          <a-button type="primary" ghost @click="openLearningPack">
            <BookOutlined /> 智能学习包
          </a-button>
          <a-button ghost @click="openImprovementPlan">
            <CalendarOutlined /> 面试提升计划
          </a-button>
          <a-button @click="backToList">返回列表</a-button>
        </div>
      </div>

      <!-- 总体评分卡片 -->
      <a-card class="score-card">
        <div class="score-overview">
          <div class="score-main">
            <div class="score-circle" :style="{ borderColor: scoreLevelColor }">
              <span class="score-value" :style="{ color: scoreLevelColor }">
                {{ formatScore(report.overallScore) }}
              </span>
              <span class="score-total">/10</span>
            </div>
            <div class="score-status">
              <a-tag :color="report.passStatus ? 'success' : 'error'" class="status-tag">
                <template #icon>
                  <CheckCircleOutlined v-if="report.passStatus" />
                  <CloseCircleOutlined v-else />
                </template>
                {{ report.passStatus ? '通过' : '未通过' }}
              </a-tag>
              <div class="confidence-level">评分信心: {{ report.confidenceLevel }}</div>
            </div>
          </div>
          <div class="score-summary">
            <h3>总体评价</h3>
            <p>{{ report.reportContent.summary }}</p>
          </div>
        </div>
      </a-card>

      <!-- 标签页 -->
      <a-tabs v-model:activeKey="activeTab" class="report-tabs">
        <!-- 详细分析 -->
        <a-tab-pane key="overview" tab="详细分析">
          <a-row :gutter="[16, 16]">
            <a-col :xs="24" :lg="12">
              <a-card title="优势" class="strength-card">
                <template #extra>
                  <TrophyOutlined style="color: #52c41a" />
                </template>
                <ul class="point-list">
                  <li v-for="(strength, index) in report.reportContent.strengths" :key="index">
                    {{ strength }}
                  </li>
                </ul>
              </a-card>
            </a-col>

            <a-col :xs="24" :lg="12">
              <a-card title="待改进方面" class="weakness-card">
                <template #extra>
                  <BulbOutlined style="color: #faad14" />
                </template>
                <ul class="point-list">
                  <li v-for="(weakness, index) in report.reportContent.weaknesses" :key="index">
                    {{ weakness }}
                  </li>
                </ul>
              </a-card>
            </a-col>
          </a-row>

          <a-card title="发展建议" class="recommendations-card" style="margin-top: 16px">
            <ul class="point-list">
              <li v-for="(rec, index) in report.reportContent.recommendations" :key="index">
                {{ rec }}
              </li>
            </ul>
          </a-card>
        </a-tab-pane>

        <!-- 维度评分 -->
        <a-tab-pane key="dimensions" tab="维度评分">
          <a-card>
            <template #extra>
              <BarChartOutlined />
            </template>
            <div class="dimensions-chart">
              <div v-for="dim in dimensionScores" :key="dim.key" class="dimension-item">
                <div class="dimension-label">{{ dim.name }}</div>
                <div class="dimension-bar">
                  <div class="bar-bg">
                    <div
                      class="bar-fill"
                      :style="{
                        width: `${(dim.score / 10) * 100}%`,
                        backgroundColor: scoreLevelColor,
                      }"
                    />
                  </div>
                  <span class="dimension-score">{{ formatScore(dim.score) }}</span>
                </div>
              </div>
            </div>
          </a-card>
        </a-tab-pane>

        <!-- 技能评估 -->
        <a-tab-pane key="skills" tab="技能评估">
          <a-card>
            <a-row :gutter="[16, 16]">
              <a-col
                v-for="skill in skillScores"
                :key="skill.skill"
                :xs="24"
                :sm="12"
                :md="8"
                :lg="6"
              >
                <div class="skill-item">
                  <div class="skill-name">{{ skill.skill }}</div>
                  <a-progress
                    :percent="(skill.score / 10) * 100"
                    :format="() => formatScore(skill.score)"
                    :stroke-color="scoreLevelColor"
                  />
                </div>
              </a-col>
            </a-row>
          </a-card>
        </a-tab-pane>

        <!-- 问题分析 -->
        <a-tab-pane key="questions" tab="问题分析">
          <a-space direction="vertical" size="large" style="width: 100%">
            <a-card
              v-for="(qa, index) in report.reportContent.question_analysis || []"
              :key="qa.question_id"
              :title="`问题 ${index + 1}: ${qa.question}`"
              class="question-card"
            >
              <template #extra>
                <a-tag
                  :color="qa.score >= 8 ? 'success' : qa.score >= 7 ? 'processing' : 'warning'"
                >
                  {{ formatScore(qa.score) }} 分
                </a-tag>
              </template>

              <div class="question-detail">
                <div class="detail-row">
                  <span class="label">回答摘要:</span>
                  <p>{{ qa.answer_summary }}</p>
                </div>

                <div class="detail-row">
                  <span class="label">评价反馈:</span>
                  <p>{{ qa.feedback }}</p>
                </div>

                <a-row :gutter="16">
                  <a-col :xs="24" :sm="12">
                    <div class="detail-row">
                      <span class="label">已覆盖要点:</span>
                      <ul class="point-list compact">
                        <li v-for="(point, i) in qa.key_points_covered" :key="i">
                          <CheckCircleOutlined style="color: #52c41a" /> {{ point }}
                        </li>
                      </ul>
                    </div>
                  </a-col>
                  <a-col :xs="24" :sm="12">
                    <div class="detail-row">
                      <span class="label">遗漏要点:</span>
                      <ul class="point-list compact">
                        <li v-for="(point, i) in qa.key_points_missed" :key="i">
                          <CloseCircleOutlined style="color: #f5222d" /> {{ point }}
                        </li>
                      </ul>
                    </div>
                  </a-col>
                </a-row>

                <a-descriptions :column="3" size="small" bordered>
                  <a-descriptions-item label="用时">
                    {{ qa.time_spent_minutes.toFixed(1) }} 分钟
                  </a-descriptions-item>
                  <a-descriptions-item label="追问次数">
                    {{ qa.follow_up_count }}
                  </a-descriptions-item>
                  <a-descriptions-item label="简历一致性">
                    {{ qa.resume_consistency }}
                  </a-descriptions-item>
                </a-descriptions>
              </div>
            </a-card>
          </a-space>
        </a-tab-pane>

        <!-- 深度评估、表达、练习计划与题库推荐 -->
        <a-tab-pane key="improvement" tab="深度评估与提升">
          <a-space direction="vertical" size="large" style="width: 100%">
            <a-card v-if="report.reportContent.content_analysis" title="内容分析（技术正确性、深度、逻辑、岗位匹配）">
              <template #extra><RiseOutlined /></template>
              <a-row :gutter="[16, 16]">
                <a-col :xs="12" :sm="6">
                  <a-statistic
                    title="技术正确性"
                    :value="report.reportContent.content_analysis.technical_correctness"
                    :precision="1"
                    suffix="/10"
                  />
                </a-col>
                <a-col :xs="12" :sm="6">
                  <a-statistic
                    title="知识深度"
                    :value="report.reportContent.content_analysis.knowledge_depth"
                    :precision="1"
                    suffix="/10"
                  />
                </a-col>
                <a-col :xs="12" :sm="6">
                  <a-statistic
                    title="逻辑严谨性"
                    :value="report.reportContent.content_analysis.logical_rigor"
                    :precision="1"
                    suffix="/10"
                  />
                </a-col>
                <a-col :xs="12" :sm="6">
                  <a-statistic
                    title="岗位匹配度"
                    :value="report.reportContent.content_analysis.job_fit"
                    :precision="1"
                    suffix="/10"
                  />
                </a-col>
              </a-row>
              <a-divider />
              <p><strong>亮点：</strong>{{ report.reportContent.content_analysis.highlights || '—' }}</p>
              <p><strong>不足：</strong>{{ report.reportContent.content_analysis.gaps || '—' }}</p>
            </a-card>

            <a-alert
              v-else
              type="info"
              show-icon
              message="本报告为历史版本，未包含结构化「内容分析」字段。重新生成报告后可查看该维度。"
            />

            <a-card v-if="report.reportContent.expression_analysis" title="表达分析（基于文本与统计推断）">
              <a-alert
                type="warning"
                show-icon
                style="margin-bottom: 16px"
                :message="report.reportContent.expression_analysis.caveat || '基于对话文本与消息条数/字数等统计推断，非语音声学或专用情感模型实测。'"
              />
              <a-row :gutter="[16, 16]">
                <a-col :xs="12" :sm="6">
                  <a-statistic
                    title="信息密度（近似语速感）"
                    :value="report.reportContent.expression_analysis.pace_score"
                    :precision="1"
                    suffix="/10"
                  />
                </a-col>
                <a-col :xs="12" :sm="6">
                  <a-statistic
                    title="清晰度"
                    :value="report.reportContent.expression_analysis.clarity_score"
                    :precision="1"
                    suffix="/10"
                  />
                </a-col>
                <a-col :xs="12" :sm="6">
                  <a-statistic
                    title="自信度"
                    :value="report.reportContent.expression_analysis.confidence_score"
                    :precision="1"
                    suffix="/10"
                  />
                </a-col>
                <a-col :xs="12" :sm="6">
                  <a-statistic
                    title="情绪稳定倾向"
                    :value="report.reportContent.expression_analysis.emotional_stability_score"
                    :precision="1"
                    suffix="/10"
                  />
                </a-col>
              </a-row>
              <a-divider />
              <p>{{ report.reportContent.expression_analysis.evidence_from_text || '—' }}</p>
            </a-card>

            <a-alert
              v-else
              type="info"
              show-icon
              message="本报告未包含「表达分析」字段。结束面试后重新生成报告即可（新版本提示词会输出该项）。"
            />

            <a-card v-if="report.reportContent.practice_plan" title="能力提升练习计划">
              <h4>{{ report.reportContent.practice_plan.focus_summary }}</h4>
              <a-divider orientation="left">周安排</a-divider>
              <ol class="point-list">
                <li v-for="(line, i) in report.reportContent.practice_plan.weekly_schedule" :key="i">
                  {{ line }}
                </li>
              </ol>
              <a-divider orientation="left">具体练习</a-divider>
              <ul class="point-list">
                <li v-for="(d, i) in report.reportContent.practice_plan.drills" :key="i">{{ d }}</li>
              </ul>
            </a-card>

            <a-card title="题库推荐（结合薄弱点）">
              <a-spin :spinning="recLoading">
                <template v-if="recError">
                  <a-alert type="warning" show-icon :message="recError" />
                </template>
                <template v-else-if="questionRec">
                  <p class="rec-reason">{{ questionRec.reason }}</p>
                  <div v-if="questionRec.weakPoints?.length" style="margin-bottom: 12px">
                    <a-tag v-for="(wp, i) in questionRec.weakPoints" :key="i" color="orange">{{ wp }}</a-tag>
                  </div>
                  <a-list
                    v-if="questionRec.recommendedQuestions?.length"
                    item-layout="vertical"
                    :data-source="questionRec.recommendedQuestions"
                  >
                    <template #renderItem="{ item }">
                      <a-list-item>
                        <a-list-item-meta>
                          <template #title>
                            <a-space>
                              <a-tag>{{ item.difficulty }}</a-tag>
                              <span v-for="(t, ti) in item.tags?.slice(0, 4)" :key="ti">{{ t }}</span>
                            </a-space>
                          </template>
                          <template #description>
                            <div>{{ item.content }}</div>
                            <div v-if="item.matchedPoint" class="matched-point">
                              关联薄弱点：{{ item.matchedPoint }}
                            </div>
                          </template>
                        </a-list-item-meta>
                      </a-list-item>
                    </template>
                  </a-list>
                  <a-empty v-else description="暂无匹配题目" />
                </template>
                <a-empty v-else description="加载中或暂无数据" />
              </a-spin>
            </a-card>
          </a-space>
        </a-tab-pane>

        <!-- 简历验证 -->
        <a-tab-pane key="resume" tab="简历验证">
          <a-card
            :title="`简历整体一致性: ${report.reportContent.resume_verification.overall_consistency}`"
          >
            <a-row :gutter="[16, 16]">
              <a-col :xs="24" :lg="12">
                <h4>已验证内容</h4>
                <ul class="point-list">
                  <li
                    v-for="(claim, index) in report.reportContent.resume_verification
                      .verified_claims"
                    :key="index"
                  >
                    <CheckCircleOutlined style="color: #52c41a" /> {{ claim }}
                  </li>
                </ul>
              </a-col>
              <a-col :xs="24" :lg="12">
                <h4>可疑内容</h4>
                <ul class="point-list">
                  <li
                    v-for="(claim, index) in report.reportContent.resume_verification
                      .questionable_claims"
                    :key="index"
                  >
                    <CloseCircleOutlined style="color: #faad14" /> {{ claim }}
                  </li>
                </ul>
              </a-col>
            </a-row>

            <a-divider />

            <h4>技能差距分析</h4>
            <a-descriptions :column="1" bordered>
              <a-descriptions-item
                v-for="(analysis, skill) in report.reportContent.resume_verification
                  .skill_gap_analysis"
                :key="skill"
                :label="skill"
              >
                {{ analysis }}
              </a-descriptions-item>
            </a-descriptions>
          </a-card>
        </a-tab-pane>

        <!-- 录用建议 -->
        <a-tab-pane key="hiring" tab="录用建议">
          <a-card>
            <a-descriptions :column="1" bordered size="large">
              <a-descriptions-item label="决策">
                <a-tag
                  :color="
                    report.reportContent.hiring_recommendation.decision === '推荐录用'
                      ? 'success'
                      : 'error'
                  "
                  style="font-size: 16px"
                >
                  {{ report.reportContent.hiring_recommendation.decision }}
                </a-tag>
              </a-descriptions-item>
              <a-descriptions-item label="信心等级">
                {{ report.reportContent.hiring_recommendation.confidence }}
              </a-descriptions-item>
              <a-descriptions-item label="适合职位">
                <a-space wrap>
                  <a-tag
                    v-for="(pos, index) in report.reportContent.hiring_recommendation
                      .suitable_positions"
                    :key="index"
                    color="blue"
                  >
                    {{ pos }}
                  </a-tag>
                </a-space>
              </a-descriptions-item>
              <a-descriptions-item label="薪酬建议">
                {{ report.reportContent.hiring_recommendation.compensation_suggestion }}
              </a-descriptions-item>
              <a-descriptions-item label="入职重点">
                <ul class="point-list compact">
                  <li
                    v-for="(focus, index) in report.reportContent.hiring_recommendation
                      .onboarding_focus"
                    :key="index"
                  >
                    {{ focus }}
                  </li>
                </ul>
              </a-descriptions-item>
            </a-descriptions>

            <a-divider />

            <h4>职级匹配分析</h4>
            <a-descriptions :column="2" bordered>
              <a-descriptions-item label="申请职级">
                {{ report.reportContent.interview_level_match.applied_level }}
              </a-descriptions-item>
              <a-descriptions-item label="评估职级">
                {{ report.reportContent.interview_level_match.evaluated_level }}
              </a-descriptions-item>
              <a-descriptions-item label="是否匹配">
                <a-tag
                  :color="report.reportContent.interview_level_match.match ? 'success' : 'warning'"
                >
                  {{ report.reportContent.interview_level_match.match ? '匹配' : '不匹配' }}
                </a-tag>
              </a-descriptions-item>
              <a-descriptions-item label="建议职级">
                {{ report.reportContent.interview_level_match.suggested_level }}
              </a-descriptions-item>
              <a-descriptions-item label="原因分析" :span="2">
                {{ report.reportContent.interview_level_match.reasoning }}
              </a-descriptions-item>
            </a-descriptions>
          </a-card>
        </a-tab-pane>
      </a-tabs>
    </div>

    <div v-else class="report-error">
      <a-result
        status="error"
        title="报告暂不可用"
        :sub-title="reportError || '报告生成失败，请稍后重试。'"
      >
        <template #extra>
          <a-button type="primary" @click="retryGeneration">重新生成</a-button>
          <a-button v-if="generationState === 'disconnected'" @click="reconnectWebSocket">
            重连状态通道
          </a-button>
          <a-button @click="backToList">返回列表</a-button>
        </template>
      </a-result>
    </div>
  </div>
</template>

<style scoped>
.interview-report {
  padding: 24px;
  background: #f5f5f5;
  min-height: calc(100vh - 64px);
}

.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: calc(100vh - 64px - 48px);
  background: white;
  border-radius: 8px;
  margin: 24px;
}

.loading-content {
  text-align: center;
  padding: 40px;
  max-width: 600px;
}

.loading-content h2 {
  margin-bottom: 16px;
  color: #262626;
  font-size: 24px;
}

.loading-desc {
  margin-bottom: 32px;
  color: #8c8c8c;
  font-size: 16px;
}

.loading-status {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-bottom: 24px;
}

.loading-tip {
  margin-top: 16px;
  color: #1890ff;
  font-size: 14px;
}

.loading-actions {
  margin-top: 16px;
  display: flex;
  justify-content: center;
  gap: 12px;
}

.report-error {
  margin: 24px;
}

.report-content {
  max-width: 1400px;
  margin: 0 auto;
}

.report-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 16px 24px;
  background: white;
  border-radius: 8px;
}

.report-title {
  margin: 0;
  font-size: 24px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.title-icon {
  color: #1890ff;
}

.report-meta {
  margin: 8px 0 0 0;
  color: #8c8c8c;
  font-size: 14px;
}

.score-card {
  margin-bottom: 16px;
}

.score-overview {
  display: flex;
  gap: 40px;
  align-items: center;
}

.score-main {
  display: flex;
  align-items: center;
  gap: 24px;
}

.score-circle {
  width: 160px;
  height: 160px;
  border-radius: 50%;
  border: 8px solid;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
}

.score-value {
  font-size: 48px;
  font-weight: bold;
  line-height: 1;
}

.score-total {
  font-size: 20px;
  color: #8c8c8c;
}

.score-status {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.status-tag {
  font-size: 18px;
  padding: 8px 16px;
}

.confidence-level {
  font-size: 14px;
  color: #595959;
}

.score-summary {
  flex: 1;
}

.score-summary h3 {
  margin-top: 0;
  margin-bottom: 12px;
  font-size: 18px;
}

.score-summary p {
  margin: 0;
  line-height: 1.8;
  color: #595959;
}

.point-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.point-list li {
  padding: 8px 0;
  line-height: 1.6;
  border-bottom: 1px solid #f0f0f0;
}

.point-list li:last-child {
  border-bottom: none;
}

.point-list.compact li {
  padding: 4px 0;
}

.dimensions-chart {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.dimension-item {
  display: flex;
  align-items: center;
  gap: 16px;
}

.dimension-label {
  width: 100px;
  font-weight: 500;
  color: #262626;
}

.dimension-bar {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
}

.bar-bg {
  flex: 1;
  height: 24px;
  background: #f0f0f0;
  border-radius: 12px;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  border-radius: 12px;
  transition: width 0.3s ease;
}

.dimension-score {
  width: 40px;
  text-align: right;
  font-weight: 500;
  color: #262626;
}

.skill-item {
  padding: 12px;
  background: #fafafa;
  border-radius: 8px;
}

.skill-name {
  margin-bottom: 8px;
  font-weight: 500;
  color: #262626;
}

.question-card {
  border-left: 4px solid #1890ff;
}

.question-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-row {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.detail-row .label {
  font-weight: 500;
  color: #262626;
}

.detail-row p {
  margin: 0;
  line-height: 1.6;
  color: #595959;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .interview-report {
    padding: 16px;
  }

  .report-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .score-overview {
    flex-direction: column;
    gap: 24px;
  }

  .score-main {
    flex-direction: column;
  }

  .score-circle {
    width: 120px;
    height: 120px;
  }

  .score-value {
    font-size: 36px;
  }

  .score-total {
    font-size: 16px;
  }

  .score-status {
    gap: 8px;
  }

  .status-tag {
    font-size: 14px;
    padding: 6px 12px;
  }

  .confidence-level {
    font-size: 12px;
  }

  .score-summary h3 {
    font-size: 16px;
  }

  .score-summary p {
    font-size: 14px;
  }

  .dimension-label {
    width: 80px;
    font-size: 13px;
  }

  .dimension-score {
    width: 32px;
    font-size: 13px;
  }

  .skill-item {
    padding: 10px;
  }

  .skill-name {
    font-size: 13px;
  }

  .question-detail {
    gap: 12px;
  }

  .detail-row .label {
    font-size: 13px;
  }

  .detail-row p {
    font-size: 13px;
  }
}

@media (max-width: 480px) {
  .interview-report {
    padding: 12px;
  }

  .score-circle {
    width: 100px;
    height: 100px;
  }

  .score-value {
    font-size: 28px;
  }

  .score-total {
    font-size: 14px;
  }

  .dimension-label {
    width: 60px;
    font-size: 11px;
  }

  .dimension-bar {
    gap: 8px;
  }

  .dimension-score {
    width: 28px;
    font-size: 11px;
  }

  .bar-bg {
    height: 18px;
  }

  .bar-fill {
    border-radius: 9px;
  }

  .skill-item {
    padding: 8px;
  }

  .question-detail {
    gap: 10px;
  }

  .detail-row .label {
    font-size: 11px;
  }

  .detail-row p {
    font-size: 11px;
  }
}

.rec-reason {
  color: #595959;
  margin-bottom: 8px;
  line-height: 1.6;
}

.matched-point {
  margin-top: 8px;
  color: #8c8c8c;
  font-size: 13px;
}
</style>
