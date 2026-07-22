<script setup lang="ts">
import Taro, { useDidShow, usePullDownRefresh } from '@tarojs/taro'
import { ref } from 'vue'
import { createIdempotencyKey } from '@serenmeet/api-client'
import { cardStatusText } from '@serenmeet/business-components'
import { api } from '../../api'
interface Slot { slotId: number; serviceName: string; staffName: string; resourceName: string; startAt: string; availableCount: number }
interface Card { id: number; name: string; remainCount: number | null; validUntil: string; status: keyof typeof cardStatusText }
const profile = ref<Record<string, unknown>>({})
const slots = ref<Slot[]>([])
const cards = ref<Card[]>([])
const message = ref('')
async function load() {
  try {
    profile.value = await api.request<Record<string, unknown>>('GET', '/member/home')
    if (profile.value.tenantStatus === 'frozen') return Taro.reLaunch({ url: '/pages/frozen/index' })
    const [availableSlots, memberCards] = await Promise.all([
      api.request<Slot[]>('GET', '/member/services'),
      api.request<Card[]>('GET', '/member/cards')
    ])
    slots.value = availableSlots
    cards.value = memberCards
  } catch (error) {
    message.value = error instanceof Error ? error.message : '页面加载失败'
  }
}
async function book(slot:Slot){message.value='';try{const available=await api.request<Card[]>('GET',`/member/cards/available?slotId=${slot.slotId}`);if(!available.length){message.value='暂无可用于该时段的会员卡，请联系门店';return}await api.request('POST','/member/bookings',{slotId:slot.slotId,memberCardId:available[0].id},createIdempotencyKey('booking'));await Taro.showToast({title:'预约成功',icon:'success'});await load()}catch(error){message.value=error instanceof Error?error.message:'预约失败'}}
useDidShow(load);usePullDownRefresh(async()=>{await load();Taro.stopPullDownRefresh()})
</script>
<template><View class="page"><Text class="brand">{{ profile.storeName || '闲遇会员端' }}</Text><Text class="title">可预约时段</Text><View v-if="slots.length" class="panel"><View v-for="slot in slots" :key="slot.slotId" class="list-row"><Text class="row-title">{{ slot.serviceName }}</Text><Text class="row-meta">{{ slot.startAt }} · {{ slot.staffName }} · {{ slot.resourceName }}</Text><Text class="row-meta">剩余 {{ slot.availableCount }} 个名额</Text><Button class="button" :disabled="slot.availableCount < 1" @tap="book(slot)">确认预约</Button></View></View><View v-else class="panel"><Text class="panel-title">暂无可约时段</Text><Text class="subtitle">门店发布新时段后会显示在这里。</Text></View><Text class="title" style="font-size: 20px; margin-top: 28px">我的卡包</Text><View class="panel"><View v-for="card in cards" :key="card.id" class="list-row"><Text class="row-title">{{ card.name }}</Text><Text class="row-meta">{{ card.remainCount === null ? '期限内使用' : `剩余 ${card.remainCount} 次` }} · 至 {{ card.validUntil }}</Text><Text class="status">{{ cardStatusText[card.status] }}</Text></View></View><Text v-if="message" class="message">{{ message }}</Text></View></template>
