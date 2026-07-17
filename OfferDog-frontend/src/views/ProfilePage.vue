<script lang="ts" setup>
import {
  UserOutlined,
  MailOutlined,
  PhoneOutlined,
  EnvironmentOutlined,
  CalendarOutlined,
  SafetyOutlined,
  TrophyOutlined,
  BookOutlined,
  TeamOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  BulbOutlined,
  UploadOutlined,
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useRouter } from 'vue-router'

import { userApi, type UserDetail, type YearMonth } from '@/services/users'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const { currentUser, isLoggedIn } = storeToRefs(userStore)
const router = useRouter()

const detail = ref<UserDetail | null>(null)
const loading = ref(false)

const reminders = [
  { icon: BulbOutlined, text: '完善个人标签可获得更精准的题目推荐', type: 'info' },
  { icon: SafetyOutlined, text: '绑定邮箱 / 手机号保障账号安全', type: 'warning' },
  { icon: CheckCircleOutlined, text: '多端同步，随时查看面试练习进度', type: 'success' },
]

const avatarUrl = computed(
  () =>
    detail.value?.profile?.avatarUrl || 'https://avatars.githubusercontent.com/u/9919?s=200&v=4',
)

const roleColors = {
  ADMIN: 'volcano',
  VIP: 'gold',
  USER: 'geekblue',
}

const roleColor = computed(() => {
  const role = currentUser.value?.role?.toUpperCase() || 'USER'
  return roleColors[role as keyof typeof roleColors] || 'geekblue'
})

const genderIcon = computed(() => {
  const gender = (detail.value?.profile?.gender || '').toLowerCase()
  if (gender === 'male' || gender === '男') return '♂️'
  if (gender === 'female' || gender === '女') return '♀️'
  return '👤'
})
const genderLabel = (gender?: string | null) => {
  const val = (gender || '').toLowerCase()
  if (val === 'male' || gender === '男') return '男'
  if (val === 'female' || gender === '女') return '女'
  if (val === 'other' || gender === '保密') return '保密'
  return '未填写'
}

const statisticsData = computed(() => [
  {
    title: '账号状态',
    value: detail.value?.isActive ? '已激活' : '未激活',
    icon: SafetyOutlined,
    color: detail.value?.isActive ? '#52c41a' : '#faad14',
  },
  {
    title: '用户角色',
    value: currentUser.value?.role?.toUpperCase() || 'USER',
    icon: TrophyOutlined,
    color: '#1890ff',
  },
  {
    title: '技能标签',
    value: detail.value?.resume?.skills?.length || 0,
    icon: BookOutlined,
    color: '#13c2c2',
  },
  {
    title: '加入天数',
    value: detail.value?.createdAt
      ? Math.floor(
          (Date.now() - new Date(detail.value.createdAt).getTime()) / (1000 * 60 * 60 * 24),
        )
      : 0,
    icon: CalendarOutlined,
    color: '#fa8c16',
  },
])

const fetchProfileDetail = async () => {
  if (!currentUser.value?.id) {
    detail.value = null
    return
  }
  loading.value = true
  try {
    const response = await userApi.getUserDetail(currentUser.value.id)
    detail.value = response.data ?? null
    if (detail.value?.profile) {
      profileForm.realName = detail.value.profile.realName || ''
      profileForm.phone = detail.value.profile.phone || ''
      const genderRaw = detail.value.profile.gender
      if (genderRaw === '男') profileForm.gender = 'male'
      else if (genderRaw === '女') profileForm.gender = 'female'
      else if (genderRaw === '保密') profileForm.gender = 'other'
      else profileForm.gender = (genderRaw || '').toLowerCase()
      profileForm.avatarUrl = detail.value.profile.avatarUrl || ''
      userStore.setUser({
        ...(currentUser.value || {}),
        avatarUrl: detail.value.profile.avatarUrl,
      } as any)
    }
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : '获取个人信息失败'
    message.error(errorMessage)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (currentUser.value?.id) {
    fetchProfileDetail()
  }
})

watch(
  () => currentUser.value?.id,
  (id) => {
    if (id) {
      fetchProfileDetail()
    } else {
      detail.value = null
    }
  },
)

const goLogin = () => {
  router.push('/login')
}

type YearMonthLike = YearMonth | string | null | undefined

