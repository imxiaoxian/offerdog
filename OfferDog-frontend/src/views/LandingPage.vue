<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { storeToRefs } from 'pinia'

const router = useRouter()
const mobileMenuOpen = ref(false)
const userStore = useUserStore()
const { isLoggedIn } = storeToRefs(userStore)

const goLogin = () => router.push('/login')
const goRegister = () => router.push('/register')

const handleFeatureClick = (path: string, _name?: string) => {
  if (isLoggedIn.value) {
    router.push(`/${path}`)
  } else {
    const redirect = `/${path}`
    router.push({ path: '/login', query: { redirect } })
  }
}

const features = [
  {
    icon: 'fas fa-user-tie',
    color: 'bg-blue-100 text-blue-500',
    title: 'AI面试官',
    desc: '模拟真实面试场景，AI智能提问并实时反馈。支持多种岗位类型，帮助你提前适应面试节奏，提升表达能力。',
  },
  {
    icon: 'fas fa-book-open',
    color: 'bg-orange-100 text-orange-500',
    title: '面试题库',
    desc: '海量面试真题库，覆盖各大行业和岗位。提供详细解析和参考答案，助你全面掌握面试要点，从容应对各类问题。',
  },
  {
    icon: 'fas fa-file-alt',
    color: 'bg-green-100 text-green-500',
    title: '个人简历',
    desc: '智能简历分析与优化，根据目标岗位自动匹配关键词，提升简历通过率。专业模板库助你打造亮眼简历。',
  },
]

const roles = [
  { icon: 'fas fa-code', text: '前端开发' },
  { icon: 'fas fa-server', text: '后端开发' },
  { icon: 'fas fa-database', text: '数据工程师' },
  { icon: 'fas fa-mobile-alt', text: '移动开发' },
  { icon: 'fas fa-palette', text: 'UI/UX设计' },
  { icon: 'fas fa-briefcase', text: '产品经理' },
]

const faqs = [
  {
    q: '如何开始使用AI面试官功能？',
    a: '登录账号后，在"AI面试官"模块中选择岗位类型，系统会自动生成面试问题，可语音或视频回答，并给出评分与建议。',
  },
  {
    q: '面试题库的题目来源是什么？',
    a: '来源于各大公司的真实面试题，由行业专家整理和验证，定期更新，保证时效性与准确性。',
  },
  {
    q: 'AI简历分析的原理是什么？',
    a: '基于NLP与机器学习，评估简历与岗位匹配度，识别关键词缺失并提供结构优化建议。',
  },
  {
    q: '是否支持移动端使用？',
    a: '完全支持，可通过手机浏览器访问或使用移动应用，随时随地练习与学习。',
  },
]
const openFaq = ref<number | null>(null)
const toggleFaq = (i: number) => (openFaq.value = openFaq.value === i ? null : i)
</script>

