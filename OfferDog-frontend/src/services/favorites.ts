import { http } from '@/utils/request'
import type { QuestionDifficulty } from './question'

export interface FavoriteQuestion {
  id: number
  bankId: number
  categoryId: number
  content: string
  difficulty: QuestionDifficulty
  source?: string
  tags?: string[]
  createdAt?: string
}

export interface FavoritePageParams {
  pageNum: number
  pageSize: number
}

export const favoriteApi = {
  addFavorite: (questionId: number) => http.post<void>(`/favorites/add/${questionId}`),
  removeFavorite: (questionId: number) => http.post<void>(`/favorites/remove/${questionId}`),
  listFavorites: (params: FavoritePageParams) =>
    http.post<FavoriteQuestion[]>('/favorites/list', undefined, { params }),
}
