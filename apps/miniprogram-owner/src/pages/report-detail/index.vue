<script setup lang="ts">
import { useLoad } from '@tarojs/taro'
import { computed, ref } from 'vue'
import OwnerTabbar from '../../components/OwnerTabbar.vue'
import OwnerTopbar from '../../components/OwnerTopbar.vue'
import { api } from '../../api'
import { guardOwner, messageOf, type CardWarning } from '../../owner'

const tabs = [
  { key: 'sales', label: '售卡', path: '/owner/reports/offline-sales' },
  { key: 'bookings', label: '预约', path: '/owner/reports/bookings' },
  { key: 'attendances', label: '到店', path: '/owner/reports/bookings' },
  { key: 'deductions', label: '核销', path: '/owner/reports/deductions' },
  { key: 'services', label: '服务', path: '/owner/reports/services' },
  { key: 'warnings', label: '提醒', path: '/owner/warnings/cards' }
] as const
const tab = ref<(typeof tabs)[number]['key']>('sales')
const period = ref('month')
const rows = ref<Record<string, unknown>[]>([])
const message = ref('')
const activeConfig = computed(() => tabs.find((item) => item.key === tab.value) || tabs[0])

async function load() {
  try {
    if (!await guardOwner()) return
    const suffix = activeConfig.value.path.includes('/warnings/') ? '' : `?period=${period.value}`
    rows.value = await api.request<Record<string, unknown>[]>('GET', `${activeConfig.value.path}${suffix}`)
  } catch (error) { rows.value = []; message.value = messageOf(error, '报表明细加载失败') }
}

function select(value: typeof tab.value) { tab.value = value; message.value = ''; void load() }
function title(row: Record<string, unknown>) {
  if (tab.value === 'sales') return `${row.memberName || '会员'} · ${row.cardName || '会员卡'}`
  if (tab.value === 'services') return `${row.serviceName || '服务项目'}`
  if (tab.value === 'warnings') return `${row.memberName || '会员'} · ${row.cardName || '会员卡'}`
  return `${row.serviceName || '服务记录'} · ${row.memberName || ''}`
}
function detail(row: Record<string, unknown>) {
  if (tab.value === 'sales') return `¥${row.saleAmountYuan || 0} · ${row.payMethodLabel || '门店实收'} · ${row.saleDate || ''}`
  if (tab.value === 'services') return `预约 ${row.bookingCount || 0} · 到店 ${row.attendanceCount || 0} · 核销 ${row.deductionCount || 0}`
  if (tab.value === 'warnings') return row.remainCount == null ? `有效期至 ${row.validUntil || '--'}` : `剩余 ${row.remainCount} 次`
  return `${row.startAt || row.createdAt || ''} · ${row.status || ''}`
}

useLoad((params) => { if (tabs.some((item) => item.key === params.tab)) tab.value = params.tab as typeof tab.value; if (params.period) period.value = params.period; void load() })
</script>

<template>
  <View class="screen">
    <OwnerTopbar title="报表明细" back />
    <View class="content owner-dense report-dense">
      <View class="owner-tabs six"><Text v-for="item in tabs" :key="item.key" :class="{ active: tab === item.key }" @tap="select(item.key)">{{ item.label }}</Text></View>
      <View v-if="message" class="error-banner">{{ message }}</View>
      <View v-if="!rows.length" class="empty-state"><Text class="empty-title">当前筛选暂无明细</Text><Text>切换上方标签可查看其他经营事实。</Text></View>
      <View v-for="(row, index) in rows" :key="String(row.id || index)" class="owner-list-card"><Text class="list-card-title">{{ title(row) }}</Text><Text class="list-card-copy">{{ detail(row) }}</Text><Text class="tag" :class="tab === 'warnings' ? 'warn' : tab === 'bookings' || tab === 'attendances' ? 'blue' : ''">{{ activeConfig.label }}</Text></View>
      <View class="owner-list-card"><Text class="list-card-title">明细说明</Text><Text class="list-card-copy">售卡、预约、到店、核销、服务表现和会员卡提醒都在本页切换，不新增独立详情页。</Text><Text class="tag blue">说明</Text></View>
    </View>
    <OwnerTabbar active="report" />
  </View>
</template>
