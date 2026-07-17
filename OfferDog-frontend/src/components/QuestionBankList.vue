<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';
import { questionBankApi } from '../services/questionBank';
import QuestionBankCard from './QuestionBankCard.vue';
import type { QuestionBank, GetQuestionBanksParams } from '../services/questionBank';

// 定义分页参数接口
type PaginationParams = Pick<GetQuestionBanksParams, 'pageNo' | 'pageSize' | 'categoryId' | 'categoryIds' | 'name'>;

// 定义组件属性
const props = defineProps<{
  // 可选的预定义题库列表
  questionBanks?: QuestionBank[];
  // 分页参数
  pagination?: PaginationParams;
  // 是否显示加载状态
  showLoading?: boolean;
  /** categoryId -> 岗位名称（用于卡片展示） */
  categoryNames?: Record<number, string>;
}>();

// 定义组件事件
const emit = defineEmits<{
  // 点击题库卡片时触发
  (e: 'select', questionBank: QuestionBank): void;
  // 加载完成时触发
  (e: 'load', questionBanks: QuestionBank[]): void;
  // 分页变化时触发
  (e: 'pageChange', pageNo: number): void;
}>();

// 题库数据列表
const questionBankList = ref<QuestionBank[]>([]);
const loadError = ref(false);
// 加载状态
const loading = ref(false);
// 分页信息
const paginationInfo = ref({
  pageNo: props.pagination?.pageNo || 1,
  pageSize: props.pagination?.pageSize || 10,
  totalCount: 0,
  totalPage: 1
});

// 加载题库数据
const loadQuestionBanks = async () => {
  if (props.questionBanks) {
    questionBankList.value = [...props.questionBanks];
    emit('load', props.questionBanks);
    return;
  }

  loading.value = true;
  loadError.value = false;
  try {
    const response = await questionBankApi.getQuestionBanks({
      pageNo: paginationInfo.value.pageNo,
      pageSize: paginationInfo.value.pageSize,
      categoryId: props.pagination?.categoryId,
      categoryIds: props.pagination?.categoryIds,
      name: props.pagination?.name
    });
    
    const data = response.data ?? [];
    questionBankList.value = data;
    
    if (response.pageNo !== undefined) {
      paginationInfo.value.pageNo = response.pageNo;
    }
    if (response.pageSize !== undefined) {
      paginationInfo.value.pageSize = response.pageSize;
    }
    if (response.totalCount !== undefined) {
      paginationInfo.value.totalCount = response.totalCount;
    }
    if (response.totalPage !== undefined) {
      paginationInfo.value.totalPage = response.totalPage;
    }
    
    emit('load', questionBankList.value);
  } catch (error) {
      console.error('加载题库失败:', error);
      loadError.value = true;
      questionBankList.value = [];
      paginationInfo.value.totalCount = 0;
      paginationInfo.value.totalPage = 1;
      emit('load', questionBankList.value);
    } finally {
      loading.value = false;
    }
  };

// 处理题库卡片点击
const handleQuestionBankClick = (questionBank: QuestionBank) => {
  emit('select', questionBank);
};

// 监听分类ID变化，重新加载题库数据
watch(
  () => [props.pagination?.categoryId, props.pagination?.categoryIds, props.pagination?.name],
  () => {
    // 当分类ID或搜索名称变化时，重置页码并重新加载题库数据
    paginationInfo.value.pageNo = 1;
    loadQuestionBanks();
  },
);

// 处理分页变化
const applyPaginationChange = (pageNo: number, pageSize: number) => {
  paginationInfo.value.pageNo = pageNo;
  paginationInfo.value.pageSize = pageSize;
  emit('pageChange', pageNo);
  loadQuestionBanks();
};

const handlePageChange = (pageNo: number, pageSize?: number) => {
  applyPaginationChange(pageNo, pageSize ?? paginationInfo.value.pageSize);
};

const handlePageSizeChange = (_: number, pageSize: number) => {
  applyPaginationChange(1, pageSize);
};

