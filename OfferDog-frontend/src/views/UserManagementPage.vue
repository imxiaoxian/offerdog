<script lang="ts" setup>
import type { FormInstance } from 'ant-design-vue'
import { message } from 'ant-design-vue'
import { onMounted, reactive, ref } from 'vue'

import {
  type CreateUserPayload,
  type PageUsersQuery,
  type ResumePayload,
  type UserDetail,
  type UserSummary,
  userApi,
} from '@/services/users'

const listLoading = ref(false)
const createSubmitting = ref(false)
const users = ref<UserSummary[]>([])
const pagination = reactive({
  pageNo: 1,
  pageSize: 10,
  totalCount: 0,
})

const queryForm = reactive<PageUsersQuery>({
  username: '',
  role: undefined,
})

const roleOptions = [
  { label: '普通用户', value: 'USER' },
  { label: '管理员', value: 'ADMIN' },
  { label: 'VIP 用户', value: 'VIP' },
]

const genderOptions = [
  { label: '男', value: 'MALE' },
  { label: '女', value: 'FEMALE' },
  { label: '其他', value: 'OTHER' },
]

const createModalVisible = ref(false)
const includeResume = ref(false)
const createFormRef = ref<FormInstance>()

const createForm = reactive({
  username: '',
  password: '',
  role: 'USER' as 'USER' | 'ADMIN' | 'VIP',
  isActive: true,
  profile: {
    email: '',
    phone: '',
    realName: '',
    avatarUrl: '',
    gender: 'OTHER' as 'MALE' | 'FEMALE' | 'OTHER',
  },
})

const resumeForm = reactive({
  name: '',
  birth: '',
  gender: 'OTHER' as 'MALE' | 'FEMALE' | 'OTHER',
  locationProvince: '',
  locationCity: '',
  educationLevel: '',
  educationSchool: '',
  educationMajor: '',
  graduationYear: undefined as undefined | number,
  graduationMonth: undefined as undefined | number,
  contactPhone: '',
  contactEmail: '',
  contactHomepage: '',
  jobIntention: '',
  skills: '',
  selfEvaluation: '',
})

const selectedUser = ref<UserDetail | null>(null)
const detailVisible = ref(false)
const detailLoading = ref(false)
const statusUpdating = reactive<Record<number, boolean>>({})
const deletingId = ref<number | null>(null)

const normalizeRole = (role: string) => role?.toUpperCase?.() ?? role
const getRoleTagColor = (role: string) => {
  const normalized = normalizeRole(role)
  if (normalized === 'ADMIN') return 'red'
  if (normalized === 'VIP') return 'purple'
  return 'blue'
}

const fetchUsers = async () => {
  listLoading.value = true
  try {
    const response = await userApi.getUsers({
      pageNo: pagination.pageNo,
      pageSize: pagination.pageSize,
      username: queryForm.username || undefined,
      role: queryForm.role || undefined,
    })
    const items = response.data ?? []
    users.value = items
    pagination.totalCount = response.totalCount ?? items.length
    pagination.pageSize = response.pageSize ?? pagination.pageSize
    pagination.pageNo = response.pageNo ?? pagination.pageNo
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : '获取用户列表失败'
    message.error(errorMessage)
  } finally {
    listLoading.value = false
  }
}

const handleSearch = () => {
  pagination.pageNo = 1
  fetchUsers()
}

const handleReset = () => {
  queryForm.username = ''
  queryForm.role = undefined
  handleSearch()
}

const handlePaginationChange = (page: number, pageSize?: number) => {
  pagination.pageNo = page
  if (pageSize) {
    pagination.pageSize = pageSize
  }
  fetchUsers()
}

const openCreateModal = () => {
  resetCreateForm()
  createModalVisible.value = true
}

const closeCreateModal = () => {
  createModalVisible.value = false
}

const resetCreateForm = () => {
  createForm.username = ''
  createForm.password = ''
  createForm.role = 'USER'
  createForm.isActive = true
  createForm.profile.email = ''
  createForm.profile.phone = ''
  createForm.profile.realName = ''
  createForm.profile.avatarUrl = ''
  createForm.profile.gender = 'OTHER'
  includeResume.value = false
  resumeForm.name = ''
  resumeForm.birth = ''
  resumeForm.gender = 'OTHER'
  resumeForm.locationProvince = ''
  resumeForm.locationCity = ''
  resumeForm.educationLevel = ''
  resumeForm.educationSchool = ''
  resumeForm.educationMajor = ''
  resumeForm.graduationYear = undefined
  resumeForm.graduationMonth = undefined
  resumeForm.contactPhone = ''
  resumeForm.contactEmail = ''
  resumeForm.contactHomepage = ''
  resumeForm.jobIntention = ''
  resumeForm.skills = ''
  resumeForm.selfEvaluation = ''
  createFormRef.value?.clearValidate()
}

