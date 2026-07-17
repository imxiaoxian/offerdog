<script lang="ts" setup>
import { computed, onMounted, ref } from 'vue';
import QuestionBankList from '../components/QuestionBankList.vue';
import { questionApi } from '../services/question';
import { favoriteApi } from '../services/favorites';
import { categoryApi } from '../services/category';
import {
  JOB_BUCKET_OPTIONS,
  type JobBucketKey,
  collectLevel2IdsForBucket,
} from '@/constants/jobCategoryBuckets';
import type { CategoryTreeNode } from '@/services/category';
import type { Question } from '../services/question';

const selectedBucket = ref<JobBucketKey>('all');
const categoryTree = ref<CategoryTreeNode[]>([]);

const questionBankKeyword = ref('');
const questionKeyword = ref('');
const questionSearchLoading = ref(false);
const questionResults = ref<Question[]>([]);
const favoriteIds = ref<Set<number>>(new Set());
const favoriteLoading = ref<Record<number, boolean>>({});
const expandedAnswers = ref<Record<number, boolean>>({});

onMounted(async () => {
  try {
    const res = await categoryApi.getCategoryTree();
    categoryTree.value = res.data ?? [];
  } catch (e) {
    console.error('加载分类失败:', e);
  }
});

const categoryNameById = computed(() => {
  const map = new Map<number, string>();
  const walk = (nodes: CategoryTreeNode[]) => {
    for (const c of nodes) {
      map.set(c.id, c.name);
      if (c.children?.length) walk(c.children);
    }
  };
  walk(categoryTree.value);
  return map;
});

const currentCategorySummary = computed(() => {
  return JOB_BUCKET_OPTIONS.find((o) => o.key === selectedBucket.value)?.label ?? '全部';
});

const categoryNamesRecord = computed<Record<number, string>>(() => {
  const o: Record<number, string> = {};
  categoryNameById.value.forEach((name, id) => {
    o[id] = name;
  });
  return o;
});

/**
 * 「非全部」时按分类树二级岗位过滤；无匹配 ID 时不传（与全部一致），
 * 避免分类树未加载、命名不一致或库里仅有「未归类」题库时出现整页空白。
 */
const categoryIdsForBucket = computed((): number[] | undefined => {
  if (selectedBucket.value === 'all') return undefined;
  const ids = collectLevel2IdsForBucket(categoryTree.value, selectedBucket.value);
  return ids.length ? ids : undefined;
});

const questionBankFilters = computed(() => ({
  name: questionBankKeyword.value.trim() || undefined,
  categoryIds: categoryIdsForBucket.value,
}));

const getQuestionPreview = (text?: string) => {
  if (!text) return '暂无内容';
  const plain = text.replace(/\s+/g, ' ').trim();
  return plain.length > 90 ? `${plain.slice(0, 90)}…` : plain;
};

const loadFavoriteSnapshot = async () => {
  try {
    const response = await favoriteApi.listFavorites({ pageNum: 1, pageSize: 500 });
    const ids = response.data?.map((item) => item.id) ?? [];
    favoriteIds.value = new Set(ids);
  } catch (error) {
    console.error('获取收藏列表失败:', error);
  }
};

const isFavorite = (questionId: number) => favoriteIds.value.has(questionId);

const toggleFavorite = async (question: Question) => {
  const targetId = question.id;
  if (favoriteLoading.value[targetId]) return;
  favoriteLoading.value = { ...favoriteLoading.value, [targetId]: true };
  try {
    if (isFavorite(targetId)) {
      await favoriteApi.removeFavorite(targetId);
      favoriteIds.value = new Set([...favoriteIds.value].filter((id) => id !== targetId));
    } else {
      await favoriteApi.addFavorite(targetId);
      favoriteIds.value = new Set([...favoriteIds.value, targetId]);
    }
  } catch (error) {
    console.error('更新收藏状态失败:', error);
  } finally {
    favoriteLoading.value = { ...favoriteLoading.value, [targetId]: false };
  }
};

