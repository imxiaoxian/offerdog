<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { HeartFilled, HeartOutlined, LeftOutlined } from '@ant-design/icons-vue';
import { questionBankApi } from '@/services/questionBank';
import { questionApi } from '@/services/question';
import { favoriteApi } from '@/services/favorites';
import { message } from 'ant-design-vue';
import type { QuestionBank } from '@/services/questionBank';
import type { Question, GetQuestionsParams } from '@/services/question';

const route = useRoute();
const router = useRouter();

/** 与路由同步的题库 ID（避免占位/假数据 id 或同页切换路由时仍用旧 id） */
const bankId = computed(() => {
  const raw = route.params.id;
  const n = Number(Array.isArray(raw) ? raw[0] : raw);
  return Number.isFinite(n) && n > 0 ? n : 0;
});

// 题库详情数据
const questionBank = ref<QuestionBank | null>(null);
// 加载状态
const loading = ref(false);
// 问题列表数据
const questions = ref<Question[]>([]);
// 问题列表加载状态
const questionsLoading = ref(false);
// 收藏状态
const favoriteIds = ref<Set<number>>(new Set());
const favoriteLoading = ref<Record<number, boolean>>({});
// 分页参数
const pagination = ref({
  current: 1,
  pageSize: 20,
  total: 0
});
// 查询参数
const searchParams = ref<GetQuestionsParams>({});
// 标签搜索参数
const tagSearch = ref('');
// 处理标签搜索
const handleTagSearch = () => {
  if (tagSearch.value) {
    searchParams.value.tags = tagSearch.value;
  } else {
    delete searchParams.value.tags;
  }
  loadQuestions();
};
// 处理标签选择
const handleTagSelect = (tag: string) => {
  tagSearch.value = tag;
  handleTagSearch();
};

// 获取题库详情
const loadQuestionBankDetail = async () => {
  if (!bankId.value) {
    message.error('题库ID无效');
    return;
  }

  loading.value = true;
  try {
    const response = await questionBankApi.getQuestionBankDetail(bankId.value);
    questionBank.value = response.data;
  } catch (error) {
    console.error('加载题库详情失败:', error);
    message.error('加载题库详情失败，请确认已登录且该题库存在');
  } finally {
    loading.value = false;
  }
};

// 获取问题列表
const loadQuestions = async () => {
  if (!bankId.value) return;

  questionsLoading.value = true;
  try {
    const params: GetQuestionsParams = {
      ...searchParams.value,
      bankId: bankId.value,
      pageNo: pagination.value.current,
      pageSize: pagination.value.pageSize
    };
    
    const response = await questionApi.getQuestions(params);
    const questionList = response.data ?? [];
    questions.value = questionList;
    pagination.value.total = response.totalCount ?? 0;
  } catch (error) {
    console.error('加载问题列表失败:', error);
    message.error('加载问题列表失败');
  } finally {
    questionsLoading.value = false;
  }
};

// 处理分页变化
const handlePageChange = (page: number) => {
  pagination.value.current = page;
  loadQuestions();
};

// 处理每页条数变化
const handlePageSizeChange = (current: number, size: number) => {
  pagination.value.pageSize = size;
  pagination.value.current = 1;
  loadQuestions();
};

const loadFavorites = async () => {
  try {
    const ids: number[] = [];
    let pageNum = 1;
    const pageSize = 100;
    while (true) {
      const response = await favoriteApi.listFavorites({ pageNum, pageSize });
      const list = response.data ?? [];
      ids.push(...list.map((item) => item.id));
      const total = response.totalCount ?? ids.length;
      if (ids.length >= total || list.length < pageSize) break;
      pageNum += 1;
    }
    favoriteIds.value = new Set(ids);
  } catch (error) {
    console.error('加载收藏列表失败:', error);
  }
};

const isFavorite = (questionId: number) => favoriteIds.value.has(questionId);

const toggleFavorite = async (question: Question) => {
  const targetId = question.id;
  if (favoriteLoading.value[targetId]) return;
  favoriteLoading.value[targetId] = true;
  try {
    if (isFavorite(targetId)) {
      await favoriteApi.removeFavorite(targetId);
      favoriteIds.value = new Set([...favoriteIds.value].filter((id) => id !== targetId));
      message.success('已取消收藏');
    } else {
      await favoriteApi.addFavorite(targetId);
      favoriteIds.value = new Set([...favoriteIds.value, targetId]);
      message.success('已收藏');
    }
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : '操作失败';
    message.error(errorMessage);
  } finally {
    favoriteLoading.value[targetId] = false;
  }
};

