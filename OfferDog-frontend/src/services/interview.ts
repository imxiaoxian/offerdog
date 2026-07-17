import { http, type ApiResponse } from '@/utils/request'
import {
  getCachedLearningPack,
  getCachedReport,
  setCachedLearningPack,
  setCachedReport,
} from '@/utils/interviewSessionCache'
import type {
  AnswerRequest,
  CreateSessionRequest,
  InterviewReport,
  InterviewSession,
  InterviewTemplate,
  AIResponse,
  SessionStatus,
  ConversationMessage,
  QuestionRecommendationRsp,
  InterviewGrowthPoint,
  InterviewLearningPack,
} from '@/types/interview'

/**
 * AI面试相关API服务
 */
export const interviewApi = {
  /**
   * 获取所有可用的面试模板
   */
  getTemplates: () => http.get<InterviewTemplate[]>('/interview/templates'),

  /**
   * 根据ID获取面试模板详情
   */
  getTemplate: (templateId: string) =>
    http.get<InterviewTemplate>(`/interview/templates/${templateId}`),

  /**
   * 创建面试会话
   */
  createSession: (payload: CreateSessionRequest) =>
    http.post<InterviewSession>('/interview/sessions', payload),

  /**
   * 获取面试会话详情
   */
  getSession: (sessionId: string) => http.get<InterviewSession>(`/interview/sessions/${sessionId}`),

  /**
   * 获取AI开场白
   */
  getOpening: (sessionId: string) => http.get<string>(`/interview/sessions/${sessionId}/opening`),

  /**
   * 提交用户回答
   */
  submitAnswer: (sessionId: string, payload: AnswerRequest) =>
    http.post<AIResponse>(`/interview/sessions/${sessionId}/answer`, payload),

  /**
   * 获取会话的所有消息历史
   */
  getMessages: (sessionId: string) =>
    http.get<ConversationMessage[]>(`/interview/sessions/${sessionId}/messages`),

  /**
   * 结束面试
   */
  endInterview: (sessionId: string) => http.post<void>(`/interview/sessions/${sessionId}/end`),

  /**
   * 生成面试报告（异步）
   */
  generateReport: (sessionId: string) =>
    http.post<void>(`/interview/sessions/${sessionId}/report/generate`),

  /**
   * 获取会话状态
   */
  getSessionStatus: (sessionId: string) =>
    http.get<SessionStatus>(`/interview/sessions/${sessionId}/status`),

  /**
   * 获取面试报告（短 TTL 内存缓存，生成新报告后应 bypass 或 invalidate）
   */
  getReport: async (sessionId: string, opts?: { bypassCache?: boolean }): Promise<ApiResponse<InterviewReport>> => {
    if (!opts?.bypassCache) {
      const hit = getCachedReport(sessionId)
      if (hit) return hit
    }
    const payload = await http.get<InterviewReport>(`/interview/sessions/${sessionId}/report`)
    setCachedReport(sessionId, payload)
    return payload
  },

  /**
   * 获取用户的所有面试会话列表
   */
  getUserSessions: (userId: number) =>
    http.get<InterviewSession[]>(`/interview/users/${userId}/sessions`),

  /**
   * 基于报告薄弱点的题库推荐（需已存在面试报告）
   */
  getQuestionRecommendations: (sessionId: string) =>
    http.get<QuestionRecommendationRsp>(`/interview/sessions/${sessionId}/recommendations`),

  /**
   * 已生成报告的历史得分（按报告时间升序），用于成长曲线
   */
  getGrowthPoints: (userId: number, limit = 20) =>
    http.get<InterviewGrowthPoint[]>(`/interview/users/${userId}/growth-points`, { params: { limit } }),

  /**
   * 智能学习包 / 面试提升计划（短 TTL 内存缓存）
   */
  getLearningPack: async (
    sessionId: string,
    opts?: { bypassCache?: boolean },
  ): Promise<ApiResponse<InterviewLearningPack>> => {
    if (!opts?.bypassCache) {
      const hit = getCachedLearningPack(sessionId)
      if (hit) return hit
    }
    const payload = await http.get<InterviewLearningPack>(`/interview/sessions/${sessionId}/learning-pack`)
    setCachedLearningPack(sessionId, payload)
    return payload
  },
}
