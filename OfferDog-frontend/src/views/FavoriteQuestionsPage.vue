<script setup lang="ts">
import { HeartFilled, LeftOutlined, RightOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { MarkdownRender } from 'vue-renderer-markdown'

import { favoriteApi, type FavoriteQuestion } from '@/services/favorites'
import { questionApi, type Question } from '@/services/question'

const router = useRouter()
const favorites = ref<FavoriteQuestion[]>([])
const loading = ref(false)
const removing = ref<Record<number, boolean>>({})
const detailLoading = ref(false)
const selectedId = ref<number | null>(null)
const selectedDetail = ref<Question | null>(null)

const pagination = ref({
  current: 1,
  pageSize: 10,
  total: 0,
})

const renderedContent = computed(() => selectedDetail.value?.content ?? '')
const renderedAnswer = computed(() => selectedDetail.value?.answer ?? '')

const getDifficultyTag = (difficulty: string) => {
  const map: Record<string, { color: string; text: string }> = {
    EASY: { color: 'green', text: '简单' },
    MEDIUM: { color: 'orange', text: '中等' },
    HARD: { color: 'red', text: '困难' },
    easy: { color: 'green', text: '简单' },
    medium: { color: 'orange', text: '中等' },
    hard: { color: 'red', text: '困难' },
  }
  return map[difficulty] || { color: 'default', text: difficulty }
}

const loadDetail = async (id: number | null | undefined) => {
  if (!id) {
    selectedDetail.value = null
    selectedId.value = null
    return
  }
  detailLoading.value = true
  try {
    const response = await questionApi.getQuestionDetail(id)
    selectedDetail.value = response.data ?? null
    selectedId.value = id
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : '获取题目详情失败'
    message.error(errorMessage)
  } finally {
    detailLoading.value = false
  }
}

const fetchFavorites = async () => {
  loading.value = true
  try {
    const response = await favoriteApi.listFavorites({
      pageNum: pagination.value.current,
      pageSize: pagination.value.pageSize,
    })
    favorites.value = response.data ?? []
    pagination.value.total = response.totalCount ?? 0

    if (favorites.value.length > 0) {
      const first = favorites.value[0]
      if (!selectedId.value || !favorites.value.find((f) => f.id === selectedId.value)) {
        if (first) {
          await loadDetail(first.id)
        }
      }
    } else {
      selectedDetail.value = null
      selectedId.value = null
    }
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : '获取收藏列表失败'
    message.error(errorMessage)
  } finally {
    loading.value = false
  }
}

const handleRemove = async (question: FavoriteQuestion) => {
  if (removing.value[question.id]) return
  removing.value[question.id] = true
  try {
    await favoriteApi.removeFavorite(question.id)
    message.success('已取消收藏')
    const afterRemoveCount = favorites.value.length - 1
    if (afterRemoveCount === 0 && pagination.value.current > 1) {
      pagination.value.current -= 1
    }
    await fetchFavorites()
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : '取消收藏失败'
    message.error(errorMessage)
  } finally {
    removing.value[question.id] = false
  }
}

const handlePageChange = (page: number, pageSize?: number) => {
  pagination.value.current = page
  if (pageSize) {
    pagination.value.pageSize = pageSize
  }
  fetchFavorites()
}

const handleSelect = (question: FavoriteQuestion) => {
  if (selectedId.value === question.id) return
  loadDetail(question.id)
}

const handlePrev = () => {
  if (!selectedId.value) return
  const idx = favorites.value.findIndex((f) => f.id === selectedId.value)
  if (idx > 0) {
    const prev = favorites.value[idx - 1]
    if (prev) {
      loadDetail(prev.id)
    }
  }
}

const handleNext = () => {
  if (!selectedId.value) return
  const idx = favorites.value.findIndex((f) => f.id === selectedId.value)
  if (idx >= 0 && idx < favorites.value.length - 1) {
    const next = favorites.value[idx + 1]
    if (next) {
      loadDetail(next.id)
    }
  }
}

const goBank = (bankId: number) => {
  router.push(`/bank/${bankId}`)
}

onMounted(() => {
  fetchFavorites()
})
</script>

<template>
  <a-space direction="vertical" size="large" class="favorite-page">
    <a-row :gutter="[16, 16]">
      <a-col :xs="24" :lg="9">
        <a-card title="我的收藏题目" :bordered="false">
          <a-list
            :data-source="favorites"
            :loading="loading"
            item-layout="vertical"
            :split="true"
            class="favorite-list"
            :locale="{ emptyText: '还没有收藏的题目，去题库逛逛吧~' }"
          >
            <template #renderItem="{ item }">
              <a-list-item
                :class="{ active: selectedId === item.id }"
                @click="() => handleSelect(item)"
              >
                <a-list-item-meta>
                  <template #title>
                    <div class="favorite-header">
                      <div class="favorite-title">{{ item.content }}</div>
                      <a-button
                        type="link"
                        danger
                        :loading="removing[item.id]"
                        @click.stop="() => handleRemove(item)"
                      >
                        <HeartFilled />
                        取消收藏
                      </a-button>
                    </div>
                  </template>
                  <template #description>
                    <div class="favorite-meta">
                      <a-tag :color="getDifficultyTag(item.difficulty).color">
                        {{ getDifficultyTag(item.difficulty).text }}
                      </a-tag>
                      <span class="favorite-time">收藏时间：{{ item.createdAt || '--' }}</span>
                    </div>
                    <div class="favorite-tags">
                      <span>标签：</span>
                      <a-space wrap>
                        <a-tag v-for="tag in item.tags" :key="tag" color="blue">
                          {{ tag }}
                        </a-tag>
                        <span v-if="!item.tags || item.tags.length === 0">暂无</span>
                      </a-space>
                    </div>
                  </template>
                </a-list-item-meta>
              </a-list-item>
            </template>
          </a-list>

          <div class="pagination-wrapper" v-if="pagination.total > 0">
            <a-pagination
              :current="pagination.current"
              :page-size="pagination.pageSize"
              :total="pagination.total"
              :page-size-options="['5','10','20','50']"
              show-size-changer
              show-quick-jumper
              :show-total="(total:number) => `共 ${total} 条`"
              @change="handlePageChange"
              @show-size-change="handlePageChange"
            />
          </div>
        </a-card>
      </a-col>
      <a-col :xs="24" :lg="15">
        <a-card :bordered="false" class="detail-card">
          <template #title>
            <div class="detail-title-bar">
              <div class="title-text">{{ selectedDetail ? '题目详情' : '选择一个收藏查看详情' }}</div>
              <div v-if="selectedDetail" class="detail-actions">
                <a-tag :color="getDifficultyTag(selectedDetail.difficulty).color">
                  {{ getDifficultyTag(selectedDetail.difficulty).text }}
                </a-tag>
                <a-space size="small">
                  <a-button size="small" @click="handlePrev" :disabled="!favorites.length || favorites[0]?.id === selectedId">
                    <LeftOutlined /> 上一题
                  </a-button>
                  <a-button size="small" @click="handleNext" :disabled="!favorites.length || favorites[favorites.length - 1]?.id === selectedId">
                    下一题 <RightOutlined />
                  </a-button>
                </a-space>
              </div>
            </div>
          </template>
          <a-spin :spinning="detailLoading">
            <div v-if="selectedDetail" class="detail-wrapper">
              <div class="detail-section">
                <h3>题目</h3>
                <MarkdownRender class="markdown-body" :content="renderedContent || '暂无内容'" />
              </div>
              <div class="detail-section">
                <h3>参考答案</h3>
                <MarkdownRender class="markdown-body" :content="renderedAnswer || '暂无内容'" />
              </div>
            </div>
            <a-empty v-else description="请选择左侧列表中的题目" />
          </a-spin>
        </a-card>
      </a-col>
    </a-row>
  </a-space>
</template>

<style scoped>
.favorite-page {
  width: 100%;
}

.favorite-list :deep(.ant-list-item) {
  padding: 12px 0;
  cursor: pointer;
}

.favorite-list :deep(.ant-list-item.active) {
  background: #f6ffed;
}

.favorite-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f1f1f;
  line-height: 1.6;
}

