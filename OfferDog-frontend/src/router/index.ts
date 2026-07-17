import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

import BasicLayout from '@/layouts/BasicLayout.vue'
import pinia from '@/stores'
import { useUserStore } from '@/stores/user'

const routes: RouteRecordRaw[] = [
  // 公共路由：独立首页
  {
    path: '/',
    redirect: '/landing',
  },
  {
    path: '/landing',
    name: 'Landing',
    component: () => import('@/views/LandingPage.vue'),
    meta: { title: '首页', public: true },
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginPage.vue'),
    meta: { title: '登录', public: true },
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/RegisterPage.vue'),
    meta: { title: '注册', public: true },
  },
  // 内部应用：使用基础布局
  {
    path: '/',
    component: BasicLayout,
    children: [
      {
        path: 'questions',
        name: 'QuestionBank',
        component: () => import('@/views/HomePage.vue'),
        meta: { title: '面试题库' },
      },
      {
        path: 'ai-interviewer',
        name: 'AiInterviewer',
        component: () => import('@/views/AiInterviewerPage.vue'),
        meta: { title: 'AI面试官' },
      },
      {
        path: 'learning',
        name: 'InterviewLearning',
        component: () => import('@/views/InterviewLearningPage.vue'),
        meta: { title: '学习提升' },
      },
      {
        path: 'improvement-plan',
        name: 'InterviewImprovementPlan',
        component: () => import('@/views/InterviewImprovementPlanPage.vue'),
        meta: { title: '面试提升计划' },
      },
      {
        path: 'interview/room/:sessionId',
        name: 'InterviewRoom',
        component: () => import('@/views/InterviewRoomPage.vue'),
        meta: { title: '面试房间' },
      },
      {
        path: 'interview/report/:sessionId',
        name: 'InterviewReport',
        component: () => import('@/views/InterviewReportPage.vue'),
        meta: { title: '面试报告' },
      },
      {
        path: 'resume',
        name: 'Resume',
        component: () => import('@/views/ResumePage.vue'),
        meta: { title: '个人简历' },
      },
      {
        path: 'favorites',
        name: 'Favorites',
        component: () => import('@/views/FavoriteQuestionsPage.vue'),
        meta: { title: '我的收藏' },
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/ProfilePage.vue'),
        meta: { title: '个人中心' },
      },
      {
        path: 'user-management',
        name: 'UserManagement',
        component: () => import('@/views/UserManagementPage.vue'),
        meta: { title: '用户管理', requiresAdmin: true },
      },
      {
        path: 'quiz-bank-management',
        name: 'QuizBankManagement',
        component: () => import('@/views/QuizBankManagementPage.vue'),
        meta: { title: '行业岗位管理', requiresAdmin: true },
      },
      {
        path: 'question-bank-management',
        name: 'QuestionBankManagement',
        component: () => import('@/views/QuestionBankManagementPage.vue'),
        meta: { title: '题库管理', requiresAdmin: true },
      },
      {
        path: 'template-management',
        name: 'TemplateManagement',
        component: () => import('@/views/TemplateManagementPage.vue'),
        meta: { title: '模版管理', requiresAdmin: true },
      },
      {
        path: 'speech-test',
        name: 'SpeechTest',
        component: () => import('@/views/SpeechTestPage.vue'),
        meta: { title: '语音识别测试' },
      },
      {
        path: 'bank/:id',
        name: 'Bank',
        component: () => import('@/views/bank.vue'),
        meta: { title: '题库详情' },
      },
      {
        path: 'question',
        name: 'Question',
        component: () => import('@/views/question.vue'),
        meta: { title: '问题详情' },
      },

    ],
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/landing',
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

router.beforeEach((to, _from, next) => {
  const userStore = useUserStore(pinia)

  // 公开页面：无需登录
  const isPublic = to.meta?.public === true ||
    to.path.startsWith('/landing') ||
    to.path.startsWith('/login') ||
    to.path.startsWith('/register')

  // 未登录访问内部页面：跳转到登录，并带上重定向参数
  if (!isPublic && !userStore.isLoggedIn) {
    return next({ path: '/login', query: { redirect: to.fullPath } })
  }

  // 管理员权限校验
  if (to.meta?.requiresAdmin && !userStore.isAdmin) {
    return next('/questions')
  }

  next()
})

export default router
