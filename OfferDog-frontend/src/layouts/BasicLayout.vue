<script lang="ts" setup>
import type { MenuProps } from 'ant-design-vue'
import { computed, ref, watch, onMounted, onBeforeUnmount } from 'vue'
import { storeToRefs } from 'pinia'
import { RouterView, useRoute, useRouter } from 'vue-router'

import logoUrl from '@/assets/icons/logo.svg'
import { useUserStore } from '@/stores/user'
import { MenuOutlined } from '@ant-design/icons-vue'

/** 小于此宽度时使用抽屉导航（含平板竖屏） */
const COMPACT_NAV_BREAKPOINT = 992

const router = useRouter()
const route = useRoute()

type MenuItem = NonNullable<MenuProps['items']>[number]

const baseMenuItems: MenuItem[] = [
  { key: 'questions', label: '面试题库' },
  { key: 'ai-interviewer', label: 'AI面试官' },
  { key: 'learning', label: '学习提升' },
  { key: 'improvement-plan', label: '面试提升计划' },
  { key: 'resume', label: '个人简历' },
  { key: 'favorites', label: '我的收藏' },
  { key: 'profile', label: '个人中心' },
]

const adminMenuItems: MenuItem[] = [
  { key: 'quiz-bank-management', label: '行业岗位管理' },
  { key: 'question-bank-management', label: '题库管理' },
  { key: 'template-management', label: '模版管理' },
  { key: 'user-management', label: '用户管理' },
]
const allMenuKeys = [
  ...baseMenuItems.map((item) => String(item?.key)),
  ...adminMenuItems.map((item) => String(item?.key)),
]

const selectedKeys = ref<string[]>([])

const userStore = useUserStore()
const { isLoggedIn, isAdmin, username } = storeToRefs(userStore)
const menuItems = computed<MenuItem[]>(() => {
  const items = isAdmin.value ? [...baseMenuItems, ...adminMenuItems] : baseMenuItems
  return items.map((item) => ({ ...item } as MenuItem))
})
const defaultAvatar = 'https://avatars.githubusercontent.com/u/9919?s=200&v=4'
const avatarSrc = computed(() => userStore.currentUser?.avatarUrl || defaultAvatar)

const handleMenuClick: MenuProps['onClick'] = (info) => {
  const target = info?.key ? `/${String(info.key)}` : '/questions'
  router.push(target)
}

const goLogin = () => {
  router.push('/login')
}

const handleLogout = () => {
  userStore.logout()
  router.push('/landing')
}

const syncSelectedMenu = (currentPath?: string) => {
  const normalized = (currentPath ?? '').split('?')[0] ?? ''
  const [, firstSegment = ''] = normalized.split('/')
  const matchedKey = allMenuKeys.find((key) => key === firstSegment)
  selectedKeys.value = matchedKey ? [String(matchedKey)] : []
}

const isCompactNav = ref(false)
const drawerOpen = ref(false)
const drawerWidth = ref(288)

const checkCompactNav = () => {
  const w = window.innerWidth
  isCompactNav.value = w < COMPACT_NAV_BREAKPOINT
  drawerWidth.value = Math.min(320, Math.round(w * 0.86))
}

onMounted(() => {
  checkCompactNav()
  window.addEventListener('resize', checkCompactNav)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', checkCompactNav)
})

const handleDrawerMenuClick: MenuProps['onClick'] = (info) => {
  drawerOpen.value = false
  handleMenuClick(info)
}

watch(
  () => route.path,
  (currentPath) => {
    syncSelectedMenu(currentPath)
    drawerOpen.value = false
  },
  { immediate: true },
)
</script>

