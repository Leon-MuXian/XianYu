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
  resourceCount: number
  memberCount: number
  futureSlotCount: number
  todayBookings: number
  todayPendingFulfillment: number
  todayCompletedFulfillment: number
  warningCount: number
  scheduleReadiness: {
    ready: boolean
    missing: string[]
    draftCount: number
    nextDraftId?: number
    nextDraftDate?: string
  }
}
const metrics = reactive<DashboardMetrics>({
  storeReady: false,
  staffCount: 0,
  serviceCount: 0,
  cardTemplateCount: 0,
  resourceCount: 0,
  memberCount: 0,
  futureSlotCount: 0,
  todayBookings: 0,
  todayPendingFulfillment: 0,
  todayCompletedFulfillment: 0,
  warningCount: 0,
  scheduleReadiness: { ready: false, missing: [], draftCount: 0 }
})
const profile = ref<OwnerProfile | null>(null)
const loading = ref(true)
const message = ref('')
const operational = computed(() => metrics.staffCount > 0 && metrics.serviceCount > 0 && metrics.cardTemplateCount > 0 && metrics.memberCount > 0 && metrics.futureSlotCount > 0)
const pendingCount = computed(() => [metrics.staffCount, metrics.serviceCount, metrics.cardTemplateCount, metrics.memberCount, metrics.futureSlotCount].filter((value) => !value).length)
const setupCopy = computed(() => metrics.futureSlotCount > 0
  ? `${profile.value?.name || '门店'}已创建。继续补齐剩余运营配置。`
  : `${profile.value?.name || '门店'}已创建。先补齐员工、服务、会员卡、会员和首个时段。`)

async function load() {
  loading.value = true
  message.value = ''
  try {
    profile.value = await guardOwner()
    if (!profile.value) return
    const dashboard = await api.request<DashboardMetrics>('GET', '/owner/dashboard')
    Object.assign(metrics, dashboard)
  } catch (error) { message.value = messageOf(error, '工作台加载失败') } finally { loading.value = false }
}

const scheduleTask = computed(() => {
  const firstMissing = metrics.scheduleReadiness.missing[0]
  const missingRoutes: Record<string, string> = {
    store: ownerRoutes.store,
    service: ownerRoutes.services,
    staff: ownerRoutes.staff,
    resource: ownerRoutes.resources,
    card: ownerRoutes.cards,
    scope: ownerRoutes.services
  }
  if (!metrics.scheduleReadiness.ready) {
    return {
      copy: firstMissing === 'scope'
        ? '服务、员工、资源或会员卡适用范围还未形成完整组合。'
        : '先补齐发布时段需要的服务、员工、资源和会员卡。',
      action: '完善配置',
      url: missingRoutes[firstMissing] || ownerRoutes.services
    }
  }
  if (!metrics.futureSlotCount && metrics.scheduleReadiness.draftCount > 0) {
    const query = ['intent=edit']
    if (metrics.scheduleReadiness.nextDraftDate) query.push(`date=${metrics.scheduleReadiness.nextDraftDate}`)
    if (metrics.scheduleReadiness.nextDraftId) query.push(`highlight=${metrics.scheduleReadiness.nextDraftId}`)
    return {
      copy: `已有 ${metrics.scheduleReadiness.draftCount} 个草稿，继续检查并发布首个预约时段。`,
      action: '继续草稿',
      url: `${ownerRoutes.schedules}?${query.join('&')}`
    }
  }
  return {
    copy: '在排期页保留当前日期，并用底部抽屉完成首个时段发布。',
    action: '去发布',
    url: `${ownerRoutes.schedules}?intent=create&source=dashboard`
  }
})

interface FollowUpItem {
  key: string
  glyph: string
  title: string
  copy: string
  action: string
  tagClass: string
  url: string
}

const followUpItems = computed<FollowUpItem[]>(() => {
  const items: FollowUpItem[] = []
  if (metrics.warningCount > 0) {
    items.push({
      key: 'card-warning',
      glyph: '卡',
      title: `${metrics.warningCount} 张会员卡需要跟进`,
      copy: '低余额或即将到期，安排后续服务。',
      action: '去跟进',
      tagClass: 'warn',
      url: `${ownerRoutes.members}?tab=warnings`
    })
  }
  if (metrics.scheduleReadiness.draftCount > 0) {
    items.push({
      key: 'schedule-draft',
      glyph: '排',
      title: `${metrics.scheduleReadiness.draftCount} 个排期草稿待处理`,
      copy: '检查内容后发布，避免会员端看不到可预约时段。',
      action: '去排期',
      tagClass: 'blue',
      url: ownerRoutes.schedules
    })
  }
  return items
})

