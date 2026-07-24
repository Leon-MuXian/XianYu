<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { adminApi } from '@/services/api'

const router = useRouter()
const username = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')

async function submit() {
  error.value = ''
  if (!username.value || !password.value) {
    error.value = '请输入后台账号和密码'
    return
  }
  loading.value = true
  try {
    await adminApi.login(username.value, password.value)
    router.push('/tenants')
  } catch (exception) {
    error.value = exception instanceof Error ? exception.message : '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-context">
      <div class="login-brand">
        <span>闲</span>
        <strong>闲遇平台后台</strong>
      </div>

      <div class="login-message">
        <small>SEREN MEET · PLATFORM OPERATIONS</small>
        <h1>租户运营状态与平台规则的统一工作台</h1>
        <p>集中查看租户使用状态、处理到期与冻结账户、维护客服信息和平台规则。</p>

        <div class="login-capabilities">
          <div>
            <strong>租户状态</strong>
            <span>试用中、即将到期、已冻结、已延长，登录后统一进入租户管理。</span>
          </div>
          <div>
            <strong>人工续期</strong>
            <span>填写新到期日和原因，保存后解冻并写入操作记录。</span>
          </div>
        </div>
      </div>
    </section>

    <section class="login-entry">
      <form class="login-form" @submit.prevent="submit">
        <small>ADMIN SIGN IN</small>
        <h2>欢迎您管理员，请登录</h2>

        <div v-if="error" class="form-error" role="alert">{{ error }}</div>

        <label>
          <span>账号</span>
          <el-input v-model="username" autocomplete="username" size="large" placeholder="请输入平台管理员账号" />
        </label>
        <label>
          <span>密码</span>
          <el-input
            v-model="password"
            autocomplete="current-password"
            size="large"
            type="password"
            show-password
            placeholder="请输入密码"
          />
        </label>
        <el-button type="primary" native-type="submit" size="large" :loading="loading">登录</el-button>
      </form>
    </section>
  </main>
</template>

<style scoped>
.login-page {
  display: grid;
  grid-template-columns: 56% 44%;
  min-height: 100vh;
  min-height: 100dvh;
  background: #fff;
}

.login-context {
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: var(--navy);
  color: #fff;
  padding: clamp(48px, 7vh, 78px) clamp(56px, 6vw, 92px);
}

.login-brand {
  display: flex;
  align-items: center;
  gap: 14px;
  min-height: 42px;
}

.login-brand > span {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 4px;
  background: #fff;
  color: var(--navy);
  font-size: 20px;
  font-weight: 900;
}

.login-brand strong {
  font-size: 20px;
}

.login-message {
  width: min(720px, 100%);
  margin: auto 0;
  padding: 72px 0 54px;
}

.login-message > small,
.login-form > small {
  display: block;
  color: #79addb;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  font-weight: 800;
}

.login-message h1 {
  max-width: 680px;
  margin: 18px 0 22px;
  color: #fff;
  font-size: clamp(38px, 3.25vw, 54px);
  font-weight: 800;
  line-height: 1.24;
  letter-spacing: 0;
}

.login-message > p {
  max-width: 650px;
  margin: 0;
  color: #aab8c5;
  font-size: 16px;
  line-height: 1.75;
}

.login-capabilities {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  max-width: 680px;
  border-top: 1px solid rgba(255, 255, 255, 0.14);
  border-bottom: 1px solid rgba(255, 255, 255, 0.14);
  margin-top: 42px;
}

.login-capabilities > div {
  min-width: 0;
  padding: 20px 24px 20px 0;
}

.login-capabilities > div + div {
  border-left: 1px solid rgba(255, 255, 255, 0.14);
  padding-right: 0;
  padding-left: 24px;
}

.login-capabilities strong,
.login-capabilities span {
  display: block;
}

.login-capabilities strong {
  font-size: 16px;
}

.login-capabilities span {
  margin-top: 8px;
  color: #9eadba;
  font-size: 13px;
  line-height: 1.65;
}

.login-entry {
  display: grid;
  place-items: center;
  min-width: 0;
  background: #fff;
  padding: clamp(42px, 6vw, 96px);
}

.login-form {
  display: grid;
  gap: 18px;
  width: min(420px, 100%);
}

.login-form h2 {
  margin: 2px 0 20px;
  color: var(--ink);
  font-size: 30px;
  line-height: 1.25;
}

.login-form label {
  display: grid;
  gap: 8px;
  color: #405065;
  font-size: 13px;
  font-weight: 700;
}

.login-form :deep(.el-input__wrapper) {
  min-height: 52px;
}

.login-form > .el-button {
  min-height: 52px;
  margin-top: 4px;
}

.form-error {
  border-left: 3px solid var(--coral);
  background: var(--coral-soft);
  padding: 12px 14px;
  color: var(--coral);
  font-size: 13px;
}

@media (max-width: 980px) {
  .login-page {
    grid-template-columns: 1fr;
  }

  .login-context {
    min-height: 460px;
    padding: 38px clamp(28px, 8vw, 70px);
  }

  .login-message {
    padding: 64px 0 20px;
  }

  .login-message h1 {
    font-size: 38px;
  }

  .login-entry {
    padding: 56px 28px 72px;
  }
}

@media (max-width: 580px) {
  .login-context {
    min-height: 400px;
    padding: 28px 22px;
  }

  .login-message {
    padding-top: 52px;
  }

  .login-message h1 {
    margin-top: 14px;
    font-size: 30px;
  }

  .login-message > p {
    font-size: 14px;
  }

  .login-capabilities {
    grid-template-columns: 1fr;
    margin-top: 28px;
  }

  .login-capabilities > div,
  .login-capabilities > div + div {
    border-left: 0;
    padding: 14px 0;
  }

  .login-capabilities > div + div {
    border-top: 1px solid rgba(255, 255, 255, 0.14);
  }

  .login-entry {
    padding: 46px 22px 64px;
  }

  .login-form h2 {
    font-size: 25px;
  }
}
</style>
