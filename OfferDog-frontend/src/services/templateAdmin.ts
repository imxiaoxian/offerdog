import { http } from '@/utils/request'

export type TemplateDifficulty = 'junior' | 'mid' | 'senior' | string

export type UuidLike = string | { mostSigBits: number; leastSigBits: number }

export interface InterviewTemplateSummary {
  templateId: UuidLike
  templateName: string
  description?: string
  category?: string
  difficultyLevel?: TemplateDifficulty
  estimatedDurationMinutes?: number
  isActive?: boolean
  version?: number
  updatedAt?: string
}

export interface PlanStructureDTO {
  [key: string]: unknown
}

export interface InterviewPlanTemplate {
  planTemplateId: UuidLike
  templateId: UuidLike
  planName: string
  planStructure?: PlanStructureDTO | unknown
  usageCount?: number
  avgCompletionRate?: number
  isActive?: boolean
  createdAt?: string
  updatedAt?: string
}

export interface InterviewTemplateDetail extends InterviewTemplateSummary {
  systemPrompt?: string
  defaultConfig?: unknown
  createdAt?: string
  planTemplate?: InterviewPlanTemplate
}

export interface PageQuery {
  pageNo?: number
  pageSize?: number
}

export interface TemplateQuery extends PageQuery {
  keyword?: string
  category?: string
  difficultyLevel?: TemplateDifficulty
  isActive?: boolean
}

export interface CreateTemplatePayload {
  templateName: string
  description?: string
  category: string
  difficultyLevel: TemplateDifficulty
  estimatedDurationMinutes?: number
  systemPrompt: string
  defaultConfig?: unknown
  isActive?: boolean
  version?: number
  planName: string
  planStructure: PlanStructureDTO | unknown
  usageCount?: number
  avgCompletionRate?: number
  planIsActive?: boolean
}

export interface UpdateTemplatePayload {
  templateName?: string
  description?: string
  category?: string
  difficultyLevel?: TemplateDifficulty
  estimatedDurationMinutes?: number
  systemPrompt?: string
  defaultConfig?: unknown
  isActive?: boolean
  version?: number
  planName?: string
  planStructure?: PlanStructureDTO | unknown
  usageCount?: number
  avgCompletionRate?: number
  planIsActive?: boolean
}

export const formatUuid = (value?: UuidLike) => {
  if (!value) return ''
  if (typeof value === 'string') return value
  return `${value.mostSigBits ?? ''}-${value.leastSigBits ?? ''}`
}

const basePath = '/admin/interview-templates'

export const templateAdminApi = {
  pageTemplates: (params?: TemplateQuery) =>
    http.get<InterviewTemplateSummary[]>(basePath, { params }),

  createTemplate: (payload: CreateTemplatePayload) =>
    http.post<UuidLike>(basePath, payload),

  getTemplate: (templateId: string) =>
    http.get<InterviewTemplateDetail>(`${basePath}/${templateId}`),

  updateTemplate: (templateId: string, payload: UpdateTemplatePayload) =>
    http.put<void>(`${basePath}/${templateId}`, payload),

  deleteTemplate: (templateId: string) =>
    http.delete<void>(`${basePath}/${templateId}`),
}
