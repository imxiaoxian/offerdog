<script setup lang="ts">
import { computed } from 'vue';
import { useRouter } from 'vue-router';
// 导入Ant Design Vue图标
import { CodeOutlined, AppstoreOutlined, DatabaseOutlined, ApiOutlined, BulbOutlined, SettingOutlined,ConsoleSqlOutlined  } from '@ant-design/icons-vue';
import type { QuestionBank } from '@/services/questionBank';

// 定义组件属性
const props = defineProps<{
  // 题库数据
  questionBank: QuestionBank;
  /** 二级岗位名称（来自分类树） */
  categoryName?: string;
}>();

// 定义组件事件
const emit = defineEmits<{
  // 点击卡片时触发
  (e: 'click', questionBank: QuestionBank): void;
}>();

// 根据分类ID获取默认图标
const getIconComponent = computed(() => {
  const categoryId = props.questionBank.categoryId;
  
  // 根据不同分类ID返回不同的图标组件
  const iconMap: Record<number, any> = {
    4: CodeOutlined,       // 前端开发
    5: SettingOutlined,    // 后端开发
    6: AppstoreOutlined,   // 移动开发
    7: BulbOutlined,       // 人工智能
    8: ConsoleSqlOutlined  // 运维/测试
  };
  
  return iconMap[categoryId] || AppstoreOutlined;
});

// 根据分类ID获取图标类名
const getIconClass = computed(() => {
  const categoryId = props.questionBank.categoryId;
  
  const classMap: Record<number, string> = {
    4: 'icon-frontend',
    5: 'icon-backend',
    6: 'icon-mobile',
    7: 'icon-ai',
    8: 'icon-ops'
  };
  
  return classMap[categoryId] || '';
});

// 路由实例
const router = useRouter();

// 格式化日期
const formatDate = (dateString: string) => {
  if (!dateString) return '';
  const date = new Date(dateString);
  return `${date.getFullYear()}.${date.getMonth() + 1}.${date.getDate()}`;
};

// 处理卡片点击
const handleCardClick = () => {
  emit('click', props.questionBank);
  router.push(`/bank/${props.questionBank.id}`);
};
</script>

<template>
  <div class="question-bank-card" @click="handleCardClick">
    <div class="card-header">
      <div class="card-icon" :class="getIconClass">
        <component :is="getIconComponent" />
      </div>
      <div class="card-badge">
        <span class="badge-icon">★</span>
        <span class="badge-text">题库</span>
      </div>
    </div>
    <div class="card-content">
      <div v-if="categoryName" class="card-category">
        <a-tag color="blue">{{ categoryName }}</a-tag>
      </div>
      <h3 class="card-title">{{ questionBank.name }}</h3>
      <p class="card-description">{{ questionBank.description }}</p>
      <div class="card-meta">
        <span class="card-date">{{ formatDate(questionBank.createdAt) }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.question-bank-card {
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  height: 100%;
  border: 1px solid #e0e0e0;
  position: relative;
  overflow: hidden;
}

.question-bank-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #1976d2, #42a5f5);
  opacity: 0;
  transition: opacity 0.2s ease;
}

.question-bank-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  transform: translateY(-4px);
  border-color: #1976d2;
}

.question-bank-card:hover::before {
  opacity: 1;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 16px;
}

.card-icon {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  background: #f5f5f5;
}

.question-bank-card:hover .card-icon {
  transform: scale(1.1);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.card-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: rgba(25, 118, 210, 0.08);
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  color: #1976d2;
  white-space: nowrap;
  letter-spacing: 0.2px;
}

.badge-icon {
  font-size: 14px;
}

.badge-text {
  font-size: 11px;
}

.card-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.card-category {
  margin-bottom: -4px;
}

.card-category :deep(.ant-tag) {
  margin: 0;
  border: none;
}

.card-title {
  font-size: 20px;
  font-weight: 600;
  margin: 0;
  color: rgba(0, 0, 0, 0.87);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1.4;
  letter-spacing: 0.2px;
}

.card-description {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.6);
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.6;
  flex: 1;
}

.card-meta {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.38);
  margin-top: 8px;
}

.card-date {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  background: rgba(0, 0, 0, 0.03);
  border-radius: 16px;
  font-weight: 500;
  font-size: 11px;
}

/* 默认图标背景色 */
.card-icon {
  color: #1976d2;
}

/* 不同分类的图标背景色 */
.card-icon.icon-frontend {
  color: #1890ff;
}

.card-icon.icon-backend {
  color: #722ed1;
}

.card-icon.icon-mobile {
  color: #13c2c2;
}

.card-icon.icon-ai {
  color: #eb2f96;
}

.card-icon.icon-ops {
  color: #52c41a;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .question-bank-card {
    padding: 20px;
  }
  
  .card-icon {
    width: 56px;
    height: 56px;
    font-size: 28px;
  }
  
  .card-title {
    font-size: 18px;
  }
  
  .card-description {
    font-size: 13px;
  }
}

@media (max-width: 480px) {
  .question-bank-card {
    padding: 16px;
  }
  
  .card-icon {
    width: 48px;
    height: 48px;
    font-size: 24px;
  }
  
  .card-title {
    font-size: 16px;
  }
  
  .card-badge {
    padding: 4px 8px;
    font-size: 10px;
  }
}
</style>
