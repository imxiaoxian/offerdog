<template>
  <div class="speech-recognition">
    <div class="recognition-container">
      <div class="status-indicator" :class="{ active: isRecording, processing: isProcessing }">
        <div class="pulse"></div>
        <el-icon :size="40">
          <Microphone v-if="!isRecording" />
          <VideoPlay v-else />
        </el-icon>
      </div>

      <div class="controls">
        <el-button
          v-if="!isRecording"
          type="primary"
          size="large"
          :loading="isProcessing"
          @click="handleStart"
        >
          开始录音
        </el-button>
        <el-button
          v-else
          type="danger"
          size="large"
          @click="handleStop"
        >
          停止录音
        </el-button>
      </div>

      <div v-if="error" class="error-message">
        <el-alert :title="error" type="error" :closable="false" />
      </div>

      <div v-if="transcript" class="transcript-box">
        <div class="transcript-header">
          <span>识别结果</span>
          <el-button size="small" text @click="copyTranscript">
            <el-icon><DocumentCopy /></el-icon>
            复制
          </el-button>
        </div>
        <div class="transcript-content">
          {{ transcript }}
        </div>
      </div>

      <div v-if="interimTranscript" class="interim-transcript">
        <span class="label">实时识别：</span>
        <span class="text">{{ interimTranscript }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { Microphone, VideoPlay, DocumentCopy } from '@element-plus/icons-vue'
import { useSpeechRecognition } from '@/composables/useSpeechRecognition'

interface Props {
  sessionId?: string
  wsPath?: string
}

const props = withDefaults(defineProps<Props>(), {
  sessionId: '',
  wsPath: '',
})

const emit = defineEmits<{
  transcript: [text: string]
  error: [error: Error]
}>()

const {
  isRecording,
  transcript,
  interimTranscript,
  isProcessing,
  error,
  startRecording,
  stopRecording,
} = useSpeechRecognition({
  sessionId: props.sessionId || undefined,
  wsPath: props.wsPath || undefined,
  onTranscript: (text, isFinal) => {
    if (isFinal) {
      emit('transcript', text)
      ElMessage.success('语音识别完成')
    }
  },
  onError: (err) => {
    emit('error', err)
  },
})

const handleStart = async () => {
  await startRecording()
}

const handleStop = async () => {
  await stopRecording()
}

const copyTranscript = () => {
  if (transcript.value) {
    navigator.clipboard.writeText(transcript.value)
    ElMessage.success('已复制到剪贴板')
  }
}
</script>

<style scoped>
.speech-recognition {
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 2rem;
}

.recognition-container {
  max-width: 600px;
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1.5rem;
}

.status-indicator {
  position: relative;
  width: 100px;
  height: 100px;
  display: flex;
  justify-content: center;
  align-items: center;
  border-radius: 50%;
  background: #f0f0f0;
  transition: all 0.3s ease;
}

.status-indicator.active {
  background: #ff4444;
  color: white;
}

.status-indicator.processing {
  background: #ffa500;
  color: white;
}

.status-indicator .pulse {
  position: absolute;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  opacity: 0;
}

.status-indicator.active .pulse {
  animation: pulse 1.5s ease-out infinite;
}

@keyframes pulse {
  0% {
    transform: scale(1);
    opacity: 0.5;
  }
  100% {
    transform: scale(1.5);
    opacity: 0;
  }
}

.controls {
  display: flex;
  gap: 1rem;
}

.error-message {
  width: 100%;
}

.transcript-box {
  width: 100%;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  overflow: hidden;
}

.transcript-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 1rem;
  background: #f5f5f5;
  border-bottom: 1px solid #e0e0e0;
  font-weight: 500;
}

.transcript-content {
  padding: 1rem;
  min-height: 100px;
  max-height: 300px;
  overflow-y: auto;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.interim-transcript {
  width: 100%;
  padding: 0.75rem 1rem;
  background: #f9f9f9;
  border-radius: 8px;
  font-size: 0.9rem;
  color: #666;
}

.interim-transcript .label {
  font-weight: 500;
  margin-right: 0.5rem;
}

.interim-transcript .text {
  font-style: italic;
}
</style>
