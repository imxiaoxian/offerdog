import type { InterviewLearningPack, InterviewReport } from '@/types/interview'
import type { ApiResponse } from '@/utils/request'

const DEFAULT_TTL_MS = 10 * 60 * 1000

interface Entry<T> {
  data: T
  storedAt: number
}

const reportMap = new Map<string, Entry<ApiResponse<InterviewReport>>>()
const learningPackMap = new Map<string, Entry<ApiResponse<InterviewLearningPack>>>()

function isFresh(entry: Entry<unknown>, ttlMs: number) {
  return Date.now() - entry.storedAt < ttlMs
}

export function getCachedReport(sessionId: string, ttlMs = DEFAULT_TTL_MS): ApiResponse<InterviewReport> | null {
  const e = reportMap.get(sessionId)
  if (!e || !isFresh(e, ttlMs)) {
    if (e) reportMap.delete(sessionId)
    return null
  }
  return e.data
}

export function setCachedReport(sessionId: string, payload: ApiResponse<InterviewReport>) {
  reportMap.set(sessionId, { data: payload, storedAt: Date.now() })
}

export function getCachedLearningPack(
  sessionId: string,
  ttlMs = DEFAULT_TTL_MS,
): ApiResponse<InterviewLearningPack> | null {
  const e = learningPackMap.get(sessionId)
  if (!e || !isFresh(e, ttlMs)) {
    if (e) learningPackMap.delete(sessionId)
    return null
  }
  return e.data
}

export function setCachedLearningPack(sessionId: string, payload: ApiResponse<InterviewLearningPack>) {
  learningPackMap.set(sessionId, { data: payload, storedAt: Date.now() })
}

/** 某场面试的报告 + 学习包（提升计划同源）一并失效 */
export function invalidateInterviewSessionCaches(sessionId: string) {
  reportMap.delete(sessionId)
  learningPackMap.delete(sessionId)
}

export function clearInterviewSessionCache() {
  reportMap.clear()
  learningPackMap.clear()
}
