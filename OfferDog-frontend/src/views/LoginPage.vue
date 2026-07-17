<script lang="ts" setup>
import type { Rule } from 'ant-design-vue/es/form'
import { message } from 'ant-design-vue'
import { onBeforeUnmount, reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'

import { authApi } from '@/services/auth'
import { useUserStore } from '@/stores/user'

type FormRules = Record<string, Rule[]>

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const activeTab = ref<'password' | 'code'>('password')

const passwordForm = reactive({
  email: '',
  password: '',
})

const codeForm = reactive({
  email: '',
  verificationCode: '',
})

const passwordRules: FormRules = {
  email: [
    { required: true, message: '请输入邮箱' },
    { type: 'email', message: '邮箱格式不正确' },
  ],
  password: [{ required: true, message: '请输入密码' }],
}

const codeRules: FormRules = {
  email: [
    { required: true, message: '请输入邮箱' },
    { type: 'email', message: '邮箱格式不正确' },
  ],
  verificationCode: [{ required: true, message: '请输入验证码' }],
}

const loading = ref(false)
const sendingCode = ref(false)
const codeCountdown = ref(0)
let countdownTimer: ReturnType<typeof setInterval> | null = null

const clearCountdown = () => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
  codeCountdown.value = 0
}

const startCountdown = () => {
  codeCountdown.value = 60
  countdownTimer = setInterval(() => {
    if (codeCountdown.value <= 1) {
      clearCountdown()
      return
    }
    codeCountdown.value -= 1
  }, 1000)
}

const sendCode = async () => {
  if (!codeForm.email) {
    message.warning('请先输入邮箱')
    return
  }
  sendingCode.value = true
  try {
    await authApi.sendLoginCode({ email: codeForm.email })
    message.success('验证码已发送，请查收邮箱')
    startCountdown()
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : '验证码发送失败'
    message.error(errorMessage)
  } finally {
    sendingCode.value = false
  }
}

const handlePasswordLogin = async () => {
  loading.value = true
  try {
    const response = await authApi.loginByPassword({ ...passwordForm })
    if (response.data) {
      userStore.setUser(response.data)
    }
    message.success('登录成功')
    const redirect = (route.query.redirect as string) || '/questions'
    router.push(redirect)
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : '登录失败，请稍后再试'
    message.error(errorMessage)
  } finally {
    loading.value = false
  }
}

const handleCodeLogin = async () => {
  loading.value = true
  try {
    const response = await authApi.loginByCode({ ...codeForm })
    if (response.data) {
      userStore.setUser(response.data)
    }
    message.success('登录成功')
    const redirect = (route.query.redirect as string) || '/questions'
    router.push(redirect)
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : '登录失败，请稍后再试'
    message.error(errorMessage)
  } finally {
    loading.value = false
  }
}

const goRegister = () => {
  router.push('/register')
}

onBeforeUnmount(() => {
  clearCountdown()
})
</script>

<template>
  <div class="auth-page">
    <a-card title="登录 OfferDog" class="auth-card">
      <a-tabs v-model:activeKey="activeTab">
        <a-tab-pane key="password" tab="邮箱密码登录">
          <a-form layout="vertical" :model="passwordForm" :rules="passwordRules" @finish="handlePasswordLogin">
            <a-form-item label="邮箱" name="email">
              <a-input v-model:value="passwordForm.email" placeholder="请输入注册邮箱" />
            </a-form-item>
            <a-form-item label="密码" name="password">
              <a-input-password v-model:value="passwordForm.password" placeholder="请输入密码" />
            </a-form-item>
            <a-form-item>
              <a-button type="primary" block html-type="submit" :loading="loading">登录</a-button>
            </a-form-item>
          </a-form>
        </a-tab-pane>

        <a-tab-pane key="code" tab="邮箱验证码登录">
          <a-form layout="vertical" :model="codeForm" :rules="codeRules" @finish="handleCodeLogin">
            <a-form-item label="邮箱" name="email">
              <a-input v-model:value="codeForm.email" placeholder="请输入注册邮箱" />
            </a-form-item>
            <a-form-item label="验证码" name="verificationCode">
              <a-input-group compact>
                <a-input
                  v-model:value="codeForm.verificationCode"
                  placeholder="请输入验证码"
                  style="width: calc(100% - 140px)"
                />
                <a-button
                  :disabled="codeCountdown > 0 || sendingCode"
                  type="primary"
                  html-type="button"
                  @click="sendCode"
                >
                  <template v-if="codeCountdown > 0">{{ codeCountdown }}s 后重发</template>
                  <template v-else>发送验证码</template>
                </a-button>
              </a-input-group>
            </a-form-item>
            <a-form-item>
              <a-button type="primary" block html-type="submit" :loading="loading">登录</a-button>
            </a-form-item>
          </a-form>
        </a-tab-pane>
      </a-tabs>

      <div class="auth-extra">
        还没有账号？
        <a-typography-link @click.prevent="goRegister">立即注册</a-typography-link>
      </div>
    </a-card>
  </div>
</template>

<style scoped>
.auth-page {
  width: 100%;
  min-height: 100vh;
  min-height: 100dvh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  padding-left: max(20px, env(safe-area-inset-left, 0px));
  padding-right: max(20px, env(safe-area-inset-right, 0px));
  padding-bottom: max(20px, env(safe-area-inset-bottom, 0px));
  box-sizing: border-box;
}

.auth-card {
  width: 100%;
  max-width: 420px;
}

.auth-extra {
  text-align: center;
}

@media (max-width: 480px) {
  .auth-page {
    padding: 12px;
    padding-left: max(12px, env(safe-area-inset-left, 0px));
    padding-right: max(12px, env(safe-area-inset-right, 0px));
    align-items: flex-start;
    padding-top: max(24px, env(safe-area-inset-top, 0px));
  }

  .auth-card {
    max-width: 100%;
  }

  :deep(.ant-input-group.ant-input-group-compact) {
    display: flex !important;
    flex-direction: column;
    width: 100%;
  }

  :deep(.ant-input-group.ant-input-group-compact .ant-input) {
    width: 100% !important;
    border-radius: 8px !important;
  }

  :deep(.ant-input-group.ant-input-group-compact .ant-btn) {
    width: 100%;
    border-radius: 8px !important;
    margin-top: 8px;
  }
}
</style>