const buildResumePayload = (): ResumePayload | undefined => {
  if (!includeResume.value) {
    return undefined
  }

  const skills = resumeForm.skills
    ? resumeForm.skills.split(/[,，\s]+/).map((item) => item.trim()).filter(Boolean)
    : undefined

  const location = {
    province: resumeForm.locationProvince || undefined,
    city: resumeForm.locationCity || undefined,
  }
  const educationEntry = {
    level: resumeForm.educationLevel || undefined,
    school: resumeForm.educationSchool || undefined,
    major: resumeForm.educationMajor || undefined,
    graduationDate:
      resumeForm.graduationYear || resumeForm.graduationMonth
        ? {
            year: resumeForm.graduationYear,
            month: resumeForm.graduationMonth,
          }
        : undefined,
  }
  const contact = {
    phone: resumeForm.contactPhone || undefined,
    email: resumeForm.contactEmail || undefined,
    homepage: resumeForm.contactHomepage || undefined,
  }

  const educationList =
    educationEntry.level || educationEntry.school || educationEntry.major || educationEntry.graduationDate
      ? [educationEntry]
      : undefined

  const resumePayload: ResumePayload = {
    name: resumeForm.name || undefined,
    birth: resumeForm.birth || undefined,
    gender: resumeForm.gender,
    location: location.province || location.city ? location : undefined,
    education: educationList,
    contact: contact.phone || contact.email || contact.homepage ? contact : undefined,
    jobIntention: resumeForm.jobIntention || undefined,
    skills: skills && skills.length > 0 ? skills : undefined,
    selfEvaluation: resumeForm.selfEvaluation || undefined,
  }

  const hasResumeData =
    Boolean(resumePayload.name) ||
    Boolean(resumePayload.birth) ||
    Boolean(resumePayload.gender) ||
    Boolean(resumePayload.location) ||
    Boolean(resumePayload.education && resumePayload.education.length > 0) ||
    Boolean(resumePayload.contact) ||
    Boolean(resumePayload.jobIntention) ||
    Boolean(resumePayload.skills && resumePayload.skills.length > 0) ||
    Boolean(resumePayload.selfEvaluation)

  return hasResumeData ? resumePayload : undefined
}

const handleCreateUser = async () => {
  createSubmitting.value = true
  try {
    const payload: CreateUserPayload = {
      username: createForm.username,
      password: createForm.password,
      role: createForm.role,
      isActive: createForm.isActive,
      profile: { ...createForm.profile },
    }
    const resume = buildResumePayload()
    if (resume) {
      payload.resume = resume
    }

    await userApi.createUser(payload)
    message.success('用户创建成功')
    closeCreateModal()
    fetchUsers()
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : '创建用户失败'
    message.error(errorMessage)
  } finally {
    createSubmitting.value = false
  }
}

const loadUserDetail = async (userId: number) => {
  detailLoading.value = true
  try {
    const response = await userApi.getUserDetail(userId)
    if (response.data) {
      selectedUser.value = response.data
    }
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : '获取用户详情失败'
    message.error(errorMessage)
  } finally {
    detailLoading.value = false
  }
}

const showUserDetail = (record: UserSummary) => {
  selectedUser.value = { ...record }
  detailVisible.value = true
  loadUserDetail(record.id)
}

const handleStatusChange = async (checked: boolean, record: UserSummary) => {
  statusUpdating[record.id] = true
  try {
    await userApi.updateUser(record.id, { profile: record.profile, isActive: checked })
    message.success('状态已更新')
    fetchUsers()
    if (selectedUser.value?.id === record.id) {
      selectedUser.value = { ...selectedUser.value, isActive: checked }
    }
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : '更新状态失败'
    message.error(errorMessage)
  } finally {
    statusUpdating[record.id] = false
  }
}

const handleDeleteUser = async (record: UserSummary) => {
  deletingId.value = record.id
  try {
    await userApi.deleteUser(record.id)
    message.success('用户已删除')
    if (selectedUser.value?.id === record.id) {
      detailVisible.value = false
      selectedUser.value = null
    }
    fetchUsers()
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : '删除用户失败'
    message.error(errorMessage)
  } finally {
    deletingId.value = null
  }
}