const parseYearMonth = (value: YearMonthLike) => {
  if (!value) return null

  if (typeof value === 'string') {
    const [yearPart, monthPart] = value.split(/[-/.]/)
    const year = yearPart?.trim()
    const month = monthPart?.trim()

    if (!year && !month) return null
    return {
      year,
      month: month ? month.padStart(2, '0') : undefined,
    }
  }

  const year =
    typeof value.year === 'number' || typeof value.year === 'string'
      ? String(value.year)
      : undefined
  const month =
    typeof value.month === 'number' || typeof value.month === 'string'
      ? String(value.month).padStart(2, '0')
      : undefined

  if (!year && !month) return null
  return { year, month }
}

const formatYearMonth = (value: YearMonthLike) => {
  const parsed = parseYearMonth(value)
  if (!parsed) return ''
  const { year, month } = parsed

  if (year && month) return `${year}年${month}月`
  if (year) return `${year}年`
  if (month) return `${month}月`
  return ''
}

const formatExperienceRange = (start?: YearMonthLike, end?: YearMonthLike) => {
  const startText = formatYearMonth(start)
  const endText = formatYearMonth(end)

  if (!startText && !endText) return ''
  return `${startText || '起始时间未填'} - ${endText || '至今'}`
}

// 编辑资料表单
const profileForm = reactive({
  realName: '',
  phone: '',
  gender: '',
  avatarUrl: '',
})
const profileLoading = ref(false)

const handleAvatarUpload = async (file: File) => {
  if (!currentUser.value?.id) return false
  profileLoading.value = true
  try {
    const response = await userApi.uploadAvatar(currentUser.value.id, file)
    if (response.data?.fileUrl) {
      profileForm.avatarUrl = response.data.fileUrl
      message.success('头像上传成功')
    }
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : '头像上传失败'
    message.error(errorMessage)
  } finally {
    profileLoading.value = false
  }
  return false
}

const handleProfileSubmit = async () => {
  if (!currentUser.value?.id || !detail.value?.profile) {
    message.error('未找到用户信息')
    return
  }
  profileLoading.value = true
  try {
    const payloadProfile = {
      ...detail.value.profile,
      email: detail.value.profile.email || currentUser.value?.email || '',
      realName: profileForm.realName || detail.value.profile.realName,
      phone: profileForm.phone || detail.value.profile.phone,
      gender: profileForm.gender || detail.value.profile.gender,
      avatarUrl: profileForm.avatarUrl || detail.value.profile.avatarUrl,
    }
    await userApi.updateUser(currentUser.value.id, { profile: payloadProfile })
    message.success('资料更新成功')
    // 同步本地用户信息（头像）
    userStore.setUser({
      ...(currentUser.value || {}),
      avatarUrl: payloadProfile.avatarUrl,
    } as any)
    fetchProfileDetail()
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : '更新失败'
    message.error(errorMessage)
  } finally {
    profileLoading.value = false
  }
}
</script>