const isAnswerExpanded = (id: number) => !!expandedAnswers.value[id];
const toggleAnswer = (id: number) => {
  expandedAnswers.value = { ...expandedAnswers.value, [id]: !expandedAnswers.value[id] };
};

const mergeQuestionDetails = async (list: Question[]) => {
  if (!list.length) return list;
  const detailed = await Promise.all(
    list.map(async (item) => {
      try {
        const detail = await questionApi.getQuestionDetail(item.id);
        const data = detail.data ?? {};
        return { ...item, ...data };
      } catch (error) {
        console.error('获取题目详情失败:', item.id, error);
        return item;
      }
    })
  );
  return detailed;
};

const handleQuestionSearch = async () => {
  const keyword = questionKeyword.value.trim();
  if (!keyword) {
    questionResults.value = [];
    return;
  }
  questionSearchLoading.value = true;
  try {
    const response = await questionApi.getQuestions({
      keyword,
      pageNo: 1,
      pageSize: 10,
      ...(categoryIdsForBucket.value ? { categoryIds: categoryIdsForBucket.value } : {}),
    });
    const baseList = response.data ?? [];
    const withAnswer = await mergeQuestionDetails(baseList);
    questionResults.value = withAnswer;
    // 更新收藏状态快照，确保按钮状态正确
    loadFavoriteSnapshot();
  } catch (error) {
    console.error('搜索题目失败:', error);
    questionResults.value = [];
  } finally {
    questionSearchLoading.value = false;
  }
};

const handleQuestionBankSearch = () => {
  questionBankKeyword.value = questionBankKeyword.value.trim();
};

// 处理题库选择（预留）
const handleQuestionBankSelect = (_questionBank: unknown) => {};

const handleQuestionBanksLoad = (_questionBanks: unknown[]) => {};
</script>

<template>
  <a-space direction="vertical" size="large" class="page-stack">
    <!-- 固定六大类：与 AI 面试官一致 -->
    <div class="category-section">
      <div class="category-toolbar">
        <span class="category-toolbar-label">岗位分类</span>
        <a-radio-group v-model:value="selectedBucket" button-style="solid" size="middle" class="bucket-radio-group">
          <a-radio-button v-for="opt in JOB_BUCKET_OPTIONS" :key="opt.key" :value="opt.key">
            {{ opt.label }}
          </a-radio-button>
        </a-radio-group>
        <a-tag color="processing" class="category-summary-tag">{{ currentCategorySummary }}</a-tag>
      </div>
    </div>

    <a-card :bordered="false" class="search-card">
      <div class="search-grid">
        <div class="search-item">
          <div class="search-label">搜索题库</div>
          <a-input-search
            v-model:value="questionBankKeyword"
            placeholder="输入题库名称关键词"
            allow-clear
            enter-button="搜索"
            @search="handleQuestionBankSearch"
          />
        </div>
        <div class="search-item">
          <div class="search-label">搜索题目</div>
          <a-input-search
            v-model:value="questionKeyword"
            placeholder="输入题目关键词"
            allow-clear
            enter-button="搜索"
            @search="handleQuestionSearch"
          />
        </div>
      </div>
    </a-card>

    <a-card
      v-if="questionKeyword || questionResults.length > 0"
      title="题目搜索结果"
      :bordered="false"
      class="question-result-card"
    >
      <a-spin :spinning="questionSearchLoading">
        <div v-if="questionResults.length" class="question-result-list">
          <div v-for="question in questionResults" :key="question.id" class="question-result-item">
            <div class="question-content">{{ getQuestionPreview(question.content) }}</div>
            <div class="question-answer">
              <span class="answer-label">答案：</span>
              <span class="answer-text">
                {{ isAnswerExpanded(question.id) ? (question.answer || '暂无内容') : getQuestionPreview(question.answer) }}
              </span>
              <a-button
                v-if="(question.answer || '').length > 90"
                type="link"
                size="small"
                @click="toggleAnswer(question.id)"
              >
                {{ isAnswerExpanded(question.id) ? '收起' : '展开' }}
              </a-button>
            </div>
            <div class="question-meta">
              <span>难度: {{ question.difficulty || '未知' }}</span>
              <a-button
                size="small"
                type="link"
                :loading="favoriteLoading[question.id]"
                @click="toggleFavorite(question)"
              >
                {{ isFavorite(question.id) ? '取消收藏' : '收藏题目' }}
              </a-button>
            </div>
          </div>
        </div>
        <a-empty v-else description="暂无匹配题目" />
      </a-spin>
    </a-card>

    <a-card title="面试题库" :bordered="false">
      <QuestionBankList
        :show-loading="true"
        :pagination="questionBankFilters"
        :category-names="categoryNamesRecord"
        @select="handleQuestionBankSelect"
        @load="handleQuestionBanksLoad"
      />
    </a-card>
  </a-space>