<template>
  <div class="font-sans text-gray-800 bg-gray-50 min-h-screen flex flex-col">
    <!-- 顶部导航 -->
    <nav class="bg-white shadow-md sticky top-0 z-50">
      <div class="container mx-auto px-4 py-3 flex justify-between items-center gap-2">
        <div class="flex items-center space-x-2 min-w-0 shrink-0">
          <img src="/src/assets/icons/logo.svg" alt="offerDog" class="w-8 h-8 text-blue-500 shrink-0" />
          <span class="text-lg sm:text-xl font-bold truncate">offerDog</span>
        </div>
        <div class="hidden md:flex flex-wrap items-center justify-end gap-4 md:gap-8">
          <a class="text-gray-700 hover:text-blue-500 font-medium" href="#">首页</a>
          <a
            class="text-gray-700 hover:text-blue-500 font-medium"
            href="#"
            @click.prevent="handleFeatureClick('ai-interviewer', 'AI面试官')"
            >AI面试官</a
          >
          <a
            class="text-gray-700 hover:text-blue-500 font-medium"
            href="#"
            @click.prevent="handleFeatureClick('resume', '个人简历')"
            >个人简历</a
          >
          <a
            class="text-gray-700 hover:text-blue-500 font-medium"
            href="#"
            @click.prevent="handleFeatureClick('questions', '面试题库')"
            >面试题库</a
          >
          <a class="text-gray-700 hover:text-blue-500 font-medium" href="#">常见问题</a>
        </div>
        <div class="flex items-center space-x-4">
          <button
            class="hidden md:block bg-blue-500 hover:bg-blue-600 text-white px-5 py-2 rounded-full font-medium"
            @click="goLogin"
          >
            立即登录
          </button>
          <button class="md:hidden text-gray-700" @click="mobileMenuOpen = !mobileMenuOpen">
            <i :class="['text-xl', mobileMenuOpen ? 'fas fa-times' : 'fas fa-bars']"></i>
          </button>
        </div>
      </div>
      <!-- 移动端菜单 -->
      <div v-show="mobileMenuOpen" class="md:hidden bg-white shadow-lg">
        <div class="container mx-auto px-4 py-4 flex flex-col space-y-4">
          <a href="#" class="text-gray-700 hover:text-blue-500 font-medium">首页</a>
          <a
            href="#"
            class="text-gray-700 hover:text-blue-500 font-medium"
            @click.prevent="handleFeatureClick('ai-interviewer', 'AI面试官')"
            >AI面试官</a
          >
          <a
            href="#"
            class="text-gray-700 hover:text-blue-500 font-medium"
            @click.prevent="handleFeatureClick('questions', '面试题库')"
            >面试题库</a
          >
          <a
            href="#"
            class="text-gray-700 hover:text-blue-500 font-medium"
            @click.prevent="handleFeatureClick('resume', '个人简历')"
            >个人简历</a
          >
          <button
            class="bg-blue-500 hover:bg-blue-600 text-white px-5 py-2 rounded-full font-medium"
            @click="goLogin"
          >
            立即登录
          </button>
        </div>
      </div>
    </nav>

    <!-- 英雄区 -->
    <section class="bg-gradient-to-r from-blue-50 to-indigo-50 py-16 md:py-24">
      <div class="container mx-auto px-4 flex flex-col md:flex-row items-center">
        <div class="md:w-1/2 mb-10 md:mb-0">
          <h1 class="text-[clamp(2rem,5vw,3.5rem)] font-bold text-gray-900 leading-tight mb-6">
            offerDog<br />助你轻松拿OFFER
          </h1>
          <p class="text-lg text-gray-600 mb-8 max-w-lg">
            智能AI面试官、海量面试题库、专业AI简历分析，全方位提升你的面试竞争力，让求职更简单。
          </p>
          <div class="flex flex-col sm:flex-row gap-4">
            <button
              class="bg-blue-500 hover:bg-blue-600 text-white px-8 py-3 rounded-full font-medium"
              @click="goLogin"
            >
              立即登录
            </button>
            <button
              class="bg-white hover:bg-gray-50 text-blue-500 border border-blue-500 px-8 py-3 rounded-full font-medium"
              @click="goRegister"
            >
              <i class="fas fa-user-plus mr-2"></i>立即注册
            </button>
          </div>
        </div>
        <div class="md:w-1/2">
          <img
            class="rounded-2xl shadow-2xl w-full h-auto object-cover"
            alt="offerDog"
            src="https://tse1-mm.cn.bing.net/th/id/OIP-C.Tt3thtyRWi-BhJ7l2mRN2gHaHa?w=195&h=195&c=7&r=0&o=7&dpr=1.9&pid=1.7&rm=3"
          />
        </div>
      </div>
    </section>

    <!-- 平台支持 -->
    <section class="py-12 bg-white">
      <div class="container mx-auto px-4 text-center">
        <h2 class="text-3xl font-bold text-gray-900 mb-2">支持ai面试平台</h2>
        <p class="text-gray-600 mb-8">
          兼容主流远程面试和笔试平台，让你在任何环境下都能发挥最佳水平
        </p>
        <p>多端通用跨平台</p>
      </div>
    </section>

    <!-- 核心功能 -->
    <section class="py-16 bg-gray-50">
      <div class="container mx-auto px-4">
        <div class="text-center mb-12">
          <h2 class="text-3xl font-bold mb-4">AI面试助手核心功能</h2>
          <p class="text-gray-600">从准备到简历优化，一站式解决你的求职难题</p>
        </div>
        <div class="grid grid-cols-1 md:grid-cols-3 gap-8">
          <div
            v-for="f in features"
            :key="f.title"
            class="bg-white rounded-xl shadow-lg hover:shadow-xl p-8 transform hover:-translate-y-1 duration-300"
          >
            <div
              class="w-16 h-16 rounded-full flex items-center justify-center mb-6"
              :class="f.color"
            >
              <i :class="[f.icon, 'text-2xl']" />
            </div>
            <h3 class="text-xl font-bold mb-3">{{ f.title }}</h3>
            <p class="text-gray-600 mb-6">{{ f.desc }}</p>
            <a
              href="#"
              class="text-blue-500 hover:text-blue-600 font-medium"
              @click.prevent="goLogin"
              >立即体验 <i class="fas fa-arrow-right ml-1"></i
            ></a>
          </div>
        </div>
      </div>
    </section>

    <!-- 适用岗位 -->
    <section class="py-16 bg-white">
      <div class="container mx-auto px-4">
        <div class="text-center mb-12">
          <h2 class="text-3xl font-bold mb-4">适用多个岗位</h2>
          <p class="text-gray-600">技术/非技术岗位均可使用</p>
        </div>
        <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-6 gap-6">
          <div
            v-for="r in roles"
            :key="r.text"
            class="bg-gray-50 hover:bg-blue-50 rounded-lg p-6 text-center"
          >
            <i :class="[r.icon, 'text-blue-500 text-3xl mb-3']" />
            <p class="font-medium">{{ r.text }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- 常见问题 -->
    <section class="py-16 bg-gray-50">
      <div class="container mx-auto px-4">
        <div class="text-center mb-12">
          <h2 class="text-3xl font-bold mb-4">常见问题</h2>
          <p class="text-gray-600">解答你关心的问题</p>
        </div>
        <div class="max-w-3xl mx-auto space-y-4">
          <div
            v-for="(item, i) in faqs"
            :key="i"
            class="bg-white rounded-lg shadow-md overflow-hidden"
          >
            <button
              class="w-full flex justify-between items-center p-6 text-left"
              @click="toggleFaq(i)"
            >
              <span class="font-medium text-lg">{{ item.q }}</span>
              <i
                class="fas fa-chevron-down text-gray-400"
                :class="{ 'rotate-180': openFaq === i }"
              />
            </button>
            <div v-show="openFaq === i" class="px-6 pb-6 text-gray-600">{{ item.a }}</div>
          </div>
        </div>
      </div>
    </section>

    <!-- 行动召唤 -->
    <section class="bg-blue-500 py-16 text-white text-center">
      <div class="container mx-auto px-4">
        <h2 class="text-3xl md:text-4xl font-bold mb-6">准备好提升你的面试竞争力了吗？</h2>
        <p class="text-xl text-blue-100 mb-8">立即登录，体验AI面试助手的强大功能</p>
        <button
          class="bg-white text-blue-500 hover:bg-gray-100 px-8 py-4 rounded-full font-bold"
          @click="goLogin"
        >
          立即登录
        </button>
      </div>
    </section>

    <!-- 页脚 -->
    <footer class="bg-gray-900 text-gray-400 py-12 mt-auto">
      <div class="container mx-auto px-4">
        <div class="grid grid-cols-1 md:grid-cols-4 gap-8">
          <div>
            <div class="flex items-center space-x-2 mb-4">
              <i class="fas fa-robot text-blue-400 text-2xl"></i><span
                class="text-xl font-bold text-white"
                >offerDog</span
              >
            </div>
            <p class="mb-4">智能AI面试辅助平台，助你轻松获取理想工作机会。</p>
            <div class="flex space-x-4 text-xl">
              <i class="fab fa-weixin"></i><i class="fab fa-weibo"></i><i class="fab fa-github"></i>
            </div>
          </div>
          <div>
            <h4 class="text-white font-medium mb-4">产品功能</h4>
            <ul class="space-y-2">
              <li><a class="hover:text-white" href="#">AI面试官</a></li>
              <li><a class="hover:text-white" href="#">面试题库</a></li>
              <li><a class="hover:text-white" href="#">AI简历</a></li>
              <li><a class="hover:text-white" href="#">面试技巧</a></li>
            </ul>
          </div>
          <div>
            <h4 class="text-white font-medium mb-4">帮助中心</h4>
            <ul class="space-y-2">
              <li><a class="hover:text-white" href="#">使用教程</a></li>
              <li><a class="hover:text-white" href="#">常见问题</a></li>
              <li><a class="hover:text-white" href="#">联系我们</a></li>
              <li><a class="hover:text-white" href="#">隐私政策</a></li>
            </ul>
          </div>
          <div>
            <h4 class="text-white font-medium mb-4">联系我们</h4>
            <ul class="space-y-2">
              <li class="flex items-start">
                <i class="fas fa-envelope mt-1 mr-3"></i><span>contact@offerdog.com</span>
              </li>
              <li class="flex items-start">
                <i class="fas fa-phone mt-1 mr-3"></i><span>400-123-4567</span>
              </li>
              <li class="flex items-start"><i class="fas fa-map-marker-alt mt-1 mr-3"></i></li>
            </ul>
          </div>
        </div>
        <div class="border-t border-gray-800 mt-12 pt-8 text-center">
          © 2026 offerDog. 保留所有权利.
        </div>
      </div>
    </footer>
  </div>
</template>

<style scoped>
.rotate-180 {
  transform: rotate(180deg);
}

@media (max-width: 768px) {
  .container {
    padding-left: 16px;
    padding-right: 16px;
  }
  
  .md-py-24 {
    padding-top: 80px !important;
    padding-bottom: 80px !important;
  }
  
  .md-mb-0 {
    margin-bottom: 32px !important;
  }
  
  .md-w-1-2 {
    width: 100% !important;
  }
  
  .md-flex-row {
    flex-direction: column !important;
  }
  
  .md-gap-8 {
    gap: 16px !important;
  }
  
  .md-grid-cols-3 {
    grid-template-columns: repeat(1, minmax(0, 1fr)) !important;
    gap: 24px !important;
  }
  
  .md-grid-cols-4 {
    grid-template-columns: repeat(1, minmax(0, 1fr)) !important;
    gap: 24px !important;
  }
  
  .md-grid-cols-6 {
    grid-template-columns: repeat(3, minmax(0, 1fr)) !important;
    gap: 16px !important;
  }
  
  .md-text-4xl {
    font-size: 28px !important;
  }
  
  .md-block {
    display: none !important;
  }
  
  h1 {
    font-size: 28px !important;
  }
  
  h2 {
    font-size: 24px !important;
  }
  
  .text-3xl {
    font-size: 22px !important;
  }
  
  .text-2xl {
    font-size: 18px !important;
  }
  
  .px-8 {
    padding-left: 24px !important;
    padding-right: 24px !important;
  }
  
  .py-4 {
    padding-top: 16px !important;
    padding-bottom: 16px !important;
  }
  
  .mb-10 {
    margin-bottom: 24px !important;
  }
  
  .mb-12 {
    margin-bottom: 24px !important;
  }
  
  .mt-12 {
    margin-top: 24px !important;
  }
  
  .pt-8 {
    padding-top: 24px !important;
  }
  
  .space-y-4 > * {
    margin-top: 16px !important;
    margin-bottom: 0 !important;
  }
  
  .space-y-2 > * {
    margin-top: 12px !important;
    margin-bottom: 0 !important;
  }
}
</style>