<template>
  <div class="profile-page">
    <template v-if="isLoggedIn">
      <a-spin :spinning="loading">
        <!-- 个人信息头部卡片 -->
        <div class="profile-header">
          <div class="header-background"></div>
          <div class="header-content">
            <a-avatar :src="avatarUrl" :size="120" class="user-avatar" />
            <div class="user-info">
              <div class="user-name-section">
                <h1 class="user-name">
                  {{ detail?.profile?.realName || detail?.username || currentUser?.username }}
                  <span class="gender-icon">{{ genderIcon }}</span>
                </h1>
                <a-tag :color="roleColor" class="role-tag">
                  <TrophyOutlined />
                  {{ currentUser?.role?.toUpperCase() || 'USER' }}
                </a-tag>
              </div>
              <div class="user-meta">
                <span class="meta-item">
                  <MailOutlined />
                  {{ currentUser?.email }}
                </span>
                <span v-if="detail?.profile?.phone" class="meta-item">
                  <PhoneOutlined />
                  {{ detail.profile.phone }}
                </span>
                <span class="meta-item">
                  <ClockCircleOutlined />
                  最近登录：{{ detail?.updatedAt || '-' }}
                </span>
              </div>
            </div>
          </div>
        </div>

        <!-- 统计数据卡片 -->
        <a-row :gutter="[16, 16]" class="statistics-row">
          <a-col :xs="24" :sm="12" :md="6" v-for="stat in statisticsData" :key="stat.title">
            <a-card class="stat-card" :bodyStyle="{ padding: '20px' }">
              <div class="stat-content">
                <component :is="stat.icon" class="stat-icon" :style="{ color: stat.color }" />
                <div class="stat-info">
                  <div class="stat-value" :style="{ color: stat.color }">{{ stat.value }}</div>
                  <div class="stat-title">{{ stat.title }}</div>
                </div>
              </div>
            </a-card>
          </a-col>
        </a-row>

        <a-row :gutter="[16, 16]" class="content-row">
          <!-- 左侧列 -->
          <a-col :xs="24" :lg="12">
            <!-- 账号档案 -->
            <a-card title="账号档案" class="info-card" :headStyle="{ background: '#fafafa' }">
              <template #extra>
                <UserOutlined style="font-size: 16px" />
              </template>
              <div class="info-item">
                <UserOutlined class="item-icon" />
                <span class="item-label">用户名</span>
                <span class="item-value">{{ detail?.username || currentUser?.username }}</span>
              </div>
              <a-divider style="margin: 12px 0" />
              <div class="info-item">
                <MailOutlined class="item-icon" />
                <span class="item-label">邮箱</span>
                <span class="item-value">{{ detail?.profile?.email || currentUser?.email }}</span>
              </div>
              <a-divider style="margin: 12px 0" />
              <div class="info-item">
                <PhoneOutlined class="item-icon" />
                <span class="item-label">手机号</span>
                <span class="item-value">{{ detail?.profile?.phone || '未绑定' }}</span>
              </div>
              <a-divider style="margin: 12px 0" />
              <div class="info-item">
                <TeamOutlined class="item-icon" />
                <span class="item-label">性别</span>
                <span class="item-value">{{ genderLabel(detail?.profile?.gender) }}</span>
              </div>
              <a-divider style="margin: 12px 0" />
              <div class="info-item">
                <SafetyOutlined class="item-icon" />
                <span class="item-label">账号状态</span>
                <a-tag :color="detail?.isActive ? 'success' : 'warning'" style="margin: 0">
                  {{ detail?.isActive ? '已激活' : '未激活' }}
                </a-tag>
              </div>
              <a-divider style="margin: 12px 0" />
              <div class="info-item">
                <CalendarOutlined class="item-icon" />
                <span class="item-label">加入时间</span>
                <span class="item-value">{{ detail?.createdAt || '-' }}</span>
              </div>
            </a-card>
            <!-- 编辑资料 -->
            <a-card
              title="编辑资料"
              class="info-card"
              :headStyle="{ background: '#fafafa' }"
              style="margin-top: 16px"
            >
              <a-form layout="vertical">
                <a-form-item label="头像">
                  <div class="avatar-edit">
                    <a-avatar :src="profileForm.avatarUrl || avatarUrl" :size="72" />
                    <a-upload
                      :show-upload-list="false"
                      :before-upload="handleAvatarUpload"
                      accept=".png,.jpg,.jpeg,.gif"
                    >
                      <a-button :loading="profileLoading" style="margin-left: 12px">
                        <template #icon><UploadOutlined /></template>
                        上传头像
                      </a-button>
                    </a-upload>
                  </div>
                </a-form-item>
                <a-form-item label="昵称/姓名">
                  <a-input v-model:value="profileForm.realName" placeholder="请输入昵称" />
                </a-form-item>
                <a-form-item label="手机号">
                  <a-input v-model:value="profileForm.phone" placeholder="请输入手机号" />
                </a-form-item>
                <a-form-item label="性别">
                  <a-select v-model:value="profileForm.gender" placeholder="请选择性别" allow-clear>
                    <a-select-option value="male">男</a-select-option>
                    <a-select-option value="female">女</a-select-option>
                    <a-select-option value="other">保密</a-select-option>
                  </a-select>
                </a-form-item>
                <a-space>
                  <a-button type="primary" :loading="profileLoading" @click="handleProfileSubmit">
                    保存
                  </a-button>
                  <a-button
                    @click="
                      () => {
                        profileForm.realName = detail?.profile?.realName || ''
                        profileForm.phone = detail?.profile?.phone || ''
                        const genderRaw = detail?.profile?.gender
                        if (genderRaw === '男') profileForm.gender = 'male'
                        else if (genderRaw === '女') profileForm.gender = 'female'
                        else if (genderRaw === '保密') profileForm.gender = 'other'
                        else profileForm.gender = (genderRaw || '').toLowerCase()
                        profileForm.avatarUrl = detail?.profile?.avatarUrl || ''
                      }
                    "
                  >
                    重置
                  </a-button>
                </a-space>
              </a-form>
            </a-card>

            <!-- 个人提醒 -->
            <a-card
              title="个人提醒"
              class="info-card reminder-card"
              :headStyle="{ background: '#fafafa' }"
            >
              <template #extra>
                <BulbOutlined style="font-size: 16px" />
              </template>
              <div v-for="(reminder, index) in reminders" :key="index" class="reminder-item">
                <component :is="reminder.icon" :class="`reminder-icon reminder-${reminder.type}`" />
                <span class="reminder-text">{{ reminder.text }}</span>
              </div>
            </a-card>
          </a-col>

          <!-- 右侧列 -->
          <a-col :xs="24" :lg="12">
            <!-- 我的简历 -->
            <a-card
              title="我的简历"
              class="info-card resume-card"
              :headStyle="{ background: '#fafafa' }"
            >
              <template #extra>
                <BookOutlined style="font-size: 16px" />
              </template>
              <template v-if="detail?.resume">
                <!-- 基本信息 -->
                <div class="resume-section">
                  <h4 class="section-title">基本信息</h4>
                  <div class="info-item">
                    <UserOutlined class="item-icon" />
                    <span class="item-label">姓名</span>
                    <span class="item-value">{{
                      detail.resume.name || detail.profile?.realName
                    }}</span>
                  </div>
                  <div class="info-item">
                    <TrophyOutlined class="item-icon" />
                    <span class="item-label">求职意向</span>
                    <span class="item-value">{{ detail.resume.jobIntention || '未填写' }}</span>
                  </div>
                  <div class="info-item">
                    <EnvironmentOutlined class="item-icon" />
                    <span class="item-label">所在地区</span>
                    <span class="item-value">
                      {{
                        detail.resume.location
                          ? `${detail.resume.location.province || ''} ${detail.resume.location.city || ''}`
                          : '未填写'
                      }}
                    </span>
                  </div>
                </div>

                <!-- 教育经历 -->
                <a-divider />
                <div class="resume-section">
                  <h4 class="section-title">
                    <BookOutlined />
                    教育经历
                  </h4>
                  <div v-if="detail.resume.education && detail.resume.education.length > 0" class="education-info">
                    <div v-for="(edu, index) in detail.resume.education" :key="index" class="education-item-container">
                      <div class="education-item">
                        <strong>{{ edu.school }}</strong>
                      </div>
                      <div class="education-item">
                        {{ edu.major }} · {{ edu.level }}
                      </div>
                      <div
                        v-if="formatYearMonth(edu?.graduationDate)"
                        class="education-item text-secondary"
                      >
                        毕业时间：
                        {{ formatYearMonth(edu?.graduationDate) }}
                      </div>
                      <a-divider v-if="index < detail.resume.education.length - 1" style="margin: 10px 0" />
                    </div>
                  </div>
                  <span v-else class="text-secondary">未填写</span>
                </div>

                <!-- 联系方式 -->
                <a-divider />
                <div class="resume-section">
                  <h4 class="section-title">
                    <PhoneOutlined />
                    联系方式
                  </h4>
                  <div class="contact-info">
                    <div v-if="detail.resume.contact?.phone" class="contact-item">
                      <PhoneOutlined />
                      {{ detail.resume.contact.phone }}
                    </div>
                    <div v-if="detail.resume.contact?.email" class="contact-item">
                      <MailOutlined />
                      {{ detail.resume.contact.email }}
                    </div>
                    <div v-if="detail.resume.contact?.homepage" class="contact-item">
                      <a :href="detail.resume.contact.homepage" target="_blank">
                        🔗 {{ detail.resume.contact.homepage }}
                      </a>
                    </div>
                    <span v-if="!detail.resume.contact" class="text-secondary">未填写</span>
                  </div>
                </div>

                <!-- 技能标签 -->
                <a-divider />
                <div class="resume-section">
                  <h4 class="section-title">
                    <BookOutlined />
                    技能标签
                  </h4>
                  <a-space wrap>
                    <a-tag
                      v-for="skill in detail.resume.skills || []"
                      :key="skill"
                      color="geekblue"
                      class="skill-tag"
                    >
                      {{ skill }}
                    </a-tag>
                    <span
                      v-if="!detail.resume.skills || detail.resume.skills.length === 0"
                      class="text-secondary"
                    >
                      暂无技能标签
                    </span>
                  </a-space>
                </div>

                <!-- 自我评价 -->
                <a-divider />
                <div class="resume-section">
                  <h4 class="section-title">自我评价</h4>
                  <a-typography-paragraph class="self-evaluation">
                    {{ detail.resume.selfEvaluation || '暂无自我评价' }}
                  </a-typography-paragraph>
                </div>

                <!-- 工作经历 -->
                <template
                  v-if="detail.resume.work?.experience && detail.resume.work.experience.length > 0"
                >
                  <a-divider />
                  <div class="resume-section">
                    <h4 class="section-title">工作经历</h4>
                    <div
                      v-for="(exp, index) in detail.resume.work.experience"
                      :key="index"
                      class="experience-item"
                    >
                      <div class="experience-header">
                        <strong>{{ exp.company }}</strong>
                        <span class="experience-title">{{ exp.title }}</span>
                      </div>
                      <div
                        v-if="formatExperienceRange(exp.startDate, exp.endDate)"
                        class="experience-date text-secondary"
                      >
                        {{ formatExperienceRange(exp.startDate, exp.endDate) }}
                      </div>
                      <div v-if="exp.description" class="experience-desc">
                        {{ exp.description }}
                      </div>
                    </div>
                  </div>
                </template>

                <!-- 项目经历 -->
                <template
                  v-if="detail.resume.work?.projects && detail.resume.work.projects.length > 0"
                >
                  <a-divider />
                  <div class="resume-section">
                    <h4 class="section-title">项目经历</h4>
                    <div
                      v-for="(project, index) in detail.resume.work.projects"
                      :key="index"
                      class="project-item"
                    >
                      <div class="project-header">
                        <strong>{{ project.name }}</strong>
                        <a-tag v-if="project.role" color="processing">{{ project.role }}</a-tag>
                      </div>
                      <div v-if="project.description" class="project-desc">
                        {{ project.description }}
                      </div>
                      <div
                        v-if="project.highlights && project.highlights.length > 0"
                        class="project-highlights"
                      >
                        <div
                          v-for="(highlight, hIndex) in project.highlights"
                          :key="hIndex"
                          class="highlight-item"
                        >
                          • {{ highlight }}
                        </div>
                      </div>
                    </div>
                  </div>
                </template>
              </template>
              <a-empty v-else description="暂无简历信息，去用户管理中完善吧" />
            </a-card>
          </a-col>
        </a-row>
      </a-spin>
    </template>
    <template v-else>
      <div class="empty-state">
        <a-card>
          <a-empty description="请先登录以查看个人信息">
            <a-button type="primary" size="large" @click="goLogin">去登录</a-button>
          </a-empty>
        </a-card>
      </div>
    </template>
  </div>