const tasks = computed(() => {
  const baseTasks = [
    { key: 'staff', title: '创建员工账号', copy: '为可履约人员分配登录账号，后续服务才能绑定员工。', count: metrics.staffCount, action: '去创建', url: ownerRoutes.staff },
    { key: 'service', title: '创建服务项目', copy: '设置名称、时长、容量和适用员工，会员才能看到服务。', count: metrics.serviceCount, action: '去设置', url: ownerRoutes.services },
    { key: 'card', title: '创建会员卡', copy: '录入售价、次数、有效期和适用服务，用于发卡和预约校验。', count: metrics.cardTemplateCount, action: '去创建', url: ownerRoutes.cards },
    { key: 'member', title: '添加会员并发卡', copy: '生成邀请码交给会员绑定，会员才能使用卡预约。', count: metrics.memberCount, action: '去添加', url: ownerRoutes.memberIssue }
  ]
  if (metrics.futureSlotCount > 0) return baseTasks
  return [...baseTasks, {
    key: 'slot',
    title: '发布预约时段',
    copy: scheduleTask.value.copy,
    count: metrics.futureSlotCount,
    action: scheduleTask.value.action,
    url: scheduleTask.value.url
  }]
})

useDidShow(load)
</script>

<template>
  <View class="screen dashboard-screen">
    <OwnerTopbar title="门店工作台" />
    <scroll-view :scroll-y="true" :enhanced="true" :show-scrollbar="false" class="dashboard-page-scroll">
      <View class="content owner-dense">
      <View v-if="loading" class="loading-state">正在加载门店工作台...</View>
      <template v-else>
        <View v-if="message" class="error-banner">{{ message }}</View>
        <template v-if="!operational">
          <View class="owner-page-head"><View><Text class="page-title">开放预约前还差 {{ pendingCount }} 项</Text><Text class="page-copy">{{ setupCopy }}</Text></View><Text class="tag warn">待配置</Text></View>
          <View class="owner-card-grid"><View class="tile"><Text>已完成必填</Text><Text class="tile-value">3 项</Text></View><View class="tile"><Text>待配置</Text><Text class="tile-value">{{ pendingCount }} 项</Text></View></View>
          <Text class="section-title">下一步待办</Text>
          <View class="owner-progress">
            <View v-for="(task, index) in tasks" :key="task.key" class="owner-step" :class="task.count ? 'done' : 'pending'" @tap="navigate(task.url)">
              <Text class="step-glyph">{{ task.count ? '✓' : index + 1 }}</Text><View><Text class="step-title">{{ task.title }}</Text><Text class="step-copy">{{ task.count ? `已完成，当前 ${task.count} 项` : task.copy }}</Text></View><Text class="tag" :class="task.count ? '' : 'blue'">{{ task.count ? '已完成' : task.action }}</Text>
            </View>
          </View>
        </template>
        <template v-else>
          <View class="owner-page-head"><View><Text class="page-title">今日履约</Text><Text class="page-copy">{{ profile?.name || '门店' }} · 员工端正在处理今日服务</Text></View><Text class="tag">运营中</Text></View>
          <View class="owner-hero"><Text class="hero-title">今天有 {{ metrics.todayBookings }} 个预约</Text><Text class="hero-copy">员工端完成到店确认和权益处理，店长关注完成情况与经营提醒。</Text><View class="owner-hero-meta"><Text>已完成 {{ metrics.todayCompletedFulfillment }} / {{ metrics.todayBookings }}</Text><Text @tap="navigate(ownerRoutes.schedules)">查看排期</Text></View></View>
          <Text class="section-title">今天先看</Text>
          <View class="owner-status-strip"><View class="owner-status"><Text>待履约</Text><Text class="status-value">{{ metrics.todayPendingFulfillment }}</Text><Text class="status-note blue-text">员工端处理中</Text></View><View class="owner-status"><Text>已完成</Text><Text class="status-value">{{ metrics.todayCompletedFulfillment }}</Text><Text class="status-note">到店并完成服务</Text></View><View class="owner-status"><Text>会员卡提醒</Text><Text class="status-value">{{ metrics.warningCount }}</Text><Text class="status-note gold-text">余额/到期跟进</Text></View></View>
          <template v-if="followUpItems.length">
            <Text class="section-title">店长要跟进</Text>
            <View class="owner-progress"><View v-for="item in followUpItems" :key="item.key" class="owner-step pending" @tap="navigate(item.url)"><Text class="step-glyph">{{ item.glyph }}</Text><View><Text class="step-title">{{ item.title }}</Text><Text class="step-copy">{{ item.copy }}</Text></View><Text class="tag" :class="item.tagClass">{{ item.action }}</Text></View></View>
          </template>
          <Text class="section-title">常用动作</Text>
          <View class="quick-grid"><View class="quick" @tap="navigate(ownerRoutes.memberIssue)"><Text class="quick-icon">卡</Text><Text>发卡</Text></View><View class="quick" @tap="navigate(ownerRoutes.members + '?tab=cards')"><Text class="quick-icon">会</Text><Text>会员卡</Text></View><View class="quick" @tap="navigate(ownerRoutes.schedules)"><Text class="quick-icon">排</Text><Text>排期</Text></View><View class="quick" @tap="navigate(ownerRoutes.report)"><Text class="quick-icon">报</Text><Text>经营报表</Text></View></View>
        </template>
      </template>
      </View>
    </scroll-view>
    <OwnerTabbar active="dashboard" />
  </View>
</template>
