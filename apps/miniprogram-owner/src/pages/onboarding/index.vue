<script setup lang="ts">
import Taro, { useDidShow } from '@tarojs/taro'
import { computed, ref } from 'vue'
import { createIdempotencyKey } from '@serenmeet/api-client'
import { presentBusinessHours } from '@serenmeet/business-components'
import OwnerTopbar from '../../components/OwnerTopbar.vue'
import { api } from '../../api'
import { guardOwner, messageOf, navigate, type OnboardingDraft, ownerRoutes } from '../../owner'

const draft = ref<OnboardingDraft | null>(null)
const loading = ref(true)
const creating = ref(false)
const message = ref('')
const showTrial = ref(false)
const acknowledgingTrial = ref(false)
const trialNoticeError = ref('')

const completionCount = computed(() => {
  if (!draft.value) return 0
  return Object.values(draft.value.completion).filter(Boolean).length
})
const ready = computed(() => completionCount.value === 3)
const profile = computed(() => draft.value?.storeProfile || {})
const resources = computed(() => draft.value?.resources || [])
const hoursSummary = computed(() => presentBusinessHours(draft.value?.businessHours).summary)

async function load() {
  loading.value = true
  message.value = ''
  try {
    const owner = await guardOwner()
    if (!owner) return
    if (owner.storeId) {
      await Taro.reLaunch({ url: ownerRoutes.dashboard })
      return
    }
    draft.value = await api.request<OnboardingDraft>('GET', '/owner/onboarding/draft')
    showTrial.value = draft.value.trialNoticeRequired
    trialNoticeError.value = ''
  } catch (error) {
    message.value = messageOf(error, '开始使用清单加载失败')
  } finally {
    loading.value = false
  }
}

async function acknowledgeTrialNotice() {
  if (acknowledgingTrial.value) return
  acknowledgingTrial.value = true
  trialNoticeError.value = ''
  try {
    await api.request('POST', '/owner/onboarding/trial-notice/acknowledge')
    if (draft.value) draft.value.trialNoticeRequired = false
    showTrial.value = false
  } catch (error) {
    trialNoticeError.value = messageOf(error, '确认失败，请重试')
  } finally {
    acknowledgingTrial.value = false
  }
}

async function complete() {
  if (!ready.value || creating.value) return
  creating.value = true
  message.value = ''
  try {
    await api.request('POST', '/owner/onboarding/complete', {}, createIdempotencyKey('owner-onboarding'))
    await Taro.showToast({ title: '门店已创建', icon: 'success' })
    await Taro.reLaunch({ url: ownerRoutes.dashboard })
  } catch (error) {
    message.value = messageOf(error, '创建门店失败')
  } finally {
    creating.value = false
  }
}

useDidShow(load)
</script>

<template>
  <View class="screen onboarding-screen">
    <OwnerTopbar title="开始使用" />
    <View class="content owner-dense onboarding-content" :class="{ 'onboarding-ready': ready }">
      <View v-if="loading" class="loading-state">正在加载开店清单...</View>
      <template v-else-if="draft">
        <View class="owner-hero">
          <Text class="hero-title">{{ ready ? '可以创建门店了' : '完成 3 项后创建门店' }}</Text>
          <Text class="hero-copy">{{ ready ? '门店资料、营业时间和履约资源已保存。创建后进入工作台，继续补齐后续运营设置。' : '先在清单里保存门店资料、营业时间和履约资源。创建门店按钮会在必填项完成后自动解锁。' }}</Text>
          <View class="owner-hero-meta"><Text>{{ ready ? '必填完成' : '清单引导' }}</Text><Text>{{ ready ? '进入工作台' : '草稿保存' }}</Text></View>
        </View>
        <View class="owner-progress">
          <View class="owner-step required-action" :class="draft.completion.storeProfileDone ? 'done' : 'pending'" @tap="navigate(ownerRoutes.store)">
            <Text class="step-glyph">{{ draft.completion.storeProfileDone ? '✓' : '1' }}</Text>
            <View><Text class="step-title">门店资料</Text><Text class="step-copy">{{ draft.completion.storeProfileDone ? `${profile.name || '已填写'} · ${(profile.serviceScopes || []).join('、')}` : '名称、服务范围、地址、电话' }}</Text></View>
            <Text class="tag" :class="draft.completion.storeProfileDone ? '' : 'blue'">{{ draft.completion.storeProfileDone ? '已完成' : '去填写' }}</Text>
          </View>
          <View class="owner-step required-action" :class="draft.completion.businessHoursDone ? 'done' : 'pending'" @tap="navigate(ownerRoutes.hours)">
            <Text class="step-glyph">{{ draft.completion.businessHoursDone ? '✓' : '2' }}</Text>
            <View><Text class="step-title">营业时间</Text><Text class="step-copy">{{ draft.completion.businessHoursDone ? hoursSummary : '按星期设置营业/休息和开始结束时间' }}</Text></View>
            <Text class="tag" :class="draft.completion.businessHoursDone ? '' : 'blue'">{{ draft.completion.businessHoursDone ? '已完成' : '去填写' }}</Text>
          </View>
          <View class="owner-step required-action" :class="draft.completion.resourceDone ? 'done' : 'pending'" @tap="navigate(ownerRoutes.resources)">
            <Text class="step-glyph">{{ draft.completion.resourceDone ? '✓' : '3' }}</Text>
            <View><Text class="step-title">履约资源</Text><Text class="step-copy">{{ draft.completion.resourceDone ? `${resources.length} 个启用资源，可用于服务项目和排期` : '至少 1 个启用资源，支持房间、场地、床位等' }}</Text></View>
            <Text class="tag" :class="draft.completion.resourceDone ? '' : 'blue'">{{ draft.completion.resourceDone ? '已完成' : '去填写' }}</Text>
          </View>
        </View>
        <View v-if="message" class="error-banner">{{ message }}</View>
        <button class="button create-store" :disabled="!ready || creating" :loading="creating" @tap="complete">创建门店</button>
      </template>
    </View>
    <View v-if="showTrial" class="trial-backdrop">
      <View class="trial-dialog">
        <Text class="trial-badge">新用户权益</Text><Text class="dialog-title">欢迎使用闲遇</Text><Text class="dialog-copy">你是新用户，已获得 {{ draft?.trialDays || 30 }} 天免费使用权益。</Text>
        <View class="trial-benefit"><Text>免费试用</Text><Text class="benefit-value">{{ draft?.trialDays || 30 }} 天</Text></View>
        <Text class="dialog-copy">免费使用期内开放完整功能，支持体验从门店配置到预约履约的全流程。</Text>
        <View v-if="trialNoticeError" class="error-banner">{{ trialNoticeError }}</View>
        <button class="button" :loading="acknowledgingTrial" :disabled="acknowledgingTrial" @tap="acknowledgeTrialNotice">我知道了</button>
      </View>
    </View>
  </View>
</template>
