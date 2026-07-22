<script setup lang="ts">
import { useDidShow } from '@tarojs/taro'
import { computed, reactive, ref } from 'vue'
import OwnerTabbar from '../../components/OwnerTabbar.vue'
import OwnerTopbar from '../../components/OwnerTopbar.vue'
import { api } from '../../api'
import { guardOwner, messageOf, navigate, type OwnerProfile, ownerRoutes } from '../../owner'

interface DashboardMetrics {
  storeReady: boolean
  staffCount: number
  serviceCount: number
  cardTemplateCount: number
  memberCount: number
  futureSlotCount: number
  todayBookings: number
  warningCount: number
}
interface ReportSummary { monthlySalesYuan: number; bookings: number; attendances: number; deductions: number; warningCount: number }

const metrics = reactive<DashboardMetrics>({ storeReady: false, staffCount: 0, serviceCount: 0, cardTemplateCount: 0, memberCount: 0, futureSlotCount: 0, todayBookings: 0, warningCount: 0 })
const report = reactive<ReportSummary>({ monthlySalesYuan: 0, bookings: 0, attendances: 0, deductions: 0, warningCount: 0 })
const profile = ref<OwnerProfile | null>(null)
const loading = ref(true)
const message = ref('')
const operational = computed(() => metrics.staffCount > 0 && metrics.serviceCount > 0 && metrics.cardTemplateCount > 0 && metrics.memberCount > 0 && metrics.futureSlotCount > 0)
const pendingCount = computed(() => [metrics.staffCount, metrics.serviceCount, metrics.cardTemplateCount, metrics.memberCount, metrics.futureSlotCount].filter((value) => !value).length)

async function load() {
  loading.value = true
  message.value = ''
  try {
    profile.value = await guardOwner()
    if (!profile.value) return
    const [dashboard, summary] = await Promise.all([
      api.request<DashboardMetrics>('GET', '/owner/dashboard'),
      api.request<ReportSummary>('GET', '/owner/reports/summary')
    ])
    Object.assign(metrics, dashboard)
    Object.assign(report, summary)
  } catch (error) { message.value = messageOf(error, '工作台加载失败') } finally { loading.value = false }
}

const tasks = computed(() => [
  { key: 'staff', title: '创建员工账号', copy: '为可履约人员分配登录账号，后续服务才能绑定员工。', count: metrics.staffCount, action: '去创建', url: ownerRoutes.staff },
  { key: 'service', title: '创建服务项目', copy: '设置名称、时长、容量和适用员工，会员才能看到服务。', count: metrics.serviceCount, action: '去设置', url: ownerRoutes.services },
  { key: 'card', title: '创建会员卡模板', copy: '录入售价、次数、有效期和适用服务，用于发卡和预约校验。', count: metrics.cardTemplateCount, action: '去创建', url: ownerRoutes.cards },
  { key: 'member', title: '添加会员并发卡', copy: '生成邀请码交给会员绑定，会员才能使用卡预约。', count: metrics.memberCount, action: '去添加', url: ownerRoutes.memberIssue },
  { key: 'slot', title: '发布预约时段', copy: '选择服务、员工、资源、日期和时间，开放会员预约。', count: metrics.futureSlotCount, action: '去发布', url: ownerRoutes.scheduleCreate }
])

useDidShow(load)
</script>

