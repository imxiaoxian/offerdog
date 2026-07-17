import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import type { UserInfo } from '@/services/auth'
import { clearInterviewSessionCache } from '@/utils/interviewSessionCache'

const STORAGE_KEY = 'inai-user'

const loadInitialUser = (): UserInfo | null => {
  if (typeof window === 'undefined') return null
  try {
    const cached = window.localStorage.getItem(STORAGE_KEY)
    return cached ? (JSON.parse(cached) as UserInfo) : null
  } catch (error) {
    console.warn('加载用户缓存失败', error)
    return null
  }
}

const persistUser = (user: UserInfo | null) => {
  if (typeof window === 'undefined') return
  try {
    if (user) {
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify(user))
    } else {
      window.localStorage.removeItem(STORAGE_KEY)
    }
  } catch (error) {
    console.warn('同步用户缓存失败', error)
  }
}

export const useUserStore = defineStore('user', () => {
  const currentUser = ref<UserInfo | null>(loadInitialUser())

  const isLoggedIn = computed(() => !!currentUser.value)
  const username = computed(() => currentUser.value?.username ?? '')
  const isAdmin = computed(() => currentUser.value?.role?.toUpperCase() === 'ADMIN')

  const setUser = (user: UserInfo) => {
    currentUser.value = user
    persistUser(user)
  }

  const logout = () => {
    currentUser.value = null
    persistUser(null)
    clearInterviewSessionCache()
  }

  return { currentUser, isLoggedIn, isAdmin, username, setUser, logout }
})
