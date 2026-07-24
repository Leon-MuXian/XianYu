<script setup lang="ts">
import Taro from '@tarojs/taro'
import { ref } from 'vue'
import { setSessionToken } from '@serenmeet/api-client/taro'
import { api, apiBaseUrl } from '../../api'
import { loginEnvironmentIssue, loginFailureMessage } from '../../login-diagnostics'
import { ownerRoutes } from '../../owner'

const loading = ref(false)
const message = ref('')
const windowInfo = Taro.getWindowInfo()
const statusBarHeight = windowInfo.statusBarHeight || 20
const menuButtonRect = Taro.getMenuButtonBoundingClientRect()
const menuButtonGap = menuButtonRect.height > 0
  ? Math.max(menuButtonRect.top - statusBarHeight, 4)
  : 4
const navigationBarHeight = menuButtonRect.height > 0
  ? menuButtonRect.height + menuButtonGap * 2
  : 44
const capsuleReserveWidth = menuButtonRect.width > 0
  ? Math.max(windowInfo.windowWidth - menuButtonRect.left + 10, 86)
  : 86
const loginSceneStyle = { paddingTop: `${statusBarHeight}px` }
const loginIdentityStyle = {
  minHeight: `${navigationBarHeight}px`,
  paddingRight: `${capsuleReserveWidth}px`
}

async function login() {
  if (loading.value) return
  const environmentIssue = loginEnvironmentIssue(apiBaseUrl, Taro.getDeviceInfo().platform)
  if (environmentIssue) {
    message.value = environmentIssue
    return
  }
  loading.value = true
  message.value = ''
  try {
    const { code } = await Taro.login()
    const result = await api.request<{ token: string; session: { nextPath: string } }>('POST', '/auth/owner/wechat-login', { code })
    setSessionToken(result.token)
    const target = result.session.nextPath.includes('frozen')
      ? ownerRoutes.frozen
      : result.session.nextPath.includes('onboarding')
        ? ownerRoutes.onboarding
        : ownerRoutes.dashboard
    await Taro.reLaunch({ url: target })
  } catch (error) {
    message.value = loginFailureMessage(error)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <View class="screen login-screen">
    <View class="login-scene art-login owner-art-login" :style="loginSceneStyle">
      <View class="login-identity" :style="loginIdentityStyle">
        <View class="mini-brand"><Text class="mini-mark">闲</Text><Text>闲遇-店长端</Text></View>
      </View>
      <View class="login-visual">
        <View class="art-scene">
          <View class="paper-window" />
          <View class="sun-cut" />
          <View class="paper-calendar"><Text class="calendar-head" /><Text class="calendar-small">今日</Text><Text class="calendar-main">服务</Text></View>
          <View class="art-ticket blue owner-note-class"><Text class="ticket-title">新时段</Text><Text>发布可约</Text></View>
          <View class="art-ticket gold owner-note-coach"><Text class="ticket-title">员工表</Text><Text>清楚履约</Text></View>
          <View class="casual-person"><View class="person-head" /><View class="person-body" /><View class="person-legs" /></View>
          <View class="paper-phone"><View /></View>
          <View class="scene-desk"><View class="desk-leg left" /><View class="desk-leg right" /></View>
        </View>
      </View>
      <View class="login-copy">
        <Text class="login-title"><Text>从一个服务时段</Text><Text class="login-title-tail">开始运营门店</Text></Text>
        <Text class="login-description">适合康复、护理、咨询、运动训练等预约制门店，先体验完整流程，再按需要联系顾问续期。</Text>
        <View class="login-badge-row"><Text class="login-badge">微信快捷登录</Text><Text class="login-badge">多种服务</Text><Text class="login-badge">免费试用</Text></View>
      </View>
      <View class="login-action">
        <button class="wechat-button" :loading="loading" :disabled="loading" @tap="login">微信一键登录</button>
        <Text v-if="message" class="login-error">{{ message }}</Text>
        <Text class="login-footnote">首次登录后进入开始使用清单，完成必填项后创建门店</Text>
      </View>
    </View>
  </View>
</template>
