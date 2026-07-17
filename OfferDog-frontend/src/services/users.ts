import { http } from '@/utils/request'

export interface UserProfile {
  email: string
  phone: string
  realName: string
  avatarUrl?: string | null
  gender?: string
}

export interface YearMonth {
  year?: number
  month?: number
}

export interface ResumeLocation {
  province?: string
  city?: string
}

export interface ResumeEducation {
  level?: string
  school?: string
  major?: string
  graduationDate?: YearMonth
}

export interface ResumeContact {
  phone?: string
  email?: string
  homepage?: string
}

export interface ResumeExperience {
  company?: string
  title?: string
  startDate?: YearMonth
  endDate?: YearMonth
  description?: string
}

export interface ResumeProject {
  name?: string
  role?: string
  description?: string
  highlights?: string[]
}

export interface ResumeWork {
  firstEmployment?: YearMonth
  experience?: ResumeExperience[]
  projects?: ResumeProject[]
}

export type ParsedResumeResponse = ResumePayload

export interface ResumePayload {
  name?: string
  birth?: string
  gender?: string
  location?: ResumeLocation
  education?: ResumeEducation[]
  contact?: ResumeContact
  work?: ResumeWork
  skills?: string[]
  jobIntention?: string
  selfEvaluation?: string
  resumeFileUrl?: string
}

export interface UserSummary {
  id: number
  username: string
  role: string
  profile: UserProfile
  isActive: boolean
  createdAt: string
  updatedAt: string
}

export interface UserDetail extends UserSummary {
  resume?: ResumePayload
}

export interface FileUploadResp {
  fileUrl: string
  originalFileName?: string
  fileSize?: number
  fileType?: string
}

export interface PageUsersQuery {
  pageNo?: number
  pageSize?: number
  username?: string
  role?: string
  isActive?: boolean
  email?: string
  skill?: string
}

export interface CreateUserPayload {
  username: string
  password: string
  role: 'USER' | 'ADMIN' | 'VIP'
  profile: UserProfile
  resume?: ResumePayload
  isActive?: boolean
}

export interface UpdateUserPayload {
  profile: UserProfile
  username?: string
  password?: string
  role?: 'USER' | 'ADMIN' | 'VIP'
  resume?: ResumePayload
  isActive?: boolean
}

export const userApi = {
  getUsers: (params: PageUsersQuery) => http.get<UserSummary[]>('/users', { params }),
  getUserDetail: (id: number) => http.get<UserDetail>(`/users/${id}`),
  createUser: (payload: CreateUserPayload) => http.post<number>('/users', payload),
  updateUser: (id: number, payload: UpdateUserPayload) => http.put<void>(`/users/${id}`, payload),
  deleteUser: (id: number) => http.delete<void>(`/users/${id}`),
  updateUserResume: (id: number, resume: ResumePayload) => http.patch<void>(`/users/${id}/resume`, { resume }),
  uploadAndParseResume: (id: number, file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return http.post<ParsedResumeResponse>(`/users/${id}/resume/upload`, formData)
  },
  uploadAvatar: (id: number, file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return http.post<FileUploadResp>(`/users/${id}/avatar`, formData)
  },
}
