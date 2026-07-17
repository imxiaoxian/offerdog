<script lang="ts" setup>
import { onMounted, onUnmounted, ref, computed, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  SendOutlined,
  CloseCircleOutlined,
  RobotOutlined,
  UserOutlined,
  VideoCameraOutlined,
  StopOutlined,
} from '@ant-design/icons-vue'
import { interviewApi } from '@/services/interview'
import type { InterviewSession, ConversationMessage } from '@/types/interview'
import { useInterviewAudioPlayer } from '@/composables/useInterviewAudioPlayer'
import { MarkdownRender } from 'vue-renderer-markdown'
import { useSpeechRecognition } from '@/composables/useSpeechRecognition'

const route = useRoute()
const router = useRouter()

const sessionId = route.params.sessionId as string
const session = ref<InterviewSession | null>(null)
const messages = ref<ConversationMessage[]>([])
const currentInput = ref('')
const isAITyping = ref(false)
const isLoading = ref(true)
const messagesContainer = ref<HTMLElement | null>(null)
const isVoiceInputEnabled = ref(false)
const speechLiveText = ref('')
const speechFinalText = ref('')
const allowSpeechSync = ref(false)
const canUseVoiceInput = computed(() => {
  const hasMedia = typeof navigator !== 'undefined' && !!navigator.mediaDevices?.getUserMedia
  const hasAudioContext =
    typeof window !== 'undefined' &&
    (Boolean((window as any).AudioContext) || Boolean((window as any).webkitAudioContext))
  return hasMedia && hasAudioContext
})

/** 摄像头/麦克风需安全上下文：HTTPS 或 localhost/127.0.0.1；用局域网 IP + HTTP 时浏览器会禁用 */
const isMediaSecureContext = computed(() =>
  typeof window === 'undefined' ? true : window.isSecureContext,
)

function explainCameraFailure(err: unknown): string {
  const name = err && typeof err === 'object' && 'name' in err ? String((err as { name: string }).name) : ''
  const hints: Record<string, string> = {
    NotAllowedError:
      '已拒绝摄像头权限。请点击地址栏左侧锁形/相机图标，选择「允许」；或在系统设置里允许浏览器访问摄像头。',
    PermissionDeniedError:
      '已拒绝摄像头权限。请在浏览器站点设置中允许摄像头，并确认未开启「禁止询问」类插件。',
    NotFoundError: '未检测到摄像头。请确认设备已连接摄像头，或未被 BIOS/隐私设置关闭。',
    NotReadableError:
      '摄像头无法打开，可能被其它程序占用（微信、腾讯会议、OBS、虚拟摄像头等）。请关闭后重试。',
    OverconstrainedError: '当前摄像头无法满足请求参数，可尝试更新驱动或换用外接摄像头。',
    SecurityError:
      '浏览器因安全策略拒绝访问摄像头。请使用 https:// 访问，或改用 http://127.0.0.1:端口 / http://localhost:端口 打开本站。',
    AbortError: '开启摄像头被中断，请重试。',
  }
  if (name && hints[name]) return hints[name]
  if (err instanceof Error && err.message) return err.message
  return '摄像头开启失败，请检查权限或设备'
}

const {
  startAudioStream,
  stopAudioStream,
  isSpeaking: isAudioSpeaking,
  speakingText: audioSpeakingText,
  connectionState: audioConnectionState,
  lastError: audioError,
  resumeAudioContext,
  audioReady,
  isAudioApiSupported,
  currentVoice: audioVoice,
} = useInterviewAudioPlayer(sessionId)