<template>
  <View class="screen">
    <OwnerTopbar title="门店工作台" />
    <View class="content owner-dense">
      <View v-if="loading" class="loading-state">正在加载门店工作台...</View>
      <template v-else>
        <View v-if="message" class="error-banner">{{ message }}</View>
        <template v-if="!operational">
          <View class="owner-page-head"><View><Text class="page-title">开放预约前还差 {{ pendingCount }} 项</Text><Text class="page-copy">{{ profile?.name || '门店' }}已创建。先补齐员工、服务、会员卡、会员和首个时段。</Text></View><Text class="tag warn">待配置</Text></View>
          <View class="owner-card-grid"><View class="tile"><Text>已完成必填</Text><Text class="tile-value">3 项</Text></View><View class="tile"><Text>待配置</Text><Text class="tile-value">{{ pendingCount }} 项</Text></View></View>
          <Text class="section-title">下一步待办</Text>
          <View class="owner-progress">
            <View v-for="(task, index) in tasks" :key="task.key" class="owner-step" :class="task.count ? 'done' : 'pending'" @tap="navigate(task.url)">
              <Text class="step-glyph">{{ task.count ? '✓' : index + 1 }}</Text><View><Text class="step-title">{{ task.title }}</Text><Text class="step-copy">{{ task.count ? `已完成，当前 ${task.count} 项` : task.copy }}</Text></View><Text class="tag" :class="task.count ? '' : 'blue'">{{ task.count ? '已完成' : task.action }}</Text>
            </View>
          </View>
        </template>
        <template v-else>
          <View class="owner-page-head"><View><Text class="page-title">今日权益闭环</Text><Text class="page-copy">按发卡、到店、核销、预警顺序处理，先保证会员权益账是准的。</Text></View><Text class="tag blue">试点主线</Text></View>
          <Text class="section-title">今天先看</Text>
          <View class="owner-status-strip"><View class="owner-status"><Text>今日预约</Text><Text class="status-value">{{ metrics.todayBookings }}</Text><Text class="status-note blue-text">员工确认</Text></View><View class="owner-status"><Text>累计核销</Text><Text class="status-value">{{ report.deductions }}</Text><Text class="status-note gold-text">服务后</Text></View><View class="owner-status"><Text>卡预警</Text><Text class="status-value">{{ metrics.warningCount }}</Text><Text class="status-note">低余额/到期</Text></View></View>
          <Text class="section-title">处理队列</Text>
          <View class="owner-progress">
            <View class="owner-step pending" @tap="navigate(ownerRoutes.schedules)"><Text class="step-glyph">1</Text><View><Text class="step-title">员工确认到店</Text><Text class="step-copy">今日 {{ metrics.todayBookings }} 个预约待按实际履约确认。</Text></View><Text class="tag blue">去排期</Text></View>
            <View class="owner-step pending" @tap="navigate(ownerRoutes.reportDetail + '?tab=deductions')"><Text class="step-glyph">2</Text><View><Text class="step-title">服务后完成核销</Text><Text class="step-copy">查看服务后的扣卡事实与余额变化。</Text></View><Text class="tag warn">去核销</Text></View>
            <View class="owner-step pending" @tap="navigate(ownerRoutes.members + '?tab=cards')"><Text class="step-glyph">3</Text><View><Text class="step-title">跟进会员卡预警</Text><Text class="step-copy">当前 {{ metrics.warningCount }} 张卡需要低余额或到期跟进。</Text></View><Text class="tag">去处理</Text></View>
          </View>
          <Text class="section-title">常用动作</Text>
          <View class="quick-grid"><View class="quick" @tap="navigate(ownerRoutes.memberIssue)"><Text class="quick-icon">卡</Text><Text>发卡</Text></View><View class="quick" @tap="navigate(ownerRoutes.members + '?tab=cards')"><Text class="quick-icon">会</Text><Text>会员卡</Text></View><View class="quick" @tap="navigate(ownerRoutes.reportDetail + '?tab=deductions')"><Text class="quick-icon">核</Text><Text>核销明细</Text></View><View class="quick" @tap="navigate(ownerRoutes.members + '?tab=warnings')"><Text class="quick-icon">续</Text><Text>续卡跟进</Text></View></View>
          <Text class="section-title">异常提醒</Text>
          <View class="owner-progress"><View class="owner-step"><Text class="step-glyph">绑</Text><View><Text class="step-title">会员绑定待跟进</Text><Text class="step-copy">发卡后未绑定的会员可重新生成邀请码。</Text></View><Text class="tag warn">提醒</Text></View><View class="owner-step"><Text class="step-glyph">卡</Text><View><Text class="step-title">{{ metrics.warningCount }} 张会员卡提醒</Text><Text class="step-copy">已过期会员卡只读，不可用于预约和核销。</Text></View><Text class="tag red">关注</Text></View></View>
          <Text class="section-title">试点摘要</Text>
          <View class="owner-card-grid"><View class="tile"><Text>会员</Text><Text class="tile-value">{{ metrics.memberCount }}</Text></View><View class="tile"><Text>本月售卡</Text><Text class="tile-value">¥{{ report.monthlySalesYuan }}</Text></View><View class="tile"><Text>累计核销</Text><Text class="tile-value">{{ report.deductions }}</Text></View><View class="tile" @tap="navigate(ownerRoutes.report)"><Text>经营明细</Text><Text class="tile-value">看报表</Text></View></View>
        </template>
      </template>
    </View>
    <OwnerTabbar active="dashboard" />
  </View>
</template>