// 获取难度显示文本
const getDifficultyText = (difficulty: string) => {
  const difficultyMap: Record<string, string> = {
    'EASY': '简单',
    'MEDIUM': '中等',
    'HARD': '困难',
    'easy': '简单',
    'medium': '中等',
    'hard': '困难'
  };
  return difficultyMap[difficulty] || difficulty;
};

// 获取难度样式类
const getDifficultyClass = (difficulty: string) => {
  const difficultyMap: Record<string, string> = {
    'EASY': 'easy',
    'MEDIUM': 'medium',
    'HARD': 'hard',
    'easy': 'easy',
    'medium': 'medium',
    'hard': 'hard'
  };
  return difficultyMap[difficulty] || '';
};

// 分页总条数显示函数
const showTotal = (total: number) => {
  return `共 ${total} 条`;
};

// 组件挂载时加载数据
onMounted(() => {
  loadQuestionBankDetail();
  loadQuestions();
  loadFavorites();
});

watch(
  () => route.params.id,
  () => {
    if (!bankId.value) return;
    pagination.value.current = 1;
    loadQuestionBankDetail();
    loadQuestions();
  },
);

// 处理开始刷题
const handleStartPractice = () => {
  const firstQuestion = questions.value[0];
  if (firstQuestion) {
    const firstQuestionId = firstQuestion.id;
    router.push({
      name: 'Question',
      query: {
        bankId: bankId.value,
        questionId: firstQuestionId
      }
    });
  } else {
    message.warning('该题库暂无题目');
  }
};

// 处理问题点击
const handleQuestionClick = (question: Question) => {
  router.push({
    name: 'Question',
    query: {
      bankId: bankId.value,
      questionId: question.id
    }
  });
};

const handleBack = () => {
  // 兼容用户直接打开详情页（无上一页历史）的场景
  if (window.history.length > 1) {
    router.back();
    return;
  }
  router.push({ name: 'QuestionBank' });
};

</script>