const {
  isRecording: isSpeechRecording,
  transcript: speechTranscript,
  isProcessing: isSpeechProcessing,
  error: speechError,
  startRecording: startSpeechRecording,
  stopRecording: stopSpeechRecording,
} = useSpeechRecognition({
  sessionId,
  onTranscript: (text: string, isFinal?: boolean) => {
    const chunk = (text || '').trim()
    if (!chunk) return

    if (isFinal) {
      // 最终结果直接覆盖（避免累积的增量出现重复/断句差异）
      speechFinalText.value = chunk
      speechLiveText.value = chunk
      currentInput.value = chunk
      message.success('语音识别完成，请确认后发送')
      return
    }

    // Whisper 伪实时会返回增量片段，这里做追加以便输入框能持续增长
    speechLiveText.value = speechLiveText.value ? `${speechLiveText.value} ${chunk}` : chunk

    // 识别过程中才同步写入输入框，收尾后不再被后续包覆盖
    if (allowSpeechSync.value) {
      currentInput.value = currentInput.value ? `${currentInput.value} ${chunk}` : chunk
    }
  },
  onError: (err: Error) => {
    const errMsg = err.message || '语音识别失败'
    message.error(errMsg)
  },
})

const cameraVideoRef = ref<HTMLVideoElement | null>(null)
const previewStream = ref<MediaStream | null>(null)
const isCameraOn = ref(false)
const isCameraLoading = ref(false)
const cameraError = ref('')

const audioStatusText = computed(() => {
  if (!isAudioApiSupported) return '浏览器不支持AI语音'
  if (isAudioSpeaking.value) return 'AI 正在播报'

  switch (audioConnectionState.value) {
    case 'connecting':
      return '音频通道连接中...'
    case 'connected':
      return '音频通道已连接'
    case 'error':
      return '音频通道异常'
    default:
      return '音频通道未连接'
  }
})

const voiceStatusText = computed(() => {
  if (!canUseVoiceInput.value) return '浏览器不支持语音采集，保持手动输入'
  if (!isVoiceInputEnabled.value) return '当前为手动输入模式'
  if (isSpeechRecording.value) return '录音中，转写结果将实时返回'
  if (isSpeechProcessing.value) return '正在收尾，等待转写结果'
  if (speechTranscript.value) return '识别完成，可在下方文本框中确认后发送'
  return '点击开始语音回答，识别结果会自动填入文本框'
})

// 计算属性
const progress = computed(() => {
  if (!session.value) return 0
  return (
    ((session.value.currentQuestionIndex + 1) / session.value.interviewPlan.total_questions) * 100
  )
})

const isReadOnlySession = computed(() => session.value?.status !== 'in_progress')

const currentQuestion = computed(() => {
  if (!session.value) return null
  return session.value.interviewPlan.questions[session.value.currentQuestionIndex]
})

const canSubmit = computed(() => {
  return (
    currentInput.value.trim().length > 0 &&
    !isAITyping.value &&
    session.value?.status === 'in_progress'
  )
})

const loadConversationHistory = async () => {
  try {
    const historyRes = await interviewApi.getMessages(sessionId)
    messages.value = historyRes.data
      .slice()
      .sort((a, b) => (a.sequenceNumber ?? 0) - (b.sequenceNumber ?? 0))
  } catch (error) {
    console.error('加载历史消息失败:', error)
    message.warning('加载历史消息失败')
  }
}

// 初始化
onMounted(async () => {
  startAudioStream()
  await resumeAudioContext()
  try {
    // 1. 获取会话信息
    const sessionRes = await interviewApi.getSession(sessionId)
    session.value = sessionRes.data

    console.log('📋 会话信息:', session.value)

    // 2. 加载历史消息
    await loadConversationHistory()

    // 3. 如果是新会话且没有历史记录，获取AI开场白
    const shouldFetchOpening =
      messages.value.length === 0 &&
      (session.value.totalMessagesCount === undefined || session.value.totalMessagesCount === 0)
    if (shouldFetchOpening) {
      try {
        const openingRes = await interviewApi.getOpening(sessionId)
        const openingText = openingRes.data
        console.log('🎤 AI开场白:', openingText)

        const firstQuestionId = session.value.interviewPlan.questions[0]?.question_id || 'opening'

        messages.value.push({
          sessionId,
          role: 'interviewer',
          content: openingText,
          relatedQuestionId: firstQuestionId,
          sequenceNumber: 1,
          timestamp: new Date().toISOString(),
        })
      } catch (error) {
        console.error('获取开场白失败:', error)
        message.warning('获取AI开场白失败')
      }
    }

    isLoading.value = false

    await nextTick()
    scrollToBottom()
  } catch (error) {
    console.error('初始化失败:', error)
    message.error('加载面试会话失败')
    router.push('/ai-interviewer')
  }
})

