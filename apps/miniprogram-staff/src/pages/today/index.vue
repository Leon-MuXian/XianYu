<script setup lang="ts">
import Taro, { useDidShow, usePullDownRefresh } from '@tarojs/taro'
import { ref } from 'vue'
import { api } from '../../api'
interface Slot { slotId: number; serviceName: string; resourceName: string; startAt: string; reservedCount: number; waitingArrival: number; waitingDeduction: number }
const slots = ref<Slot[]>([])
const account = ref<Record<string, unknown>>({})
const message = ref('')
async function load() {
  try {
    account.value = await api.request('GET', '/staff/account')
    if (account.value.tenantStatus === 'frozen') return Taro.reLaunch({ url: '/pages/frozen/index' })
    slots.value = await api.request('GET', '/staff/today')
  } catch (error) { message.value = error instanceof Error ? error.message : '今日服务加载失败' }
}
function open(slotId: number) { Taro.navigateTo({ url: `/pages/roster/index?slotId=${slotId}` }) }
useDidShow(load)
usePullDownRefresh(async () => { await load(); Taro.stopPullDownRefresh() })
</script>
<template><View class="page"><Text class="brand">{{ account.storeName || '闲遇员工端' }}</Text><Text class="title">今日服务</Text><View v-if="slots.length" class="panel"><View v-for="slot in slots" :key="slot.slotId" class="list-row" @tap="open(slot.slotId)"><Text class="row-title">{{ slot.serviceName }}</Text><Text class="row-meta">{{ slot.startAt }} · {{ slot.resourceName }}</Text><Text class="row-meta">预约 {{ slot.reservedCount }} · 待到店 {{ slot.waitingArrival }} · 待核销 {{ slot.waitingDeduction }}</Text></View></View><View v-else class="panel"><Text class="panel-title">今天暂无服务</Text><Text class="subtitle">历史记录和个人资料保持可用。</Text></View><Text v-if="message" class="message">{{ message }}</Text></View></template>
