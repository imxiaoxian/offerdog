<script lang="ts" setup>
import type { FormInstance } from 'ant-design-vue'
import { message } from 'ant-design-vue'
import { computed, onMounted, reactive, ref } from 'vue'

import {
  type CreateQuizBankPayload,
  type PageQuizBanksQuery,
  type QuizBankDetail,
  type QuizBankSummary,
  quizBankApi,
} from '@/services/categories'

const listLoading = ref(false)
const submitting = ref(false)
const quizBanks = ref<QuizBankSummary[]>([])
const activePanels = ref<(string | number)[]>([])

const queryForm = reactive<PageQuizBanksQuery>({
  name: '',
})

const createModalVisible = ref(false)
const formMode = ref<'create' | 'edit'>('create')
const formRef = ref<FormInstance>()
const editingId = ref<number | null>(null)
const formState = reactive({
  name: '',
  position: '',
  tags: [] as string[],
  description: '',
  parentId: null as number | null,
  level: 1 as number | null,
})

const detailVisible = ref(false)
const detailLoading = ref(false)
const selectedBank = ref<QuizBankDetail | null>(null)

const tagPresets = ['热门', '高频', '基础', '进阶', '场景题', '案例题', '行为面试']
const tagColors = ['blue', 'green', 'volcano', 'purple', 'magenta', 'gold', 'cyan']

const entityLabel = computed(() => ((formState.level ?? 1) === 1 ? '行业' : '岗位'))
const modalTitle = computed(() => {
  const action = formMode.value === 'create' ? '新建' : '编辑'
  return `${action}${entityLabel.value}`
})

const getTagColor = (tag: string) => {
  if (!tag) return 'default'
  const index = Math.abs(tag.charCodeAt(0) + tag.length) % tagColors.length
  return tagColors[index]
}

const fetchQuizBanks = async () => {
  listLoading.value = true
  try {
    const response = await quizBankApi.getQuizBanks({})
    quizBanks.value = response.data ?? []
    const firstPanel = quizBanks.value.find((item) => (item.level ?? 1) === 1 || item.parentId == null)
    if (firstPanel) {
      activePanels.value = [firstPanel.id]
    }
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : '获取岗位列表失败'
    message.error(errorMessage)
  } finally {
    listLoading.value = false
  }
}

const handleSearch = () => {
  // 仅依赖本地过滤，保持响应式
}

const handleReset = () => {
  queryForm.name = ''
}

const resetForm = () => {
  formState.name = ''
  formState.position = ''
  formState.tags = []
  formState.description = ''
  formState.parentId = null
  formState.level = 1
  formRef.value?.clearValidate()
}

const openCreateModal = (parentId?: number | null, level?: number | null) => {
  formMode.value = 'create'
  editingId.value = null
  resetForm()
  formState.parentId = parentId ?? null
  formState.level = level ?? 1
  createModalVisible.value = true
}

const openCreateIndustry = () => {
  openCreateModal(null, 1)
}

const openEditModal = async (record: QuizBankSummary) => {
  formMode.value = 'edit'
  editingId.value = record.id
  createModalVisible.value = true
  detailLoading.value = true
  try {
    const response = await quizBankApi.getQuizBankDetail(record.id)
    if (response.data) {
      const detail = response.data
      formState.name = detail.name
      formState.position = detail.position ?? ''
      formState.tags = detail.tags?.slice() ?? []
      formState.description = detail.description ?? ''
      formState.parentId = detail.parentId ?? null
      formState.level = detail.level ?? 1
    }
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : '加载岗位详情失败'
    message.error(errorMessage)
  } finally {
    detailLoading.value = false
  }
}

const closeCreateModal = () => {
  createModalVisible.value = false
}

const buildPayload = (): CreateQuizBankPayload => ({
  name: formState.name,
  position: formState.position || undefined,
  tags: formState.tags?.filter(Boolean),
  description: formState.description || undefined,
  parentId: formState.parentId ?? undefined,
  level: formState.level ?? undefined,
})

const handleSubmit = async () => {
  submitting.value = true
  try {
    const payload = buildPayload()
    if (!payload.name) {
      message.warning(entityLabel.value === '行业' ? '请填写行业名称' : '请填写岗位名称')
      return
    }
    if (formMode.value === 'create') {
      await quizBankApi.createQuizBank(payload)
      message.success(`${entityLabel.value}创建成功`)
    } else if (editingId.value) {
      await quizBankApi.updateQuizBank(editingId.value, payload)
      message.success(`${entityLabel.value}更新成功`)
    }
    closeCreateModal()
    fetchQuizBanks()
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : '提交失败'
    message.error(errorMessage)
  } finally {
    submitting.value = false
  }
}

const showDetail = async (record: QuizBankSummary) => {
  detailVisible.value = true
  detailLoading.value = true
  selectedBank.value = null
  try {
    const response = await quizBankApi.getQuizBankDetail(record.id)
    if (response.data) {
      selectedBank.value = response.data
    }
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : '获取岗位详情失败'
    message.error(errorMessage)
  } finally {
    detailLoading.value = false
  }
}

