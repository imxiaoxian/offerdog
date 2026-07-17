<script lang="ts" setup>
import { UploadOutlined } from '@ant-design/icons-vue'
import type { FormInstance, UploadFile, UploadProps } from 'ant-design-vue'
import { message, Upload } from 'ant-design-vue'
import { computed, onMounted, reactive, ref } from 'vue'

import {
  type CreateQuestionBankPayload,
  type GetQuestionBanksParams,
  type QuestionBank,
  questionBankApi,
} from '@/services/questionBank'
import {
  type CreateQuestionPayload,
  type GetQuestionsParams,
  type Question,
  type QuestionDifficulty,
  type UpdateQuestionPayload,
  questionApi,
} from '@/services/question'
import { categoryApi, type CategoryTreeNode } from '@/services/categories'

const listLoading = ref(false)
const submitting = ref(false)
const questionBanks = ref<QuestionBank[]>([])
const activePanels = ref<(string | number)[]>([])
const categories = ref<CategoryTreeNode[]>([])

const queryForm = reactive<GetQuestionBanksParams>({
  name: '',
  categoryId: undefined,
  pageNo: 1,
  pageSize: 5,
})

// 分页信息
const pagination = reactive({
  current: 1,
  pageSize: 5,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`,
  pageSizeOptions: ['5','10', '20', '50', '100'],
})

// 题库表单
const bankModalVisible = ref(false)
const bankFormMode = ref<'create' | 'edit'>('create')
const bankFormRef = ref<FormInstance>()
const editingBankId = ref<number | null>(null)
const bankFormState = reactive({
  name: '',
  description: '',
  categoryId: undefined as number | undefined,
})

// 题目表单
const questionModalVisible = ref(false)
const questionFormMode = ref<'create' | 'edit'>('create')
const questionFormRef = ref<FormInstance>()
const editingQuestionId = ref<number | null>(null)
const currentBankId = ref<number | null>(null)
const questionFormState = reactive({
  bankId: 0,
  categoryId: undefined as number | undefined,
  content: '',
  answer: '',
  tips: '',
  difficulty: 'easy' as QuestionDifficulty,
  tags: [] as string[],
  remark: '',
})

// 批量上传
const batchModalVisible = ref(false)
const batchFormRef = ref<FormInstance>()
const batchSubmitting = ref(false)
const bankOptionsLoading = ref(false)
const bankOptions = ref<QuestionBank[]>([])
const batchFileList = ref<UploadFile[]>([])
const batchFormState = reactive({
  bankId: undefined as number | undefined,
  categoryId: undefined as number | undefined,
})
const allowedFileTypes = ['md', 'doc', 'docx', 'txt']
const allowedFileAccept = allowedFileTypes.map((item) => `.${item}`).join(',')

// 题目详情
const detailVisible = ref(false)
const detailLoading = ref(false)
const selectedQuestion = ref<Question | null>(null)

const tagColors = ['blue', 'green', 'volcano', 'purple', 'magenta', 'gold', 'cyan']
const difficultyOptions = [
  { label: '简单', value: 'EASY' },
  { label: '中等', value: 'MEDIUM' },
  { label: '困难', value: 'HARD' },
]

const bankModalTitle = computed(() => {
  const action = bankFormMode.value === 'create' ? '新建' : '编辑'
  return `${action}题库`
})

const questionModalTitle = computed(() => {
  const action = questionFormMode.value === 'create' ? '新建' : '编辑'
  return `${action}题目`
})

const getTagColor = (tag: string) => {
  if (!tag) return 'default'
  const index = Math.abs(tag.charCodeAt(0) + tag.length) % tagColors.length
  return tagColors[index]
}

const getDifficultyTag = (difficulty: string) => {
  const map: Record<string, { color: string; text: string }> = {
    easy: { color: 'green', text: '简单' },
    medium: { color: 'orange', text: '中等' },
    hard: { color: 'red', text: '困难' },
    EASY: { color: 'green', text: '简单' },
    MEDIUM: { color: 'orange', text: '中等' },
    HARD: { color: 'red', text: '困难' },
  }
  return map[difficulty] || { color: 'default', text: difficulty }
}

// 获取二级分类选项
const level2Categories = computed(() => {
  const result: CategoryTreeNode[] = []
  categories.value.forEach((cat: CategoryTreeNode) => {
    if (cat.children && cat.children.length > 0) {
      result.push(...cat.children)
    }
  })
  return result
})

const fetchCategories = async () => {
  try {
    const response = await categoryApi.getCategoryTree()
    categories.value = response.data ?? []
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : '获取分类失败'
    message.error(errorMessage)
  }
}

const fetchQuestionBanks = async () => {
  listLoading.value = true
  try {
    const response = await questionBankApi.getQuestionBanks(queryForm)
    // 直接使用response，因为http.get已经返回了ApiResponse结构
    if (response && response.data) {
      questionBanks.value = response.data
      // 更新分页信息
      pagination.total = response.totalCount || 0
      pagination.current = response.pageNo || 1
      pagination.pageSize = response.pageSize || 5

      // 为所有题库初始化查询表单
      questionBanks.value.forEach((bank) => {
        initQuestionQuery(bank.id)
      })

      const firstBank = questionBanks.value[0]
      if (firstBank && activePanels.value.length === 0) {
        activePanels.value = [firstBank.id]
        // 自动加载第一个题库的题目
        fetchQuestions(firstBank.id)
      }
    }
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : '获取题库列表失败'
    message.error(errorMessage)
  } finally {
    listLoading.value = false
  }
}

const fetchBankOptions = async () => {
  bankOptionsLoading.value = true
  try {
    const pageSize = 100
    const allBanks: QuestionBank[] = []
    let pageNo = 1
    let total = 0

    while (true) {
      const response = await questionBankApi.getQuestionBanks({ pageNo, pageSize })
      const list = response.data ?? []
      if (list.length === 0) break
      allBanks.push(...list)
      total = response.totalCount ?? allBanks.length

      const reachedEnd = allBanks.length >= total || list.length < pageSize
      if (reachedEnd) break

      pageNo += 1
    }

    bankOptions.value = allBanks
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : '获取题库列表失败'
    message.error(errorMessage)
  } finally {
    bankOptionsLoading.value = false
  }
}

const handleSearch = () => {
  // 重置到第一页
  queryForm.pageNo = 1
  pagination.current = 1
  fetchQuestionBanks()
}

const handleReset = () => {
  queryForm.name = ''
  queryForm.categoryId = undefined
  queryForm.pageNo = 1
  pagination.current = 1
  fetchQuestionBanks()
}

// 分页变化处理
const handlePageChange = (page: number, pageSize: number) => {
  queryForm.pageNo = page
  queryForm.pageSize = pageSize
  pagination.current = page
  pagination.pageSize = pageSize
  fetchQuestionBanks()
}

const resetBatchForm = () => {
  batchFormState.bankId = undefined
  batchFormState.categoryId = undefined
  batchFileList.value = []
  batchFormRef.value?.resetFields()
}

const openBatchModal = () => {
  if (!bankOptions.value.length) {
    fetchBankOptions()
  }
  resetBatchForm()
  batchModalVisible.value = true
}

const closeBatchModal = () => {
  batchModalVisible.value = false
  resetBatchForm()
}

const handleBatchBeforeUpload: UploadProps['beforeUpload'] = (file) => {
  const ext = file.name.split('.').pop()?.toLowerCase()
  if (!ext || !allowedFileTypes.includes(ext)) {
    message.error('仅支持上传 md、doc、docx、txt 文件')
    return Upload.LIST_IGNORE
  }
  batchFileList.value = [file as UploadFile]
  return false
}

const handleBatchRemove: UploadProps['onRemove'] = () => {
  batchFileList.value = []
}

const handleBatchSubmit = async () => {
  try {
    await batchFormRef.value?.validate()
  } catch {
    return
  }

  const rawFile = batchFileList.value[0]?.originFileObj as File | undefined
  if (!rawFile) {
    message.warning('请上传题目文件')
    return
  }

  const targetBankId = batchFormState.bankId!
  batchSubmitting.value = true
  try {
    await questionApi.batchAddQuestions({
      file: rawFile,
      bankId: targetBankId,
      categoryId: batchFormState.categoryId!,
    })
    message.success('批量上传成功')
    closeBatchModal()
    fetchQuestions(targetBankId)
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : '批量上传失败'
    message.error(errorMessage)
  } finally {
    batchSubmitting.value = false
  }
}

// 题库操作
const openCreateBankModal = () => {
  bankFormMode.value = 'create'
  editingBankId.value = null
  bankFormState.name = ''
  bankFormState.description = ''
  bankFormState.categoryId = undefined
  bankModalVisible.value = true
}

const openEditBankModal = async (bank: QuestionBank) => {
  bankFormMode.value = 'edit'
  editingBankId.value = bank.id
  detailLoading.value = true
  try {
    const response = await questionBankApi.getQuestionBankDetail(bank.id)
    if (response.data) {
      const detail = response.data
      bankFormState.name = detail.name
      bankFormState.description = detail.description ?? ''
      bankFormState.categoryId = detail.categoryId
    }
    bankModalVisible.value = true
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : '加载题库详情失败'
    message.error(errorMessage)
  } finally {
    detailLoading.value = false
  }
}

const closeBankModal = () => {
  bankModalVisible.value = false
  bankFormRef.value?.resetFields()
}

const handleBankSubmit = async () => {
  try {
    await bankFormRef.value?.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    const payload: CreateQuestionBankPayload = {
      name: bankFormState.name,
      description: bankFormState.description,
      categoryId: bankFormState.categoryId!,
    }

    if (bankFormMode.value === 'create') {
      await questionBankApi.createQuestionBank(payload)
      message.success('题库创建成功')
    } else if (editingBankId.value) {
      await questionBankApi.updateQuestionBank(editingBankId.value, payload)
      message.success('题库更新成功')
    }
    closeBankModal()
    fetchQuestionBanks()
    fetchBankOptions()
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : '提交失败'
    message.error(errorMessage)
  } finally {
    submitting.value = false
  }
}

const handleDeleteBank = async (bank: QuestionBank) => {
  try {
    await questionBankApi.deleteQuestionBank(bank.id)
    message.success('题库已删除')
    fetchQuestionBanks()
    fetchBankOptions()
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : '删除题库失败'
    message.error(errorMessage)
  }
}

// 题目操作
const questions = ref<Record<number, Question[]>>({})
const questionsLoading = ref<Record<number, boolean>>({})

// 每个题库的题目查询参数
const questionQueryForms = ref<Record<number, GetQuestionsParams>>({})

// 每个题库的题目分页信息
const questionPaginations = ref<
  Record<
    number,
    {
      current: number
      pageSize: number
      total: number
    }
  >
>({})

const getQuestionQueryForm = (bankId: number): GetQuestionsParams => {
  if (!questionQueryForms.value[bankId]) {
    questionQueryForms.value[bankId] = {
      bankId,
      pageNo: 1,
      pageSize: 5,
      keyword: '',
      difficulty: undefined,
    }
  }
  return questionQueryForms.value[bankId]
}

const getQuestionPagination = (
  bankId: number
): {
  current: number
  pageSize: number
  total: number
} => {
  if (!questionPaginations.value[bankId]) {
    questionPaginations.value[bankId] = {
      current: 1,
      pageSize: 5,
      total: 0,
    }
  }
  return questionPaginations.value[bankId]
}

// 初始化题库的查询表单和分页
const initQuestionQuery = (bankId: number) => {
  getQuestionQueryForm(bankId)
  getQuestionPagination(bankId)
}

const fetchQuestions = async (bankId: number) => {
  // 初始化查询表单和分页
  initQuestionQuery(bankId)

  questionsLoading.value[bankId] = true
  try {
    const params = getQuestionQueryForm(bankId)
    const response = await questionApi.getQuestions(params)
    // 直接使用response，因为http.get已经返回了ApiResponse结构
    if (response && response.data) {
      questions.value[bankId] = response.data
      // 更新分页信息
      const pagination = getQuestionPagination(bankId)
      pagination.total = response.totalCount || 0
      pagination.current = response.pageNo || 1
      pagination.pageSize = response.pageSize || 5
    }
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : '获取题目列表失败'
    message.error(errorMessage)
  } finally {
    questionsLoading.value[bankId] = false
  }
}

// 题目搜索
const handleQuestionSearch = (bankId: number) => {
  const form = getQuestionQueryForm(bankId)
  const pagination = getQuestionPagination(bankId)
  form.pageNo = 1
  pagination.current = 1
  fetchQuestions(bankId)
}

// 题目重置
const handleQuestionReset = (bankId: number) => {
  const form = getQuestionQueryForm(bankId)
  const pagination = getQuestionPagination(bankId)
  form.keyword = ''
  form.difficulty = undefined
  form.pageNo = 1
  pagination.current = 1
  fetchQuestions(bankId)
}

// 题目分页变化
const handleQuestionPageChange = (bankId: number, page: number, pageSize: number) => {
  const form = getQuestionQueryForm(bankId)
  const pagination = getQuestionPagination(bankId)
  form.pageNo = page
  form.pageSize = pageSize
  pagination.current = page
  pagination.pageSize = pageSize
  fetchQuestions(bankId)
}

const createQuestionPageChangeHandler = (bankId: number) => (page: number, pageSize: number) =>
  handleQuestionPageChange(bankId, page, pageSize)

const renderSimpleTotal = (total: number) => `共 ${total} 条`
const openCreateQuestionModal = (bankId: number, categoryId: number) => {
  questionFormMode.value = 'create'
  editingQuestionId.value = null
  currentBankId.value = bankId
  questionFormState.bankId = bankId
  questionFormState.categoryId = categoryId
  questionFormState.content = ''
  questionFormState.answer = ''
  questionFormState.tips = ''
  questionFormState.difficulty = 'EASY'
  questionFormState.tags = []
  questionFormState.remark = ''
  questionModalVisible.value = true
}

const openEditQuestionModal = async (question: Question) => {
  questionFormMode.value = 'edit'
  editingQuestionId.value = question.id
  currentBankId.value = question.bankId
  detailLoading.value = true
  try {
    const response = await questionApi.getQuestionDetail(question.id)
    if (response.data) {
      const detail = response.data
      questionFormState.bankId = detail.bankId
      questionFormState.categoryId = detail.categoryId
      questionFormState.content = detail.content
      questionFormState.answer = detail.answer ?? ''
      questionFormState.tips = detail.tips ?? ''
      questionFormState.difficulty = detail.difficulty
      questionFormState.tags = detail.tags?.slice() ?? []
      questionFormState.remark = detail.remark ?? ''
    }
    questionModalVisible.value = true
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : '加载题目详情失败'
    message.error(errorMessage)
  } finally {
    detailLoading.value = false
  }
}

const closeQuestionModal = () => {
  questionModalVisible.value = false
  questionFormRef.value?.resetFields()
}

const handleQuestionSubmit = async () => {
  try {
    await questionFormRef.value?.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    const payload: CreateQuestionPayload | UpdateQuestionPayload = {
      bankId: questionFormState.bankId,
      categoryId: questionFormState.categoryId!,
      content: questionFormState.content,
      answer: questionFormState.answer,
      tips: questionFormState.tips,
      difficulty: questionFormState.difficulty,
      tags: questionFormState.tags,
      remark: questionFormState.remark,
    }

    if (questionFormMode.value === 'create') {
      await questionApi.createQuestion(payload as CreateQuestionPayload)
      message.success('题目创建成功')
    } else if (editingQuestionId.value) {
      await questionApi.updateQuestion(editingQuestionId.value, payload as UpdateQuestionPayload)
      message.success('题目更新成功')
    }
    closeQuestionModal()
    if (currentBankId.value) {
      fetchQuestions(currentBankId.value)
    }
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : '提交失败'
    message.error(errorMessage)
  } finally {
    submitting.value = false
  }
}

const handleDeleteQuestion = async (question: Question) => {
  try {
    await questionApi.deleteQuestion(question.id)
    message.success('题目已删除')
    if (currentBankId.value) {
      fetchQuestions(currentBankId.value)
    }
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : '删除题目失败'
    message.error(errorMessage)
  }
}

const showQuestionDetail = async (question: Question) => {
  detailLoading.value = true
  detailVisible.value = true
  try {
    const response = await questionApi.getQuestionDetail(question.id)
    selectedQuestion.value = response.data ?? null
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : '加载题目详情失败'
    message.error(errorMessage)
  } finally {
    detailLoading.value = false
  }
}

const handlePanelChange = (key: string | number | (string | number)[]) => {
  const keys = Array.isArray(key) ? key : [key]
  keys.forEach((k) => {
    const bankId = Number(k)
    if (bankId) {
      // 初始化查询表单（如果还没初始化）
      initQuestionQuery(bankId)
      // 如果还没加载过题目，则加载
      if (!questions.value[bankId]) {
        fetchQuestions(bankId)
      }
    }
  })
}

onMounted(() => {
  fetchCategories()
  fetchQuestionBanks()
  fetchBankOptions()
})
</script>

<template>
  <a-space direction="vertical" size="large" class="question-bank-page">
    <a-card title="题库筛选">
      <a-form layout="inline" :model="queryForm" @submit.prevent>
        <a-form-item label="题库名称">
          <a-input v-model:value="queryForm.name" placeholder="请输入题库名称" allow-clear />
        </a-form-item>
        <a-form-item label="分类">
          <a-select
            v-model:value="queryForm.categoryId"
            placeholder="请选择分类"
            allow-clear
            style="width: 200px"
          >
            <a-select-option v-for="cat in level2Categories" :key="cat.id" :value="cat.id">
              {{ cat.name }}
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

    <a-card title="题库管理" class="bank-card">
      <template #extra>
        <a-space>
          <a-button @click="openBatchModal">
            <upload-outlined />
            批量上传
          </a-button>
          <a-button type="primary" @click="openCreateBankModal">新增题库</a-button>
        </a-space>
      </template>
      <a-spin :spinning="listLoading">
        <a-empty v-if="!questionBanks.length" description="暂无题库数据" />
        <a-collapse v-else v-model:activeKey="activePanels" @change="handlePanelChange">
          <a-collapse-panel v-for="bank in questionBanks" :key="bank.id">
            <template #header>
              <div class="panel-header">
                <span class="panel-header__title">{{ bank.name }}</span>
                <a-space size="small" @click.stop>
                  <a-button type="link" @click.stop="openEditBankModal(bank)">编辑</a-button>
                  <a-popconfirm
                    title="删除该题库将同时删除其下所有题目，确认删除？"
                    ok-text="删除"
                    ok-type="danger"
                    cancel-text="取消"
                    @confirm.stop="() => handleDeleteBank(bank)"
                  >
                    <a-button type="link" danger>删除</a-button>
                  </a-popconfirm>
                </a-space>
              </div>
            </template>
            <!-- 题目查询表单 -->
            <div v-if="questionQueryForms[bank.id]" class="question-query-form">
              <a-form layout="inline" style="margin-bottom: 16px">
                <a-form-item label="关键字">
                  <a-input
                    v-model:value="questionQueryForms[bank.id]!.keyword"
                    placeholder="搜索题目内容"
                    allow-clear
                    style="width: 200px"
                  />
                </a-form-item>
                <a-form-item label="难易度">
                  <a-select
                    v-model:value="questionQueryForms[bank.id]!.difficulty"
                    placeholder="请选择难易度"
                    allow-clear
                    style="width: 120px"
                  >
                    <a-select-option
                      v-for="item in difficultyOptions"
                      :key="item.value"
                      :value="item.value"
                    >
                      {{ item.label }}
                    </a-select-option>
                  </a-select>
                </a-form-item>
                <a-form-item>
                  <a-space>
                    <a-button type="primary" size="small" @click="handleQuestionSearch(bank.id)">
                      查询
                    </a-button>
                    <a-button size="small" @click="handleQuestionReset(bank.id)">重置</a-button>
                  </a-space>
                </a-form-item>
              </a-form>
            </div>

            <div class="bank-panel__toolbar">
              <span class="bank-panel__meta">
                共 {{ questionPaginations[bank.id]?.total || 0 }} 道题目
              </span>
              <a-button
                size="small"
                type="primary"
                @click="() => openCreateQuestionModal(bank.id, bank.categoryId)"
              >
                新增题目
              </a-button>
            </div>
            <a-spin :spinning="questionsLoading[bank.id]">
              <a-empty v-if="!questions[bank.id]?.length" description="该题库暂无题目" />
              <a-list
                v-else
                :data-source="questions[bank.id]"
                :split="false"
                class="bank-panel__list"
              >
                <template #renderItem="{ item }">
                  <a-list-item class="question-item">
                    <div class="question-item__content">
                      <div class="question-item__header">
                        <div class="question-item__title">{{ item.content }}</div>
                        <a-tag :color="getDifficultyTag(item.difficulty).color">
                          {{ getDifficultyTag(item.difficulty).text }}
                        </a-tag>
                      </div>
                      <div class="question-item__meta">
                        <span>创建时间：{{ item.createdAt || '--' }}</span>
                      </div>
                      <div class="question-item__tags">
                        <span>标签：</span>
                        <a-space wrap>
                          <a-tag v-for="tag in item.tags" :key="tag" :color="getTagColor(tag)">
                            {{ tag }}
                          </a-tag>
                          <span v-if="!item.tags || item.tags.length === 0">暂无</span>
                        </a-space>
                      </div>
                    </div>
                    <div class="question-item__actions">
                      <a-button type="link" @click="showQuestionDetail(item)">查看</a-button>
                      <a-button type="link" @click="openEditQuestionModal(item)">编辑</a-button>
                      <a-popconfirm
                        title="确认删除该题目？"
                        ok-text="删除"
                        ok-type="danger"
                        cancel-text="取消"
                        @confirm="() => handleDeleteQuestion(item)"
                      >
                        <a-button type="link" danger>删除</a-button>
                      </a-popconfirm>
                    </div>
                  </a-list-item>
                </template>
              </a-list>
              <!-- 题目分页 -->
              <div
                v-if="questions[bank.id]?.length && questionPaginations[bank.id]"
                class="question-pagination-wrapper"
              >
                <a-pagination
                  v-model:current="questionPaginations[bank.id]!.current"
                  v-model:page-size="questionPaginations[bank.id]!.pageSize"
                  :total="questionPaginations[bank.id]!.total"
                  :show-size-changer="true"
                  :show-quick-jumper="true"
                  :show-total="renderSimpleTotal"
                  :page-size-options="['5','10', '20', '50', '100']"
                  @change="createQuestionPageChangeHandler(bank.id)"
                  @show-size-change="createQuestionPageChangeHandler(bank.id)"
                />
              </div>
            </a-spin>
          </a-collapse-panel>
        </a-collapse>
        <!-- 分页组件 -->
        <div v-if="questionBanks.length > 0" class="pagination-wrapper">
          <a-pagination
            v-model:current="pagination.current"
            v-model:page-size="pagination.pageSize"
            :total="pagination.total"
            :show-size-changer="pagination.showSizeChanger"
            :show-quick-jumper="pagination.showQuickJumper"
            :show-total="pagination.showTotal"
            :page-size-options="pagination.pageSizeOptions"
            @change="handlePageChange"
          />
        </div>
      </a-spin>
    </a-card>

    <!-- 批量上传弹窗 -->
    <a-modal
      v-model:open="batchModalVisible"
      title="批量上传题目"
      width="540px"
      destroy-on-close
      :confirm-loading="batchSubmitting"
      @ok="handleBatchSubmit"
      @cancel="closeBatchModal"
    >
      <a-form layout="vertical" :model="batchFormState" ref="batchFormRef">
        <a-form-item
          label="选择题库"
          name="bankId"
          :rules="[{ required: true, message: '请选择题库' }]"
        >
          <a-select
            v-model:value="batchFormState.bankId"
            placeholder="请选择题库"
            show-search
            allow-clear
            option-filter-prop="label"
            :loading="bankOptionsLoading"
            :options="bankOptions.map((item) => ({ label: item.name, value: item.id }))"
          />
        </a-form-item>
        <a-form-item
          label="行业分类"
          name="categoryId"
          :rules="[{ required: true, message: '请选择行业分类' }]"
        >
          <a-select
            v-model:value="batchFormState.categoryId"
            placeholder="请选择行业分类"
            show-search
            option-filter-prop="children"
            allow-clear
          >
            <a-select-option v-for="cat in level2Categories" :key="cat.id" :value="cat.id">
              {{ cat.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item
          label="题目文件"
          :validate-status="batchFileList.length ? '' : 'error'"
          :help="batchFileList.length ? '支持 md、doc、docx、txt 格式文件' : '请上传题目文件'"
        >
          <a-upload
            v-model:file-list="batchFileList"
            :before-upload="handleBatchBeforeUpload"
            :max-count="1"
            :accept="allowedFileAccept"
            @remove="handleBatchRemove"
          >
            <a-button>
              <upload-outlined />
              选择文件
            </a-button>
          </a-upload>
          <div class="upload-hint">上传前请确认文件内容格式正确，支持 md、doc、docx、txt</div>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 题库表单弹窗 -->
    <a-modal
      v-model:open="bankModalVisible"
      :title="bankModalTitle"
      width="640px"
      destroy-on-close
      :confirm-loading="submitting"
      @ok="handleBankSubmit"
      @cancel="closeBankModal"
    >
      <a-form layout="vertical" :model="bankFormState" ref="bankFormRef">
        <a-form-item
          label="题库名称"
          name="name"
          :rules="[{ required: true, message: '请输入题库名称' }]"
        >
          <a-input v-model:value="bankFormState.name" placeholder="请输入题库名称" />
        </a-form-item>
        <a-form-item
          label="所属分类"
          name="categoryId"
          :rules="[{ required: true, message: '请选择分类' }]"
        >
          <a-select
            v-model:value="bankFormState.categoryId"
            placeholder="请选择二级分类"
          >
            <a-select-option v-for="cat in level2Categories" :key="cat.id" :value="cat.id">
              {{ cat.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="题库简介" name="description">
          <a-textarea
            v-model:value="bankFormState.description"
            :rows="4"
            placeholder="简要说明题库内容与适用场景"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 题目表单弹窗 -->
    <a-modal
      v-model:open="questionModalVisible"
      :title="questionModalTitle"
      width="800px"
      destroy-on-close
      :confirm-loading="submitting"
      @ok="handleQuestionSubmit"
      @cancel="closeQuestionModal"
    >
      <a-form layout="vertical" :model="questionFormState" ref="questionFormRef">
        <a-form-item
          label="题目内容"
          name="content"
          :rules="[{ required: true, message: '请输入题目内容' }]"
        >
          <a-textarea
            v-model:value="questionFormState.content"
            :rows="4"
            placeholder="请输入题目内容"
          />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item
              label="所属分类"
              name="categoryId"
              :rules="[{ required: true, message: '请选择分类' }]"
            >
              <a-select
                v-model:value="questionFormState.categoryId"
                placeholder="请选择二级分类"
              >
                <a-select-option v-for="cat in level2Categories" :key="cat.id" :value="cat.id">
                  {{ cat.name }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item
              label="难度"
              name="difficulty"
              :rules="[{ required: true, message: '请选择难度' }]"
            >
              <a-select v-model:value="questionFormState.difficulty" placeholder="请选择难度">
                <a-select-option
                  v-for="opt in difficultyOptions"
                  :key="opt.value"
                  :value="opt.value"
                >
                  {{ opt.label }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="参考答案" name="answer">
          <a-textarea
            v-model:value="questionFormState.answer"
            :rows="6"
            placeholder="请输入参考答案"
          />
        </a-form-item>
        <a-form-item label="解题提示" name="tips">
          <a-textarea
            v-model:value="questionFormState.tips"
            :rows="3"
            placeholder="请输入解题提示"
          />
        </a-form-item>
        <a-form-item label="标签" name="tags">
          <a-select
            v-model:value="questionFormState.tags"
            mode="tags"
            placeholder="直接输入自定义标签，按回车添加"
            allow-clear
          >
          </a-select>
          <div style="color: #999; font-size: 12px; margin-top: 4px;">
            💡 提示：直接输入标签内容，按回车键添加，支持多个标签
          </div>
        </a-form-item>
        <a-form-item label="备注" name="remark">
          <a-textarea
            v-model:value="questionFormState.remark"
            :rows="2"
            placeholder="备注信息"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 题目详情弹窗 -->
    <a-modal
      v-model:open="detailVisible"
      title="题目详情"
      width="800px"
      :footer="null"
    >
      <a-spin :spinning="detailLoading">
        <div v-if="selectedQuestion" class="question-detail">
          <a-descriptions bordered :column="2">
            <a-descriptions-item label="题目内容" :span="2">
              <div class="detail-content">{{ selectedQuestion.content }}</div>
            </a-descriptions-item>
            <a-descriptions-item label="难度">
              <a-tag :color="getDifficultyTag(selectedQuestion.difficulty).color">
                {{ getDifficultyTag(selectedQuestion.difficulty).text }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="来源">
              {{ selectedQuestion.source || '暂无' }}
            </a-descriptions-item>
            <a-descriptions-item label="标签" :span="2">
              <a-space wrap>
                <a-tag
                  v-for="tag in selectedQuestion.tags"
                  :key="tag"
                  :color="getTagColor(tag)"
                >
                  {{ tag }}
                </a-tag>
                <span v-if="!selectedQuestion.tags || selectedQuestion.tags.length === 0">
                  暂无
                </span>
              </a-space>
            </a-descriptions-item>
            <a-descriptions-item label="参考答案" :span="2">
              <div class="detail-content">{{ selectedQuestion.answer || '暂无' }}</div>
            </a-descriptions-item>
            <a-descriptions-item label="解题提示" :span="2">
              <div class="detail-content">{{ selectedQuestion.tips || '暂无' }}</div>
            </a-descriptions-item>
            <a-descriptions-item label="备注" :span="2">
              <div class="detail-content">{{ selectedQuestion.remark || '暂无' }}</div>
            </a-descriptions-item>
            <a-descriptions-item label="创建时间">
              {{ selectedQuestion.createdAt }}
            </a-descriptions-item>
            <a-descriptions-item label="更新时间">
              {{ selectedQuestion.updatedAt || '--' }}
            </a-descriptions-item>
          </a-descriptions>
        </div>
      </a-spin>
    </a-modal>
  </a-space>
</template>

<style scoped>
.question-bank-page {
  width: 100%;
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

.bank-panel__toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding: 8px 12px;
  background: #f7f9fc;
  border-radius: 8px;
}

.bank-panel__meta {
  color: rgba(0, 0, 0, 0.45);
}

.bank-panel__list {
  width: 100%;
  margin-top: 12px;
}

.question-item {
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

.question-item:hover {
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
  background: #fff;
}

.question-item__content {
  flex: 1;
  padding-right: 16px;
}

.question-item__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 8px;
  gap: 12px;
}

.question-item__title {
  flex: 1;
  font-size: 16px;
  font-weight: 600;
  color: #1f1f1f;
  line-height: 1.5;
}

.question-item__meta {
  display: flex;
  gap: 16px;
  color: rgba(0, 0, 0, 0.45);
  margin-bottom: 8px;
  font-size: 13px;
}

.question-item__tags {
  display: flex;
  gap: 8px;
  align-items: center;
  color: rgba(0, 0, 0, 0.65);
}

.question-item__actions {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
  min-width: 120px;
}

.question-item__actions a-button {
  padding: 0;
}

.question-detail {
  padding: 16px 0;
}

.detail-content {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.6;
}

.question-query-form {
  background: #fafafa;
  padding: 16px;
  border-radius: 4px;
  margin-bottom: 16px;
}

.question-pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: center;
  align-items: center;
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

.upload-hint {
  margin-top: 8px;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}

@media (max-width: 768px) {
  .question-bank-page {
    padding: 12px 0;
  }

  .panel-header__title {
    font-size: 14px;
  }

  .question-item {
    padding: 12px;
    margin-bottom: 8px;
  }

  .question-item__title {
    font-size: 14px;
  }

  .question-item__meta {
    font-size: 12px;
  }

  .bank-panel__toolbar {
    padding: 6px 8px;
  }

  .question-query-form {
    padding: 12px;
  }

  :deep(.ant-card-head) {
    padding: 12px 16px;
  }

  :deep(.ant-card-body) {
    padding: 16px;
  }
}

@media (max-width: 480px) {
  .question-bank-page {
    padding: 8px 0;
  }

  .question-item {
    padding: 10px;
    margin-bottom: 6px;
  }

  .question-item__title {
    font-size: 13px;
  }

  .question-item__actions {
    min-width: 100px;
  }

  .question-item__actions a-button {
    font-size: 12px;
  }

  .question-query-form {
    padding: 10px;
  }

  :deep(.ant-card-head) {
    padding: 10px 12px;
  }

  :deep(.ant-card-body) {
    padding: 12px;
  }
}
