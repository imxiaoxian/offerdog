import { createApp, type Plugin } from 'vue'
import { VueRendererMarkdown } from 'vue-renderer-markdown'
import 'vue-renderer-markdown/index.css'
import 'katex/dist/katex.min.css'

if (typeof window !== 'undefined' && typeof (window as any).global === 'undefined') {
  ;(window as any).global = window
}

import App from './App.vue'
import router from './router'
import Antd from 'ant-design-vue'
import pinia from './stores'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import './styles/main.css'

const app = createApp(App)
const markdownPlugin = VueRendererMarkdown as unknown as Plugin

// 注册所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(ElementPlus)

app.use(pinia)
app.use(router)
app.use(Antd)
app.use(markdownPlugin)

app.mount('#app')
