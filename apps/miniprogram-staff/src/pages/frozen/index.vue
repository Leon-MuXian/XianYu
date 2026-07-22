<script setup lang="ts">
import { ref } from 'vue'
import { useLoad } from '@tarojs/taro'
import { api } from '../../api'

const info = ref<Record<string, unknown>>({})
useLoad(async () => {
  info.value = await api.request('GET', '/staff/frozen')
})
</script>
<template><View class="page"><Text class="brand">闲遇 · 员工端</Text><Text class="title">门店服务已暂停</Text><Text class="subtitle">名单、到店、核销和服务记录暂不可用。</Text><View class="panel"><Text class="panel-title">{{ info.storeName }}</Text><Text class="subtitle">{{ info.address }}</Text><Text class="subtitle">请联系店长处理恢复。</Text></View></View></template>