const handleDelete = async (record: QuizBankSummary) => {
  try {
    await quizBankApi.deleteQuizBank(record.id)
    message.success('岗位已删除')
    if (selectedBank.value?.id === record.id) {
      detailVisible.value = false
      selectedBank.value = null
    }
    fetchQuizBanks()
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : '删除岗位失败'
    message.error(errorMessage)
  }
}

onMounted(() => {
  fetchQuizBanks()
})

const openEditIndustry = (industry: QuizBankSummary) => {
  openEditModal(industry)
}

const handleDeleteIndustry = async (industry: QuizBankSummary) => {
  try {
    await quizBankApi.deleteQuizBank(industry.id)
    message.success('行业及岗位已删除')
    if (selectedBank.value?.id === industry.id) {
      detailVisible.value = false
      selectedBank.value = null
    }
    fetchQuizBanks()
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : '删除行业失败'
    message.error(errorMessage)
  }
}

interface IndustryPanel extends QuizBankSummary {
  children: QuizBankSummary[]
}

const industryPanels = computed<IndustryPanel[]>(() => {
  const keyword = (queryForm.name || '').trim().toLowerCase()
  const list = quizBanks.value
  const parents = list.filter((item) => (item.level ?? 1) === 1 || item.parentId == null)
  return parents
    .map((industry) => {
      const children = list.filter((child) => child.parentId === industry.id)
      return { ...industry, children }
    })
    .filter((industry) => !keyword || industry.name?.toLowerCase().includes(keyword))
})
</script>

