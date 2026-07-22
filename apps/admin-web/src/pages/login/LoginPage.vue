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
    <section class="admin-login">
      <div class="admin-login-hero" aria-hidden="true">
        <div class="brand-lockup">
          <span class="mark">闲</span>
          <strong>闲遇</strong>
        </div>
        <div class="hero-content">
          <h1>平台试点运营<br>从租户状态开始</h1>
          <p>用于平台内部观察自动开通租户、试用到期、冻结解冻、人工续期和运营配置变更。所有关键操作都保留原因与审计记录。</p>
          <div class="admin-login-points">
            <div class="admin-login-point">
              <strong>租户试用</strong>
              <small>查看门店开通、到期窗口、发卡、到店和核销信号。</small>
            </div>
            <div class="admin-login-point">
              <strong>冻结解冻</strong>
              <small>人工确认后暂停或恢复三端业务入口，历史数据保留。</small>
            </div>
            <div class="admin-login-point">
              <strong>平台配置</strong>
              <small>维护默认试用天数、提醒阈值和邀请码有效期。</small>
            </div>
            <div class="admin-login-point">
              <strong>审计留痕</strong>
              <small>记录操作人、旧值、新值和可追溯原因。</small>
            </div>
          </div>
        </div>
      </div>
      <form class="admin-login-card" @submit.prevent="submit">
        <span class="eyebrow">SEREN MEET ADMIN</span>
        <h2>闲遇平台后台</h2>
        <p>仅供平台内部运营使用，用于租户试用、冻结解冻、期限调整、客服微信和平台配置维护。</p>
        <div v-if="error" class="form-error">{{ error }}</div>
        <label>
          <span>后台账号</span>
          <el-input v-model="username" autocomplete="username" size="large" />
        </label>
        <label>
          <span>登录密码</span>
          <el-input v-model="password" autocomplete="current-password" size="large" show-password />
        </label>
        <el-button type="primary" native-type="submit" size="large" :loading="loading">登录</el-button>
      </form>
    </section>
  </main>
</template>

<style scoped>
.login-page {
  display: flex;
  min-height: 100vh;
  min-height: 100dvh;
  background: linear-gradient(135deg, #f8fbf7 0%, #edf4ee 100%);
  overflow: hidden;
}

.admin-login {
  flex: 1;
  display: grid;
  grid-template-columns: minmax(560px, 1fr) minmax(440px, clamp(440px, 34vw, 620px));
  width: 100%;
  min-height: 680px;
  height: 100vh;
  height: 100dvh;
}

.admin-login-hero {
  position: relative;
  isolation: isolate;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  background:
    linear-gradient(145deg, rgba(18, 34, 29, 0.98), rgba(18, 34, 29, 0.94)),
    #12221d;
  color: white;
  overflow: hidden;
  padding: clamp(34px, 6vh, 64px) clamp(44px, 5vw, 68px);
}

.admin-login-hero::after {
  content: "";
  position: absolute;
  right: -120px;
  bottom: -120px;
  z-index: -1;
  width: 460px;
  height: 460px;
  border-radius: 999px;
  background: rgba(47, 125, 110, 0.34);
}

.brand-lockup {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 22px;
  font-weight: 900;
}

.mark {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: var(--green);
  font-size: 18px;
  font-weight: 900;
}

.hero-content {
  align-self: center;
  transform: translateY(clamp(24px, 4.8vh, 56px));
}

.admin-login-hero h1 {
  width: min(580px, 100%);
  margin: 0 0 20px;
  font-size: clamp(36px, 3.4vw, 54px);
  line-height: 1.18;
}

.admin-login-hero p {
  width: min(560px, 100%);
  margin: 0;
  color: #c9d8d1;
  font-size: 15px;
  line-height: 1.8;
}

.admin-login-points {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  width: min(720px, 100%);
  margin-top: clamp(26px, 5vh, 42px);
}

.admin-login-point {
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.08);
  padding: clamp(14px, 2.2vh, 20px);
}

.admin-login-point strong {
  display: block;
  color: #fff;
  font-size: 18px;
  line-height: 1.35;
}

.admin-login-point small {
  display: block;
  margin-top: 8px;
  color: #c9d8d1;
  font-size: 13px;
  line-height: 1.7;
}

.admin-login > form {
  align-self: center;
  min-width: 0;
  padding: clamp(28px, 5vw, 84px);
}

.admin-login-card {
  align-self: center;
  justify-self: center;
  display: grid;
  gap: 16px;
  width: min(400px, 100%);
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: var(--surface);
  padding: 34px;
  box-shadow: var(--shadow-soft);
}

.eyebrow {
  color: var(--green);
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0;
}

h2 {
  margin: 0;
  font-size: 24px;
}

p {
  margin: 0 0 10px;
  color: var(--muted);
  line-height: 1.7;
}

label {
  display: grid;
  gap: 8px;
  font-weight: 800;
}

.form-error {
  border-radius: 8px;
  background: var(--red-soft);
  color: var(--red);
  padding: 12px;
}

@media (max-width: 1120px) {
  .login-page {
    overflow: auto;
  }

  .admin-login {
    grid-template-columns: 1fr;
    height: auto;
    min-height: 100dvh;
  }

  .admin-login-hero {
    min-height: 500px;
  }

  .admin-login-card {
    margin: 48px 0;
  }
}

@media (max-height: 760px) and (min-width: 1121px) {
  .admin-login-hero {
    padding-top: 32px;
    padding-bottom: 32px;
  }

  .hero-content {
    transform: translateY(clamp(12px, 2.5vh, 24px));
  }

  .admin-login-hero h1 {
    font-size: 38px;
    margin-bottom: 14px;
  }

  .admin-login-hero p {
    font-size: 14px;
    line-height: 1.65;
  }

  .admin-login-points {
    gap: 12px;
    margin-top: 24px;
  }

  .admin-login-point {
    padding: 14px;
  }

  .admin-login-point strong {
    font-size: 16px;
  }

  .admin-login-point small {
    font-size: 12px;
    line-height: 1.55;
  }

  .admin-login-card {
    gap: 14px;
    padding: 30px;
  }
}

@media (max-width: 680px) {
  .admin-login-hero {
    min-height: auto;
    padding: 36px 22px;
  }

  .hero-content {
    align-self: start;
    transform: none;
  }

  .admin-login-hero h1 {
    margin-top: 48px;
    font-size: 31px;
  }

  .admin-login-points {
    grid-template-columns: 1fr;
  }

  .admin-login-point {
    min-height: 0;
    padding: 16px;
  }

  .admin-login-card {
    width: 100%;
    margin: 24px 0 96px;
    padding: 24px;
  }
}
</style>