</template>

<style scoped>
.profile-page {
  width: 100%;
  max-width: 1400px;
  margin: 0 auto;
  padding-bottom: 24px;
}

/* 头部卡片样式 */
.profile-header {
  position: relative;
  margin-bottom: 24px;
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.header-background {
  height: 180px;
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
  position: relative;
}

.header-content {
  padding: 0 32px 24px;
  display: flex;
  align-items: flex-start;
  gap: 24px;
  margin-top: -60px;
  position: relative;
}

.user-avatar {
  border: 4px solid white;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  flex-shrink: 0;
}

.user-info {
  flex: 1;
  margin-top: 60px;
}

.user-name-section {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}

.user-name {
  font-size: 28px;
  font-weight: 600;
  margin: 0;
  color: #262626;
  display: flex;
  align-items: center;
  gap: 8px;
}

.gender-icon {
  font-size: 24px;
}

.role-tag {
  font-size: 14px;
  padding: 4px 12px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.user-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  color: #8c8c8c;
  font-size: 14px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 统计卡片样式 */
.statistics-row {
  margin-bottom: 16px;
}

.stat-card {
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
  cursor: pointer;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  font-size: 36px;
  opacity: 0.9;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  line-height: 1.2;
  margin-bottom: 4px;
}

.stat-title {
  font-size: 14px;
  color: #8c8c8c;
}

/* 内容行样式 */
.content-row {
  margin-top: 16px;
}

.info-card {
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  margin-bottom: 16px;
  transition: box-shadow 0.3s ease;
}

.info-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
}

.info-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 4px 0;
}