onUnmounted(() => {
  stopAudioStream()
  stopCamera()
})

// 滚动到底部
const scrollToBottom = async (smooth = true) => {
  await nextTick()
  if (messagesContainer.value) {
    if (smooth) {
      messagesContainer.value.scrollTo({
        top: messagesContainer.value.scrollHeight,
        behavior: 'smooth',
      })
    } else {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  }
}

// 监听消息变化，自动滚动
watch(
  () => messages.value.length,
  () => {
    scrollToBottom()
  },
)

// 监听AI输入状态变化，自动滚动
watch(isAITyping, () => {
  if (isAITyping.value) {
    scrollToBottom()
  }
})

watch(isReadOnlySession, (readonly) => {
  if (readonly) {
    isVoiceInputEnabled.value = false
    if (isSpeechRecording.value) {
      stopSpeechRecording()
    }
  }
})

const stopCamera = () => {
  previewStream.value?.getTracks().forEach((t) => t.stop())
  previewStream.value = null
  isCameraOn.value = false
  if (cameraVideoRef.value) {
    cameraVideoRef.value.srcObject = null
  }
}

const startCamera = async () => {
  if (typeof window !== 'undefined' && !window.isSecureContext) {
    cameraError.value =
      '当前通过非安全地址访问（常见为 http://局域网IP:端口）。浏览器会禁止摄像头，请改用 http://127.0.0.1:5173 或 https:// 访问。'
    return
  }
  if (!navigator.mediaDevices?.getUserMedia) {
    cameraError.value =
      '当前环境无法使用摄像头 API（可能仍为非安全上下文或浏览器过旧）。请用 Chrome/Edge 最新版，并通过 localhost 或 HTTPS 打开。'
    return
  }
  isCameraLoading.value = true
  cameraError.value = ''
  try {
    stopCamera()
    let stream: MediaStream
    try {
      stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: 'user' },
        audio: false,
      })
    } catch {
      stream = await navigator.mediaDevices.getUserMedia({ video: true, audio: false })
    }
    previewStream.value = stream
    if (cameraVideoRef.value) {
      cameraVideoRef.value.srcObject = stream
      await cameraVideoRef.value.play().catch(() => { })
    }
    isCameraOn.value = true
  } catch (err: unknown) {
    cameraError.value = explainCameraFailure(err)
    stopCamera()
  } finally {
    isCameraLoading.value = false
  }
}

