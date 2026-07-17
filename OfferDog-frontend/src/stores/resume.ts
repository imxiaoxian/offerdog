import { defineStore } from 'pinia'
import { ref, watch } from 'vue'
import { userApi, type ResumeLocation, type ResumePayload, type YearMonth } from '@/services/users'
import { message } from 'ant-design-vue'

export type ResumeTemplate = 'modern' | 'classic' | 'creative'

export interface ResumePersonalInfo {
  name: string
  title: string
  phone: string
  email: string
  location: string
  summary: string
  birth?: string
  gender?: string
  homepage?: string
  resumeFileUrl?: string
}

export interface ResumeEducationItem {
  id: string
  school: string
  major: string
  degree: string
  startDate: string
  endDate: string
  description: string
}

export interface ResumeWorkExperienceItem {
  id: string
  company: string
  position: string
  startDate: string
  endDate: string
  description: string
}

export interface ResumeProjectItem {
  id: string
  name: string
  role: string
  startDate: string
  endDate: string
  description: string
  highlights?: string[]
}

export interface ResumeSkill {
  id: string
  name: string
  level: number
}

export interface ResumeData {
  personalInfo: ResumePersonalInfo
  education: ResumeEducationItem[]
  workExperience: ResumeWorkExperienceItem[]
  projects: ResumeProjectItem[]
  skills: ResumeSkill[]
}

const STORAGE_KEYS = {
  data: 'resumeData',
  template: 'currentTemplate',
} as const

const formatYearMonth = (ym?: YearMonth | string | null): string => {
  if (!ym) return ''

  if (typeof ym === 'string') {
    return ym
  }

  if (!ym.year) return ''
  return ym.month ? `${ym.year}-${String(ym.month).padStart(2, '0')}` : `${ym.year}`
}

const parseYearMonthValue = (value?: string | null): YearMonth | undefined => {
  if (!value) return undefined
  const normalized = value.trim()
  if (!normalized || normalized === '至今') return undefined

  const match = normalized.match(/^(\d{4})(?:[-/](\d{1,2}))?$/)
  if (!match) return undefined

  const year = Number(match[1])
  if (!Number.isFinite(year)) return undefined

  const month = match[2] ? Number(match[2]) : undefined
  if (!month) {
    return { year }
  }

  const parsedMonth = Number(month)
  if (!Number.isFinite(parsedMonth) || parsedMonth < 1 || parsedMonth > 12) {
    return { year }
  }

  return { year, month }
}

const buildLocationPayload = (location?: string): ResumeLocation | undefined => {
  if (!location) return undefined

  const parts = location.split(/[\s,，]/).filter(Boolean)
  if (parts.length === 0) return undefined

  const province = parts[0]
  const city = parts.length > 1 ? parts[1] : undefined

  if (!province) return undefined

  return {
    province,
    city,
  }
}