<template>
  <a-layout class="app-layout">
    <a-layout-header class="layout-header">
      <div class="header-left">
        <a-space :size="isCompactNav ? 'small' : 'middle'" align="center">
          <a-avatar shape="square" :size="isCompactNav ? 'default' : 'large'" :src="logoUrl" />
          <span class="logo-text">offerDog</span>
        </a-space>
        <div v-if="isCompactNav" class="mobile-menu-toggle">
          <a-button
            type="text"
            class="menu-btn"
            aria-label="打开导航菜单"
            @click="drawerOpen = true"
          >
            <MenuOutlined />
          </a-button>
        </div>
        <a-menu
          v-else
          v-model:selectedKeys="selectedKeys"
          mode="horizontal"
          :items="menuItems"
          class="header-menu"
          @click="handleMenuClick"
        />
      </div>
      <div class="header-right">
        <a-button v-if="!isLoggedIn" type="primary" size="middle" class="login-btn" @click="goLogin">
          登录
        </a-button>
        <a-space v-else align="center" :size="isCompactNav ? 4 : 8">
          <a-avatar :src="avatarSrc" :size="isCompactNav ? 'default' : 40" />
          <span class="header-username">{{ username }}</span>
          <template v-if="!isCompactNav">
            <a-divider type="vertical" />
          </template>
          <a-button type="link" size="small" class="logout-btn" @click="handleLogout">退出</a-button>
        </a-space>
      </div>
    </a-layout-header>

    <a-drawer
      v-model:open="drawerOpen"
      placement="left"
      :width="drawerWidth"
      title="导航"
      :body-style="{ padding: 0 }"
      :destroy-on-close="false"
      root-class-name="app-nav-drawer"
    >
      <a-menu
        v-model:selectedKeys="selectedKeys"
        mode="vertical"
        :items="menuItems"
        class="drawer-menu"
        @click="handleDrawerMenuClick"
      />
    </a-drawer>
    <a-layout-content class="layout-content">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </a-layout-content>
    <a-layout-footer class="layout-footer">©2026 offerDog平台</a-layout-footer>
  </a-layout>
</template>
<style lang="css" scoped>
.app-layout {
  min-height: 100vh;
  min-height: 100dvh;
}

.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  background: #fff;
  padding: 0 24px;
  padding-left: max(16px, env(safe-area-inset-left, 0px));
  padding-right: max(16px, env(safe-area-inset-right, 0px));
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  min-height: 56px;
  flex-wrap: nowrap;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 24px;
  min-width: 0;
  flex: 1;
}

.logo-text {
  font-size: 18px;
  font-weight: 600;
  white-space: nowrap;
}

.header-right {
  flex-shrink: 0;
  min-width: 0;
}

.header-username {
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
}

.logout-btn {
  padding: 0 4px;
}

.header-menu {
  line-height: 64px;
}

.mobile-menu-toggle {
  display: none;
}

.menu-btn {
  padding: 0;
  border: none;
  background: transparent;
}

.menu-btn :deep(.anticon) {
  font-size: 20px;
  color: #000;
}

.layout-content {
  padding: 24px;
  padding-bottom: max(24px, env(safe-area-inset-bottom, 0px));
  background: #f5f5f5;
  width: 100%;
  min-width: 0;
  box-sizing: border-box;
}

.layout-footer {
  text-align: center;
  padding-bottom: max(12px, env(safe-area-inset-bottom, 0px));
}

.drawer-menu {
  border-inline-end: none !important;
}

@media (max-width: 991px) {
  .layout-header {
    padding: 0 12px;
    padding-left: max(12px, env(safe-area-inset-left, 0px));
    padding-right: max(12px, env(safe-area-inset-right, 0px));
  }

  .header-left {
    gap: 8px;
  }

  .logo-text {
    font-size: 15px;
  }

  :deep(.ant-space-item .ant-avatar) {
    flex-shrink: 0;
  }

  .header-menu {
    display: none;
  }

  .mobile-menu-toggle {
    display: block;
  }

  .layout-content {
    padding: 12px;
    padding-bottom: max(12px, env(safe-area-inset-bottom, 0px));
  }

  .header-username {
    max-width: 64px;
    font-size: 13px;
  }
}

@media (max-width: 380px) {
  .header-username {
    display: none;
  }

  .login-btn {
    padding: 4px 12px;
    font-size: 13px;
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
