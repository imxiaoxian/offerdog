import { onUnmounted, ref } from 'vue'

export interface SpeechRecognitionOptions {
  sessionId?: string
  wsPath?: string
  onTranscript?: (text: string, isFinal?: boolean) => void
  onError?: (error: Error) => void
}

type ConnectionState = 'idle' | 'connecting' | 'connected' | 'error'

const DEFAULT_WS_PATH = '/ws/asr'

function createClientSessionId() {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return crypto.randomUUID()
  }
  return `asr-${Date.now()}`
}

export function useSpeechRecognition(options: SpeechRecognitionOptions = {}) {
  const isRecording = ref(false)
  const transcript = ref('')
  const interimTranscript = ref('')
  const isProcessing = ref(false)
  const connectionState = ref<ConnectionState>('idle')
  const error = ref<string | null>(null)

  let ws: WebSocket | null = null
  let audioContext: AudioContext | null = null
  let mediaStream: MediaStream | null = null
  let processor: ScriptProcessorNode | null = null
  let sourceNode: MediaStreamAudioSourceNode | null = null
  let currentSessionId = options.sessionId || createClientSessionId()
  let finalCloseTimer: number | null = null
  let hasFinalResult = false

  const getWsUrl = () => {
    const protocol = location.protocol === 'https:' ? 'wss' : 'ws'
    const path = options.wsPath || DEFAULT_WS_PATH
    return `${protocol}://${location.host}${path}`
  }

  const connectWebSocket = () =>
    new Promise<void>((resolve, reject) => {
      try {
        connectionState.value = 'connecting'
        ws = new WebSocket(getWsUrl())

        ws.onopen = () => {
          connectionState.value = 'connected'
          ws?.send(
            JSON.stringify({
              type: 'init',
              sessionId: currentSessionId,
            }),
          )
          resolve()
        }

        ws.onerror = (event) => {
          connectionState.value = 'error'
          const err = new Error('语音通道连接失败')
          error.value = err.message
          options.onError?.(err)
          reject(err)
        }

        ws.onclose = () => {
          connectionState.value = 'idle'
          ws = null
          isProcessing.value = false
        }

        ws.onmessage = (event) => {
          if (typeof event.data !== 'string') return

          try {
            const data = JSON.parse(event.data)
            handleServerEvent(data)
          } catch (err) {
            console.warn('ASR 返回数据解析失败:', err)
          }
        }
      } catch (err) {
        reject(err)
      }
    })

  const handleServerEvent = (data: any) => {
    switch (data?.type) {
      case 'backend_connected':
        return
      case 'transcript': {
        const text = extractText(data)
        const isFinal: boolean =
          Boolean(data.isFinal) ||
          Boolean(data?.result?.definite) ||
          Boolean(data?.result?.is_final)

        // 调试日志：便于检查残留 uuid 或异常文本
        // eslint-disable-next-line no-console
        console.debug('[asr] transcript event', {
          rawText: data?.text,
          resultText: data?.result?.text,
          utteranceText: Array.isArray(data?.result?.utterances)
            ? data.result.utterances[0]?.text
            : undefined,
          picked: text,
          isFinal,
        })

        // 如果已经拿到最终结果，再收到任何文本都忽略，防止收尾包覆盖
        if (hasFinalResult) {
          return
        }

        if (text) {
          if (isFinal) {
            transcript.value = text
            interimTranscript.value = ''
            hasFinalResult = true
            options.onTranscript?.(transcript.value, true)
          } else {
            interimTranscript.value = text
            transcript.value = text
            options.onTranscript?.(text, false)
          }
        }

        if (isFinal) {
          isProcessing.value = false
          scheduleSocketClose()
        }
        return
      }
      case 'error': {
        const message = data.message || '语音识别异常'
        const err = new Error(message)
        error.value = message
        options.onError?.(err)
        return
      }
      default:
        return
    }
  }

  const setupAudioPipeline = async () => {
    // Request microphone access
    mediaStream = await navigator.mediaDevices.getUserMedia({
      audio: {
        channelCount: 1,
        sampleRate: 16000,
        sampleSize: 16,
        echoCancellation: true,
        noiseSuppression: true,
      },
    })

    audioContext = new AudioContext({ sampleRate: 16000 })

    // 把浏览器最终采用的采样率告诉后端（部分浏览器会忽略 16000，实际仍是 48000）
    // 后端 Whisper 链路需要正确的 sampleRate 来写 WAV 头，否则容易出现空转写结果。
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(
        JSON.stringify({
          type: 'init',
          sessionId: currentSessionId,
          sampleRate: audioContext.sampleRate,
        }),
      )
    }
    sourceNode = audioContext.createMediaStreamSource(mediaStream)
    processor = audioContext.createScriptProcessor(4096, 1, 1)

    processor.onaudioprocess = (event) => {
      if (!ws || ws.readyState !== WebSocket.OPEN) return
      const input = event.inputBuffer.getChannelData(0)
      const pcm16 = float32ToPcm16(input)
      ws.send(pcm16.buffer)
    }

    sourceNode.connect(processor)
    processor.connect(audioContext.destination)
  }

  const startRecording = async () => {
    if (isRecording.value) return

    error.value = null
    transcript.value = ''
    interimTranscript.value = ''
    isProcessing.value = false
    hasFinalResult = false

    try {
      await connectWebSocket()
      await setupAudioPipeline()
      isRecording.value = true
    } catch (err) {
      await cleanup()
      const friendlyError = new Error('无法开始语音识别，请检查麦克风或网络')
      error.value = friendlyError.message
      options.onError?.(friendlyError)
      console.error('启动语音识别失败:', err)
    }
  }

  const stopRecording = async () => {
    if (!isRecording.value && !isProcessing.value) return

    isRecording.value = false
    isProcessing.value = true

    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({ type: 'end' }))
    }

    await closeAudioPipeline()
    // 如果后端没有返回 isFinal，也保证几秒后释放 WebSocket
    scheduleSocketClose(2000)
  }

  const closeAudioPipeline = async () => {
    processor?.disconnect()
    processor = null

    sourceNode?.disconnect()
    sourceNode = null

    if (audioContext) {
      await audioContext.close()
      audioContext = null
    }

    if (mediaStream) {
      mediaStream.getTracks().forEach((track) => track.stop())
      mediaStream = null
    }
  }

  const cleanup = async () => {
    clearCloseTimer()
    await closeAudioPipeline()
    if (ws) {
      ws.onopen = null
      ws.onmessage = null
      ws.onerror = null
      ws.onclose = null
      if (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING) {
        ws.close()
      }
      ws = null
    }
    connectionState.value = 'idle'
    isProcessing.value = false
  }

  const scheduleSocketClose = (delay = 800) => {
    clearCloseTimer()
    if (ws && ws.readyState === WebSocket.OPEN && !isRecording.value) {
      finalCloseTimer = window.setTimeout(() => {
        ws?.close()
        finalCloseTimer = null
      }, delay)
    }
  }

  const clearCloseTimer = () => {
    if (finalCloseTimer) {
      window.clearTimeout(finalCloseTimer)
      finalCloseTimer = null
    }
  }

  const float32ToPcm16 = (input: Float32Array) => {
    const output = new Int16Array(input.length)
    for (let i = 0; i < input.length; i++) {
      const sample = input[i] ?? 0
      const s = Math.max(-1, Math.min(1, sample))
      output[i] = s < 0 ? s * 0x8000 : s * 0x7fff
    }
    return output
  }

  const extractText = (payload: any): string | null => {
    // 1) 优先使用 result.text
    const resultText = payload?.result?.text
    if (typeof resultText === 'string' && isLikelyTranscript(resultText)) {
      return resultText.trim()
    }

    // 2) 其次尝试从 result.utterances 中取第一条 text
    const utterances = payload?.result?.utterances
    if (Array.isArray(utterances) && utterances.length > 0) {
      const candidate = utterances.find(
        (item: any) => typeof item?.text === 'string' && isLikelyTranscript(item.text),
      )
      if (candidate?.text?.trim() && isLikelyTranscript(candidate.text)) {
        return candidate.text.trim()
      }
    }

    // 3) 顶层 text
    const topLevelText = payload?.text
    if (typeof topLevelText === 'string' && topLevelText.trim() && isLikelyTranscript(topLevelText)) {
      const trimmed = topLevelText.trim()
      // 如果是 JSON 字符串，尝试解析后递归提取
      if (trimmed.startsWith('{') || trimmed.startsWith('[')) {
        try {
          const parsed = JSON.parse(trimmed)
          return extractText(parsed)
        } catch {
          // 无法解析则退回原文本
        }
      }

      return trimmed
    }

    return null
  }

  const isLikelyTranscript = (text: string): boolean => {
    if (!text || typeof text !== 'string') return false
    const trimmed = text.trim()
    if (!trimmed) return false

    // 过滤纯 uuid/hex 串
    if (/^[0-9a-fA-F-]{32,}$/.test(trimmed)) return false
    // 过滤带 {} 的 session 包装
    if (/^[0-9a-fA-F\-{}]+$/.test(trimmed)) return false

    // 包含中文或英文字母则认为是正文
    if (/[a-zA-Z\u4e00-\u9fa5]/.test(trimmed)) return true

    // 长度过短且无文字，忽略
    if (trimmed.length < 4) return false

    return true
  }

  onUnmounted(async () => {
    if (isRecording.value || connectionState.value !== 'idle') {
      await cleanup()
    }
  })

  return {
    isRecording,
    transcript,
    interimTranscript,
    isProcessing,
    connectionState,
    error,
    startRecording,
    stopRecording,
  }
}