const getDefaultData = (): ResumeData => ({
  personalInfo: {
    name: '张三',
    title: '前端开发工程师',
    phone: '138-8888-8888',
    email: 'zhangsan@example.com',
    location: '北京市朝阳区',
    summary:
      '5年前端开发经验，精通Vue、React等主流框架，具有良好的编程习惯和团队协作能力。熟悉前端工程化、性能优化，有大型项目开发经验。',
  },
  education: [
    {
      id: '1',
      school: '清华大学',
      major: '计算机科学与技术',
      degree: '本科',
      startDate: '2015-09',
      endDate: '2019-06',
      description: 'GPA: 3.8/4.0，主修课程：数据结构、算法设计、操作系统、计算机网络',
    },
  ],
  workExperience: [
    {
      id: '1',
      company: '字节跳动',
      position: '高级前端开发工程师',
      startDate: '2025-07',
      endDate: '至今',
      description:
        '负责抖音Web端核心功能开发，日活用户超过1000万\n使用Vue3、TypeScript开发高性能组件，提升页面加载速度30%\n参与前端架构设计，推动组件库建设，提高团队开发效率\n指导初级开发人员，进行代码评审和技术分享',
    },
    {
      id: '2',
      company: '阿里巴巴',
      position: '前端开发工程师',
      startDate: '2019-07',
      endDate: '2025-06',
      description:
        '参与淘宝商家平台开发，维护核心业务模块\n使用Vue.js开发多个数据可视化功能模块\n优化前端性能，将首屏加载时间减少40%\n参与前端工程化建设，搭建CI/CD流程',
    },
  ],
  projects: [
    {
      id: '1',
      name: '在线协作文档平台',
      role: '前端负责人',
      startDate: '2026-03',
      endDate: '2026-12',
      description:
        '基于Vue3和WebSocket开发实时协作编辑功能\n实现富文本编辑器，支持多人同时在线编辑\n使用IndexedDB实现离线数据存储\n项目上线后用户满意度达到95%',
    },
    {
      id: '2',
      name: '电商移动端H5应用',
      role: '核心开发',
      startDate: '2024-06',
      endDate: '2024-11',
      description:
        '使用Vue.js + Vant开发移动端电商应用\n实现商品展示、购物车、订单管理等核心功能\n优化移动端性能，首屏加载时间控制在1秒内\n接入第三方支付，完成交易闭环',
    },
  ],
  skills: [
    { id: '1', name: 'Vue.js / Vue3', level: 90 },
    { id: '2', name: 'JavaScript / TypeScript', level: 88 },
    { id: '3', name: 'React', level: 80 },
    { id: '4', name: 'HTML / CSS', level: 90 },
    { id: '5', name: 'Node.js', level: 75 },
    { id: '6', name: 'Webpack / Vite', level: 82 },
  ],
})

const convertApiResumeToStoreFormat = (apiResume?: ResumePayload | null): Partial<ResumeData> | null => {
  if (!apiResume) return null

  const personalInfo: ResumePersonalInfo = {
    name: apiResume.name || '未填写',
    title: apiResume.jobIntention || '求职中',
    phone: apiResume.contact?.phone || '',
    email: apiResume.contact?.email || '',
    location: apiResume.location
      ? `${apiResume.location.province || ''} ${apiResume.location.city || ''}`.trim()
      : '',
    summary: apiResume.selfEvaluation || '',
    birth: apiResume.birth || '',
    gender: apiResume.gender || '',
    homepage: apiResume.contact?.homepage || '',
    resumeFileUrl: apiResume.resumeFileUrl || '',
  }

  const education: ResumeEducationItem[] = Array.isArray(apiResume.education) 
    ? apiResume.education.map((edu, index) => ({
        id: String(index + 1),
        school: edu.school || '',
        major: edu.major || '',
        degree: edu.level || '本科',
        startDate: '',
        endDate: formatYearMonth(edu.graduationDate),
        description: '',
      }))
    : []

  const workExperience: ResumeWorkExperienceItem[] =
    apiResume.work?.experience?.map((exp, index) => ({
      id: String(index + 1),
      company: exp.company || '',
      position: exp.title || '',
      startDate: formatYearMonth(exp.startDate),
      endDate: formatYearMonth(exp.endDate) || '至今',
      description: exp.description || '',
    })) || []

  const projects: ResumeProjectItem[] =
    apiResume.work?.projects?.map((proj, index) => ({
      id: String(index + 1),
      name: proj.name || '',
      role: proj.role || '',
      startDate: '',
      endDate: '',
      description: proj.description || proj.highlights?.join('\n') || '',
    })) || []

  const skills: ResumeSkill[] = apiResume.skills?.map((skill, index) => ({
    id: String(index + 1),
    name: skill,
    level: 75,
  })) || []

  return {
    personalInfo,
    education,
    workExperience,
    projects,
    skills,
  }
}

