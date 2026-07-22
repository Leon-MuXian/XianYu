<script setup lang="ts">
import { ref } from 'vue'
import { getCurrentInstance, useLoad } from '@tarojs/taro'
import { createIdempotencyKey } from '@serenmeet/api-client'
import { api } from '../../api'
interface Booking { bookingId: number; memberName: string; cardName: string; remainCount: number | null; status: string; deductCount: number }
const bookings = ref<Booking[]>([])
const slotId = ref(0)
const message = ref('')
async function load() { bookings.value = await api.request('GET', `/staff/slots/${slotId.value}/roster`) }
async function attend(item: Booking) { await act(item, '/staff/attendance', 'attendance') }
async function deduct(item: Booking) { await act(item, '/staff/deductions', 'deduction') }
async function act(item: Booking, path: string, scope: string) {
  message.value = ''
  try { await api.request('POST', path, { bookingId: item.bookingId }, createIdempotencyKey(scope)); await load() }
  catch (error) { message.value = error instanceof Error ? error.message : '操作失败' }
}
useLoad(async () => { slotId.value = Number(getCurrentInstance().router?.params.slotId || 0); await load() })
</script>
<template><View class="page"><Text class="brand">履约</Text><Text class="title">预约名单</Text><View class="panel"><View v-for="item in bookings" :key="item.bookingId" class="list-row"><Text class="row-title">{{ item.memberName }}</Text><Text class="row-meta">{{ item.cardName }} · 剩余 {{ item.remainCount ?? '期限内不限次' }} · 本次 {{ item.deductCount }} 次</Text><Text class="status">{{ item.status }}</Text><Button v-if="item.status === 'reserved'" class="button secondary" @tap="attend(item)">确认到店</Button><Button v-if="item.status === 'arrived'" class="button" @tap="deduct(item)">确认核销</Button></View></View><Text v-if="message" class="message">{{ message }}</Text></View></template>