</template>

<style scoped>
.page-stack {
  width: 100%;
  padding: 24px 0;
}

.category-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.category-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px 16px;
}

.bucket-radio-group {
  flex: 1;
  min-width: 0;
}

.bucket-radio-group :deep(.ant-radio-button-wrapper) {
  margin-bottom: 8px;
}

.category-toolbar-label {
  font-weight: 600;
  color: #1a1a1a;
}

.category-summary-tag {
  margin: 0;
  font-size: 13px;
}

.search-hint {
  font-size: 13px;
  color: #8c8c8c;
  margin-bottom: 12px;
  line-height: 1.5;
}

.search-card {
  border: 1px solid #f0f0f0;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.03);
  border-radius: 12px;
}

.search-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 16px;
}

.search-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.search-label {
  font-weight: 600;
  color: #1a1a1a;
}

.question-result-card {
  border: 1px solid #f0f0f0;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.03);
  border-radius: 12px;
}

.question-result-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.question-result-item {
  padding: 12px 14px;
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  background: #fafafa;
}

.question-content {
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 6px;
}

.question-answer {
  color: #333;
  margin-bottom: 6px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.answer-label {
  font-weight: 600;
}

.answer-text {
  white-space: pre-wrap;
}

.question-meta {
  color: #666;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 卡片样式美化 */
:deep(.ant-card) {
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  border: 1px solid #f0f0f0;
}

:deep(.ant-card-head) {
  background-color: #fafafa;
  border-bottom: 1px solid #f0f0f0;
  border-radius: 12px 12px 0 0;
  padding: 16px 24px;
}

:deep(.ant-card-head-title) {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
}

:deep(.ant-card-body) {
  padding: 24px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .page-stack {
    padding: 16px 0;
  }

  .search-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }
  
  :deep(.ant-card-head) {
    padding: 12px 16px;
  }
  
  :deep(.ant-card-body) {
    padding: 16px;
  }
  
  :deep(.ant-card-head-title) {
    font-size: 16px;
  }
}

@media (max-width: 480px) {
  .page-stack {
    padding: 8px 0;
  }

  .question-result-item {
    padding: 10px 12px;
  }
  
  :deep(.ant-card-head) {
    padding: 10px 12px;
  }
  
  :deep(.ant-card-body) {
    padding: 12px;
  }
}

/* 小屏：搜索框与按钮纵向排列，避免挤压 */
@media (max-width: 576px) {
  :deep(.search-card .ant-input-search .ant-input-group) {
    display: flex;
    flex-direction: column;
    width: 100%;
  }

  :deep(.search-card .ant-input-search .ant-input-affix-wrapper) {
    border-radius: 8px 8px 0 0;
  }

  :deep(.search-card .ant-input-search .ant-input-group-addon) {
    width: 100%;
  }

  :deep(.search-card .ant-input-search .ant-input-search-button) {
    width: 100%;
    border-radius: 0 0 8px 8px;
  }

  .question-answer {
    flex-wrap: wrap;
    align-items: flex-start;
  }

  .question-meta {
    flex-wrap: wrap;
  }
}
</style>
