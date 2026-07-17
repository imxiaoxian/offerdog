<template>
  <div class="speech-test-page">
    <el-card class="page-card">
      <template #header>
        <div class="card-header">
          <h2>语音识别测试</h2>
          <el-tag type="success">火山引擎实时转写</el-tag>
        </div>
      </template>

      <div class="content">
        <el-alert
          title="使用说明"
          type="info"
          :closable="false"
          class="info-alert"
        >
          <p>1. 浏览器会通过 WebSocket 将麦克风 PCM 音频推送到后端 /ws/asr</p>
          <p>2. 后端转发到火山引擎 ASR，实时返回"transcript"消息</p>
          <p>3. 开始录音后讲话，结束后会自动输出识别文本</p>
        </el-alert>

        <SpeechRecognition @transcript="handleTranscript" @error="handleError" />

        <el-divider />

        <div v-if="transcriptHistory.length > 0" class="history-section">
          <h3>识别历史</h3>
          <el-timeline>
            <el-timeline-item
              v-for="(item, index) in transcriptHistory"
              :key="index"
              :timestamp="item.timestamp"
              placement="top"
            >
              <el-card>
                <p>{{ item.text }}</p>
              </el-card>
            </el-timeline-item>
          </el-timeline>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import SpeechRecognition from '@/components/SpeechRecognition.vue'

interface TranscriptItem {
  text: string
  timestamp: string
}

const transcriptHistory = ref<TranscriptItem[]>([])

const handleTranscript = (text: string) => {
  console.log('识别结果:', text)
  
  // 添加到历史记录
  transcriptHistory.value.unshift({
    text,
    timestamp: new Date().toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    }),
  })

  // 限制历史记录数量
  if (transcriptHistory.value.length > 10) {
    transcriptHistory.value = transcriptHistory.value.slice(0, 10)
  }
}

const handleError = (error: Error) => {
  console.error('识别错误:', error)
  ElMessage.error(`语音识别失败: ${error.message}`)
}
</script>

<style scoped>
.speech-test-page {
  width: 100%;
  min-height: 100vh;
  padding: 2rem;
  background: #f5f7fa;
  display: flex;
  justify-content: center;
}

.page-card {
  width: 100%;
  max-width: 1000px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h2 {
  margin: 0;
  font-size: 1.5rem;
}

.content {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.info-alert {
  margin-bottom: 1rem;
}

.info-alert p {
  margin: 0.5rem 0;
  line-height: 1.6;
}

.history-section {
  margin-top: 1rem;
}

.history-section h3 {
  margin-bottom: 1rem;
  font-size: 1.2rem;
  color: #333;
}

.el-timeline {
  padding-left: 0;
}

@media (max-width: 768px) {
  .speech-test-page {
    padding: 1rem;
  }

  .page-card {
    max-width: 100%;
  }

  .card-header h2 {
    font-size: 1.2rem;
  }

  .content {
    gap: 1rem;
  }

  .info-alert p {
    font-size: 0.9rem;
  }

  .history-section h3 {
    font-size: 1rem;
  }
}

@media (max-width: 480px) {
  .speech-test-page {
    padding: 0.8rem;
  }

  .card-header h2 {
    font-size: 1rem;
  }

  .content {
    gap: 0.8rem;
  }

  .info-alert p {
    font-size: 0.8rem;
  }

  .history-section h3 {
    font-size: 0.9rem;
  }
}
</style>
