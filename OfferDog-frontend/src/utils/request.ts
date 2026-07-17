import axios, { type AxiosError, type AxiosRequestConfig } from 'axios'

import { API_BASE_URL } from '@/config/env'

export interface ApiResponse<T> {
  success: boolean
  message?: string | null
  errorCode?: string | null
  data: T
  pageNo?: number
  pageSize?: number
  totalCount?: number
  totalPage?: number
}

export class ApiError extends Error {
  code?: string

  constructor(message: string, code?: string) {
    super(message)
    this.name = 'ApiError'
    this.code = code
  }
}

const axiosInstance = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
    'X-Requested-With': 'XMLHttpRequest',
  },
})

// 请求拦截器
axiosInstance.interceptors.request.use(
  (config) => {
    if (!config.headers) {
      config.headers = {} as any
    }
    
    // FormData 必须由浏览器自动设置 multipart boundary；手动写 multipart/form-data 且无 boundary 会导致上传失败
    const isFormData = config.data instanceof FormData
    if (isFormData) {
      delete (config.headers as Record<string, unknown>)['Content-Type']
      delete (config.headers as Record<string, unknown>)['content-type']
      if (import.meta.env.DEV) {
        console.log('📋 FormData：已移除 Content-Type，由浏览器自动带 boundary')
      }
    } else {
      config.headers['Content-Type'] = 'application/json'
    }
    
    // 开发环境下打印请求信息
    if (import.meta.env.DEV) {
      console.log('🚀 API请求:', config.method?.toUpperCase(), config.url)
      console.log('请求方法:', config.method?.toUpperCase())
      console.log('请求URL:', config.url)
      console.log('是否携带凭证:', config.withCredentials)
      console.log('Content-Type:', config.headers['Content-Type'])
    }
    // 从localStorage获取用户信息，添加认证信息
    if (typeof window !== 'undefined') {
      const userStr = window.localStorage.getItem('inai-user')
      console.log('localStorage中的用户信息:', userStr)
      if (userStr) {
        try {
          const user = JSON.parse(userStr)
          console.log('解析后的用户信息:', user)
          // 添加用户ID到请求头
          if (user.id) {
            config.headers['X-User-Id'] = user.id.toString()
            console.log('已添加X-User-Id请求头:', user.id.toString())
          } else {
            console.warn('用户信息中没有id字段')
          }
          // 检查是否有token字段，如果有则添加到Authorization头
          if (user.token) {
            config.headers.Authorization = `Bearer ${user.token}`
            console.log('已添加Authorization请求头')
          } else {
            console.log('用户信息中没有token字段')
          }
        } catch (error) {
          console.warn('解析用户信息失败', error)
        }
      } else {
        console.warn('localStorage中没有用户信息，用户可能未登录')
      }
      
      // 添加CSRF令牌支持
      const csrfToken = document.cookie.split(';').find(cookie => cookie.trim().startsWith('XSRF-TOKEN='))
      if (csrfToken) {
        const [, tokenValue] = csrfToken.split('=')
        if (tokenValue) {
          config.headers['X-XSRF-TOKEN'] = decodeURIComponent(tokenValue)
          console.log('已添加X-XSRF-TOKEN请求头:', tokenValue)
        }
      } else {
        console.log('没有找到XSRF-TOKEN Cookie')
      }
      // 打印完整的请求头信息
      console.log('请求头信息:', JSON.stringify(config.headers, null, 2))
      // 打印请求体信息
      if (config.data) {
        console.log('请求体信息:', JSON.stringify(config.data, null, 2))
      }
      // 打印浏览器中的cookie信息
      if (document.cookie) {
        console.log('浏览器中的Cookie:', document.cookie)
        // 检查是否包含INAI_SESSION
        const hasSessionCookie = document.cookie.includes('INAI_SESSION')
        console.log('是否包含INAI_SESSION Cookie:', hasSessionCookie)
        // 提取并打印INAI_SESSION的值
        const sessionCookie = document.cookie.split(';').find(cookie => cookie.trim().startsWith('INAI_SESSION='))
        if (sessionCookie) {
          const [, sessionValue] = sessionCookie.split('=')
          if (sessionValue) {
            console.log('INAI_SESSION Cookie值:', sessionValue)
          }
        }
      } else {
        console.warn('浏览器中没有Cookie')
      }
    }
    return config
  },
  (error) => {
    console.error('❌ 请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
axiosInstance.interceptors.response.use(
  (response) => {
    // 开发环境下打印响应信息
    if (import.meta.env.DEV) {
      console.log('✅ API响应:', response.config.method?.toUpperCase(), response.config.url, '-', response.data.success ? '成功' : '失败')
      
      // 只在失败时打印详细信息
      if (!response.data.success) {
        console.error('❌ 业务错误:', response.data.message, response.data.errorCode)
      }
    }
    return response
  },
  (error) => {
    const ax = error as AxiosError
    const st = ax.response?.status
    const data = ax.response?.data
    console.error('❌ 请求失败:', ax.config?.url, st, data ?? ax.message)
    return Promise.reject(error)
  }
)

/** 从 Axios 错误中解析可读信息（含 Spring Boot /error 的 JSON） */
function resolveAxiosErrorMessage(error: AxiosError): string {
  const status = error.response?.status
  const raw = error.response?.data

  if (raw && typeof raw === 'object' && !Array.isArray(raw)) {
    const o = raw as Record<string, unknown>
    // 业务 Response<T>
    if (typeof o.message === 'string' && o.message.trim() && o.message !== 'No message available') {
      return o.message
    }
    // Spring Boot Whitelabel /error JSON
    const err = typeof o.error === 'string' ? o.error : ''
    const path = typeof o.path === 'string' ? o.path : ''
    const detail = typeof o.message === 'string' ? o.message : ''
    if (status && status >= 400) {
      const parts = [`HTTP ${status}`]
      if (err) parts.push(err)
      if (detail && detail !== 'No message available') parts.push(detail)
      if (path) parts.push(path)
      return parts.filter(Boolean).join(' · ')
    }
  }
  if (typeof raw === 'string') {
    const s = raw.trim()
    if (s.startsWith('<!')) return `HTTP ${status ?? '?'}: 服务器返回 HTML 错误页（请查看后端控制台或 Network 响应体）`
    return s.length > 240 ? `${s.slice(0, 240)}…` : s
  }
  return error.message ?? '请求失败'
}

const request = async <T>(config: AxiosRequestConfig) => {
  try {
    // 调试：检查浏览器是否保存了cookies
    if (typeof window !== 'undefined') {
      console.log('当前页面的cookies:', document.cookie)
    }
    
    // 调试：检查请求是否携带了cookies
    console.log(`🔍 准备发送请求到 ${config.url}`)
    console.log('📋 请求方法:', config.method?.toUpperCase())
    console.log('📋 请求URL:', config.url)
    console.log('📋 请求体:', JSON.stringify(config.data, null, 2))
    console.log('📋 请求配置:', JSON.stringify(config, null, 2))
    
    console.log('🚀 正在发送请求...')
    const response = await axiosInstance.request<ApiResponse<T>>(config)
    console.log('✅ 请求发送成功！')
    const payload = response.data

    // 调试：检查响应头
    console.log('响应头:', JSON.stringify(response.headers, null, 2))
    
    // 检查是否是登录相关的请求，如果是，查看并记录cookies
    if (config.url?.includes('/auth/email/login') && payload && payload.success) {
      const cookies = response.headers['set-cookie']
      if (cookies) {
        console.log('登录成功，服务器返回的cookies:', cookies)
        // 由于axios已经配置了withCredentials: true，浏览器会自动保存cookies
        // 这些cookies将在后续请求中自动发送
      }
    } else {
      // 调试：检查非登录请求是否携带了认证信息
      console.log(`请求 ${config.url} 的响应:`, JSON.stringify(payload, null, 2))
    }

    if (payload && payload.success === false) {
      // 检查是否为未登录错误
      if (payload.message?.includes('未登录') || payload.errorCode === 'UNAUTHORIZED') {
        console.warn('⚠️ 用户未登录或Session已过期，清除本地数据')
        // 清除localStorage中的用户数据
        localStorage.removeItem('inai-user')
        // 延迟跳转，给用户看到错误提示的时间
        setTimeout(() => {
          window.location.href = '/login'
        }, 1500)
      }
      throw new ApiError(payload.message ?? '请求失败', payload.errorCode ?? undefined)
    }

    return payload ?? {
      success: true,
      message: '',
      errorCode: undefined,
      data: undefined as T,
    }
  } catch (error) {
    if (axios.isAxiosError(error)) {
      const payload = error.response?.data as ApiResponse<unknown> | undefined
      const message = resolveAxiosErrorMessage(error)
      const code =
        payload && typeof payload === 'object' && 'errorCode' in payload
          ? (payload as ApiResponse<unknown>).errorCode
          : undefined

      // 处理401未授权错误
      if (error.response?.status === 401) {
        console.warn('⚠️ 401未授权，清除本地数据')
        localStorage.removeItem('inai-user')
        setTimeout(() => {
          window.location.href = '/login'
        }, 1500)
      }

      if (import.meta.env.DEV && error.response?.status && error.response.status >= 500) {
        console.error('[HTTP 5xx] 详情:', error.response?.data)
      }

      throw new ApiError(message, code ?? undefined)
    }
    throw error as Error
  }
}

export const http = {
  get: <T>(path: string, config?: AxiosRequestConfig) => request<T>({ url: path, method: 'GET', ...config }),
  post: <T>(path: string, data?: unknown, config?: AxiosRequestConfig) =>
    request<T>({
      url: path,
      method: 'POST',
      data,
      ...config,
    }),
  put: <T>(path: string, data?: unknown, config?: AxiosRequestConfig) =>
    request<T>({
      url: path,
      method: 'PUT',
      data,
      ...config,
    }),
  patch: <T>(path: string, data?: unknown, config?: AxiosRequestConfig) =>
    request<T>({
      url: path,
      method: 'PATCH',
      data,
      ...config,
    }),
  delete: <T>(path: string, config?: AxiosRequestConfig) =>
    request<T>({
      url: path,
      method: 'DELETE',
      ...config,
    }),
}
