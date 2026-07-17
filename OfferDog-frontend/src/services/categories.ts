import { http, type ApiResponse } from '@/utils/request'

const DEFAULT_PARENT_ID: number | null = null
const DEFAULT_LEVEL = 1

export interface CategoryMetadata {
  industry?: string
  position?: string
  tags?: string[]
  description?: string
  questionCount?: number
  display_tag?: string
  related_major?: string[]
  [key: string]: unknown
}

interface CategoryRecord {
  id: number
  name: string
  parentId: number | null
  level: number
  metadata?: CategoryMetadata
  createdAt?: string
  updatedAt?: string
  path?: number[]
}

export interface CategoryTreeNode {
  id: number
  name: string
  level?: number
  metadata?: CategoryMetadata
  createdAt?: string
  updatedAt?: string
  children?: CategoryTreeNode[]
}

export interface QuizBankSummary {
  id: number
  name: string
  industry?: string
  position?: string
  tags?: string[]
  questionCount?: number
  createdAt?: string
  updatedAt?: string
  parentId?: number | null
  level?: number
  path?: number[]
}

export interface QuizBankDetail extends QuizBankSummary {
  description?: string
  parentId?: number | null
  level?: number
  path?: number[]
}

export interface PageQuizBanksQuery {
  id?: number | null
  categoryId?: number | null
  pageNo?: number
  pageSize?: number
  name?: string
  industry?: string
  position?: string
  tags?: string[]
}

export interface CreateQuizBankPayload {
  name: string
  industry?: string
  position?: string
  tags?: string[]
  description?: string
  parentId?: number | null
  level?: number
  questionCount?: number
}

export type UpdateQuizBankPayload = Partial<CreateQuizBankPayload>

const normalizeTags = (tags?: unknown): string[] | undefined => {
  if (!Array.isArray(tags)) return undefined
  return tags.map((item) => (typeof item === 'string' ? item.trim() : '')).filter(Boolean)
}

const extractIndustry = (metadata?: CategoryMetadata): string | undefined => {
  if (!metadata) return undefined
  const candidate = metadata.industry ?? metadata.display_tag
  return typeof candidate === 'string' ? candidate : undefined
}

const extractTags = (metadata?: CategoryMetadata): string[] | undefined => {
  if (!metadata) return undefined
  return normalizeTags(metadata.tags ?? metadata.related_major)
}

const mapCategoryToQuizBank = (item: CategoryRecord): QuizBankSummary => ({
  id: item.id,
  name: item.name,
  industry: extractIndustry(item.metadata),
  position: (item.metadata?.position as string | undefined) ?? undefined,
  tags: extractTags(item.metadata),
  questionCount:
    typeof item.metadata?.questionCount === 'number' ? (item.metadata?.questionCount as number) : undefined,
  createdAt: item.createdAt,
  updatedAt: item.updatedAt,
  parentId: item.parentId,
  level: item.level,
  path: item.path,
})

const mapCategoryToDetail = (item: CategoryRecord): QuizBankDetail => ({
  ...mapCategoryToQuizBank(item),
  description: item.metadata?.description as string | undefined,
  parentId: item.parentId,
  level: item.level,
  path: item.path,
})

const buildMetadata = (payload: CreateQuizBankPayload | UpdateQuizBankPayload): CategoryMetadata | undefined => {
  const metadata: CategoryMetadata = {}
  if (payload.industry) {
    metadata.industry = payload.industry
    metadata.display_tag = payload.industry
  }
  if (payload.position) metadata.position = payload.position
  if (payload.tags && payload.tags.length > 0) {
    const normalized = payload.tags.filter(Boolean)
    metadata.tags = normalized
    metadata.related_major = normalized
  }
  if (payload.description) metadata.description = payload.description
  if (typeof payload.questionCount === 'number') {
    metadata.questionCount = payload.questionCount
  }
  return Object.keys(metadata).length > 0 ? metadata : { '': {} }
}

