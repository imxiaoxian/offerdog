<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, ref } from 'vue'
import { categoryApi } from '../services/category'
import type { CategoryTreeNode } from '../services/category'

// 定义组件属性
const props = defineProps<{
  // 可选的预定义分类列表
  categories?: CategoryTreeNode[]
  // 是否显示热门标签
  showHot?: boolean
  // 是否显示"全部"标签
  showAll?: boolean
}>()

// 定义组件事件
const emit = defineEmits<{
  // 点击标签时触发
  (e: 'select', category: CategoryTreeNode | { id: string | number; name: string }): void
  // 加载完成时触发
  (e: 'load', categories: CategoryTreeNode[]): void
}>()

// 完整的分类数据（树形结构）
const categoryData = ref<CategoryTreeNode[]>([])
// 加载状态
const loading = ref(false)
// 当前选中的一级分类ID，使用'special'字符串标识热门/全部状态
const selectedCategoryId = ref<number | null | 'special'>('special')
// 当前选中的二级标签ID
const selectedLevel2TagId = ref<number | null>(null)
// 一级标签容器引用
const level1ScrollRef = ref<HTMLElement | null>(null)

// 判断是否处于显示所有二级标签的状态（热门或全部）
const isShowAllLevel2Tags = () => {
  return selectedCategoryId.value === 'special'
}

// 判断某个分类是否被选中
const isCategorySelected = (categoryId: number) => {
  return selectedCategoryId.value === categoryId
}

// 判断某个二级标签是否被选中
const isLevel2TagSelected = (tagId: number) => {
  return selectedLevel2TagId.value === tagId
}

// 加载分类数据
const loadCategories = async () => {
  if (props.categories) {
    categoryData.value = [...props.categories]
    emit('load', props.categories)
    return
  }

  loading.value = true
  try {
    const response = await categoryApi.getCategoryTree()
    // 保存完整的树形结构数据
    categoryData.value = response.data
    emit('load', categoryData.value)
  } catch (error) {
    console.error('加载分类失败:', error)
  } finally {
    loading.value = false
  }
}

// 获取纯一级分类（不含特殊标签）
const getPureLevel1Categories = () => {
  return categoryData.value.filter((cat) => cat.level === 1)
}

// 获取需要显示的一级分类（最多显示8个）
const getDisplayLevel1Categories = () => {
  return getPureLevel1Categories().slice(0, 8)
}

// 获取某个一级分类的所有二级子分类
const getLevel2Categories = (parentId: number): CategoryTreeNode[] => {
  const parent = categoryData.value.find((cat) => cat.id === parentId)
  return parent?.children || []
}

// 处理标签点击
const handleTagClick = (category: CategoryTreeNode | { id: string | number; name: string }) => {
  // 二级岗位：再次点击已选项则取消筛选，通知父级回到「全部」
  if ('level' in category && category.level === 2 && typeof category.id === 'number') {
    const wasSelected = selectedLevel2TagId.value === category.id
    selectedLevel2TagId.value = wasSelected ? null : category.id
    if (wasSelected) {
      emit('select', { id: 'all', name: '全部' })
      return
    }
    emit('select', category)
    return
  }

  selectedLevel2TagId.value = null

  // 如果点击的是特殊标签（热门或全部）
  if ('id' in category && category.id === 'hot') {
    // 热门标签仍显示热门二级标签
    selectedCategoryId.value = 'special'
  } else if ('id' in category && category.id === 'all') {
    // 全部标签显示一级标签
    selectedCategoryId.value = 'special'
  }
  // 如果点击的是一级分类，更新选中状态
  else if ('level' in category && category.level === 1 && typeof category.id === 'number') {
    selectedCategoryId.value = selectedCategoryId.value === category.id ? null : category.id

    // 如果选中了一级分类，自动选择该分类下的第一条二级标签
    if (selectedCategoryId.value === category.id) {
      const level2Categories = getLevel2Categories(category.id)
      const firstChild = level2Categories[0]
      if (firstChild) {
        selectedLevel2TagId.value = firstChild.id
        // 发送二级标签选择事件
        emit('select', firstChild)
        return
      }
    }
  }
  emit('select', category)
}

// 实现拖拽与滚轮的平滑滚动
let isDragging = false
let lastX = 0
let velocity = 0
let animationFrameId: number | null = null

const scrollContainer = ref<HTMLElement | null>(null)

const stopMomentum = () => {
  if (animationFrameId !== null) {
    cancelAnimationFrame(animationFrameId)
    animationFrameId = null
  }
}

