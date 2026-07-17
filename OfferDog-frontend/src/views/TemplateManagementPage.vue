<script lang="ts" setup>
import type { FormInstance } from 'ant-design-vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { computed, onMounted, reactive, ref } from 'vue'

import {
  formatUuid,
  templateAdminApi,
  type CreateTemplatePayload,
  type InterviewTemplateDetail,
  type InterviewTemplateSummary,
  type TemplateDifficulty,
  type UpdateTemplatePayload,
} from '@/services/templateAdmin'

const listLoading = ref(false)
const templates = ref<InterviewTemplateSummary[]>([])
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showTotal: (total: number) => `共 ${total} 条`,
})

const queryForm = reactive({
  keyword: '',
  category: '',
  difficultyLevel: undefined as TemplateDifficulty | undefined,
  isActive: undefined as boolean | undefined,
})

const difficultyOptions = [
  { label: '初级', value: 'junior' },
  { label: '中级', value: 'mid' },
  { label: '高级', value: 'senior' },
]

const difficultyTextMap: Record<string, string> = {
  junior: '初级',
  mid: '中级',
  senior: '高级',
}

const getDifficultyText = (value?: string) => difficultyTextMap[value?.toLowerCase?.() ?? ''] || value || '-'

const formatDateTime = (value?: string) => (value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-')

const rowKeyOfTemplate = (record: InterviewTemplateSummary) => formatUuid(record.templateId) || record.templateName

const fetchTemplates = async () => {
  listLoading.value = true
  try {
    const response = await templateAdminApi.pageTemplates({
      keyword: queryForm.keyword || undefined,
      category: queryForm.category || undefined,
      difficultyLevel: queryForm.difficultyLevel || undefined,
      isActive: queryForm.isActive,
      pageNo: pagination.current,
      pageSize: pagination.pageSize,
    })
    templates.value = response.data ?? []
    pagination.total = response.totalCount ?? templates.value.length
    pagination.current = response.pageNo ?? pagination.current
    pagination.pageSize = response.pageSize ?? pagination.pageSize
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : '获取模板列表失败'
    message.error(errorMessage)
  } finally {
    listLoading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  fetchTemplates()
}

const handleReset = () => {
  queryForm.keyword = ''
  queryForm.category = ''
  queryForm.difficultyLevel = undefined
  queryForm.isActive = undefined
  pagination.current = 1
  fetchTemplates()
}

const handleTableChange = (pager: { current?: number; pageSize?: number }) => {
  pagination.current = pager.current ?? 1
  pagination.pageSize = pager.pageSize ?? pagination.pageSize
  fetchTemplates()
}

const templateModalVisible = ref(false)
const templateFormMode = ref<'create' | 'edit'>('create')
const templateModalTitle = computed(() => (templateFormMode.value === 'create' ? '新建面试模板' : '编辑面试模板'))
const templateSubmitting = ref(false)
const templateFormRef = ref<FormInstance>()
const editingTemplateId = ref('')

const templateForm = reactive({
  templateName: '',
  description: '',
  category: '',
  difficultyLevel: 'junior' as TemplateDifficulty,
  estimatedDurationMinutes: 30 as number | undefined,
  systemPrompt: '',
  defaultConfigText: '',
  isActive: true,
  version: 1 as number | undefined,
  planName: '',
  planStructureText: '',
  planIsActive: true,
})

const safeStringify = (value: unknown) => {
  if (value === null || value === undefined) return ''
  try {
    return JSON.stringify(value, null, 2)
  } catch (error) {
    console.warn('字符串化失败', error)
    return String(value)
  }
}

const parseJsonField = (text: string, fieldLabel: string) => {
  if (!text || !text.trim()) return undefined
  try {
    return JSON.parse(text)
  } catch (error) {
    message.error(`${fieldLabel}需要是合法的 JSON`)
    throw error
  }
}

const resetTemplateForm = () => {
  templateForm.templateName = ''
  templateForm.description = ''
  templateForm.category = ''
  templateForm.difficultyLevel = 'junior'
  templateForm.estimatedDurationMinutes = 30
  templateForm.systemPrompt = ''
  templateForm.defaultConfigText = ''
  templateForm.isActive = true
  templateForm.version = 1
  templateForm.planName = ''
  templateForm.planStructureText = ''
  templateForm.planIsActive = true
  templateFormRef.value?.clearValidate()
}

const openCreateTemplate = () => {
  resetTemplateForm()
  templateFormMode.value = 'create'
  editingTemplateId.value = ''
  templateModalVisible.value = true
}

const openEditTemplate = async (record: InterviewTemplateSummary) => {
  const id = formatUuid(record.templateId)
  if (!id) {
    message.warning('未找到模板ID，无法编辑')
    return
  }
  templateFormMode.value = 'edit'
  editingTemplateId.value = id
  templateModalVisible.value = true
  try {
    const response = await templateAdminApi.getTemplate(id)
    const detail = response.data
    if (detail) {
      templateForm.templateName = detail.templateName
      templateForm.description = detail.description ?? ''
      templateForm.category = detail.category ?? ''
      templateForm.difficultyLevel = detail.difficultyLevel ?? 'junior'
      templateForm.estimatedDurationMinutes = detail.estimatedDurationMinutes
      templateForm.systemPrompt = detail.systemPrompt ?? ''
      templateForm.defaultConfigText = safeStringify(detail.defaultConfig)
      templateForm.isActive = detail.isActive ?? true
      templateForm.version = detail.version
      templateForm.planName = detail.planTemplate?.planName ?? ''
      templateForm.planStructureText = safeStringify(detail.planTemplate?.planStructure ?? detail.planTemplate)
      templateForm.planIsActive = detail.planTemplate?.isActive ?? true
    }
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : '加载模板详情失败'
    message.error(errorMessage)
  }
}

const buildCreateTemplatePayload = (): CreateTemplatePayload => {
  if (!templateForm.templateName) {
    throw new Error('请填写模板名称')
  }
  if (!templateForm.category) {
    throw new Error('请填写分类')
  }
  if (!templateForm.difficultyLevel) {
    throw new Error('请选择难度')
  }
  if (!templateForm.systemPrompt) {
    throw new Error('请填写系统提示词')
  }
  if (!templateForm.planName) {
    throw new Error('请填写计划名称')
  }
  if (!templateForm.planStructureText) {
    throw new Error('请填写计划结构')
  }

  const defaultConfig = parseJsonField(templateForm.defaultConfigText, '默认配置')
  const planStructure = parseJsonField(templateForm.planStructureText, '计划结构')

  const payload: CreateTemplatePayload = {
    templateName: templateForm.templateName,
    description: templateForm.description || undefined,
    category: templateForm.category,
    difficultyLevel: templateForm.difficultyLevel,
    estimatedDurationMinutes: templateForm.estimatedDurationMinutes,
    systemPrompt: templateForm.systemPrompt,
    defaultConfig,
    isActive: templateForm.isActive,
    version: templateForm.version,
    planName: templateForm.planName,
    planStructure: planStructure ?? [],
    planIsActive: templateForm.planIsActive,
  }

  return payload
}

const buildUpdateTemplatePayload = (): UpdateTemplatePayload => {
  if (!templateForm.templateName) {
    throw new Error('请填写模板名称')
  }
  if (!templateForm.category) {
    throw new Error('请填写分类')
  }
  if (!templateForm.difficultyLevel) {
    throw new Error('请选择难度')
  }
  if (!templateForm.systemPrompt) {
    throw new Error('请填写系统提示词')
  }

  const defaultConfig = parseJsonField(templateForm.defaultConfigText, '默认配置')
  const planStructure = templateForm.planStructureText
    ? parseJsonField(templateForm.planStructureText, '计划结构')
    : undefined

  const payload: UpdateTemplatePayload = {
    templateName: templateForm.templateName,
    description: templateForm.description || undefined,
    category: templateForm.category,
    difficultyLevel: templateForm.difficultyLevel,
    estimatedDurationMinutes: templateForm.estimatedDurationMinutes,
    systemPrompt: templateForm.systemPrompt,
    defaultConfig,
    isActive: templateForm.isActive,
    version: templateForm.version,
    planIsActive: templateForm.planIsActive,
  }

  if (templateForm.planName) {
    payload.planName = templateForm.planName
  }
  if (planStructure !== undefined) {
    payload.planStructure = planStructure
  }

  return payload
}

const handleTemplateSubmit = async () => {
  try {
    const payload = templateFormMode.value === 'create'
      ? buildCreateTemplatePayload()
      : buildUpdateTemplatePayload()
    templateSubmitting.value = true
    if (templateFormMode.value === 'create') {
      await templateAdminApi.createTemplate(payload as CreateTemplatePayload)
      message.success('模板创建成功')
    } else if (editingTemplateId.value) {
      await templateAdminApi.updateTemplate(editingTemplateId.value, payload as UpdateTemplatePayload)
      message.success('模板更新成功')
    }
    templateModalVisible.value = false
    fetchTemplates()
    if (currentTemplateId.value && editingTemplateId.value === currentTemplateId.value) {
      fetchTemplateDetail(currentTemplateId.value)
    }
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : '提交失败'
    message.error(errorMessage)
  } finally {
    templateSubmitting.value = false
  }
}

const templateStatusLoading = reactive<Record<string, boolean>>({})
const handleTemplateStatusChange = async (record: InterviewTemplateSummary, nextStatus: boolean) => {
  const id = formatUuid(record.templateId)
  if (!id) {
    message.warning('无法识别模板ID')
    return
  }
  templateStatusLoading[id] = true
  try {
    await templateAdminApi.updateTemplate(id, { isActive: nextStatus })
    record.isActive = nextStatus
    message.success('状态已更新')
    if (templateDetail.value && formatUuid(templateDetail.value.templateId) === id) {
      templateDetail.value.isActive = nextStatus
    }
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : '更新状态失败'
    message.error(errorMessage)
  } finally {
    templateStatusLoading[id] = false
  }
}

const handleDeleteTemplate = async (record: InterviewTemplateSummary) => {
  const id = formatUuid(record.templateId)
  if (!id) {
    message.warning('无法识别模板ID')
    return
  }
  try {
    await templateAdminApi.deleteTemplate(id)
    message.success('模板已删除')
    if (currentTemplateId.value === id) {
      detailVisible.value = false
      templateDetail.value = null
    }
    fetchTemplates()
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : '删除失败'
    message.error(errorMessage)
  }
}

const detailVisible = ref(false)
const detailLoading = ref(false)
const templateDetail = ref<InterviewTemplateDetail | null>(null)
const currentTemplateId = ref('')

const fetchTemplateDetail = async (templateId: string) => {
  detailLoading.value = true
  try {
    const response = await templateAdminApi.getTemplate(templateId)
    templateDetail.value = response.data ?? null
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : '获取模板详情失败'
    message.error(errorMessage)
  } finally {
    detailLoading.value = false
  }
}

const openDetail = (record: InterviewTemplateSummary) => {
  const id = formatUuid(record.templateId)
  if (!id) {
    message.warning('未找到模板ID，无法查看详情')
    return
  }
  currentTemplateId.value = id
  detailVisible.value = true
  fetchTemplateDetail(id)
}

const closeDetail = () => {
  detailVisible.value = false
  currentTemplateId.value = ''
  templateDetail.value = null
}

const tableColumns = [
  { title: '模板名称', dataIndex: 'templateName', key: 'templateName' },
  { title: '分类', dataIndex: 'category', key: 'category', width: 140 },
  { title: '难度', dataIndex: 'difficultyLevel', key: 'difficultyLevel', width: 100 },
  { title: '预计时长(分钟)', dataIndex: 'estimatedDurationMinutes', key: 'estimatedDurationMinutes', width: 140 },
  { title: '状态', dataIndex: 'isActive', key: 'isActive', width: 100 },
  { title: '版本', dataIndex: 'version', key: 'version', width: 80 },
  { title: '更新时间', dataIndex: 'updatedAt', key: 'updatedAt', width: 180 },
  { title: '操作', key: 'actions', width: 240 },
]

onMounted(() => {
  fetchTemplates()
})
</script>

<template>
  <div class="template-management">
    <a-space direction="vertical" size="large" style="width: 100%">
      <a-card title="模板筛选">
        <a-form :model="queryForm" layout="inline">
          <a-form-item label="关键词">
            <a-input
              v-model:value="queryForm.keyword"
              placeholder="模板名称/描述"
              allow-clear
              style="width: 200px"
              @press-enter="handleSearch"
            />
          </a-form-item>
          <a-form-item label="分类">
            <a-input
              v-model:value="queryForm.category"
              placeholder="请输入分类"
              allow-clear
              style="width: 200px"
              @press-enter="handleSearch"
            />
          </a-form-item>
          <a-form-item label="难度">
            <a-select
              v-model:value="queryForm.difficultyLevel"
              :options="difficultyOptions"
              allow-clear
              placeholder="全部"
              style="width: 180px"
            />
          </a-form-item>
          <a-form-item label="状态">
            <a-select
              v-model:value="queryForm.isActive"
              allow-clear
              placeholder="全部"
              style="width: 160px"
            >
              <a-select-option :value="true">启用</a-select-option>
              <a-select-option :value="false">停用</a-select-option>
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

      <a-card>
        <template #title>模板列表</template>
        <template #extra>
          <a-space>
            <a-button :loading="listLoading" @click="fetchTemplates">刷新</a-button>
            <a-button type="primary" @click="openCreateTemplate">新建模板</a-button>
          </a-space>
        </template>
        <a-table
          :data-source="templates"
          :columns="tableColumns"
          :loading="listLoading"
          :row-key="rowKeyOfTemplate"
          bordered
          :pagination="{
            current: pagination.current,
            pageSize: pagination.pageSize,
            total: pagination.total,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: pagination.showTotal,
          }"
          @change="handleTableChange"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'difficultyLevel'">
              <a-tag color="blue">{{ getDifficultyText(record.difficultyLevel) }}</a-tag>
            </template>
            <template v-else-if="column.key === 'isActive'">
              <a-switch
                :checked="record.isActive"
                size="small"
                :loading="templateStatusLoading[formatUuid(record.templateId)]"
                @change="(checked: boolean) => handleTemplateStatusChange(record, checked)"
              />
            </template>
            <template v-else-if="column.key === 'updatedAt'">
              {{ formatDateTime(record.updatedAt) }}
            </template>
            <template v-else-if="column.key === 'actions'">
              <a-space>
                <a-button type="link" size="small" @click="openDetail(record)">详情</a-button>
                <a-button type="link" size="small" @click="openEditTemplate(record)">编辑</a-button>
                <a-popconfirm title="确定删除该模板吗？" @confirm="() => handleDeleteTemplate(record)">
                  <a-button type="link" size="small" danger>删除</a-button>
                </a-popconfirm>
              </a-space>
            </template>
            <template v-else-if="column.key === 'category'">
              {{ record.category || '-' }}
            </template>
            <template v-else>
              {{ record[column.dataIndex] ?? '-' }}
            </template>
          </template>
        </a-table>
      </a-card>
    </a-space>

    <a-modal
      v-model:open="templateModalVisible"
      :title="templateModalTitle"
      :confirm-loading="templateSubmitting"
      width="900px"
      destroy-on-close
      @ok="handleTemplateSubmit"
    >
      <a-form
        ref="templateFormRef"
        :model="templateForm"
        :label-col="{ span: 5 }"
        :wrapper-col="{ span: 19 }"
      >
        <a-form-item label="模板名称" required>
          <a-input v-model:value="templateForm.templateName" placeholder="请输入模板名称" />
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea
            v-model:value="templateForm.description"
            :rows="2"
            placeholder="补充模板说明"
            allow-clear
          />
        </a-form-item>
        <a-form-item label="分类" required>
          <a-input v-model:value="templateForm.category" placeholder="如：后端/算法/前端" />
        </a-form-item>
        <a-form-item label="难度" required>
          <a-select v-model:value="templateForm.difficultyLevel" :options="difficultyOptions" placeholder="请选择难度" />
        </a-form-item>
        <a-form-item label="预计时长(分)">
          <a-input-number v-model:value="templateForm.estimatedDurationMinutes" :min="1" style="width: 100%" />
        </a-form-item>
        <a-form-item label="系统提示词" required>
          <a-textarea
            v-model:value="templateForm.systemPrompt"
            :rows="3"
            placeholder="用于生成面试问题的系统Prompt"
          />
        </a-form-item>
        <a-form-item label="默认配置(JSON)">
          <a-textarea
            v-model:value="templateForm.defaultConfigText"
            :rows="3"
            placeholder="可选，JSON格式"
          />
        </a-form-item>
        <a-form-item label="版本号">
          <a-input-number v-model:value="templateForm.version" :min="1" style="width: 100%" />
        </a-form-item>
        <a-form-item label="模板状态">
          <a-switch v-model:checked="templateForm.isActive" checked-children="启用" un-checked-children="停用" />
        </a-form-item>
        <a-divider orientation="left">计划模板</a-divider>
        <a-form-item label="计划名称" required>
          <a-input v-model:value="templateForm.planName" placeholder="计划名称" />
        </a-form-item>
        <a-form-item label="计划结构(JSON)" required>
          <a-textarea
            v-model:value="templateForm.planStructureText"
            :rows="5"
            placeholder='示例: {"questions":[],"totalQuestions":0}'
          />
          <div class="json-hint">以 JSON 形式配置面试流程，支持提问列表、过渡规则等结构。</div>
        </a-form-item>
        <a-form-item label="计划状态">
          <a-switch v-model:checked="templateForm.planIsActive" checked-children="启用" un-checked-children="停用" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-drawer
      v-model:open="detailVisible"
      title="模板详情"
      width="720"
      :destroy-on-close="true"
      @close="closeDetail"
    >
      <a-spin :spinning="detailLoading">
        <template v-if="templateDetail">
          <a-descriptions bordered :column="1" size="small">
            <a-descriptions-item label="模板名称">
              {{ templateDetail.templateName }}
            </a-descriptions-item>
            <a-descriptions-item label="分类">
              {{ templateDetail.category || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="难度">
              {{ getDifficultyText(templateDetail.difficultyLevel) }}
            </a-descriptions-item>
            <a-descriptions-item label="预计时长">
              {{ templateDetail.estimatedDurationMinutes ? `${templateDetail.estimatedDurationMinutes} 分钟` : '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="状态">
              <a-tag :color="templateDetail.isActive ? 'green' : 'red'">
                {{ templateDetail.isActive ? '启用' : '停用' }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="版本">
              {{ templateDetail.version ?? '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="更新时间">
              {{ formatDateTime(templateDetail.updatedAt) }}
            </a-descriptions-item>
            <a-descriptions-item label="描述">
              <a-typography-paragraph :ellipsis="{ rows: 2, expandable: true }">
                {{ templateDetail.description || '无' }}
              </a-typography-paragraph>
            </a-descriptions-item>
            <a-descriptions-item label="系统提示词">
              <a-typography-paragraph :ellipsis="{ rows: 3, expandable: true }">
                {{ templateDetail.systemPrompt || '无' }}
              </a-typography-paragraph>
            </a-descriptions-item>
            <a-descriptions-item label="默认配置">
              <pre class="json-viewer">{{ safeStringify(templateDetail.defaultConfig) || '未配置' }}</pre>
            </a-descriptions-item>
          </a-descriptions>

          <a-divider />

          <a-card title="计划模板">
            <a-descriptions bordered :column="1" size="small">
              <a-descriptions-item label="计划名称">
                {{ templateDetail.planTemplate?.planName || '未设置' }}
              </a-descriptions-item>
              <a-descriptions-item label="状态">
                <a-tag :color="templateDetail.planTemplate?.isActive ? 'green' : 'red'">
                  {{ templateDetail.planTemplate?.isActive ? '启用' : '停用' }}
                </a-tag>
              </a-descriptions-item>
              <a-descriptions-item label="使用次数">
                {{ templateDetail.planTemplate?.usageCount ?? '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="平均完成率">
                {{
                  templateDetail.planTemplate?.avgCompletionRate != null
                    ? `${(templateDetail.planTemplate.avgCompletionRate * 100).toFixed(1)}%`
                    : '-'
                }}
              </a-descriptions-item>
              <a-descriptions-item label="计划结构">
                <pre class="json-viewer">
{{ safeStringify(templateDetail.planTemplate?.planStructure) || '未配置' }}</pre>
              </a-descriptions-item>
            </a-descriptions>
          </a-card>
        </template>
        <a-empty v-else description="未选择模板" />
      </a-spin>
    </a-drawer>
  </div>
</template>

<style scoped>
.template-management :deep(.ant-card) {
  border-radius: 8px;
}

.json-hint {
  margin-top: 4px;
  color: #999;
  font-size: 12px;
}

.json-viewer {
  background: #f6f8fa;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  padding: 12px;
  max-height: 240px;
  overflow: auto;
  white-space: pre-wrap;
}

@media (max-width: 768px) {
  .template-management :deep(.ant-card) {
    border-radius: 8px;
  }

  :deep(.ant-card-head) {
    padding: 12px 16px;
  }

  :deep(.ant-card-body) {
    padding: 16px;
  }

  .json-hint {
    font-size: 11px;
  }

  .json-viewer {
    padding: 10px;
    max-height: 180px;
  }
}

@media (max-width: 480px) {
  :deep(.ant-card-head) {
    padding: 10px 12px;
  }

  :deep(.ant-card-body) {
    padding: 12px;
  }

  .json-viewer {
    padding: 8px;
    max-height: 150px;
    font-size: 12px;
  }

  .json-hint {
    font-size: 10px;
  }
}