.item-icon {
  font-size: 16px;
  color: #1890ff;
  width: 20px;
  text-align: center;
}

.item-label {
  color: #8c8c8c;
  font-size: 14px;
  min-width: 80px;
}

.item-value {
  flex: 1;
  color: #262626;
  font-size: 14px;
  font-weight: 500;
}

/* 提醒卡片样式 */
.reminder-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px;
  margin-bottom: 8px;
  border-radius: 6px;
  background: #fafafa;
  transition: all 0.3s ease;
}

.reminder-item:last-child {
  margin-bottom: 0;
}

.reminder-item:hover {
  background: #f0f0f0;
  transform: translateX(4px);
}

.reminder-icon {
  font-size: 18px;
  margin-top: 2px;
  flex-shrink: 0;
}

.reminder-icon.reminder-info {
  color: #1890ff;
}

.reminder-icon.reminder-warning {
  color: #faad14;
}

.reminder-icon.reminder-success {
  color: #52c41a;
}

.reminder-text {
  flex: 1;
  color: #595959;
  font-size: 14px;
  line-height: 1.6;
}

/* 简历卡片样式 */
.resume-section {
  margin-bottom: 12px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #262626;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.education-info,
.contact-info {
  padding-left: 8px;
}

.education-item,
.contact-item {
  margin-bottom: 8px;
  color: #595959;
  font-size: 14px;
  line-height: 1.6;
}

.contact-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.education-item-container {
  padding: 16px;
  background: #fafafa;
  border-radius: 6px;
  margin-bottom: 12px;
}

.education-item-container:last-child {
  margin-bottom: 0;
}

.education-item {
  margin-bottom: 4px;
}

.education-item:last-child {
  margin-bottom: 0;
}

.text-secondary {
  color: #8c8c8c;
}

.skill-tag {
  font-size: 13px;
  padding: 4px 12px;
  border-radius: 4px;
  cursor: default;
}

.self-evaluation {
  background: #fafafa;
  padding: 16px;
  border-radius: 6px;
  color: #595959;
  font-size: 14px;
  line-height: 1.8;
  margin: 0;
}

.experience-item,
.project-item {
  padding: 16px;
  background: #fafafa;
  border-radius: 6px;
  margin-bottom: 12px;
}

.experience-item:last-child,
.project-item:last-child {
  margin-bottom: 0;
}

.experience-header,
.project-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  gap: 12px;
}