// 提交回答
const submitAnswer = async () => {
  if (!canSubmit.value || !currentQuestion.value) return

  const answer = currentInput.value.trim()
  currentInput.value = ''

  // 立即显示用户消息
  const userMessage: ConversationMessage = {
    sessionId,
    role: 'candidate',
    content: answer,
    relatedQuestionId: currentQuestion.value.question_id,
    sequenceNumber: messages.value.length + 1,
    timestamp: new Date().toISOString(),
  }
  messages.value.push(userMessage)

  // 标记AI正在输入
  isAITyping.value = true

  try {
    // 提交回答并获取AI响应
    const response = await interviewApi.submitAnswer(sessionId, {
      sessionId,
      questionId: currentQuestion.value.question_id,
      answer,
    })

    console.log('🤖 AI响应:', response.data)

    // 显示AI响应
    if (response.data && response.data.content) {
      isAITyping.value = false

      messages.value.push({
        sessionId,
        role: 'interviewer',
        content: response.data.content,
        relatedQuestionId: currentQuestion.value.question_id,
        sequenceNumber: response.data.sequenceNumber || messages.value.length + 1,
        timestamp: new Date().toISOString(),
      })

      // 更新会话状态
      if (response.data.completed && session.value) {
        session.value.status = 'completed'
        message.success('面试已结束，正在生成报告...')

        // 调用后端生成报告接口
        try {
          await interviewApi.generateReport(sessionId)
          message.info('报告生成任务已提交，请稍候...')
        } catch (err) {
          console.error('提交报告生成任务失败:', err)
        }

        // 3秒后跳转到报告页面
        setTimeout(() => {
          router.push(`/interview/report/${sessionId}`)
        }, 3000)
      }

      // 更新当前问题索引
      if (session.value && response.data.currentQuestionIndex !== undefined) {
        session.value.currentQuestionIndex = response.data.currentQuestionIndex

        // 更新问题完成数
        if (response.data.action === 'NEXT_QUESTION') {
          session.value.questionsCompleted += 1
          message.info('进入下一个问题')
        } else if (response.data.action === 'FOLLOW_UP') {
          message.info('AI进行追问')
        }
      }
    }
  } catch (error) {
    console.error('提交回答失败:', error)
    message.error('提交回答失败,请重试')
    isAITyping.value = false
  }
}

// 结束面试
const endInterview = async () => {
  try {
    // 1. 调用结束面试接口
    await interviewApi.endInterview(sessionId)
    if (session.value) {
      session.value.status = 'completed'
    }
    message.success('面试已结束')

    // 2. 触发报告生成
    try {
      await interviewApi.generateReport(sessionId)
      message.info('报告生成任务已提交，请稍候...')
    } catch (err) {
      console.error('提交报告生成任务失败:', err)
    }

    // 3. 跳转到报告页面
    setTimeout(() => {
      router.push(`/interview/report/${sessionId}`)
    }, 2000)
  } catch (error) {
    console.error('结束面试失败:', error)
    message.error('结束面试失败')
  }
}

// 格式化时间
const formatTime = (timestamp: string) => {
  const date = new Date(timestamp)
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

// 处理键盘事件
const handleKeyDown = (e: KeyboardEvent) => {
  if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) {
    e.preventDefault()
    submitAnswer()
  }
}

const handleVoiceToggle = (checked: boolean) => {
  if (checked && !canUseVoiceInput.value) {
    message.warning('当前环境不支持语音识别')
    return
  }
  speechLiveText.value = ''
  speechFinalText.value = ''
  isVoiceInputEnabled.value = checked
  if (!checked && isSpeechRecording.value) {
    stopSpeechRecording()
  }
}

const toggleSpeechRecording = async () => {
  if (!isVoiceInputEnabled.value) {
    message.info('请先开启语音输入开关')
    return
  }

  if (!canUseVoiceInput.value) {
    message.warning('当前环境不支持语音识别')
    return
  }

  try {
    if (isSpeechRecording.value) {
      allowSpeechSync.value = false
      await stopSpeechRecording()

      // 停止后写入最终文本，优先使用最终块，其次使用最新实时内容
      if (speechFinalText.value) {
        currentInput.value = speechFinalText.value
      } else if (speechLiveText.value) {
        currentInput.value = speechLiveText.value
      }
    } else {
      speechLiveText.value = ''
      speechFinalText.value = ''
      allowSpeechSync.value = true
      await startSpeechRecording()
    }
  } catch (err) {
    console.error('语音控制失败:', err)
    message.error('语音输入控制失败，请重试')
  }
}

const clearCurrentInput = () => {
  currentInput.value = ''
}

</script>

