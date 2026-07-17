import { http } from '@/utils/request'

export interface Experience {
  id: number
  companyName: string
  position?: string
  experienceType?: string
  content: string
  formattedContent?: string
  sourceUrl?: string
  source?: string
  author?: string
  views?: number
  likes?: number
  createdAt?: string
}

export interface GetExperiencesParams {
  /** 与后端 ExperiencePageReqVO.pageNum 一致 */
  pageNum?: number
  pageSize?: number
  keyword?: string
  companyName?: string
  position?: string
  experienceType?: string
}

export const experiencesApi = {
  getExperiences: (params?: GetExperiencesParams) =>
    http.get<Experience[]>('/experiences', { params }),

  getExperienceDetail: (id: number) =>
    http.get<Experience>(`/experiences/${id}`),

  createExperience: (payload: Omit<Experience, 'id' | 'createdAt' | 'views' | 'likes'>) =>
    http.post<number>('/experiences', payload),

  updateExperience: (id: number, payload: Partial<Experience>) =>
    http.put<void>(`/experiences/${id}`, payload),

  deleteExperience: (id: number) =>
    http.delete<void>(`/experiences/${id}`)
}

export const defaultExperiences: Experience[] = [
  {
    id: 1,
    companyName: '腾讯',
    position: '高级前端工程师',
    experienceType: 'full_time',
    content: '面试流程主要包括技术面试和HR面。技术面试主要考察Vue源码、性能优化、工程化等。建议深入理解Vue3的响应式原理和Composition API。',
    formattedContent: '<p>面试流程主要包括技术面试和HR面</p><p><strong>技术面试</strong>主要考察Vue源码、性能优化、工程化等</p><p>建议深入理解Vue3的响应式原理和Composition API</p>',
    source: '用户投稿',
    author: '匿名',
    views: 1250,
    likes: 89,
    createdAt: '2026-01-15T10:30:00Z'
  },
  {
    id: 2,
    companyName: '阿里',
    position: '前端开发工程师',
    experienceType: 'full_time',
    content: '阿里非常注重基础功底,问了很多React相关的问题。包括虚拟DOM、diff算法、hooks原理等。项目经历要准备充分,最好能讲出技术选型的原因。',
    formattedContent: '<p>阿里非常注重基础功底,问了很多React相关的问题</p><p>包括虚拟DOM、diff算法、hooks原理等</p><p>项目经历要准备充分,最好能讲出技术选型的原因</p>',
    source: '用户投稿',
    author: '前端小哥',
    views: 980,
    likes: 76,
    createdAt: '2026-02-20T14:20:00Z'
  },
  {
    id: 3,
    companyName: '字节跳动',
    position: '前端工程师',
    experienceType: 'full_time',
    content: '字节的面试节奏很快,技术面非常深入。除了常规的前端知识,还问了微前端架构、跨端方案等。现场编码题是实现一个简单的发布订阅模式。',
    formattedContent: '<p>字节的面试节奏很快,技术面非常深入</p><p>除了常规的前端知识,还问了微前端架构、跨端方案等</p><p>现场编码题是实现一个简单的发布订阅模式</p>',
    source: '用户投稿',
    author: '字节面试者',
    views: 1560,
    likes: 112,
    createdAt: '2026-03-10T09:15:00Z'
  },
  {
    id: 4,
    companyName: '美团',
    position: '高级前端开发',
    experienceType: 'full_time',
    content: '美团特别看重工程化能力,问了很多关于CI/CD、构建优化、监控体系的问题。建议对前端性能优化有系统的了解。',
    formattedContent: '<p>美团特别看重工程化能力,问了很多关于CI/CD、构建优化、监控体系的问题</p><p>建议对前端性能优化有系统的了解</p>',
    source: '用户投稿',
    author: '老前端',
    views: 890,
    likes: 67,
    createdAt: '2026-03-25T16:45:00Z'
  },
  {
    id: 5,
    companyName: '拼多多',
    position: '前端开发',
    experienceType: 'full_time',
    content: '拼多多的技术面比较务实,主要考察实际解决问题的能力。问了浏览器原理、网络协议、安全问题等。编码题是实现一个防抖节流函数。',
    formattedContent: '<p>拼多多的技术面比较务实,主要考察实际解决问题的能力</p><p>问了浏览器原理、网络协议、安全问题等</p><p>编码题是实现一个防抖节流函数</p>',
    source: '用户投稿',
    author: '求职者',
    views: 1120,
    likes: 83,
    createdAt: '2026-04-05T11:30:00Z'
  }
]