.avatar-edit {
  display: flex;
  align-items: center;
  gap: 12px;
}

.experience-title {
  color: #595959;
  font-size: 14px;
  margin-left: 12px;
}

.experience-date {
  font-size: 13px;
  margin-bottom: 8px;
}

.experience-desc,
.project-desc {
  color: #595959;
  font-size: 14px;
  line-height: 1.6;
  margin-top: 8px;
}

.project-highlights {
  margin-top: 12px;
}

.highlight-item {
  color: #595959;
  font-size: 14px;
  line-height: 1.8;
  padding-left: 8px;
}

/* 空状态样式 */
.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .header-content {
    flex-direction: column;
    align-items: center;
    text-align: center;
    padding: 0 16px 24px;
  }

  .user-info {
    margin-top: 16px;
  }

  .user-name {
    font-size: 24px;
  }

  .user-meta {
    justify-content: center;
  }

  .stat-value {
    font-size: 20px;
  }

  .item-label {
    min-width: 60px;
  }
}

@media (max-width: 480px) {
  .profile-page {
    padding-bottom: 12px;
  }

  .header-content {
    padding: 0 12px 16px;
  }

  .user-name {
    font-size: 20px;
  }

  .stat-value {
    font-size: 18px;
  }

  .stat-icon {
    font-size: 28px;
  }

  .stat-info {
    flex: 1;
  }

  .stat-title {
    font-size: 12px;
  }

  .item-label {
    min-width: 50px;
    font-size: 12px;
  }

  .item-value {
    font-size: 12px;
  }

  .item-icon {
    font-size: 14px;
    width: 16px;
  }

  .info-item {
    gap: 8px;
  }

  .section-title {
    font-size: 14px;
  }

  .education-item,
  .contact-item {
    font-size: 12px;
  }

  .skill-tag {
    font-size: 11px;
    padding: 3px 8px;
  }

  .self-evaluation {
    padding: 12px;
    font-size: 12px;
  }

  .experience-item,
  .project-item {
    padding: 12px;
    margin-bottom: 8px;
  }

  .experience-header,
  .project-header {
    margin-bottom: 6px;
    gap: 8px;
  }

  .experience-title,
  .project-title {
    font-size: 12px;
  }

  .experience-date,
  .project-desc {
    font-size: 11px;
  }

  .reminder-item {
    padding: 8px;
    margin-bottom: 6px;
  }

  .reminder-icon {
    font-size: 16px;
  }

  .reminder-text {
    font-size: 12px;
  }
}
</style>
