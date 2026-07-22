<script setup lang="ts">
import Taro from '@tarojs/taro'
import { reactive, ref } from 'vue'
import { setSessionToken } from '@serenmeet/api-client/taro'
import { api } from '../../api'
const form = reactive({ loginName: '', password: '' })
const loading = ref(false)
const message = ref('')
async function login() {
  loading.value = true; message.value = ''
  try {
    const result = await api.request<{ token: string; session: { nextPath: string } }>('POST', '/auth/staff/login', form)
    setSessionToken(result.token)
    await Taro.reLaunch({ url: result.session.nextPath.includes('frozen') ? '/pages/frozen/index' : '/pages/today/index' })
  } catch (error) { message.value = error instanceof Error ? error.message : '登录失败' } finally { loading.value = false }
}
</script>
<template><View class="page"><Text class="brand">闲遇 · 员工端</Text><Text class="title">回到今天的服务</Text><View class="panel"><View class="field"><Text class="label">登录账号</Text><input v-model="form.loginName" class="input" /></View><View class="field"><Text class="label">密码</Text><input v-model="form.password" class="input" password /></View><Button class="button" :loading="loading" :disabled="loading" @tap="login">登录</Button><Text v-if="message" class="message">{{ message }}</Text></View></View></template>
