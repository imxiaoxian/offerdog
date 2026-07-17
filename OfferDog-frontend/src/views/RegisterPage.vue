<script lang="ts" setup>
import type { Rule } from 'ant-design-vue/es/form'
import { message } from 'ant-design-vue'
import { onBeforeUnmount, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import { authApi } from '@/services/auth'

type FormRules = Record<string, Rule[]>

const router = useRouter()
const form = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  verificationCode: '',
})

const loading = ref(false)
const sendingCode = ref(false)
const codeCountdown = ref(0)
let countdownTimer: ReturnType<typeof setInterval> | null = null

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名' }],
  email: [
    { required: true, message: '请输入邮箱' },
    { type: 'email', message: '邮箱格式不正确' },
  ],
  password: [
    { required: true, message: '请输入密码' },
    { min: 6, message: '密码至少 6 位' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码' },
    {
      validator: (_: Rule, value: string) => {
        if (!value) {
          return Promise.reject('请再次输入密码')
        }
        if (value !== form.password) {
          return Promise.reject('两次输入的密码不一致')
        }
        return Promise.resolve()
      },
      trigger: 'blur',
    },
  ],
  verificationCode: [{ required: true, message: '请输入验证码' }],
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

const clearCountdown = () => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
  codeCountdown.value = 0
}

const sendCode = async () => {
  if (!form.username || !form.email) {
    message.warning('请先填写用户名和邮箱')
    return
  }
  sendingCode.value = true
  try {
    await authApi.sendRegisterCode({ username: form.username, email: form.email })
    message.success('验证码已发送，请注意查收邮箱')
    startCountdown()
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : '验证码发送失败'
    message.error(errorMessage)
  } finally {
    sendingCode.value = false
  }
}

const handleFinish = async () => {
  loading.value = true
  try {
    await authApi.register({
      username: form.username,
      email: form.email,
      password: form.password,
      verificationCode: form.verificationCode,
    })
    message.success('注册成功，请登录')
    router.push('/login')
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : '注册失败，请稍后再试'
    message.error(errorMessage)
  } finally {
    loading.value = false
  }
}

const goLogin = () => {
  router.push('/login')
}

onBeforeUnmount(() => {
  clearCountdown()
})
</script>

<template>
  <div class="auth-page">
    <a-card title="注册 OfferDog" class="auth-card">
      <a-form layout="vertical" :model="form" :rules="rules" @finish="handleFinish">
        <a-form-item label="用户名" name="username">
          <a-input v-model:value="form.username" placeholder="请设置用户名" />
        </a-form-item>
        <a-form-item label="邮箱" name="email">
          <a-input v-model:value="form.email" placeholder="请输入常用邮箱" />
        </a-form-item>
        <a-form-item label="密码" name="password">
          <a-input-password v-model:value="form.password" placeholder="请设置登录密码" />
        </a-form-item>
        <a-form-item label="确认密码" name="confirmPassword">
          <a-input-password v-model:value="form.confirmPassword" placeholder="请再次输入密码" />
        </a-form-item>
        <a-form-item label="验证码" name="verificationCode">
          <a-input-group compact>
            <a-input
              v-model:value="form.verificationCode"
              placeholder="请输入邮箱验证码"
              style="width: calc(100% - 140px)"
            />
            <a-button
              type="primary"
              html-type="button"
              :disabled="codeCountdown > 0 || sendingCode"
              @click="sendCode"
            >
              <template v-if="codeCountdown > 0">{{ codeCountdown }}s 后重发</template>
              <template v-else>发送验证码</template>
            </a-button>
          </a-input-group>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" block html-type="submit" :loading="loading">注册</a-button>
        </a-form-item>
        <div class="auth-extra">
          已有账号？
          <a-typography-link @click.prevent="goLogin">立即登录</a-typography-link>
        </div>
      </a-form>
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
  max-width: 520px;
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