.favorite-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 8px;
}

.favorite-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 6px;
  margin-bottom: 6px;
  color: rgba(0, 0, 0, 0.45);
}

.favorite-tags {
  display: flex;
  align-items: center;
  gap: 8px;
  color: rgba(0, 0, 0, 0.65);
}

.favorite-time {
  font-size: 12px;
}

.pagination-wrapper {
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

.detail-card :deep(.ant-card-head) {
  padding: 0 16px;
}

.detail-title-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.title-text {
  font-size: 16px;
  font-weight: 600;
}

.detail-wrapper {
  min-height: 260px;
}

.detail-section {
  margin-bottom: 16px;
}

.detail-section h3 {
  margin-bottom: 8px;
  font-size: 15px;
  font-weight: 600;
}

.detail-content {
  padding: 0;
  min-height: 80px;
  line-height: 1.8;
  word-break: break-word;
}

.markdown-body :deep(*) {
  line-height: 1.8;
  font-size: 14px;
}

@media (max-width: 768px) {
  .favorite-page {
    padding: 12px 0;
  }

  .favorite-title {
    font-size: 14px;
  }

  .favorite-meta {
    gap: 8px;
    font-size: 12px;
  }

  .detail-card :deep(.ant-card.head) {
    padding: 12px 16px;
  }

  .detail-title-bar {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .title-text {
    font-size: 14px;
  }

  .detail-section h3 {
    font-size: 14px;
  }

  .detail-wrapper {
    min-height: 200px;
  }

  :deep(.ant-card.head) {
    padding: 12px 16px;
  }

  :deep(.ant-card.body) {
    padding: 16px;
  }

  .pagination-wrapper {
    justify-content: center;
  }

  :deep(.ant-pagination) {
    flex-wrap: wrap;
    justify-content: center;
  }

  :deep(.ant-pagination-item),
  :deep(.ant-pagination-prev),
  :deep(.ant-pagination-next) {
    margin: 4px 8px;
  }

  :deep(.ant-pagination-options) {
    flex-wrap: wrap;
    justify-content: center;
    margin-top: 8px;
  }
}

@media (max-width: 480px) {
  .favorite-page {
    padding: 8px 0;
  }

  .favorite-title {
    font-size: 13px;
  }

  .favorite-meta {
    font-size: 11px;
  }

  .detail-card :deep(.ant-card-head) {
    padding: 10px 12px;
  }

  .title-text {
    font-size: 13px;
  }

  .detail-section h3 {
    font-size: 13px;
  }

  .detail-wrapper {
    min-height: 160px;
  }

  :deep(.ant-card-head) {
    padding: 10px 12px;
  }

  :deep(.ant-card-body) {
    padding: 12px;
  }

  .pagination-wrapper {
    margin-top: 12px;
  }

  :deep(.ant-pagination) {
    font-size: 12px;
  }

  :deep(.ant-pagination-item) {
    min-width: 32px;
    height: 32px;
    line-height: 32px;
  }

  :deep(.ant-pagination-prev),
  :deep(.ant-pagination-next) {
    min-width: 32px;
    height: 32px;
    line-height: 32px;
  }

  :deep(.ant-pagination-options) {
    flex-direction: column;
    gap: 8px;
  }
}
</style>