<template>
  <div class="interview-room">
    <a-spin class="room-spin" :spinning="isLoading" size="large" tip="加载面试会话中...">
      <template v-if="!isLoading && session">
        <div class="room-shell">
          <header class="session-header">
            <div class="session-meta">
              <div class="meta-block">
                <span class="label">面试进度</span>
                <strong>{{ session.currentQuestionIndex + 1 }}/{{ session.interviewPlan.total_questions }}</strong>
              </div>
              <div class="meta-block">
                <span class="label">当前话题</span>
                <strong>{{ currentQuestion?.topic || '未知' }}</strong>
              </div>
              <div class="meta-block">
                <span class="label">已完成</span>
                <strong>{{ session.questionsCompleted }} 个</strong>
              </div>
              <div class="meta-block">
                <span class="label">状态</span>
                <a-tag :color="session.status === 'in_progress' ? 'green' : 'default'">
                  {{ session.status === 'in_progress' ? '进行中' : '已完成' }}
                </a-tag>
              </div>
            </div>
            <div class="header-progress">
              <a-progress :percent="progress" :show-info="false" stroke-color="#52c41a" />
              <div v-if="isAudioApiSupported" class="audio-status" :class="{ speaking: isAudioSpeaking }">
                <span>
                  {{ audioStatusText }}
                  <template v-if="isAudioSpeaking && audioSpeakingText">：{{ audioSpeakingText }}</template>
                </span>
                <a-tag v-if="audioVoice" size="small">{{ audioVoice }}</a-tag>
                <a-button v-if="!audioReady" size="small" @click="resumeAudioContext">启用声音</a-button>
              </div>
              <a-alert
                v-else
                class="audio-alert"
                type="warning"
                show-icon
                message="当前浏览器不支持AI语音播放"
              />
              <a-alert
                v-if="audioError"
                class="audio-alert"
                type="warning"
                show-icon
                :message="audioError"
              />
            </div>
          </header>

          <div class="room-grid">
            <section class="chat-panel">
              <div ref="messagesContainer" class="messages-container">
                <div
                  v-for="(msg, index) in messages"
                  :key="index"
                  :class="['message-item', msg.role === 'interviewer' ? 'ai-message' : 'user-message']"
                >
                  <div class="message-avatar">
                    <RobotOutlined v-if="msg.role === 'interviewer'" class="avatar-icon ai" />
                    <UserOutlined v-else class="avatar-icon user" />
                  </div>
                  <div class="message-content">
                    <div class="message-text">
                      <MarkdownRender class="markdown-body" :content="msg.content" />
                    </div>
                    <div class="message-time">{{ formatTime(msg.timestamp) }}</div>
                  </div>
                </div>

                <div v-if="isAITyping" class="message-item ai-message typing">
                  <div class="message-avatar">
                    <RobotOutlined class="avatar-icon ai" />
                  </div>
                  <div class="message-content">
                    <div class="typing-indicator">
                      <span></span>
                      <span></span>
                      <span></span>
                    </div>
                    <div class="typing-text">AI正在思考...</div>
                  </div>
                </div>
              </div>

              <div class="input-area" :class="{ 'input-area--readonly': isReadOnlySession }">
                <template v-if="!isReadOnlySession">
                  <div class="input-form">
                    <p class="multimodal-hint">
                      支持<strong>语音</strong>与<strong>文字</strong>两种输入：语音经识别后与键盘输入共用下方文本框，提交后由 AI
                      面试官多轮对话、结合关键词追问并控制面试节奏。
                    </p>
                    <div class="input-mode-bar">
                      <div class="mode-info">
                        <span class="mode-label">语音输入</span>
                        <a-tag v-if="isVoiceInputEnabled" color="blue">已开启</a-tag>
                        <a-tag v-else color="default">手动输入</a-tag>
                      </div>
                      <a-switch
                        :checked="isVoiceInputEnabled"
                        checked-children="开启"
                        un-checked-children="关闭"
                        :disabled="!canUseVoiceInput"
                        @change="handleVoiceToggle"
                      />
                    </div>
                    <a-alert
                      v-if="!canUseVoiceInput"
                      class="voice-alert"
                      type="info"
                      show-icon
                      message="当前浏览器无法开启语音识别，将保持手动输入"
                    />
                    <div v-if="isVoiceInputEnabled" class="voice-panel">
                      <div class="voice-panel-row">
                        <div class="voice-status">
                          <a-tag v-if="isSpeechRecording" color="red">录音中</a-tag>
                          <a-tag v-else-if="isSpeechProcessing" color="blue">识别中</a-tag>
                          <a-tag v-else color="default">待机</a-tag>
                          <span class="voice-status-text">{{ voiceStatusText }}</span>
                        </div>
                        <div class="voice-actions">
                          <a-button
                            type="primary"
                            :danger="isSpeechRecording"
                            :loading="isSpeechProcessing"
                            @click="toggleSpeechRecording"
                          >
                            {{ isSpeechRecording ? '停止录音' : '开始语音回答' }}
                          </a-button>
                          <a-button v-if="currentInput" type="link" size="small" @click="clearCurrentInput">
                            清空内容
                          </a-button>
                        </div>
                      </div>
                      <div class="voice-panel-row" v-if="speechTranscript">
                        <span class="voice-result-label">最新识别</span>
                        <a-tag color="green">已写入输入框，可直接发送</a-tag>
                      </div>
                      <a-alert v-if="speechError" type="warning" show-icon class="voice-alert" :message="speechError" />
                      <p class="voice-hint">
                        火山引擎实时转写结果会自动写入下方输入框，可确认后直接发送。
                      </p>
                    </div>
                    <a-textarea
                      v-model:value="currentInput"
                      :rows="4"
                      :disabled="isAITyping"
                      :placeholder="isAITyping ? 'AI正在回复中，请稍候...' : '输入你的回答... (Ctrl+Enter发送)'"
                      @keydown="handleKeyDown"
                    />
                    <div class="input-actions">
                      <a-button danger @click="endInterview" :disabled="isAITyping">
                        <template #icon><CloseCircleOutlined /></template>
                        结束面试
                      </a-button>
                      <a-button type="primary" :disabled="!canSubmit" :loading="isAITyping" @click="submitAnswer">
                        <template #icon><SendOutlined /></template>
                        {{ isAITyping ? '等待AI回复...' : '发送 (Ctrl+Enter)' }}
                      </a-button>
                    </div>
                  </div>
                </template>
                <template v-else>
                  <div class="readonly-message">
                    <div class="readonly-text">本场面试已结束，可继续查看完整对话记录。</div>
                    <div class="readonly-actions">
                      <a-button type="primary" @click="router.push(`/interview/report/${sessionId}`)">
                        查看报告
                      </a-button>
                      <a-button @click="router.push('/ai-interviewer')"> 返回面试列表 </a-button>
                    </div>
                  </div>
                </template>
              </div>
            </section>

            <aside class="side-panel">
              <a-card class="side-card" title="摄像头预览" :bordered="false">
                <a-alert
                  v-if="!isMediaSecureContext"
                  type="info"
                  show-icon
                  style="margin-bottom: 12px"
                  message="请用 127.0.0.1 或 HTTPS 访问"
                  description="通过 http://192.168.x.x 等打开时，浏览器会禁用摄像头。开发环境请访问 http://127.0.0.1:5173（或配置 Vite HTTPS）。"
                />
                <div class="camera-actions">
                  <a-button
                    type="primary"
                    block
                    :loading="isCameraLoading"
                    :disabled="!isMediaSecureContext"
                    @click="isCameraOn ? stopCamera() : startCamera()"
                  >
                    <template #icon>
                      <VideoCameraOutlined v-if="!isCameraOn" />
                      <StopOutlined v-else />
                    </template>
                    {{ isCameraOn ? '关闭摄像头' : '开启摄像头' }}
                  </a-button>
                  <a-tag :color="isCameraOn ? 'green' : 'default'">
                    {{ isCameraOn ? '预览中' : '未开启' }}
                  </a-tag>
                </div>
                <div class="camera-preview" :class="{ off: !isCameraOn }">
                  <video ref="cameraVideoRef" autoplay playsinline muted></video>
                  <div v-if="!isCameraOn" class="camera-placeholder">仅本地预览，不会上传</div>
                </div>
                <a-alert v-if="cameraError" type="warning" :message="cameraError" show-icon />
              </a-card>
            </aside>
          </div>
        </div>
      </template>
    </a-spin>
  </div>
