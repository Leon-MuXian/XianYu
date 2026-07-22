<script setup lang="ts">
import { ref } from 'vue'
import { useLoad } from '@tarojs/taro'
import { api } from '../../api'

const info = ref<Record<string, unknown>>({})
const cards = ref<unknown[]>([])
useLoad(async () => {
  const [frozenInfo, memberCards] = await Promise.all([
    api.request<Record<string, unknown>>('GET', '/member/frozen'),
    api.request<unknown[]>('GET', '/member/cards')
  ])
  info.value = frozenInfo
  cards.value = memberCards
})
</script>
<template><View class="page"><Text class="brand">{{ info.storeName || '闲遇会员端' }}</Text><Text class="title">门店服务已暂停</Text><Text class="subtitle">约课与其他业务操作暂不可用，已有会员卡权益仍可查看。</Text><View class="panel"><Text class="panel-title">联系门店</Text><Text class="subtitle">{{ info.contactPhone }}</Text><Text class="subtitle">{{ info.address }}</Text></View><View class="panel"><Text class="panel-title">我的卡包</Text><Text class="subtitle">共 {{ cards.length }} 张会员卡</Text></View></View></template>
