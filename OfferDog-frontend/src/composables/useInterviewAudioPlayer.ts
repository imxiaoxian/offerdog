import { computed, onBeforeUnmount, ref, type Ref } from 'vue'
import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { WS_BASE_URL } from '@/config/env'
import type { AudioStreamEvent } from '@/types/interview'

const PCM_SAMPLE_RATE = 24000

type ConnectionState = 'idle' | 'connecting' | 'connected' | 'error'

export interface UseInterviewAudioPlayerOptions {
  autoStart?: boolean
}

const getSessionIdValue = (sessionId: Ref<string> | string): string =>
  typeof sessionId === 'string' ? sessionId : sessionId.value

export const useInterviewAudioPlayer = (
  sessionId: Ref<string> | string,
  _options: UseInterviewAudioPlayerOptions = {},
) => {
  const connectionState = ref<ConnectionState>('idle')
  const speakingText = ref('')
  const currentVoice = ref('')
  const lastError = ref('')
  const lastSequenceNumber = ref<number | null>(null)
  const hasActiveSpeech = ref(false)
  const audioReady = ref(false)
  const isAudioApiSupported =
    typeof window !== 'undefined' && (!!window.AudioContext || !!(window as any).webkitAudioContext)

  let stompClient: Client | null = null
  let subscription: StompSubscription | null = null
  let audioCtx: AudioContext | null = null
  let chunkQueue: AudioBuffer[] = []
  let currentSource: AudioBufferSourceNode | null = null
  let isPlaying = false
  let pendingComplete = false

  const isSpeaking = computed(() => hasActiveSpeech.value || isPlaying || chunkQueue.length > 0)

  const ensureAudioContext = () => {
    if (audioCtx) return audioCtx
    if (typeof window === 'undefined') return null
    const AudioContextConstructor = window.AudioContext || (window as any).webkitAudioContext
    if (!AudioContextConstructor) return null
    audioCtx = new AudioContextConstructor()
    return audioCtx
  }

  const resumeAudioContext = async () => {
    const ctx = ensureAudioContext()
    if (!ctx) {
      lastError.value = '当前浏览器不支持音频播放'
      return
    }
    if (ctx.state === 'suspended') {
      try {
        await ctx.resume()
      } catch (error) {
        console.error('恢复 AudioContext 失败', error)
        lastError.value = '浏览器阻止了自动播放，请点击页面启用音频'
        return
      }
    }
    audioReady.value = true
  }

  const resetPlayback = () => {
    chunkQueue = []
    pendingComplete = false
    isPlaying = false
    if (currentSource) {
      try {
        currentSource.stop()
      } catch (error) {
        console.warn('停止音频source失败', error)
      }
      currentSource.disconnect()
      currentSource = null
    }
  }

  const finishSpeech = () => {
    hasActiveSpeech.value = false
    speakingText.value = ''
    pendingComplete = false
  }

  const playNextChunk = () => {
    if (!audioCtx) return
    if (!chunkQueue.length) {
      isPlaying = false
      if (pendingComplete) {
        finishSpeech()
      }
      return
    }

    isPlaying = true
    const buffer = chunkQueue.shift()!
    const source = audioCtx.createBufferSource()
    source.buffer = buffer
    source.connect(audioCtx.destination)
    source.onended = () => {
      source.disconnect()
      if (currentSource === source) {
        currentSource = null
      }
      playNextChunk()
    }
    currentSource = source
    try {
      source.start()
    } catch (error) {
      console.error('播放音频失败', error)
      lastError.value = '音频播放失败'
      isPlaying = false
    }
  }

  const enqueueChunk = async (buffer: AudioBuffer) => {
    chunkQueue.push(buffer)
    await resumeAudioContext()
    if (!isPlaying) {
      playNextChunk()
    }
  }

  const decodePcmChunk = (base64Audio: string): AudioBuffer | null => {
    const ctx = ensureAudioContext()
    if (!ctx) return null
    if (typeof atob !== 'function') {
      lastError.value = '当前环境不支持Base64解码'
      return null
    }

    const binary = atob(base64Audio)
    const bytes = new Uint8Array(binary.length)
    for (let i = 0; i < binary.length; i += 1) {
      bytes[i] = binary.charCodeAt(i)
    }

    const sampleCount = Math.floor(bytes.length / 2)
    const buffer = ctx.createBuffer(1, sampleCount, PCM_SAMPLE_RATE)
    const channelData = buffer.getChannelData(0)

    for (let i = 0, offset = 0; i < sampleCount; i += 1, offset += 2) {
      const low = bytes[offset] ?? 0
      const high = bytes[offset + 1] ?? 0
      let sample = low | (high << 8)
      if (sample >= 0x8000) {
        sample = sample - 0x10000
      }
      channelData[i] = sample / 0x8000
    }

    return buffer
  }

  const handleStart = (event: AudioStreamEvent) => {
    lastSequenceNumber.value = event.sequenceNumber
    speakingText.value = event.text ?? ''
    currentVoice.value = event.voice ?? ''
    hasActiveSpeech.value = true
    pendingComplete = false
    resetPlayback()
  }

  const handleChunk = async (event: AudioStreamEvent) => {
    if (lastSequenceNumber.value !== event.sequenceNumber) {
      // 丢弃旧的 chunk
      return
    }

    if (!event.base64Audio) return
    const buffer = decodePcmChunk(event.base64Audio)
    if (buffer) {
      await enqueueChunk(buffer)
    }
  }

  const handleComplete = () => {
    pendingComplete = true
    if (!isPlaying && chunkQueue.length === 0) {
      finishSpeech()
    }
  }

  const handleError = (message: string) => {
    lastError.value = message
    pendingComplete = false
    hasActiveSpeech.value = false
    resetPlayback()
  }

  const handleAudioEvent = (event: AudioStreamEvent) => {
    switch (event.type) {
      case 'START':
        handleStart(event)
        break
      case 'CHUNK':
        handleChunk(event)
        break
      case 'COMPLETE':
        handleComplete()
        break
      case 'ERROR':
        handleError(event.message || '语音生成失败')
        break
      default:
        break
    }
  }

  const subscribeToAudioChannel = () => {
    if (!stompClient) return
    const id = getSessionIdValue(sessionId)
    subscription = stompClient.subscribe(`/topic/interview/audio/${id}`, (frame: IMessage) => {
      try {
        const payload: AudioStreamEvent = JSON.parse(frame.body)
        handleAudioEvent(payload)
      } catch (error) {
        console.error('解析音频事件失败', error)
      }
    })
  }

  const startAudioStream = () => {
    if (!isAudioApiSupported) {
      lastError.value = '浏览器不支持 Web Audio API'
      return
    }

    const id = getSessionIdValue(sessionId)
    if (!id) return
    if (connectionState.value === 'connected' || connectionState.value === 'connecting') return

    connectionState.value = 'connecting'
    const socket = new SockJS(`${WS_BASE_URL}/ws`)

    stompClient = new Client({
      webSocketFactory: () => socket as unknown as WebSocket,
      reconnectDelay: 5000,
      onConnect: () => {
        connectionState.value = 'connected'
        lastError.value = ''
        subscribeToAudioChannel()
      },
      onStompError: (frame) => {
        console.error('音频 STOMP 错误', frame)
        connectionState.value = 'error'
        lastError.value = frame.headers['message'] || '音频通道异常'
      },
      onWebSocketClose: () => {
        connectionState.value = 'idle'
      },
      onWebSocketError: (event) => {
        console.error('音频 WebSocket 错误', event)
        connectionState.value = 'error'
        lastError.value = '音频通道连接失败'
      },
    })

    stompClient.activate()
  }

  const stopAudioStream = () => {
    if (subscription) {
      subscription.unsubscribe()
      subscription = null
    }
    if (stompClient) {
      stompClient.deactivate()
      stompClient = null
    }
    connectionState.value = 'idle'
    lastSequenceNumber.value = null
    resetPlayback()
  }

  const dispose = () => {
    stopAudioStream()
    if (audioCtx) {
      audioCtx.close().catch(() => { })
      audioCtx = null
    }
  }

  onBeforeUnmount(() => {
    dispose()
  })

  return {
    connectionState,
    speakingText,
    currentVoice,
    isSpeaking,
    audioReady,
    lastError,
    isAudioApiSupported,
    startAudioStream,
    stopAudioStream,
    resumeAudioContext,
  }
}
