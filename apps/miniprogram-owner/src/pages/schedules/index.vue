<script setup lang="ts">
import { useDidShow, useLoad } from '@tarojs/taro'
import { computed, ref } from 'vue'
import OwnerTabbar from '../../components/OwnerTabbar.vue'
import OwnerTopbar from '../../components/OwnerTopbar.vue'
import { api } from '../../api'
import { formatTime, guardOwner, messageOf, navigate, type Schedule, ownerRoutes } from '../../owner'

const dates = Array.from({ length: 5 }, (_, index) => {
  const date = new Date(); date.setDate(date.getDate() + index)
  const value = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
  return { value, label: index === 0 ? '今天' : ['周日','周一','周二','周三','周四','周五','周六'][date.getDay()], day: String(date.getDate()).padStart(2, '0') }
})
const selectedDate = ref(dates[0].value)
const rows = ref<Schedule[]>([])
const filter = ref<'all' | 'published' | 'draft'>('all')
const message = ref('')
const highlightedId = ref(0)
const filteredRows = computed(() => filter.value === 'all' ? rows.value : rows.value.filter((item) => item.status === filter.value))

async function load() {
  try {
    if (!await guardOwner()) return
    rows.value = await api.request<Schedule[]>('GET', `/owner/schedules?date=${selectedDate.value}`)
  } catch (error) { message.value = messageOf(error, '排期加载失败') }
}

function selectDate(value: string) { selectedDate.value = value; void load() }
useLoad((params) => {
  if (params.date) selectedDate.value = params.date
  highlightedId.value = Number(params.highlight || 0)
})
useDidShow(load)
</script>

<template>
  <View class="screen">
    <OwnerTopbar title="排期" />
    <View class="content owner-dense">
      <View class="owner-page-head"><View><Text class="page-title">{{ selectedDate.slice(5).replace('-', '月') }}日</Text><Text class="page-copy">按日期查看已发布、草稿和需要完善的服务时段。</Text></View><Text class="tag blue">{{ rows.length }} 个</Text></View>
      <View class="calendar-strip"><View v-for="date in dates" :key="date.value" class="day" :class="{ active: selectedDate === date.value }" @tap="selectDate(date.value)"><Text>{{ date.label }}</Text><Text class="day-number">{{ date.day }}</Text></View></View>
      <View class="owner-tabs"><Text :class="{ active: filter === 'all' }" @tap="filter = 'all'">全部</Text><Text :class="{ active: filter === 'published' }" @tap="filter = 'published'">已发布</Text><Text :class="{ active: filter === 'draft' }" @tap="filter = 'draft'">草稿</Text></View>
      <View v-if="message" class="error-banner">{{ message }}</View>
      <View v-if="!filteredRows.length" class="empty-state" @tap="navigate(`${ownerRoutes.scheduleCreate}?date=${selectedDate}`)"><Text class="empty-title">当天还没有排期</Text><Text>新建服务时段后，会员端和员工名单会同步刷新。</Text></View>
      <View v-else class="schedule-lane"><View v-for="slot in filteredRows" :key="slot.id" class="schedule-item" :class="{ 'new-slot': slot.id === highlightedId }"><Text class="hour">{{ formatTime(slot.startAt) }}</Text><View><Text class="schedule-title">{{ slot.serviceName }}</Text><Text class="schedule-copy">{{ slot.staffName }} · {{ slot.resourceName }} · {{ slot.status === 'published' ? '会员可约' : '草稿' }}</Text></View><Text class="tag" :class="slot.reservedCount >= slot.capacity ? 'warn' : slot.status === 'draft' ? 'red' : ''">{{ slot.id === highlightedId ? '新时段' : slot.reservedCount >= slot.capacity ? '满员' : `${slot.reservedCount}/${slot.capacity}` }}</Text></View></View>
      <View class="form-footer"><button class="button secondary" disabled>复制昨日</button><button class="button" @tap="navigate(`${ownerRoutes.scheduleCreate}?date=${selectedDate}`)">新建时段</button></View>
    </View>
    <OwnerTabbar active="schedules" />
  </View>
</template>
