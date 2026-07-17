import { http } from '@/utils/request'

export interface UserInfo {
  id: number
  username: string
  email: string
  role: string
  isActive: boolean
  createdAt: string
  updatedAt: string
  avatarUrl?: string
}

export interface RegisterPayload {
  username: string
  email: string
  password: string
  verificationCode: string
}

export interface LoginByPasswordPayload {
  email: string
  password: string
}

export interface LoginByCodePayload {
  email: string
  verificationCode: string
}

export const authApi = {
  sendRegisterCode: (payload: { username: string; email: string }) =>
    http.post<void>('/auth/email/register/code', payload),
  register: (payload: RegisterPayload) => http.post<UserInfo>('/auth/email/register', payload),
  sendLoginCode: (payload: { email: string }) => http.post<void>('/auth/email/login/code/send', payload),
  loginByCode: (payload: LoginByCodePayload) => http.post<UserInfo>('/auth/email/login/code', payload),
  loginByPassword: (payload: LoginByPasswordPayload) =>
    http.post<UserInfo>('/auth/email/login/password', payload),
}