</template>

<style scoped>
.interview-room {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 64px);
  background: #f5f5f5;
}

.room-spin,
:deep(.room-spin .ant-spin-nested-loading),
:deep(.room-spin .ant-spin-container) {
  flex: 1;
  display: flex;
}

.room-shell {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 12px 16px 20px;
}

.session-header {
  background: white;
  border-radius: 12px;
  padding: 14px 16px;
  box-shadow: 0 6px 14px rgba(0, 0, 0, 0.06);
  display: grid;
  gap: 8px;
}

.session-meta {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 10px;
}

.meta-block {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #1f2937;
  font-weight: 600;
}

.meta-block .label {
  color: #6b7280;
  font-weight: 500;
}

.header-progress {
  display: grid;
  gap: 8px;
}

.audio-status {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #4b5563;
}

.audio-status.speaking {
  color: #1890ff;
  font-weight: 600;
}

.audio-alert {
  margin: 0;
}

.room-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 14px;
  align-items: start;
  min-height: 0;
}

.chat-panel {
  display: flex;
  flex-direction: column;
  background: white;
  border-radius: 12px;
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.05);
  min-height: 0;
  overflow: hidden;
}

.side-panel {
  position: sticky;
  top: 80px;
  display: grid;
  gap: 12px;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 18px 18px 140px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  background: #fafafa;
}

