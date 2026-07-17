<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { questionApi } from '@/services/question'
import { favoriteApi } from '@/services/favorites'
import { message } from 'ant-design-vue'
import { BookOutlined, FileTextOutlined, LeftOutlined, RightOutlined, HeartFilled } from '@ant-design/icons-vue'
import MarkdownIt from 'markdown-it'
import katex from 'katex'
import 'katex/dist/katex.css'
import type { Question, GetQuestionsParams } from '@/services/question'

// 初始化Markdown渲染器
const md = new MarkdownIt({
  html: true,
  linkify: true,
  typographer: true,
});

// 扩展MarkdownIt以支持KaTeX
md.use((md) => {
  const defaultRender =
    md.renderer.rules.fence ??
    ((tokens, idx, options, env, self) => self.renderToken(tokens, idx, options));
  md.renderer.rules.fence = (tokens, idx, options, env, self) => {
    const token = tokens[idx];
    if (!token) {
      return defaultRender(tokens, idx, options, env, self);
    }
    if (token.info === 'math') {
      try {
        return `<div class="katex-display">${katex.renderToString(token.content, { 
          throwOnError: false
        })}</div>`;
      } catch (e) {
        console.error('KaTeX rendering error:', e);
        return `<div class="math-error">${token.content}</div>`;
      }
    }
    return defaultRender(tokens, idx, options, env, self);
  }

  // 处理行内和块级公式
  md.core.ruler.after('inline', 'math', (state) => {
    const tokens = state.tokens;
    for (let i = 0; i < tokens.length; i++) {
      const inlineToken = tokens[i];
      if (!inlineToken) continue;
      if (inlineToken.type === 'inline') {
        let current = 0;
        const newTokens: any[] = [];
        const content = inlineToken.content || '';
        
        while (current < content.length) {
          const dollarIndex = content.indexOf('$', current);
          if (dollarIndex === -1) {
            // 没有更多的$，添加剩余内容
            if (current < content.length) {
              newTokens.push({
                type: 'text',
                content: content.slice(current)
              });
            }
            break;
          }
          
          // 检查是否是双美元符号$$
          if (content[dollarIndex + 1] === '$') {
            // 是块级公式
            const endIndex = content.indexOf('$$', dollarIndex + 2);
            if (endIndex === -1) {
              // 没有配对的$$，将$$视为普通文本
              newTokens.push({
                type: 'text',
                content: '$$'
              });
              current = dollarIndex + 2;
              continue;
            }
            
            // 添加$$前面的内容
            if (dollarIndex > current) {
              newTokens.push({
                type: 'text',
                content: content.slice(current, dollarIndex)
              });
            }
            
            // 提取公式内容
            const mathContent = content.slice(dollarIndex + 2, endIndex).trim();
            try {
              // 渲染KaTeX块级公式
              const renderedMath = katex.renderToString(mathContent, {
                throwOnError: false,
                displayMode: true
              });
              newTokens.push({
                type: 'html_inline',
                content: `<div class="katex-display">${renderedMath}</div>`
              });
            } catch (e) {
              console.error('KaTeX block rendering error:', e);
              // 渲染失败时显示原始公式
              newTokens.push({
                type: 'text',
                content: `$$${mathContent}$$`
              });
            }
            
            current = endIndex + 2;
          } else {
            // 是行内公式
            // 添加$前面的内容
            if (dollarIndex > current) {
              newTokens.push({
                type: 'text',
                content: content.slice(current, dollarIndex)
              });
            }
            
            // 查找配对的$
            const endIndex = content.indexOf('$', dollarIndex + 1);
            if (endIndex === -1) {
              // 没有配对的$，将$视为普通文本
              newTokens.push({
                type: 'text',
                content: '$'
              });
              current = dollarIndex + 1;
              continue;
            }
            
            // 提取公式内容
            const mathContent = content.slice(dollarIndex + 1, endIndex);
            try {
              // 渲染KaTeX行内公式
              const renderedMath = katex.renderToString(mathContent, {
                throwOnError: false,
                displayMode: false
              });
              newTokens.push({
                type: 'html_inline',
                content: renderedMath
              });
            } catch (e) {
              console.error('KaTeX inline rendering error:', e);
              // 渲染失败时显示原始公式
              newTokens.push({
                type: 'text',
                content: `$${mathContent}$`
              });
            }
            
            current = endIndex + 1;
          }
        }
        
        // 替换原始token的子token
        if (newTokens.length > 0) {
          inlineToken.children = newTokens;
          inlineToken.content = '';
        }
      }
    }
    return false;
  });
});

// 获取路由参数
const route = useRoute()
const router = useRouter()
const bankId = Number(route.query.bankId || route.params.bankId)
const questionId = Number(route.query.questionId || route.params.questionId)