const momentumStep = () => {
  const container = scrollContainer.value
  if (!container) return

  const maxScroll = container.scrollWidth - container.clientWidth
  container.scrollLeft = Math.min(maxScroll, Math.max(0, container.scrollLeft + velocity))
  velocity *= 0.92

  if (Math.abs(velocity) > 0.4) {
    animationFrameId = requestAnimationFrame(momentumStep)
  } else {
    animationFrameId = null
  }
}

const handlePointerDown = (e: PointerEvent) => {
  const container = scrollContainer.value
  if (!container) return
  stopMomentum()
  isDragging = true
  lastX = e.clientX
  velocity = 0
  container.classList.add('dragging')
}

const handlePointerMove = (e: PointerEvent) => {
  if (!isDragging || !scrollContainer.value) return
  const delta = e.clientX - lastX
  scrollContainer.value.scrollLeft -= delta
  velocity = -delta
  lastX = e.clientX
}

const handlePointerUp = () => {
  if (!isDragging) return
  isDragging = false
  scrollContainer.value?.classList.remove('dragging')
  if (Math.abs(velocity) > 0.5) {
    animationFrameId = requestAnimationFrame(momentumStep)
  }
}

const normalizeWheelDelta = (e: WheelEvent, container: HTMLElement) => {
  const rawDelta = Math.abs(e.deltaY) > Math.abs(e.deltaX) ? e.deltaY : e.deltaX
  if (!rawDelta) return 0
  // 在部分浏览器（尤其是生产环境的非 Chrome 环境）滚轮是“行”单位，需要放大到像素
  const pixelDelta =
    e.deltaMode === WheelEvent.DOM_DELTA_LINE
      ? rawDelta * 16
      : e.deltaMode === WheelEvent.DOM_DELTA_PAGE
        ? rawDelta * container.clientWidth
        : rawDelta
  return pixelDelta * 1.35
}

const handleWheel = (e: WheelEvent) => {
  const container = scrollContainer.value
  if (!container) return
  const delta = normalizeWheelDelta(e, container)
  if (delta === 0) return
  e.preventDefault()
  container.scrollLeft += delta
  velocity = delta
  stopMomentum()
  animationFrameId = requestAnimationFrame(momentumStep)
}

const initDragScroll = () => {
  const container = scrollContainer.value
  if (!container) return
  container.addEventListener('pointerdown', handlePointerDown)
  container.addEventListener('pointermove', handlePointerMove)
  container.addEventListener('pointerup', handlePointerUp)
  container.addEventListener('pointerleave', handlePointerUp)
  container.addEventListener('pointercancel', handlePointerUp)
  container.addEventListener('wheel', handleWheel, { passive: false })
}

// 组件挂载时加载数据
onMounted(() => {
  loadCategories()

  nextTick(() => {
    scrollContainer.value = level1ScrollRef.value
    initDragScroll()
  })
})

// 组件卸载时清理事件监听器
onUnmounted(() => {
  const container = scrollContainer.value
  if (container) {
    container.removeEventListener('pointerdown', handlePointerDown)
    container.removeEventListener('pointermove', handlePointerMove)
    container.removeEventListener('pointerup', handlePointerUp)
    container.removeEventListener('pointerleave', handlePointerUp)
    container.removeEventListener('pointercancel', handlePointerUp)
    container.removeEventListener('wheel', handleWheel)
  }

  // 取消动画
  stopMomentum()
})
</script>