.message-item {
  display: flex;
  gap: 12px;
  animation: fadeIn 0.3s ease-in;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.ai-message {
  align-self: flex-start;
  max-width: 75%;
}

.user-message {
  align-self: flex-end;
  flex-direction: row-reverse;
  max-width: 75%;
}

.message-avatar {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-icon {
  font-size: 20px;
}

.avatar-icon.ai {
  color: #1890ff;
  background: #e6f7ff;
  padding: 10px;
  border-radius: 50%;
}

.avatar-icon.user {
  color: #52c41a;
  background: #f6ffed;
  padding: 10px;
  border-radius: 50%;
}

.message-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.message-text {
  background: white;
  padding: 14px 18px;
  border-radius: 8px;
  line-height: 1.75;
  word-wrap: break-word;
  white-space: normal;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  font-size: 16px;
}

.message-text :deep(p) {
  margin: 0 0 8px;
}

.message-text :deep(p:last-child) {
  margin-bottom: 0;
}

.message-text :deep(pre) {
  background: #0f172a;
  color: #e2e8f0;
  padding: 12px;
  border-radius: 6px;
  overflow-x: auto;
  margin: 8px 0;
  font-size: 13px;
}

.message-text :deep(code) {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 14px;
  background: #f5f5f5;
  padding: 2px 4px;
  border-radius: 4px;
}

.message-text :deep(pre code) {
  background: transparent;
  padding: 0;
}

.message-text :deep(ul),
.message-text :deep(ol) {
  padding-left: 20px;
  margin: 8px 0;
}

.message-text :deep(blockquote) {
  border-left: 3px solid #91d5ff;
  margin: 8px 0;
  padding-left: 12px;
  color: #4a4a4a;
  background: #f0f9ff;
}

.message-text :deep(a) {
  color: #1890ff;
}

.input-area {
  background: white;
  border-top: 1px solid #e8e8e8;
  padding: 16px 20px;
  position: sticky;
  bottom: 0;
  z-index: 5;
  box-shadow: 0 -6px 12px rgba(0, 0, 0, 0.04);
}

.input-area--readonly {
  background: #fafafa;
  box-shadow: none;
}

.readonly-message {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 12px 0;
}

.readonly-text {
  color: #595959;
}

.readonly-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.user-message .message-text {
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
  color: white;
}

.message-time {
  font-size: 12px;
  color: #8c8c8c;
  padding: 0 4px;
}

.user-message .message-time {
  text-align: right;
}

.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 12px 16px;
  background: white;
  border-radius: 8px;
  width: 60px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #1890ff;
  animation: typing 1.4s infinite;
}

.typing-indicator span:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-indicator span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0%,
  60%,
  100% {
    opacity: 0.3;
    transform: translateY(0);
  }
  30% {
    opacity: 1;
    transform: translateY(-8px);
  }
}