onMounted(() => {
  fetchUsers()
})
</script>

<template>
  <a-space direction="vertical" size="large" class="user-page">
    <a-card title="用户筛选">
      <a-form layout="inline" :model="queryForm" @submit.prevent>
        <a-form-item label="用户名/邮箱">
          <a-input v-model:value="queryForm.username" placeholder="请输入用户名或邮箱" allow-clear />
        </a-form-item>
        <a-form-item label="角色">
          <a-select v-model:value="queryForm.role" placeholder="全部角色" allow-clear style="width: 180px">
            <a-select-option v-for="item in roleOptions" :key="item.value" :value="item.value">
              {{ item.label }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="handleSearch">查询</a-button>
            <a-button @click="handleReset">重置</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <a-card title="用户列表">
      <template #extra>
        <a-button type="primary" @click="openCreateModal">创建用户</a-button>
      </template>
      <a-table :data-source="users" :loading="listLoading" row-key="id" :pagination="false">
        <a-table-column title="ID" data-index="id" width="80" />
        <a-table-column title="用户名" data-index="username" />
        <a-table-column title="角色" data-index="role">
          <template #default="{ record }">
            <a-tag :color="getRoleTagColor(record.role)">
              {{ normalizeRole(record.role) }}
            </a-tag>
          </template>
        </a-table-column>
        <a-table-column title="邮箱">
          <template #default="{ record }">
            {{ record.profile?.email }}
          </template>
        </a-table-column>
        <a-table-column title="联系人">
          <template #default="{ record }">
            {{ record.profile?.realName }}
          </template>
        </a-table-column>
        <a-table-column title="状态">
          <template #default="{ record }">
            <a-badge :status="record.isActive ? 'success' : 'default'" :text="record.isActive ? '已激活' : '未激活'" />
          </template>
        </a-table-column>
        <a-table-column title="操作" width="240">
          <template #default="{ record }">
            <a-space size="middle">
              <a-button type="link" @click="showUserDetail(record)">查看详情</a-button>
              <a-switch
                :checked="record.isActive"
                checked-children="激活"
                un-checked-children="停用"
                :loading="statusUpdating[record.id]"
                @change="handleStatusChange($event as boolean, record)"
              />
              <a-popconfirm
                title="确认删除该用户？"
                ok-text="删除"
                ok-type="danger"
                cancel-text="取消"
                @confirm="() => handleDeleteUser(record)"
              >
                <a-button type="link" danger :loading="deletingId === record.id">删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </a-table-column>
      </a-table>
      <div class="table-pagination">
        <a-pagination
          :current="pagination.pageNo"
          :page-size="pagination.pageSize"
          :total="pagination.totalCount"
          show-size-changer
          @change="handlePaginationChange"
          @showSizeChange="handlePaginationChange"
        />
      </div>
    </a-card>

    <a-modal
      v-model:open="createModalVisible"
      title="创建新用户"
      width="720px"
      :destroyOnClose="true"
      :footer="null"
      @cancel="closeCreateModal"
    >
      <a-form layout="vertical" :model="createForm" ref="createFormRef" @finish="handleCreateUser">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="用户名" name="username" :rules="[{ required: true, message: '请输入用户名' }]">
              <a-input v-model:value="createForm.username" placeholder="请输入用户名" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="密码" name="password" :rules="[{ required: true, message: '请设置密码', min: 6 }]">
              <a-input-password v-model:value="createForm.password" placeholder="至少 6 位" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="角色" name="role" :rules="[{ required: true, message: '请选择角色' }]">
              <a-select v-model:value="createForm.role">
                <a-select-option v-for="item in roleOptions" :key="item.value" :value="item.value">
                  {{ item.label }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="状态">
              <a-switch v-model:checked="createForm.isActive" checked-children="激活" un-checked-children="停用" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-divider>个人档案</a-divider>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item :rules="[{ required: true, message: '请输入邮箱' }]" label="邮箱" :name="['profile', 'email']">
              <a-input v-model:value="createForm.profile.email" placeholder="请输入邮箱" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :rules="[{ required: true, message: '请输入手机号' }]" label="手机号" :name="['profile', 'phone']">
              <a-input v-model:value="createForm.profile.phone" placeholder="请输入手机号" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item :rules="[{ required: true, message: '请输入真实姓名' }]" label="真实姓名" :name="['profile', 'realName']">
              <a-input v-model:value="createForm.profile.realName" placeholder="请输入真实姓名" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="性别" :name="['profile', 'gender']">
              <a-select v-model:value="createForm.profile.gender">
                <a-select-option v-for="item in genderOptions" :key="item.value" :value="item.value">
                  {{ item.label }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="头像地址" :name="['profile', 'avatarUrl']">
          <a-input v-model:value="createForm.profile.avatarUrl" placeholder="可选，填写头像 URL" />
        </a-form-item>

        <a-divider>简历信息（可选）</a-divider>
        <a-form-item label="同步简历信息">
          <a-switch v-model:checked="includeResume" checked-children="是" un-checked-children="否" />
        </a-form-item>
        <template v-if="includeResume">
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="姓名">
                <a-input v-model:value="resumeForm.name" placeholder="简历姓名" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="出生日期">
                <a-input v-model:value="resumeForm.birth" placeholder="例如：1994-08" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="所在省份">
                <a-input v-model:value="resumeForm.locationProvince" placeholder="省份" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="所在城市">
                <a-input v-model:value="resumeForm.locationCity" placeholder="城市" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="最高学历">
                <a-input v-model:value="resumeForm.educationLevel" placeholder="本科 / 硕士" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="毕业院校">
                <a-input v-model:value="resumeForm.educationSchool" placeholder="学校" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="专业">
                <a-input v-model:value="resumeForm.educationMajor" placeholder="专业" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-row :gutter="8">
                <a-col :span="12">
                  <a-form-item label="毕业年份">
                    <a-input-number v-model:value="resumeForm.graduationYear" :min="1970" :max="2100" style="width: 100%" />
                  </a-form-item>
                </a-col>
                <a-col :span="12">
                  <a-form-item label="毕业月份">
                    <a-input-number v-model:value="resumeForm.graduationMonth" :min="1" :max="12" style="width: 100%" />
                  </a-form-item>
                </a-col>
              </a-row>
            </a-col>
          </a-row>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="联系电话">
                <a-input v-model:value="resumeForm.contactPhone" placeholder="简历联系电话" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="联系邮箱">
                <a-input v-model:value="resumeForm.contactEmail" placeholder="简历邮箱" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-form-item label="个人主页">
            <a-input v-model:value="resumeForm.contactHomepage" placeholder="可选" />
          </a-form-item>
          <a-form-item label="求职意向">
            <a-input v-model:value="resumeForm.jobIntention" placeholder="想要从事的职位" />
          </a-form-item>
          <a-form-item label="技能标签">
            <a-input
              v-model:value="resumeForm.skills"
              placeholder="使用逗号分隔多个技能，如 Vue, Node.js"
            />
          </a-form-item>
          <a-form-item label="自我评价">
            <a-textarea v-model:value="resumeForm.selfEvaluation" :rows="3" />
          </a-form-item>
        </template>

        <a-form-item>
          <a-space style="display: flex; justify-content: flex-end">
            <a-button @click="closeCreateModal">取消</a-button>
            <a-button type="primary" html-type="submit" :loading="createSubmitting">创建用户</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-modal>

    <a-drawer v-model:open="detailVisible" placement="right" width="420" :title="selectedUser?.username">
      <template v-if="selectedUser">
        <a-spin :spinning="detailLoading">
          <a-descriptions title="账户信息" :column="1" size="small" bordered>
            <a-descriptions-item label="用户ID">{{ selectedUser.id }}</a-descriptions-item>
            <a-descriptions-item label="用户名">{{ selectedUser.username }}</a-descriptions-item>
            <a-descriptions-item label="角色">{{ normalizeRole(selectedUser.role) }}</a-descriptions-item>
            <a-descriptions-item label="状态">
              {{ selectedUser.isActive ? '已激活' : '未激活' }}
            </a-descriptions-item>
            <a-descriptions-item label="创建时间">{{ selectedUser.createdAt }}</a-descriptions-item>
            <a-descriptions-item label="更新时间">{{ selectedUser.updatedAt }}</a-descriptions-item>
          </a-descriptions>

          <a-descriptions title="个人档案" :column="1" size="small" bordered style="margin-top: 16px">
            <a-descriptions-item label="姓名">{{ selectedUser.profile?.realName }}</a-descriptions-item>
            <a-descriptions-item label="邮箱">{{ selectedUser.profile?.email }}</a-descriptions-item>
            <a-descriptions-item label="电话">{{ selectedUser.profile?.phone }}</a-descriptions-item>
            <a-descriptions-item label="性别">{{ selectedUser.profile?.gender }}</a-descriptions-item>
          </a-descriptions>

          <a-empty v-if="!selectedUser.resume" description="暂无简历信息" style="margin-top: 16px" />
          <template v-else>
            <a-descriptions title="简历信息" :column="1" size="small" bordered style="margin-top: 16px">
              <a-descriptions-item label="姓名">{{ selectedUser.resume?.name }}</a-descriptions-item>
              <a-descriptions-item label="求职意向">{{ selectedUser.resume?.jobIntention || '未填写' }}</a-descriptions-item>
              <a-descriptions-item label="所在地区">
              {{
                selectedUser.resume?.location
                  ? `${selectedUser.resume?.location?.province || ''} ${selectedUser.resume?.location?.city || ''}`
                  : '未填写'
              }}
              </a-descriptions-item>
              <a-descriptions-item label="教育经历">
                <div v-if="selectedUser.resume?.education && selectedUser.resume.education.length">
                  <div v-for="(edu, index) in selectedUser.resume.education" :key="index" class="education-info">
                    <div>
                      {{ edu.school || '未填写学校' }}
                      <template v-if="edu.major">
                        · {{ edu.major }}
                      </template>
                    </div>
                    <div>{{ edu.level || '未填写学历' }}</div>
                  </div>
                </div>
                <span v-else>未填写</span>
              </a-descriptions-item>
              <a-descriptions-item label="技能">
                <a-space wrap>
                  <a-tag v-for="skill in selectedUser.resume?.skills || []" :key="skill">{{ skill }}</a-tag>
                  <span v-if="!selectedUser.resume?.skills || selectedUser.resume?.skills.length === 0">暂无</span>
                </a-space>
              </a-descriptions-item>
              <a-descriptions-item label="联系方式">
                <div>{{ selectedUser.resume?.contact?.phone || '暂无电话' }}</div>
                <div>{{ selectedUser.resume?.contact?.email || '暂无邮箱' }}</div>
              </a-descriptions-item>
              <a-descriptions-item label="自我评价">{{ selectedUser.resume?.selfEvaluation || '未填写' }}</a-descriptions-item>
            </a-descriptions>
          </template>
        </a-spin>
      </template>
      <a-empty v-else description="请选择用户" />
    </a-drawer>
  </a-space>
</template>

<style scoped>
.user-page {
  width: 100%;
}

.table-pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 16px;
}

:deep(.ant-pagination) {
  display: flex;
  align-items: center;
  gap: 8px;
}

:deep(.ant-pagination .ant-pagination-item) {
  border-radius: 8px;
  min-width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid #d9d9d9;
  background: #ffffff;
  color: #333333;
}

:deep(.ant-pagination .ant-pagination-item:hover) {
  transform: translateY(-2px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
  border-color: #1976d2;
  background: rgba(25, 118, 210, 0.04);
}

:deep(.ant-pagination .ant-pagination-item-active) {
  background: linear-gradient(135deg, #1976d2 0%, #42a5f5 100%);
  border-color: transparent;
  color: white;
  box-shadow: 0 2px 8px rgba(25, 118, 210, 0.3);
}

:deep(.ant-pagination .ant-pagination-item-active:hover) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(25, 118, 210, 0.4);
}

:deep(.ant-pagination .ant-pagination-item-disabled) {
  opacity: 0.5;
}

:deep(.ant-pagination .ant-pagination-item-disabled:hover) {
  transform: none;
  box-shadow: none;
}

:deep(.ant-pagination-options) {
  display: flex;
  align-items: center;
  gap: 8px;
}

:deep(.ant-pagination-total-text) {
  margin-right: 0;
  color: #8c8c8c;
  font-size: 14px;
}

@media (max-width: 768px) {
  .user-page {
    padding: 12px 0;
  }

  :deep(.ant-card-head) {
    padding: 12px 16px;
  }

  :deep(.ant-card-body) {
    padding: 16px;
  }

  .table-pagination {
    margin-top: 12px;
  }
}

@media (max-width: 480px) {
  :deep(.ant-card-head) {
    padding: 10px 12px;
  }

  :deep(.ant-card-body) {
    padding: 12px;
  }

  .table-pagination {
    margin-top: 10px;
  }
}
