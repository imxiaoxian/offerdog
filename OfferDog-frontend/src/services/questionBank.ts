import { http } from '@/utils/request'
import type { QuestionDifficulty } from './question'

// 题库详情接口
export interface QuestionBank {
  id: number
  name: string
  description: string
  categoryId: number
  createdBy?: number | null
  createdAt: string
  updatedAt: string
}

// 创建题库的请求参数
export interface CreateQuestionBankPayload {
  name: string
  description: string
  categoryId: number
}

// 修改题库的请求参数
export interface UpdateQuestionBankPayload {
  name: string
  description: string
  categoryId: number
}

// 获取题库列表的查询参数
export interface GetQuestionBanksParams {
  pageNo?: number
  pageSize?: number
  name?: string
  categoryId?: number
  /** 多岗位 OR 筛选；与 categoryId 二选一，优先本字段 */
  categoryIds?: number[]
  difficulty?: QuestionDifficulty
}

function buildListUrl(
  path: string,
  params: Record<string, string | number | undefined | null>,
  categoryIds?: number[],
): string {
  const sp = new URLSearchParams()
  Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') sp.append(k, String(v))
  })
  categoryIds?.forEach((id) => sp.append('categoryIds', String(id)))
  const q = sp.toString()
  return q ? `${path}?${q}` : path
}

export const questionBankApi = {
  // 创建题库
  createQuestionBank: (payload: CreateQuestionBankPayload) =>
    http.post<number>('/question-banks', payload),

  // 修改题库
  updateQuestionBank: (id: number, payload: UpdateQuestionBankPayload) =>
    http.put<void>(`/question-banks/${id}`, payload),

  // 删除题库
  deleteQuestionBank: (id: number) =>
    http.delete<void>(`/question-banks/${id}`),

  // 获取题库详情
  getQuestionBankDetail: (id: number) =>
    http.get<QuestionBank>(`/question-banks/${id}`),

  // 获取题库列表 - 返回的data是QuestionBank数组，ApiResponse会自动包装
  getQuestionBanks: (params?: GetQuestionBanksParams) => {
    const { categoryIds, ...rest } = params ?? {}
    if (categoryIds?.length) {
      return http.get<QuestionBank[]>(
        buildListUrl('/question-banks', rest as Record<string, string | number | undefined | null>, categoryIds),
      )
    }
    return http.get<QuestionBank[]>('/question-banks', { params: rest })
  },
}