const loadFromStorage = (): ResumeData => {
  if (typeof window === 'undefined') {
    return getDefaultData()
  }

  const saved = localStorage.getItem(STORAGE_KEYS.data)
  if (!saved) {
    return getDefaultData()
  }

  try {
    const parsed = JSON.parse(saved) as ResumeData
    return {
      ...getDefaultData(),
      ...parsed,
    }
  } catch (error) {
    console.warn('读取本地简历数据失败', error)
    return getDefaultData()
  }
}

const resolveStoredTemplate = (): ResumeTemplate => {
  if (typeof window === 'undefined') {
    return 'modern'
  }

  const stored = localStorage.getItem(STORAGE_KEYS.template)
  return stored === 'classic' || stored === 'creative' ? (stored as ResumeTemplate) : 'modern'
}

export const useResumeStore = defineStore('resume', () => {
  const resumeData = ref<ResumeData>(loadFromStorage())
  const currentTemplate = ref<ResumeTemplate>(resolveStoredTemplate())
  const loading = ref(false)

  watch(
    resumeData,
    (newData) => {
      if (typeof window !== 'undefined') {
        localStorage.setItem(STORAGE_KEYS.data, JSON.stringify(newData))
      }
    },
    { deep: true }
  )

  watch(currentTemplate, (newTemplate) => {
    if (typeof window !== 'undefined') {
      localStorage.setItem(STORAGE_KEYS.template, newTemplate)
    }
  })

  const resetData = () => {
    resumeData.value = getDefaultData()
  }

  const exportData = () => JSON.stringify(resumeData.value, null, 2)

  const importData = (jsonData: string) => {
    try {
      const parsed = JSON.parse(jsonData) as ResumeData
      resumeData.value = {
        ...getDefaultData(),
        ...parsed,
      }
      return true
    } catch (error) {
      console.error('导入简历数据失败', error)
      return false
    }
  }

  const loadUserResume = async (userId?: number): Promise<boolean> => {
    if (!userId) {
      console.warn('未提供用户ID，无法加载简历')
      return false
    }

    loading.value = true
    try {
      const response = await userApi.getUserDetail(userId)
      const convertedData = convertApiResumeToStoreFormat(response.data?.resume)

      if (convertedData) {
        resumeData.value = {
          ...getDefaultData(),
          ...convertedData,
        }
        return true
      }

      message.warning('简历数据为空，使用默认模板')
      return false
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '加载简历失败'
      message.error(errorMessage)
      console.error('加载简历失败', error)
      return false
    } finally {
      loading.value = false
    }
  }

  const parseResumeFromFile = async (userId: number | undefined, file: File): Promise<boolean> => {
    if (!userId) {
      message.warning('请先登录再上传简历解析')
      return false
    }

    loading.value = true
    try {
      console.log('🔍 开始上传简历文件', file.name, '文件大小:', file.size, 'bytes')
      const response = await userApi.uploadAndParseResume(userId, file)
      console.log('✅ 简历上传成功，响应数据:', response.data)
      const converted = convertApiResumeToStoreFormat(response.data)
      if (converted) {
        resumeData.value = {
          ...getDefaultData(),
          ...converted,
        }
        message.success('简历解析成功，请确认信息后保存')
        return true
      }
      console.warn('⚠️ 解析成功但未获取到有效信息')
      message.warning('解析成功但未获取到有效信息')
      return false
    } catch (error: any) {
      console.error('❌ 简历解析失败', error)
      const errorMessage = error instanceof Error ? error.message : '简历解析失败'
      const errorDetail = error?.response?.data?.message || error?.message || '未知错误'
      message.error(`简历解析失败: ${errorDetail}`)
      return false
    } finally {
      loading.value = false
    }
  }

  const saveUserResume = async (userId?: number): Promise<boolean> => {
    if (!userId) {
      console.warn('未提供用户ID，无法保存简历')
      message.warning('请先登录再保存简历')
      return false
    }

    loading.value = true
    try {
      const locationPayload = buildLocationPayload(resumeData.value.personalInfo.location)
      const educationPayload = resumeData.value.education
        .map((edu) => ({
          school: edu.school || undefined,
          major: edu.major || undefined,
          level: edu.degree || undefined,
          graduationDate: parseYearMonthValue(edu.endDate),
        }))
        .filter((edu) => edu.school || edu.major || edu.level || edu.graduationDate)

      const experiencePayload = resumeData.value.workExperience
        .map((exp) => ({
          company: exp.company || undefined,
          title: exp.position || undefined,
          startDate: parseYearMonthValue(exp.startDate),
          endDate: parseYearMonthValue(exp.endDate),
          description: exp.description || undefined,
        }))
        .filter(
          (exp) =>
            exp.company || exp.title || exp.startDate || exp.endDate || (exp.description && exp.description.trim())
        )

      const projectPayload = resumeData.value.projects
        .map((proj) => ({
          name: proj.name || undefined,
          role: proj.role || undefined,
          description: proj.description || undefined,
          highlights: proj.highlights && proj.highlights.length > 0 ? proj.highlights : undefined,
        }))
        .filter((proj) => proj.name || proj.role || proj.description || (proj.highlights && proj.highlights.length > 0))

      const lastExperience = resumeData.value.workExperience[resumeData.value.workExperience.length - 1]
      const workPayload: ResumePayload['work'] = {}
      const firstEmployment = parseYearMonthValue(lastExperience?.startDate)
      if (firstEmployment) {
        workPayload.firstEmployment = firstEmployment
      }
      if (experiencePayload.length > 0) {
        workPayload.experience = experiencePayload
      }
      if (projectPayload.length > 0) {
        workPayload.projects = projectPayload
      }
      const hasWorkPayload =
        Boolean(workPayload.firstEmployment) ||
        Boolean(workPayload.experience && workPayload.experience.length > 0) ||
        Boolean(workPayload.projects && workPayload.projects.length > 0)

      const skillsPayload = resumeData.value.skills
        .map((skill) => skill.name.trim())
        .filter((name) => Boolean(name))

      const contactPhone = resumeData.value.personalInfo.phone?.trim()
      const contactEmail = resumeData.value.personalInfo.email?.trim()
      const contactHomepage = resumeData.value.personalInfo.homepage?.trim()
      const contactPayload = {
        phone: contactPhone || undefined,
        email: contactEmail || undefined,
        homepage: contactHomepage || undefined,
      }
      const hasContactPayload = Boolean(contactPayload.phone || contactPayload.email || contactPayload.homepage)

      const resumePayload: ResumePayload = {
        name: resumeData.value.personalInfo.name,
        birth: resumeData.value.personalInfo.birth || undefined,
        gender: resumeData.value.personalInfo.gender || undefined,
        jobIntention: resumeData.value.personalInfo.title,
        selfEvaluation: resumeData.value.personalInfo.summary,
        resumeFileUrl: resumeData.value.personalInfo.resumeFileUrl || undefined,
        location: locationPayload,
        contact: hasContactPayload ? contactPayload : undefined,
        education: educationPayload.length > 0 ? educationPayload : undefined,
        work: hasWorkPayload ? workPayload : undefined,
        skills: skillsPayload.length > 0 ? skillsPayload : undefined,
      }

      console.log('🔍 保存简历前的调试信息')
      console.log('📋 用户ID:', userId)
      console.log('📋 简历数据:', JSON.stringify(resumePayload, null, 2))
      console.log('📋 localStorage用户信息:', window.localStorage.getItem('inai-user'))
      console.log('📋 浏览器Cookie:', document.cookie)
      
      console.log('🚀 调用userApi.updateUserResume...')
      const result = await userApi.updateUserResume(userId, resumePayload)
      console.log('✅ userApi.updateUserResume调用成功:', result)
      message.success('简历保存成功')
      return true
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '保存简历失败'
      message.error(errorMessage)
      console.error('保存简历失败', error)
      return false
    } finally {
      loading.value = false
    }
  }

  return {
    resumeData,
    currentTemplate,
    loading,
    resetData,
    exportData,
    importData,
    loadUserResume,
    parseResumeFromFile,
    saveUserResume,
  }
})