<template>
  <a-space direction="vertical" size="large" class="quiz-page">
    <a-card title="岗位筛选">
      <a-form layout="inline" :model="queryForm" @submit.prevent>
        <a-form-item label="行业名称">
          <a-input v-model:value="queryForm.name" placeholder="请输入行业名称" allow-clear />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="handleSearch">查询</a-button>
            <a-button @click="handleReset">重置</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <a-card title="行业岗位" class="industry-card">
      <template #extra>
        <a-button type="primary" @click="openCreateIndustry">新增行业</a-button>
      </template>
      <a-spin :spinning="listLoading">
        <a-empty v-if="!industryPanels.length" description="暂无行业数据" />
        <a-collapse v-else v-model:activeKey="activePanels" accordion>
          <a-collapse-panel v-for="industry in industryPanels" :key="industry.id">
            <template #header>
              <div class="panel-header">
                <span class="panel-header__title">{{ industry.name }}</span>
                <a-space size="small" @click.stop>
                  <a-button type="link" @click.stop="showDetail(industry)">查看</a-button>
                  <a-button type="link" @click.stop="openEditIndustry(industry)">编辑</a-button>
                  <a-popconfirm
                    title="删除该行业将同时删除其岗位，确认删除？"
                    ok-text="删除"
                    ok-type="danger"
                    cancel-text="取消"
                    @confirm.stop="() => handleDeleteIndustry(industry)"
                  >
                    <a-button type="link" danger>删除</a-button>
                  </a-popconfirm>
                </a-space>
              </div>
            </template>
            <div class="industry-panel__toolbar">
              <span class="industry-panel__meta">共 {{ industry.children.length }} 个岗位</span>
              <a-button
                size="small"
                type="primary"
                @click="() => openCreateModal(industry.id, (industry.level ?? 1) + 1)"
              >
                新增岗位
              </a-button>
            </div>
            <a-empty v-if="industry.children.length === 0" description="该行业暂无岗位" />
            <a-list
              v-else
              :data-source="industry.children"
              :split="false"
              class="industry-panel__list"
            >
              <template #renderItem="{ item }">
                <a-list-item class="quiz-item">
                  <div class="quiz-item__content">
                    <div class="quiz-item__title">{{ item.name }}</div>
                    <div class="quiz-item__desc">{{ item.description || '暂无简介' }}</div>
                    <div class="quiz-item__meta">
                      <span>相关岗位：{{ item.position || '暂无' }}</span>
                      <span>更新时间：{{ item.updatedAt || '--' }}</span>
                    </div>
                    <div class="quiz-item__tags">
                      <span>标签：</span>
                      <a-space wrap>
                        <a-tag v-for="tag in item.tags" :key="tag" :color="getTagColor(tag)">
                          {{ tag }}
                        </a-tag>
                        <span v-if="!item.tags || item.tags.length === 0">暂无</span>
                      </a-space>
                    </div>
                  </div>
                  <div class="quiz-item__actions">
                    <a-button type="link" @click="showDetail(item)">查看</a-button>
                    <a-button type="link" @click="openEditModal(item)">编辑</a-button>
                    <a-popconfirm
                      title="确认删除该岗位？"
                      ok-text="删除"
                      ok-type="danger"
                      cancel-text="取消"
                      @confirm="() => handleDelete(item)"
                    >
                      <a-button type="link" danger>删除</a-button>
                    </a-popconfirm>
                  </div>
                </a-list-item>
              </template>
            </a-list>
          </a-collapse-panel>
        </a-collapse>
      </a-spin>
    </a-card>

    <a-modal
      v-model:open="createModalVisible"
      :title="modalTitle"
      width="640px"
      destroy-on-close
      :confirm-loading="submitting"
      @ok="handleSubmit"
      @cancel="closeCreateModal"
    >
      <a-form layout="vertical" :model="formState" ref="formRef">
        <a-form-item label="行业名称" name="name" :rules="[{ required: true, message: '请输入行业名称' }]">
          <a-input v-model:value="formState.name" placeholder="请输入行业名称" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="父级分类ID" name="parentId">
              <a-input-number
                v-model:value="formState.parentId"
                :min="0"
                style="width: 100%"
                placeholder="顶级可留空或填 0"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="层级" name="level" :rules="[{ required: true, message: '请输入层级' }]">
              <a-input-number v-model:value="formState.level" :min="1" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="相关岗位" name="position">
          <a-input v-model:value="formState.position" placeholder="请输入相关岗位，如：前端工程师" />
        </a-form-item>
        <a-form-item label="标签" name="tags">
          <a-select v-model:value="formState.tags" mode="tags" placeholder="输入或选择标签" allow-clear>
            <a-select-option v-for="tag in tagPresets" :key="tag" :value="tag">
              {{ tag }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="岗位简介" name="description">
          <a-textarea v-model:value="formState.description" :rows="4" placeholder="简要说明岗位内容与适用场景" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-drawer v-model:open="detailVisible" title="岗位详情" placement="right" width="420">
      <a-spin :spinning="detailLoading">
        <template v-if="selectedBank">
          <a-descriptions :column="1" bordered size="small">
            <a-descriptions-item label="岗位ID">{{ selectedBank.id }}</a-descriptions-item>
            <a-descriptions-item label="行业名称">{{ selectedBank.name }}</a-descriptions-item>
            <a-descriptions-item label="相关岗位">{{ selectedBank.position || '暂无' }}</a-descriptions-item>
            <a-descriptions-item label="父级ID">{{ selectedBank.parentId ?? '-' }}</a-descriptions-item>
            <a-descriptions-item label="层级">{{ selectedBank.level ?? '--' }}</a-descriptions-item>
            <a-descriptions-item label="标签">
              <a-space wrap>
                <a-tag v-for="tag in selectedBank.tags" :key="tag" :color="getTagColor(tag)">
                  {{ tag }}
                </a-tag>
                <span v-if="!selectedBank.tags || selectedBank.tags.length === 0">暂无</span>
              </a-space>
            </a-descriptions-item>
            <a-descriptions-item v-if="selectedBank.path?.length" label="路径">
              {{ selectedBank.path.join(' / ') }}
            </a-descriptions-item>
            <a-descriptions-item label="创建时间">{{ selectedBank.createdAt }}</a-descriptions-item>
          </a-descriptions>

          <a-descriptions title="简介" :column="1" bordered size="small" style="margin-top: 16px">
            <a-descriptions-item label="内容">
              {{ selectedBank.description || '暂无简介' }}
            </a-descriptions-item>
          </a-descriptions>
        </template>
        <a-empty v-else description="请选择岗位" />
      </a-spin>
    </a-drawer>
  </a-space>
</template>

<style scoped>
.quiz-page {
  width: 100%;
}

.industry-card {
  width: 100%;
  border-radius: 12px;
  overflow: hidden;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.panel-header__title {
  font-weight: 600;
  font-size: 16px;
}

.industry-panel__toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding: 8px 12px;
  background: #f7f9fc;
  border-radius: 8px;
}

.industry-panel__meta {
  color: rgba(0, 0, 0, 0.45);
}

.industry-panel__list {
  width: 100%;
  margin-top: 12px;
}

.quiz-item {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 16px;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  margin-bottom: 12px;
  transition: box-shadow 0.2s ease, transform 0.2s ease;
}

.quiz-item:hover {
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
  background: #fff;
}

.quiz-item__content {
  flex: 1;
  padding-right: 16px;
}

.quiz-item__title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 6px;
  color: #1f1f1f;
}

.quiz-item__desc {
  color: rgba(0, 0, 0, 0.65);
  margin-bottom: 10px;
  line-height: 1.6;
}

.quiz-item__meta {
  display: flex;
  gap: 16px;
  color: rgba(0, 0, 0, 0.45);
  margin-bottom: 8px;
  font-size: 13px;
}

.quiz-item__tags {
  display: flex;
  gap: 8px;
  align-items: center;
  color: rgba(0, 0, 0, 0.65);
}

.quiz-item__actions {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
  min-width: 120px;
}

.quiz-item__actions a-button {
  padding: 0;
}
</style>