<template>
  <div class="bank-detail-container">
    <a-spin :spinning="loading">
      <div v-if="questionBank" class="bank-detail-content">
        <div class="bank-topbar">
          <a-button class="back-btn" @click="handleBack">
            <template #icon>
              <LeftOutlined />
            </template>
            返回题库
          </a-button>
        </div>
        <h1 class="bank-title">{{ questionBank.name }}</h1>
        <div class="bank-description">{{ questionBank.description }}</div>
        <div class="bank-info">
          <div class="info-item">
            <span class="info-label">创建时间：</span>
            <span class="info-value">{{ new Date(questionBank.createdAt).toLocaleString() }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">更新时间：</span>
            <span class="info-value">{{ new Date(questionBank.updatedAt).toLocaleString() }}</span>
          </div>
        </div>
        <div class="bank-actions">
          <a-button type="primary" size="large" @click="handleStartPractice">开始刷题</a-button>
          <a-button size="large">分享</a-button>
        </div>
        
        <!-- 问题列表卡片 -->
        <div class="questions-section">
          <h2 class="section-title">题目列表</h2>
          
          <!-- 搜索筛选区域 -->
            <div class="search-filter-section">
              <a-input
                v-model:value="searchParams.keyword"
                placeholder="搜索题目"
                style="width: 200px; margin-right: 16px;"
              />
              <a-select
                v-model:value="searchParams.difficulty"
                placeholder="难度筛选"
                style="width: 120px; margin-right: 16px;"
              >
                <a-select-option value="">全部难度</a-select-option>
                <a-select-option value="EASY">简单</a-select-option>
                <a-select-option value="MEDIUM">中等</a-select-option>
                <a-select-option value="HARD">困难</a-select-option>
              </a-select>
              <a-input
                v-model:value="tagSearch"
                placeholder="搜索标签"
                style="width: 150px; margin-right: 16px;"
              />
              <a-button type="primary" @click="handleTagSearch">搜索</a-button>
            </div>
          
          <!-- 问题列表 -->
            <a-spin :spinning="questionsLoading">
              <div v-if="questions.length > 0" class="questions-table">
                <!-- 表头 -->
                <div class="table-header">
                  <div class="header-item header-question">题目</div>
                  <div class="header-item header-difficulty">难度</div>
                  <div class="header-item header-tags">标签</div>
                  <div class="header-item header-actions">操作</div>
                </div>
                <!-- 表格内容 -->
                <div class="table-body">
                  <div 
                    v-for="question in questions" 
                    :key="question.id"
                    class="table-row"
                  >
                    <div class="table-cell cell-question">
                      <a href="#" class="question-text" @click.prevent="handleQuestionClick(question)">{{ question.content }}</a>
                    </div>
                    <div class="table-cell cell-difficulty">
                      <span :class="['difficulty', getDifficultyClass(question.difficulty)]">
                        {{ getDifficultyText(question.difficulty) }}
                      </span>
                    </div>
                    <div class="table-cell cell-tags">
                      <div class="tags">
                        <a-tag 
                          v-for="tag in question.tags" 
                          :key="tag"
                          color="blue"
                          @click="handleTagSelect(tag)"
                        >
                          {{ tag }}
                        </a-tag>
                      </div>
                    </div>
                    <div class="table-cell cell-actions">
                      <a-button
                        type="link"
                        :loading="favoriteLoading[question.id]"
                        @click="() => toggleFavorite(question)"
                      >
                        <template #icon>
                          <HeartFilled v-if="isFavorite(question.id)" style="color:#ff4d4f;" />
                          <HeartOutlined v-else />
                        </template>
                        {{ isFavorite(question.id) ? '取消收藏' : '收藏' }}
                      </a-button>
                    </div>
                  </div>
                </div>
              </div>
              <div v-else class="empty-questions">
                <a-empty description="暂无题目" />
              </div>
            </a-spin>
          
          <!-- 分页控件 -->
          <div class="pagination-section" v-if="pagination.total > 0">
            <a-pagination
              v-model:current="pagination.current"
              v-model:pageSize="pagination.pageSize"
              :total="pagination.total"
              :page-size-options="['1', '5', '10', '20', '50']"
              show-size-changer
              show-quick-jumper
              :show-total="showTotal"
              @change="handlePageChange"
              @showSizeChange="handlePageSizeChange"
            />
          </div>
        </div>
      </div>
      <div v-else class="empty-state">
        <a-empty description="题库不存在或已被删除" />
      </div>
    </a-spin>
  </div>
</template>

<style scoped>
.bank-detail-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

.bank-topbar {
  display: flex;
  justify-content: flex-start;
  margin-bottom: 12px;
}

.back-btn {
  border-radius: 10px;
}

.bank-detail-content {
  background-color: #ffffff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  padding: 32px;
}

.bank-title {
  font-size: 28px;
  font-weight: 600;
  margin: 0 0 16px 0;
  color: #262626;
}

.bank-description {
  font-size: 16px;
  color: #595959;
  margin: 0 0 24px 0;
  line-height: 1.6;
}

.bank-info {
  display: flex;
  gap: 32px;
  margin: 0 0 32px 0;
  padding: 16px 0;
  border-top: 1px solid #f0f0f0;
  border-bottom: 1px solid #f0f0f0;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.info-label {
  font-size: 14px;
  color: #8c8c8c;
}

.info-value {
  font-size: 14px;
  color: #262626;
}

.bank-actions {
  display: flex;
  gap: 16px;
  margin-bottom: 32px;
}

.empty-state {
  text-align: center;
  padding: 64px 0;
}

/* 问题列表部分样式 */
.questions-section {
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid #f0f0f0;
}

.section-title {
  font-size: 20px;
  font-weight: 600;
  margin: 0 0 24px 0;
  color: #262626;
}

/* 搜索筛选区域 */
.search-filter-section {
  display: flex;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

/* 问题表格 */
.questions-table {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  overflow: hidden;
}

/* 表头 */
.table-header {
  display: flex;
  background-color: #fafafa;
  border-bottom: 1px solid #f0f0f0;
}

.header-item {
  padding: 12px 16px;
  font-size: 14px;
  font-weight: 600;
  color: #262626;
  text-align: left;
}

.header-question {
  flex: 1;
  min-width: 400px;
}

.header-difficulty {
  width: 100px;
}

.header-tags {
  flex: 1;
  min-width: 200px;
}

.header-actions {
  width: 140px;
  text-align: center;
}

/* 表格内容 */
.table-body {
  background-color: #ffffff;
}

.table-row {
  display: flex;
  border-bottom: 1px solid #f0f0f0;
  transition: background-color 0.3s ease;
}

.table-row:hover {
  background-color: #f5f5f5;
}

.table-row:last-child {
  border-bottom: none;
}

.table-cell {
  padding: 16px;
  display: flex;
  align-items: center;
}

.cell-question {
  flex: 1;
  min-width: 400px;
}

.cell-difficulty {
  width: 100px;
}

.cell-tags {
  flex: 1;
  min-width: 200px;
}

.cell-actions {
  width: 140px;
  justify-content: center;
}

.question-text {
  font-size: 16px;
  color: #262626;
  line-height: 1.5;
  text-decoration: none;
  transition: color 0.3s ease;
  word-break: break-word;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  max-height: 3em; /* 2行 * 1.5行高 */
}

.question-text:hover {
  color: #1890ff;
}

/* 标签容器 */
.tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.tags .ant-tag {
  cursor: pointer;
}

.tags .ant-tag:hover {
  opacity: 0.8;
}

/* 难度标签样式 */
.difficulty {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.difficulty.easy {
  background-color: #f6ffed;
  color: #52c41a;
  border: 1px solid #b7eb8f;
}

.difficulty.medium {
  background-color: #fff7e6;
  color: #fa8c16;
  border: 1px solid #ffd591;
}

.difficulty.hard {
  background-color: #fff1f0;
  color: #ff4d4f;
  border: 1px solid #ffccc7;
}

/* 标签容器 */
.tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

/* 空状态 */
.empty-questions {
  text-align: center;
  padding: 48px 0;
}

/* 分页控件 */
.pagination-section {
  margin-top: 24px;
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

/* 响应式设计 */
@media (max-width: 768px) {
  .bank-detail-container {
    padding: 12px;
  }

  .bank-detail-content {
    padding: 16px;
  }
  
  .bank-title {
    font-size: 22px;
    word-break: break-word;
  }
  
  .bank-info {
    flex-direction: column;
    gap: 16px;
  }
  
  .bank-actions {
    flex-direction: column;
  }
  
  .bank-actions :deep(.ant-btn) {
    width: 100%;
  }
  
  .search-filter-section {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }
  
  .search-filter-section .ant-input,
  .search-filter-section .ant-select {
    width: 100% !important;
    margin-right: 0 !important;
  }

  .search-filter-section :deep(.ant-btn) {
    width: 100%;
  }
  
  .question-meta {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .questions-table {
    overflow-x: visible;
    border: none;
    background: transparent;
  }
  
  .table-header {
    display: none;
  }
  
  .table-row {
    flex-direction: column;
    align-items: stretch;
    min-width: 0;
    width: 100%;
    margin-bottom: 12px;
    padding: 12px;
    border: 1px solid #f0f0f0;
    border-radius: 10px;
    background: #fff;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  }
  
  .table-cell {
    width: 100%;
    padding: 8px 0;
    flex-direction: row;
    flex-wrap: wrap;
    align-items: flex-start;
    border: none;
  }
  
  .cell-question {
    min-width: 0;
    padding-top: 0;
  }
  
  .cell-difficulty {
    width: 100%;
    margin-top: 0;
  }

  .cell-difficulty::before {
    content: '难度';
    font-size: 12px;
    color: #8c8c8c;
    margin-right: 8px;
    flex-shrink: 0;
  }
  
  .cell-tags {
    min-width: 0;
    margin-top: 0;
    width: 100%;
  }

  .cell-tags::before {
    content: '标签';
    display: block;
    width: 100%;
    font-size: 12px;
    color: #8c8c8c;
    margin-bottom: 6px;
  }
  
  .cell-actions {
    width: 100%;
    margin-top: 4px;
    justify-content: flex-start;
    padding-bottom: 0;
  }
  
  .pagination-section {
    justify-content: center;
  }
  
  :deep(.ant-pagination) {
    flex-wrap: nowrap !important;
    justify-content: center;
    gap: 8px;
    display: flex !important;
  }
  
  :deep(.ant-pagination) > * {
    flex: none !important;
    white-space: nowrap;
  }
  
  :deep(.ant-pagination-item),
  :deep(.ant-pagination-prev),
  :deep(.ant-pagination-next) {
    min-width: 36px;
    height: 36px;
    line-height: 36px;
    margin: 0;
    display: inline-flex !important;
    align-items: center;
    justify-content: center;
  }
  
  :deep(.ant-pagination-item) {
    font-size: 14px;
    font-weight: 500;
  }
  
  :deep(.ant-pagination-prev .ant-pagination-item-link),
  :deep(.ant-pagination-next .ant-pagination-item-link) {
    font-size: 16px;
  }
  
  :deep(.ant-pagination-options) {
    display: none !important;
  }
  
  :deep(.ant-pagination-jump-prev),
  :deep(.ant-pagination-jump-next) {
    display: none !important;
  }
  
  :deep(.ant-pagination-total-text) {
    display: none !important;
  }
}
</style>