const buildCreateRequestBody = (payload: CreateQuizBankPayload) => ({
  name: payload.name,
  parentId: payload.parentId ?? DEFAULT_PARENT_ID,
  level: payload.level ?? DEFAULT_LEVEL,
  metadata: buildMetadata(payload),
})

const buildUpdateRequestBody = (payload: UpdateQuizBankPayload) => ({
  name: payload.name ?? '',
  metadata: buildMetadata(payload),
})

const adaptDetailResponse = (response: ApiResponse<CategoryRecord>): ApiResponse<QuizBankDetail> => ({
  ...response,
  data: response.data ? mapCategoryToDetail(response.data) : (undefined as unknown as QuizBankDetail),
})

const flattenCategoryTree = (
  nodes: CategoryTreeNode[],
  parentId: number | null = null,
  parentPath: number[] = [],
): CategoryRecord[] => {
  const records: CategoryRecord[] = []
  nodes?.forEach((node) => {
    const currentPath = [...parentPath, node.id]
    const record: CategoryRecord = {
      id: node.id,
      name: node.name,
      parentId,
      level: node.level ?? currentPath.length,
      metadata: node.metadata,
      createdAt: node.createdAt,
      updatedAt: node.updatedAt,
      path: currentPath,
    }
    records.push(record)
    if (node.children && node.children.length > 0) {
      records.push(...flattenCategoryTree(node.children, node.id, currentPath))
    }
  })
  return records
}

const filterSummaries = (items: QuizBankSummary[], params?: PageQuizBanksQuery) => {
  let filtered = [...items]
  if (params?.id != null) {
    filtered = filtered.filter((item) => item.id === params.id)
  }
  if (params?.categoryId != null) {
    filtered = filtered.filter((item) => item.id === params.categoryId)
  }
  if (params?.name) {
    filtered = filtered.filter((item) => item.name?.includes(params.name || ''))
  }
  if (params?.industry) {
    filtered = filtered.filter((item) => (item.industry ?? '').includes(params.industry || ''))
  }
  if (params?.position) {
    filtered = filtered.filter((item) => (item.position ?? '').includes(params.position || ''))
  }
  if (params?.tags && params.tags.length > 0) {
    filtered = filtered.filter((item) => {
      const recordTags = item.tags ?? []
      return params.tags?.every((tag) => recordTags.includes(tag))
    })
  }
  return filtered
}

const fetchQuizBankDetail = async (id: number) => {
  const response = await http.get<CategoryRecord>(`/categories/${id}`)
  return adaptDetailResponse(response)
}

export const quizBankApi = {
  getQuizBanks: async (params: PageQuizBanksQuery) => {
    const response = await http.get<CategoryTreeNode[]>('/categories/tree')
    const flattened = flattenCategoryTree(response.data ?? [])
    const summaries = flattened.map(mapCategoryToQuizBank)
    const filtered = filterSummaries(summaries, params)
    const totalCount = filtered.length
    const pageSize = params?.pageSize ?? (totalCount || 1)
    const totalPage = Math.max(1, Math.ceil(totalCount / pageSize))
    const pageNo = Math.min(params?.pageNo ?? 1, totalPage)
    const startIndex = (pageNo - 1) * pageSize
    const paged = filtered.slice(startIndex, startIndex + pageSize)
    const adapted: ApiResponse<QuizBankSummary[]> = {
      success: response.success,
      message: response.message,
      errorCode: response.errorCode,
      data: paged,
      pageNo,
      pageSize,
      totalCount,
      totalPage,
    }
    return adapted
  },
  getQuizBankDetail: fetchQuizBankDetail,
  createQuizBank: (payload: CreateQuizBankPayload) => http.post<number>('/categories', buildCreateRequestBody(payload)),
  updateQuizBank: (id: number, payload: UpdateQuizBankPayload) =>
    http.put<void>(`/categories/${id}`, buildUpdateRequestBody(payload)),
  deleteQuizBank: (id: number) => http.delete<void>(`/categories/${id}`),
}

// 分类API
export const categoryApi = {
  // 获取分类树
  getCategoryTree: () => http.get<CategoryTreeNode[]>('/categories/tree'),
}