const showPaginationTotal = (total: number) => `共 ${total} 条`;

// 组件挂载时加载数据
onMounted(() => {
  loadQuestionBanks();
});
</script>

<template>
  <div class="question-bank-list">
    <!-- 加载状态 -->
    <a-spin :spinning="loading && props.showLoading !== false">
      <!-- 题库卡片网格 -->
      <div class="question-bank-grid">
        <QuestionBankCard
          v-for="questionBank in questionBankList"
          :key="questionBank.id"
          :question-bank="questionBank"
          :category-name="props.categoryNames?.[questionBank.categoryId]"
          @click="handleQuestionBankClick(questionBank)"
        />
      </div>
      
      <!-- 空状态 -->
      <div v-if="!loading && questionBankList.length === 0" class="empty-state">
        <a-empty :description="loadError ? '题库加载失败，请检查登录状态与网络后刷新' : '暂无题库数据'" />
      </div>
      
      <!-- 分页组件 -->
      <div v-if="paginationInfo.totalPage > 1" class="pagination-container">
        <a-pagination
          :current="paginationInfo.pageNo"
          :page-size="paginationInfo.pageSize"
          :total="paginationInfo.totalCount"
          :show-total="showPaginationTotal"
          :page-size-options="[10, 18, 30, 50]"
          show-size-changer
          @change="handlePageChange"
          @showSizeChange="handlePageSizeChange"
        />
      </div>
    </a-spin>
  </div>
</template>

<style scoped>
.question-bank-list {
  width: 100%;
  padding: 16px 0;
}

/* 题库卡片网格布局 - Material Design 3 */
.question-bank-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 24px;
  margin-bottom: 32px;
}

/* 空状态样式 - Material Design 3 */
.empty-state {
  padding: 80px 24px;
  text-align: center;
  background: linear-gradient(135deg, #fafafa 0%, #f5f5f5 100%);
  border-radius: 16px;
  margin: 24px 0;
  border: 2px dashed #e0e0e0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.empty-state .ant-empty-description {
  color: #8c8c8c;
  font-size: 14px;
  font-weight: 500;
}

/* 分页容器样式 - Material Design 3 */
.pagination-container {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 32px;
  padding: 24px 0;
  border-top: 1px solid rgba(0, 0, 0, 0.08);
  gap: 16px;
}

:deep(.ant-pagination) {
  padding: 8px 0;
  display: flex;
  align-items: center;
}

:deep(.ant-pagination .ant-pagination-item) {
  border-radius: 8px;
  margin: 0 2px;
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

:deep(.ant-pagination .ant-pagination-item-active .ant-pagination-item-link) {
  color: white;
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
  margin-left: 0;
}

:deep(.ant-pagination-total-text) {
  margin-right: 0;
  color: #8c8c8c;
  font-size: 14px;
}

:deep(.ant-select-selector) {
  border-radius: 8px !important;
}

/* 响应式设计 - Material Design 3 */
@media (max-width: 1024px) {
  .question-bank-grid {
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 20px;
  }
}

@media (max-width: 768px) {
  .question-bank-grid {
    grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
    gap: 16px;
  }
  
  .empty-state {
    padding: 64px 16px;
    margin: 16px 0;
  }
  
  .question-bank-list {
    padding: 12px 0;
  }
  
  .pagination-container {
    padding: 16px 0;
  }
}

@media (max-width: 480px) {
  .question-bank-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }
  
  .question-bank-list {
    padding: 8px 0;
  }
  
  .empty-state {
    padding: 48px 16px;
  }
  
  :deep(.ant-pagination) {
    flex-wrap: wrap;
    justify-content: center;
  }
  
  :deep(.ant-pagination .ant-pagination-item) {
    margin: 2px 4px;
  }
  
  :deep(.ant-pagination-options) {
    margin-left: 0;
    width: 100%;
    justify-content: center;
    margin-top: 8px;
  }
}
</style>