<template>
  <div class="category-tags-container">
    <a-spin :spinning="loading">
      <div class="category-content">
        <div class="level1-header">
          <div class="section-title">分类筛选</div>
          <div class="section-hint">拖拽或滚轮可以左右滑动查看</div>
        </div>
        <!-- 顶部一级标签栏 -->
        <div class="level1-container">
          <!-- 固定的全部标签 -->
          <div v-if="props.showAll" class="level1-tag-container fixed-tag">
            <a-tag class="level1-tag all-tag" @click="handleTagClick({ id: 'all', name: '全部' })">
              全部
            </a-tag>
          </div>
          <!-- 固定的热门标签 -->
          <div v-if="props.showHot" class="level1-tag-container fixed-tag">
            <a-tag class="level1-tag hot-tag" @click="handleTagClick({ id: 'hot', name: '热门' })">
              热门
            </a-tag>
          </div>
          <!-- 可滚动的普通一级标签 -->
          <div ref="level1ScrollRef" class="level1-scroll-container">
            <div
              v-for="category in getPureLevel1Categories()"
              :key="category.id"
              class="level1-tag-container"
            >
              <a-tag
                class="level1-tag"
                :class="{ selected: isCategorySelected(category.id) }"
                @click="handleTagClick(category)"
              >
                {{ category.name }}
              </a-tag>
            </div>
          </div>
        </div>

        <!-- 下方二级标签区域 -->
        <div class="level2-section">
          <!-- 如果没有选中任何分类，显示提示信息 -->
          <div v-if="selectedCategoryId === null" class="no-selection-hint">
            请选择一个分类查看详细内容
          </div>

          <!-- 显示所有二级标签（当点击热门或全部时） -->
          <div v-if="isShowAllLevel2Tags()" class="all-level2-content">
            <!-- 一级标签列表（使用二级标签样式） -->
            <div class="all-level2-tags-wrapper">
              <div
                v-for="category in getPureLevel1Categories()"
                :key="`all-${category.id}`"
                class="irregular-tag"
                :class="{ selected: isCategorySelected(category.id) }"
                @click="handleTagClick(category)"
              >
                {{ category.name }}
              </div>
            </div>
          </div>

          <!-- 只显示选中的一级分类对应的二级分类 -->
          <div
            v-for="category in getPureLevel1Categories()"
            :key="`level2-${category.id}`"
            class="level2-content"
            v-show="isCategorySelected(category.id)"
          >
            <div class="level2-tags-wrapper">
              <div
                v-for="child in getLevel2Categories(category.id)"
                :key="child.id"
                class="irregular-tag"
                :class="{ selected: isLevel2TagSelected(child.id) }"
                @click="handleTagClick(child)"
              >
                {{ child.name }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </a-spin>
  </div>
</template>

<style scoped>
.category-tags-container {
  --primary: #1677ff;
  --text: #1f2a3d;
  --muted: #6b7280;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.06);
  padding: 18px 20px;
}

.category-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.level1-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.section-title {
  font-weight: 700;
  color: var(--text);
}

.section-hint {
  color: var(--muted);
  font-size: 13px;
}

.level1-container {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px 0 6px;
  border-bottom: 1px dashed #e5e7eb;
  position: relative;
}

.level1-scroll-container {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding: 6px 2px 10px;
  flex: 1;
  white-space: nowrap;
  scroll-behavior: smooth;
  user-select: none;
  touch-action: pan-x;
  -ms-overflow-style: none;
  scrollbar-width: none;
  cursor: grab;
}

.level1-scroll-container::-webkit-scrollbar {
  display: none;
}

.level1-tag-container {
  flex: none;
}

.level1-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 78px;
  padding: 8px 14px;
  border-radius: 999px;
  background: #f4f6fb;
  color: var(--text);
  border: 1px solid transparent;
  transition: all 0.2s ease;
  font-weight: 600;
  cursor: pointer;
}

.level1-tag:hover {
  border-color: #d5e2ff;
  color: var(--primary);
  box-shadow: 0 6px 16px rgba(22, 119, 255, 0.08);
}

.level1-tag.selected {
  background: rgba(22, 119, 255, 0.12);
  border-color: var(--primary);
  color: var(--primary);
  box-shadow: 0 6px 16px rgba(22, 119, 255, 0.12);
}

.hot-tag {
  background: #fff1f0;
  border-color: #ffd8d3;
  color: #d4380d;
}

.all-tag {
  background: #e6f4ff;
  border-color: #c3e2ff;
  color: var(--primary);
}

.level2-section {
  padding: 10px 2px 2px;
}

.level1-scroll-container.dragging {
  cursor: grabbing;
}

.no-selection-hint {
  text-align: center;
  padding: 28px 14px;
  color: var(--muted);
  font-size: 14px;
  background: #f7f9fc;
  border: 1px dashed #e5e7eb;
  border-radius: 10px;
}

.level2-content {
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(6px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.level2-tags-wrapper,
.all-level2-tags-wrapper {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.all-level2-content {
  background: #f7f9fc;
  border-radius: 10px;
  padding: 12px;
}

.irregular-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  font-size: 14px;
  color: var(--muted);
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #fff;
  cursor: pointer;
  transition: all 0.18s ease;
}

.irregular-tag:hover {
  color: var(--primary);
  border-color: #c5dcff;
  background: #f4f7ff;
  box-shadow: 0 6px 16px rgba(22, 119, 255, 0.08);
}

.irregular-tag.selected {
  color: #fff;
  background: var(--primary);
  border-color: var(--primary);
  box-shadow: 0 8px 20px rgba(22, 119, 255, 0.2);
}

@media (max-width: 768px) {
  .category-tags-container {
    padding: 14px;
  }

  .level1-container {
    flex-wrap: wrap;
  }

  .level1-scroll-container {
    width: 100%;
  }

  .section-hint {
    font-size: 12px;
  }
}
</style>
