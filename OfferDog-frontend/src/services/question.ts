import { http } from '@/utils/request'

function buildQuestionsUrl(
  params: Record<string, string | number | undefined | null>,
  categoryIds?: number[],
): string {
  const sp = new URLSearchParams()
  Object.entries(params).forEach(([k, v]) => {
    if (v === undefined || v === null || v === '') return
    if (k === 'categoryIds') return
    sp.append(k, String(v))
  })
  categoryIds?.forEach((id) => sp.append('categoryIds', String(id)))
  const q = sp.toString()
  return q ? `/questions?${q}` : '/questions'
}

// 题目难度枚举
export type QuestionDifficulty = 'EASY' | 'MEDIUM' | 'HARD' | 'easy' | 'medium' | 'hard'

// 题目统计信息接口
export interface QuestionStats {
  views: number
  likes: number
  favorites: number
}

// 题目详情接口
export interface Question {
  id: number
  bankId: number
  categoryId: number
  content: string
  answer?: string
  tips?: string
  difficulty: QuestionDifficulty
  source?: string
  tags: string[]
  stats?: QuestionStats
  remark?: string
  createdBy?: number
  createdAt: string
  updatedAt?: string
  isFavorite?: boolean
}

// 创建题目的请求参数
export interface CreateQuestionPayload {
  bankId: number
  categoryId: number
  content: string
  answer: string
  tips: string
  difficulty: QuestionDifficulty
  tags: string[]
  remark: string
}

// 修改题目的请求参数
export interface UpdateQuestionPayload {
  bankId: number
  categoryId: number
  content: string
  answer: string
  tips: string
  difficulty: QuestionDifficulty
  tags: string[]
  remark: string
}

// 获取题目列表的查询参数
export interface GetQuestionsParams {
  pageNo?: number
  pageSize?: number
  keyword?: string
  answerKeyword?: string
  difficulty?: QuestionDifficulty
  tags?: string
  bankId?: number
  categoryId?: number
  /** 多岗位 OR 筛选；与 categoryId 二选一，优先本字段 */
  categoryIds?: number[]
}

export interface BatchAddQuestionsPayload {
  file: File
  bankId: number
  categoryId: number
}

export const questionApi = {
  // 新建题目
  createQuestion: (payload: CreateQuestionPayload) =>
    http.post<number>('/questions', payload),

  // 修改题目
  updateQuestion: (id: number, payload: UpdateQuestionPayload) =>
    http.put<void>(`/questions/${id}`, payload),

  // 删除题目
  deleteQuestion: (id: number) =>
    http.delete<void>(`/questions/${id}`),

  // 获取题目详情
  getQuestionDetail: (id: number) =>
    http.get<Question>(`/questions/${id}`),

  // 获取题目列表 - 返回的data是Question数组，ApiResponse会自动包装
  getQuestions: (params?: GetQuestionsParams) => {
    const { categoryIds, ...rest } = params ?? {}
    if (categoryIds?.length) {
      return http.get<Question[]>(
        buildQuestionsUrl(rest as Record<string, string | number | undefined | null>, categoryIds),
      )
    }
    return http.get<Question[]>('/questions', { params: rest })
  },

  // 批量上传题目
  batchAddQuestions: (payload: BatchAddQuestionsPayload) => {
    const formData = new FormData()
    formData.append('file', payload.file)
    formData.append('bankId', String(payload.bankId))
    formData.append('categoryId', String(payload.categoryId))
    return http.post<void>('/questions/batch', formData)
  },
}
