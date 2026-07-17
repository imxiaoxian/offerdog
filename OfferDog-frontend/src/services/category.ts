import { http } from '@/utils/request'

// Category元数据接口
export interface CategoryMetadata {
  [key: string]: any
  display_tag?: string
  related_major?: string[]
}

// Category详情接口
export interface CategoryDetail {
  id: number
  name: string
  parentId: number | null
  level: number
  path?: number[]
  metadata: CategoryMetadata
  createdAt?: string
  updatedAt?: string
}

// Category树节点接口
export interface CategoryTreeNode {
  id: number
  name: string
  level: number
  metadata: CategoryMetadata
  children: CategoryTreeNode[]
}

// 创建Category的请求参数
export interface CreateCategoryPayload {
  name: string
  parentId: number | null
  level: number
  metadata: CategoryMetadata
}

// 修改Category的请求参数
export interface UpdateCategoryPayload {
  name: string
  metadata: CategoryMetadata
}

export const categoryApi = {
  // 创建category
  createCategory: (payload: CreateCategoryPayload) =>
    http.post<number>('/categories', payload),
  
  // 修改category
  updateCategory: (id: number, payload: UpdateCategoryPayload) =>
    http.put<void>(`/categories/${id}`, payload),
  
  // 删除category
  deleteCategory: (id: number) =>
    http.delete<void>(`/categories/${id}`),
  
  // 获取category详情
  getCategoryDetail: (id: number) =>
    http.get<CategoryDetail>(`/categories/${id}`),
  
  // 获取所有category树结构
  getCategoryTree: () =>
    http.get<CategoryTreeNode[]>('/categories/tree')
}