// 问题列表数据
const questions = ref<Question[]>([])
// 当前问题详情
const currentQuestion = ref<Question | null>(null)
// 加载状态
const loading = ref(false)
const questionLoading = ref(false)
// 收藏状态
const favoriting = ref(false)
// 搜索参数
const searchParams = ref<GetQuestionsParams>({
  bankId,
  pageSize: 100 // 加载该题库下所有问题
})
// 搜索关键词
const keyword = ref('')

// 统计数据
const totalQuestions = computed(() => questions.value.length)
const currentIndex = computed(() => {
  if (!currentQuestion.value) return -1
  return questions.value.findIndex(q => q.id === currentQuestion.value?.id)
})
const viewingPosition = computed(() => (currentIndex.value >= 0 ? currentIndex.value + 1 : 0))
const viewingPercent = computed(() => {
  if (!totalQuestions.value || viewingPosition.value === 0) return 0
  return Math.round((viewingPosition.value / totalQuestions.value) * 100)
})

// 渲染Markdown内容的计算属性
const renderedQuestion = computed(() => {
  if (!currentQuestion.value?.content) return '';
  return md.render(currentQuestion.value.content);
});

const renderedAnswer = computed(() => {
  if (!currentQuestion.value?.answer) return '';
  return md.render(currentQuestion.value.answer);
});

