import { http } from '@/utils/request'

export interface SpeechToTextPayload {
  text: string
  timestamp: number
  sessionId?: string
}

export interface SpeechToTextResponse {
  success: boolean
  message?: string
}

export const speechApi = {
  // 将识别的文字发送到后端
  sendTranscription: (payload: SpeechToTextPayload) =>
    http.post<SpeechToTextResponse>('/speech/transcription', payload),

  // 创建语音会话
  createSession: () => http.post<{ sessionId: string }>('/speech/session/create'),

  // 结束语音会话
  endSession: (sessionId: string) => http.post<void>('/speech/session/end', { sessionId }),
}
