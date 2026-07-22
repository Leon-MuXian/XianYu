<script setup lang="ts">
import { useDidShow } from '@tarojs/taro'
import { reactive, ref } from 'vue'
import OwnerTabbar from '../../components/OwnerTabbar.vue'
import OwnerTopbar from '../../components/OwnerTopbar.vue'
import { api } from '../../api'
import { formatMoney, guardOwner, messageOf, navigate, type CardWarning, ownerRoutes } from '../../owner'

interface Summary { monthlySalesYuan: number; bookings: number; attendances: number; deductions: number; warningCount: number }
const summary = reactive<Summary>({ monthlySalesYuan: 0, bookings: 0, attendances: 0, deductions: 0, warningCount: 0 })
const warnings = ref<CardWarning[]>([])
const period = ref('month')
const message = ref('')

async function load() {
  try {
    if (!await guardOwner()) return
    Object.assign(summary, await api.request<Summary>('GET', `/owner/reports/summary?period=${period.value}`))
    warnings.value = await api.request<CardWarning[]>('GET', '/owner/warnings/cards')
  } catch (error) { message.value = messageOf(error, '报表加载失败') }
}

function switchPeriod(value: string) { period.value = value; void load() }
const detail = (tab: string) => navigate(`${ownerRoutes.reportDetail}?tab=${tab}&period=${period.value}`)
useDidShow(load)
</script>

<template>
  <View class="screen">
    <OwnerTopbar title="门店报表" />
    <View class="content owner-dense report-dense">
      <View class="owner-page-head"><View><Text class="page-title">本月经营概览</Text><Text class="page-copy">先看四个结果，再进入明细处理到店、核销和会员卡提醒。</Text></View><button class="button secondary period-button">换月份</button></View>
      <View class="owner-tabs four"><Text :class="{ active: period === 'month' }" @tap="switchPeriod('month')">本月</Text><Text :class="{ active: period === 'last_month' }" @tap="switchPeriod('last_month')">上月</Text><Text :class="{ active: period === 'selected' }" @tap="switchPeriod('selected')">选月份</Text><Text :class="{ active: period === 'all' }" @tap="switchPeriod('all')">全部</Text></View>
      <View class="member-pass" @tap="detail('sales')"><Text class="pass-title">销售额</Text><Text class="pass-value">¥{{ formatMoney(summary.monthlySalesYuan) }}</Text><Text class="pass-copy">全部为门店线下实收 · 进入售卡明细</Text></View>
      <View class="owner-card-grid"><View class="tile" @tap="detail('bookings')"><Text>预约</Text><Text class="tile-value">{{ summary.bookings }} 次</Text><Text class="tile-copy">含取消与候补事实</Text></View><View class="tile" @tap="detail('attendances')"><Text>到店</Text><Text class="tile-value">{{ summary.attendances }} 人</Text><Text class="tile-copy">员工确认记录</Text></View><View class="tile" @tap="detail('deductions')"><Text>核销</Text><Text class="tile-value">{{ summary.deductions }} 次</Text><Text class="tile-copy">服务后扣卡记录</Text></View><View class="tile" @tap="detail('warnings')"><Text>会员卡提醒</Text><Text class="tile-value">{{ summary.warningCount }} 条</Text><Text class="tile-copy">低余额与到期</Text></View></View>
      <Text class="section-title">点哪里看明细</Text>
      <View class="owner-progress"><View class="owner-step" @tap="detail('sales')"><Text class="step-glyph">售</Text><View><Text class="step-title">售卡明细</Text><Text class="step-copy">看会员、卡名、实收金额和售卡日期。</Text></View><Text class="tag">去看</Text></View><View class="owner-step" @tap="detail('bookings')"><Text class="step-glyph">约</Text><View><Text class="step-title">预约到店</Text><Text class="step-copy">把预约、候补、取消、到店放在一起。</Text></View><Text class="tag blue">去看</Text></View><View class="owner-step" @tap="detail('services')"><Text class="step-glyph">服</Text><View><Text class="step-title">服务表现</Text><Text class="step-copy">看哪些服务预约多、到店多、核销多。</Text></View><Text class="tag">去看</Text></View><View class="owner-step" @tap="detail('warnings')"><Text class="step-glyph">卡</Text><View><Text class="step-title">会员卡提醒</Text><Text class="step-copy">低余额、14 天内到期集中在这里处理。</Text></View><Text class="tag warn">处理</Text></View></View>
      <View class="chart-card"><Text class="chart-title">近 5 天预约 / 核销</Text><View class="bars"><View style="height:54%;background:#c9dded"/><View style="height:46%;background:#2f7d6e"/><View style="height:72%;background:#c9dded"/><View style="height:58%;background:#2f7d6e"/><View style="height:64%;background:#c9dded"/><View style="height:51%;background:#2f7d6e"/><View style="height:86%;background:#c9dded"/><View style="height:73%;background:#2f7d6e"/><View style="height:78%;background:#c9dded"/><View style="height:66%;background:#2f7d6e"/></View><Text class="chart-note">蓝色是预约，绿色是核销；用于判断预约有没有转成实际服务。</Text></View>
      <Text class="section-title">今天先处理</Text>
      <View class="owner-list-card" @tap="detail('attendances')"><Text class="list-card-title">到店确认</Text><Text class="list-card-copy">提醒员工在名单里确认到店，确认后才能核销。</Text><Text class="tag blue">去名单</Text></View>
      <View class="owner-list-card" @tap="detail('warnings')"><Text class="list-card-title">{{ warnings.length }} 条会员卡提醒</Text><Text class="list-card-copy">优先联系低余额和 14 天内到期会员，减少服务中断。</Text><Text class="tag">去处理</Text></View>
      <View v-if="message" class="error-banner">{{ message }}</View>
    </View>
    <OwnerTabbar active="report" />
  </View>
</template>