const getQuestionPreview = (text?: string) => {
  if (!text) return '暂无内容';
  const plain = text
    .replace(/[#>*`]/g, '')
    .replace(/\n+/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
  return plain.length > 80 ? `${plain.slice(0, 80)}…` : plain;
};

// 获取问题列表
const loadQuestions = async () => {
  if (!bankId) return

  loading.value = true
  try {
    const params: GetQuestionsParams = {
      ...searchParams.value,
      keyword: keyword.value
    }
    const response = await questionApi.getQuestions(params)
    const questionList = response.data ?? []
    questions.value = questionList
    
    const initialQuestionId = Number(questionId) || undefined
    if (initialQuestionId) {
      loadQuestionDetail(initialQuestionId)
    } else {
      const firstQuestion = questionList[0]
      if (firstQuestion) {
        loadQuestionDetail(firstQuestion.id)
      }
    }
  } catch (error) {
    console.error('加载问题列表失败:', error)
    message.error('加载问题列表失败')
  } finally {
    loading.value = false
  }
}

// 获取问题详情
const loadQuestionDetail = async (id: number) => {
  questionLoading.value = true
  try {
    const response = await questionApi.getQuestionDetail(id)
    currentQuestion.value = response.data
  } catch (error) {
    console.error('加载问题详情失败:', error)
    message.error('加载问题详情失败')
  } finally {
    questionLoading.value = false
  }
}

// 处理问题点击
const handleQuestionClick = (question: Question) => {
  // 更新URL参数，但不重新加载页面
  router.push({
    name: 'Question',
    query: {
      bankId,
      questionId: question.id
    }
  })
  // 加载选中问题的详情
  loadQuestionDetail(question.id)
}

// 处理搜索
const handleSearch = () => {
  loadQuestions()
}

// 处理上一题
const handlePrevQuestion = () => {
  if (!currentQuestion.value || questions.value.length === 0) return
  
  const currentIndex = questions.value.findIndex(q => q.id === currentQuestion.value?.id)
  if (currentIndex > 0) {
    const prevQuestion = questions.value[currentIndex - 1]
    if (!prevQuestion) return
    router.push({
      name: 'Question',
      query: {
        bankId,
        questionId: prevQuestion.id
      }
    })
    loadQuestionDetail(prevQuestion.id)
  }
}

// 处理下一题
const handleNextQuestion = () => {
  if (!currentQuestion.value || questions.value.length === 0) return
  
  const currentIndex = questions.value.findIndex(q => q.id === currentQuestion.value?.id)
  if (currentIndex < questions.value.length - 1) {
    const nextQuestion = questions.value[currentIndex + 1]
    if (!nextQuestion) return
    router.push({
      name: 'Question',
      query: {
        bankId,
        questionId: nextQuestion.id
      }
    })
    loadQuestionDetail(nextQuestion.id)
  }
}

const handleBack = () => {
  if (window.history.length > 1) {
    router.back()
    return
  }
  if (Number.isFinite(bankId) && bankId > 0) {
    router.push({ name: 'Bank', params: { id: bankId } })
    return
  }
  router.push({ name: 'QuestionBank' })
}

// 获取当前问题的上一个和下一个问题
const getNavigationQuestions = computed(() => {
  if (!currentQuestion.value || questions.value.length === 0) {
    return { prev: null, next: null }
  }
  
  const currentIndex = questions.value.findIndex(q => q.id === currentQuestion.value?.id)
  
  return {
    prev: currentIndex > 0 ? questions.value[currentIndex - 1] : null,
    next: currentIndex < questions.value.length - 1 ? questions.value[currentIndex + 1] : null
  }
})

// 处理收藏/取消收藏
const handleToggleFavorite = async () => {
  if (!currentQuestion.value) return
  favoriting.value = true
  try {
    if (currentQuestion.value.isFavorite) {
      await favoriteApi.removeFavorite(currentQuestion.value.id)
      message.success('已取消收藏')
      currentQuestion.value.isFavorite = false
    } else {
      await favoriteApi.addFavorite(currentQuestion.value.id)
      message.success('已收藏')
      currentQuestion.value.isFavorite = true
    }
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : '操作失败'
    message.error(errorMessage)
  } finally {
    favoriting.value = false
  }
}

// 组件挂载时加载数据
onMounted(() => {
  loadQuestions()
})
</script>

<template>
  <div class="question-page">
    <section class="page-header">
      <div class="header-main">
        <div class="back-row">
          <a-button class="back-btn" @click="handleBack">
            <template #icon>
              <LeftOutlined />
            </template>
            返回题库
          </a-button>
        </div>
        <p class="eyebrow">offerDog 题库</p>
        <h1>面试题库</h1>
        <p class="subtitle">精炼版题目与答案，便于快速复习与模拟面试。</p>
        <div class="progress-row">
          <div class="progress-copy">
            <span>进度</span>
            <strong>{{ viewingPosition || 0 }}/{{ totalQuestions }}</strong>
          </div>
          <div class="progress-bar">
            <span class="progress-fill" :style="{ width: `${viewingPercent}%` }"></span>
          </div>
        </div>
      </div>
      <div class="header-stats">
        <div class="stat-card">
          <span class="label">题目总数</span>
          <span class="value">{{ totalQuestions }}</span>
        </div>
        <div class="stat-card">
          <span class="label">正在查看</span>
          <span class="value">{{ viewingPosition || '--' }}</span>
          <span class="tagline" v-if="currentQuestion?.tags?.length">
            {{ currentQuestion?.tags?.join('、') }}
          </span>
        </div>
      </div>
    </section>
    <div class="question-grid">
      <aside class="panel-left">
        <a-card class="list-card" :bordered="false">
          <template #title>
            <div class="card-title">
              <BookOutlined />
              <span>题库题目</span>
            </div>
          </template>
          <div class="search-box">
            <a-input-search
              v-model:value="keyword"
              placeholder="搜索题目"
              allow-clear
              size="large"
              enter-button="搜索"
              @search="handleSearch"
            />
          </div>
          <a-spin :spinning="loading">
            <div class="question-list">
              <a-list
                :data-source="questions"
                :locale="{ emptyText: '暂无题目' }"
                :split="false"
              >
                <template #renderItem="{ item }">
                  <a-list-item
                    class="question-list-item"
                    :class="{ active: currentQuestion?.id === item.id }"
                    @click="handleQuestionClick(item)"
                  >
                    <a-list-item-meta>
                      <template #avatar>
                        <FileTextOutlined class="item-icon" />
                      </template>
                      <template #title>
                        <div class="item-title">
                          {{ getQuestionPreview(item.content) }}
                        </div>
                      </template>
                    </a-list-item-meta>
                  </a-list-item>
                </template>
              </a-list>
            </div>
          </a-spin>
        </a-card>
      </aside>
      <section class="panel-right">
        <a-spin :spinning="questionLoading">
          <template v-if="currentQuestion">
            <div class="detail-stack">
              <a-card class="info-card" :bordered="false">
                <template #title>
                  <div class="card-title">
                    <FileTextOutlined />
                    <span>题目</span>
                  </div>
                </template>
                <div v-if="currentQuestion?.tags?.length" class="tag-group">
                  <a-tag color="blue" v-for="tag in currentQuestion.tags" :key="tag">
                    {{ tag }}
                  </a-tag>
                </div>
                <div class="markdown-body" v-html="renderedQuestion"></div>
                <div class="favorite-action">
                  <a-button
                    type="link"
                    danger
                    :loading="favoriting"
                    @click="handleToggleFavorite"
                  >
                    <HeartFilled />
                    {{ currentQuestion?.isFavorite ? '取消收藏' : '收藏' }}
                  </a-button>
                </div>
              </a-card>
              <a-card class="info-card" :bordered="false">
                <template #title>
                  <div class="card-title">
                    <BookOutlined />
                    <span>回答重点</span>
                  </div>
                </template>
                <div class="markdown-body" v-html="renderedAnswer"></div>
                <div class="navigation-bar">
                  <a-space>
                    <a-button
                      size="large"
                      :disabled="!getNavigationQuestions.prev"
                      @click="handlePrevQuestion"
                    >
                      <template #icon>
                        <LeftOutlined />
                      </template>
                      上一题
                    </a-button>
                    <a-button
                      type="primary"
                      size="large"
                      :disabled="!getNavigationQuestions.next"
                      @click="handleNextQuestion"
                    >
                      下一题
                      <RightOutlined class="next-icon" />
                    </a-button>
                  </a-space>
                </div>
              </a-card>
            </div>
          </template>
          <a-card v-else class="info-card" :bordered="false">
            <a-empty description="请选择左侧题目" />
          </a-card>
        </a-spin>
      </section>
    </div>
  </div>
</template>

<style scoped>
.question-page {
  --primary: #1677ff;
  --surface: #ffffff;
  --border: #e4e8f1;
  --text: #0f172a;
  --muted: #5b6477;
  background: linear-gradient(180deg, #f7f9fc 0%, #f2f5fb 100%);
  min-height: 100vh;
  padding: 28px 24px 40px;
  color: var(--text);
}

.back-row {
  margin-bottom: 10px;
}

.back-btn {
  border-radius: 10px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px;
  border-radius: 14px;
  background: var(--surface);
  border: 1px solid var(--border);
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.08);
}

.header-main h1 {
  margin: 4px 0;
  font-size: 26px;
  font-weight: 800;
}

.eyebrow {
  margin: 0;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--muted);
}

.subtitle {
  margin: 0 0 8px;
  color: var(--muted);
}

.progress-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.progress-copy {
  display: flex;
  gap: 6px;
  align-items: baseline;
  font-weight: 600;
}

.progress-bar {
  flex: 1;
  height: 8px;
  background: #eef2f8;
  border-radius: 999px;
  overflow: hidden;
}

.progress-fill {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, #1677ff, #52c4f9);
  transition: width 0.25s ease;
}

.header-stats {
  display: flex;
  gap: 10px;
  align-items: stretch;
}

.stat-card {
  min-width: 140px;
  padding: 10px 12px;
  border-radius: 12px;
  border: 1px solid var(--border);
  background: #f8fafc;
  display: grid;
  gap: 2px;
}

.label {
  font-size: 12px;
  color: var(--muted);
  letter-spacing: 0.04em;
}

.value {
  font-size: 22px;
  font-weight: 700;
}

.tagline {
  color: var(--muted);
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.question-grid {
  margin-top: 20px;
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 16px;
}

.panel-left {
  position: sticky;
  top: 20px;
}

.list-card,
.info-card {
  border-radius: 14px;
  border: 1px solid var(--border);
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.08);
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 700;
}

.card-title :deep(.anticon) {
  color: var(--primary);
}

.list-card :deep(.ant-card-head),
.info-card :deep(.ant-card-head) {
  border: none;
  padding: 16px 16px 0;
}

.list-card :deep(.ant-card-body),
.info-card :deep(.ant-card-body) {
  padding: 12px 16px 16px;
}

.search-box {
  margin: 12px 0;
}

.question-list {
  max-height: calc(100vh - 240px);
  overflow: auto;
}

.question-list-item {
  margin-bottom: 8px;
  border-radius: 10px;
  border: 1px solid transparent;
  background: #f6f8fc;
  transition: border-color 0.2s ease, background 0.2s ease, transform 0.15s ease;
}

.question-list-item:hover {
  border-color: #c9d7f2;
  transform: translateY(-1px);
}

.question-list-item.active {
  border-color: var(--primary);
  background: #e9f2ff;
}

.item-title {
  color: var(--text);
  font-weight: 600;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.item-icon {
  font-size: 18px;
  color: var(--primary);
  background: #edf3ff;
  padding: 8px;
  border-radius: 10px;
}

.detail-stack {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.tag-group {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

.tag-group :deep(.ant-tag) {
  border-radius: 8px;
}

.markdown-body {
  font-size: 15px;
  line-height: 1.7;
  color: var(--text);
}

.markdown-body :deep(p) {
  margin-bottom: 10px;
}

.markdown-body :deep(code) {
  background: #0f172a;
  color: #e5edff;
  padding: 2px 6px;
  border-radius: 6px;
  font-size: 13px;
}

.markdown-body :deep(pre) {
  background: #0f172a;
  color: #e5edff;
  border-radius: 10px;
  padding: 14px;
  overflow-x: auto;
}

.favorite-action {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--border);
  display: flex;
  justify-content: flex-start;
}

.favorite-action :deep(.ant-btn) {
  border-radius: 10px;
}

.navigation-bar {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--border);
  display: flex;
  justify-content: flex-end;
}

.navigation-bar :deep(.ant-btn) {
  border-radius: 10px;
}

@media (max-width: 1100px) {
  .question-grid {
    grid-template-columns: 1fr;
  }

  .panel-left {
    position: static;
  }
}

@media (max-width: 720px) {
  .question-page {
    padding: 20px 16px 32px;
  }

  .page-header {
    flex-direction: column;
  }

  .header-stats {
    width: 100%;
  }
}
</style>
