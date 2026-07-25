<script setup lang="ts">
import Taro, { useLoad } from '@tarojs/taro'
import { computed, reactive, ref } from 'vue'
import { createIdempotencyKey } from '@serenmeet/api-client'
import OwnerTopbar from '../../components/OwnerTopbar.vue'
import { api } from '../../api'
import { messageOf, type DayHours, type OnboardingDraft, ownerRoutes } from '../../owner'

const dayMeta = [
  ['monday', '一'], ['tuesday', '二'], ['wednesday', '三'], ['thursday', '四'], ['friday', '五'], ['saturday', '六'], ['sunday', '日']
] as const
const days = reactive<Record<string, DayHours>>(Object.fromEntries(dayMeta.map(([key], index) => [key, { open: index < 5, start: '09:00', end: '21:00' }])))
const saving = ref(false)
const message = ref('')
const storeMode = ref(false)
const openDayCount = computed(() => dayMeta.filter(([key]) => days[key].open).length)

async function load(params?: Record<string, string>) {
  storeMode.value = params?.mode === 'store'
  try {
    if (storeMode.value) {
      const store = await api.request<{ businessHours?: { days?: Record<string, DayHours> } }>('GET', '/owner/store')
      if (store.businessHours?.days) Object.assign(days, store.businessHours.days)
    } else {
      const draft = await api.request<OnboardingDraft>('GET', '/owner/onboarding/draft')
      if (draft.businessHours?.days) Object.assign(days, draft.businessHours.days)
    }
  } catch (error) { message.value = messageOf(error, '营业时间加载失败') }
}

function toggle(key: string) { days[key].open = !days[key].open }
function setTime(key: string, field: 'start' | 'end', event: { detail: { value: string } }) { days[key][field] = event.detail.value }

async function save() {
  const openDays = Object.values(days).filter((day) => day.open)
  if (!openDays.length) { message.value = '请至少选择一个营业日'; return }
  if (openDays.some((day) => !day.start || !day.end || day.end <= day.start)) { message.value = '结束时间必须晚于开始时间'; return }
  saving.value = true
  message.value = ''
  try {
    if (storeMode.value) {
      await api.request('PUT', '/owner/store', { days }, createIdempotencyKey('hours-update'))
    } else {
      await api.request('PUT', '/owner/onboarding/business-hours', { days })
    }
    await Taro.showToast({ title: '营业时间已保存', icon: 'success' })
    if (storeMode.value) await Taro.navigateBack()
    else await Taro.redirectTo({ url: ownerRoutes.onboarding })
  } catch (error) { message.value = messageOf(error, '营业时间保存失败') } finally { saving.value = false }
}

useLoad((params) => { void load(params) })
</script>

<template>
  <View class="screen">
    <OwnerTopbar title="营业时间" back />
    <View class="content ohours-flow">
      <View class="ohours-guide"><Text class="guide-title">{{ storeMode ? '正在修改营业时间' : '正在设置营业时间' }}</Text><Text class="guide-copy">从周一至周日选择营业日，再逐日填写营业开始和结束时间。</Text></View>
      <View class="ohours-card">
        <View class="card-title"><Text>选择营业日 <Text class="required-mark">必填</Text></Text><Text class="card-caption">已选择 {{ openDayCount }} 天</Text></View>
        <View class="ohours-week-grid"><View v-for="([key, label]) in dayMeta" :key="key" class="ohours-day" :class="{ rest: !days[key].open }" @tap="toggle(key)"><Text>{{ label }}</Text><Text>{{ days[key].open ? '营业' : '休息' }}</Text></View></View>
        <Text class="card-help">周一至周日统一在这里切换；选中后自动加入下方逐日时间设置。</Text>
      </View>
      <View class="ohours-card">
        <View class="card-title"><Text>营业时间 <Text class="required-mark">必填</Text></Text><Text class="card-caption">逐日设置</Text></View>
        <View v-for="([key, label]) in dayMeta.filter(([value]) => days[value].open)" :key="key" class="day-time-row">
          <Text class="day-label">周{{ label }}</Text>
          <picker mode="time" :value="days[key].start" @change="setTime(key, 'start', $event)"><View class="time-picker">{{ days[key].start }} ▾</View></picker>
          <Text class="time-mid">至</Text>
          <picker mode="time" :value="days[key].end" @change="setTime(key, 'end', $event)"><View class="time-picker">{{ days[key].end }} ▾</View></picker>
        </View>
      </View>
      <View v-if="message" class="error-banner">{{ message }}</View>
      <View class="form-footer"><button class="button secondary" @tap="Taro.navigateBack()">返回</button><button class="button" :loading="saving" :disabled="saving" @tap="save">{{ storeMode ? '保存修改' : '保存并返回清单' }}</button></View>
    </View>
  </View>
</template>