.typing-text {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 4px;
}

.input-area {
  background: white;
  border-top: 1px solid #e8e8e8;
  padding: 16px 24px;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.05);
}

.input-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.multimodal-hint {
  margin: 0;
  font-size: 13px;
  color: #595959;
  line-height: 1.55;
}

.input-mode-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.mode-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.mode-label {
  color: #262626;
  font-weight: 500;
}

.voice-panel {
  border: 1px dashed #d9d9d9;
  border-radius: 8px;
  padding: 12px;
  background: #fafafa;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.voice-panel-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.voice-status {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.voice-status-text {
  font-size: 13px;
  color: #595959;
}

.voice-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.voice-result-label {
  color: #8c8c8c;
  font-size: 13px;
}

.voice-hint {
  font-size: 12px;
  color: #8c8c8c;
  margin: 0;
}

.voice-alert {
  margin-top: 4px;
}

.input-actions {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.interview-ended {
  padding: 40px 0;
}

.side-card :deep(.ant-card-head) {
  border: none;
  padding-bottom: 8px;
}

.camera-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.camera-preview {
  position: relative;
  width: 100%;
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  background: #f7f9fb;
  overflow: hidden;
  aspect-ratio: 4 / 3;
}

.camera-preview.off {
  background: repeating-linear-gradient(
      45deg,
      rgba(0, 0, 0, 0.02),
      rgba(0, 0, 0, 0.02) 10px,
      rgba(0, 0, 0, 0.04) 10px,
      rgba(0, 0, 0, 0.04) 20px
    ),
    #f7f9fb;
}

.camera-preview video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.camera-placeholder {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #8c8c8c;
  font-size: 13px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .interview-room {
    height: calc(100vh - 56px);
  }

  .room-shell {
    padding: 8px 12px 120px;
  }

  .session-header {
    padding: 10px 12px;
  }

  .session-meta {
    grid-template-columns: repeat(2, 1fr);
    gap: 8px;
  }

  .meta-block {
    font-size: 12px;
  }

  .header-progress {
    gap: 6px;
  }

  .room-grid {
    grid-template-columns: 1fr;
  }

  .side-panel {
    position: static;
  }

  .ai-message,
  .user-message {
    max-width: 85%;
  }

  .message-text {
    font-size: 14px;
    padding: 10px 12px;
  }

  .messages-container {
    padding: 12px;
  }

  .input-area {
    padding: 10px 12px;
  }

  .input-area {
    padding-bottom: 120px;
  }

  .avatar-icon {
    width: 32px;
    height: 32px;
    font-size: 16px;
    padding: 8px;
  }

  .meta-block .label {
    font-size: 11px;
  }

  .meta-block {
    gap: 4px;
  }
}

@media (max-width: 480px) {
  .interview-room {
    height: calc(100vh - 52px);
  }

  .room-shell {
    padding: 6px 10px 110px;
  }

  .session-header {
    padding: 8px 10px;
  }

  .session-meta {
    grid-template-columns: 1fr;
  }

  .message-text {
    font-size: 13px;
    padding: 8px 10px;
  }

  .avatar-icon {
    width: 28px;
    height: 28px;
    font-size: 14px;
    padding: 6px;
  }

  .input-area {
    padding-bottom: 110px;
  }
}
</style>
