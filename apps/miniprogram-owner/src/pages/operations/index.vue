<script setup lang="ts">
import Taro, { useDidShow } from '@tarojs/taro'
import { computed, reactive, ref } from 'vue'
import { createIdempotencyKey } from '@serenmeet/api-client'
import { api } from '../../api'

interface Entity { id: number; name?: string; staffName?: string; memberNo?: string }
interface Summary { salesAmountYuan?: number; bookings?: number; attendance?: number; deductions?: number }

const resources = ref<Entity[]>([])
const staff = ref<Entity[]>([])
const services = ref<Entity[]>([])
const templates = ref<Entity[]>([])
const members = ref<Entity[]>([])
const warnings = ref<Record<string, unknown>[]>([])
const summary = reactive<Summary>({})
const message = ref('')
const loading = ref(false)
const inviteCode = ref('')

const staffForm = reactive({ loginName: '', password: '', staffName: '', roleLabel: '教练' })
const serviceForm = reactive({ name: '', serviceType: '课程', durationMin: 60, defaultCapacity: 1, deductCount: 1 })
const cardForm = reactive({ name: '', salePriceYuan: 0, totalCount: 10, validDays: 90, lowBalanceThreshold: 2 })
const memberForm = reactive({ name: '', memberNo: '', contactText: '' })
const issueForm = reactive({ receivedAmountYuan: 0, saleDate: new Date().toISOString().slice(0, 10), payMethodLabel: '线下收款' })
const startAt = new Date(Date.now() + 24 * 60 * 60 * 1000)
startAt.setMinutes(0, 0, 0)
const slotForm = reactive({ startAt: startAt.toISOString(), endAt: new Date(startAt.getTime() + 60 * 60 * 1000).toISOString(), capacity: 1 })

const readyForService = computed(() => resources.value.length > 0 && staff.value.length > 0)
const readyForCard = computed(() => services.value.length > 0)
const readyForIssue = computed(() => templates.value.length > 0 && members.value.length > 0)
const readyForSlot = computed(() => resources.value.length > 0 && staff.value.length > 0 && services.value.length > 0)

async function load() {
  message.value = ''
  try {
    const [resourceRows, staffRows, serviceRows, templateRows, memberRows, report, warningRows] = await Promise.all([
      api.request<Entity[]>('GET', '/owner/resources'),
      api.request<Entity[]>('GET', '/owner/staff'),
      api.request<Entity[]>('GET', '/owner/services'),
      api.request<Entity[]>('GET', '/owner/card-templates'),
      api.request<Entity[]>('GET', '/owner/members'),
      api.request<Summary>('GET', '/owner/reports/summary'),
      api.request<Record<string, unknown>[]>('GET', '/owner/warnings/cards')
    ])
    resources.value = resourceRows
    staff.value = staffRows
    services.value = serviceRows
    templates.value = templateRows
    members.value = memberRows
    Object.assign(summary, report)
    warnings.value = warningRows
  } catch (error) {
    message.value = error instanceof Error ? error.message : '运营数据加载失败'
  }
}

async function run(scope: string, action: (key: string) => Promise<unknown>, success: string) {
  loading.value = true
  message.value = ''
  try {
    await action(createIdempotencyKey(scope))
    await Taro.showToast({ title: success, icon: 'success' })
    await load()
  } catch (error) {
    message.value = error instanceof Error ? error.message : '操作失败'
  } finally {
    loading.value = false
  }
}

function createStaff() {
  return run('staff', (key) => api.request('POST', '/owner/staff', staffForm, key), '员工已创建')
}

function createService() {
  return run('service', (key) => api.request('POST', '/owner/services', {
    ...serviceForm,
    resourceIds: [resources.value[0].id],
    staffIds: [staff.value[0].id],
    status: 'active'
  }, key), '服务已创建')
}

function createCard() {
  return run('card-template', (key) => api.request('POST', '/owner/card-templates', {
    ...cardForm,
    cardType: 'count',
    serviceIds: [services.value[0].id],
    staffIds: staff.value.length ? [staff.value[0].id] : []
  }, key), '卡模板已创建')
}

function createMember() {
  return run('member', (key) => api.request('POST', '/owner/members', memberForm, key), '会员已创建')
}

function issueCard() {
  return run('issue-card', (key) => api.request('POST', `/owner/members/${members.value[0].id}/cards`, {
    templateId: templates.value[0].id,
    ...issueForm
  }, key), '发卡与实收已记录')
}

async function createInvite() {
  loading.value = true
  message.value = ''
  try {
    const result = await api.request<{ code: string }>(
      'POST',
      `/owner/members/${members.value[0].id}/invite-codes`,
      {},
      createIdempotencyKey('invite')
    )
    inviteCode.value = result.code
  } catch (error) {
    message.value = error instanceof Error ? error.message : '邀请码生成失败'
  } finally {
    loading.value = false
  }
}

function publishSlot() {
  return run('slot', (key) => api.request('POST', '/owner/schedules/publish', {
    serviceId: services.value[0].id,
    staffId: staff.value[0].id,
    resourceId: resources.value[0].id,
    ...slotForm
  }, key), '时段已发布')
}

useDidShow(load)
</script>

<template>
  <View class="page">
    <Text class="brand">试点运营</Text>
    <Text class="title">完成第一笔真实履约</Text>

    <View class="metric-grid">
      <View class="metric"><Text class="metric-value">{{ summary.salesAmountYuan || 0 }}</Text><Text class="metric-label">线下实收</Text></View>
      <View class="metric"><Text class="metric-value">{{ summary.bookings || 0 }}</Text><Text class="metric-label">预约</Text></View>
      <View class="metric"><Text class="metric-value">{{ summary.attendance || 0 }}</Text><Text class="metric-label">到店</Text></View>
      <View class="metric"><Text class="metric-value">{{ summary.deductions || 0 }}</Text><Text class="metric-label">核销</Text></View>
    </View>

    <View class="panel">
      <Text class="panel-title">1. 员工账号</Text>
      <View class="field"><Text class="label">登录账号</Text><input v-model="staffForm.loginName" class="input" /></View>
      <View class="field"><Text class="label">初始密码，至少 8 位</Text><input v-model="staffForm.password" class="input" password /></View>
      <View class="field"><Text class="label">员工姓名</Text><input v-model="staffForm.staffName" class="input" /></View>
      <button class="button" :disabled="loading || !staffForm.loginName || staffForm.password.length < 8 || !staffForm.staffName" @tap="createStaff">创建员工</button>
    </View>

    <View class="panel">
      <Text class="panel-title">2. 服务项目</Text>
      <Text class="subtitle">默认绑定第一个资源与员工，可在完整预约阶段调整。</Text>
      <View class="field"><Text class="label">服务名称</Text><input v-model="serviceForm.name" class="input" /></View>
      <View class="field"><Text class="label">服务时长（分钟）</Text><input v-model.number="serviceForm.durationMin" class="input" type="number" /></View>
      <button class="button" :disabled="loading || !readyForService || !serviceForm.name" @tap="createService">创建服务</button>
    </View>

    <View class="panel">
      <Text class="panel-title">3. 次数卡模板</Text>
      <View class="field"><Text class="label">卡名称</Text><input v-model="cardForm.name" class="input" /></View>
      <View class="field"><Text class="label">售价</Text><input v-model.number="cardForm.salePriceYuan" class="input" type="digit" /></View>
      <View class="field"><Text class="label">总次数</Text><input v-model.number="cardForm.totalCount" class="input" type="number" /></View>
      <button class="button" :disabled="loading || !readyForCard || !cardForm.name" @tap="createCard">创建卡模板</button>
    </View>

    <View class="panel">
      <Text class="panel-title">4. 会员与发卡</Text>
      <View class="field"><Text class="label">会员姓名</Text><input v-model="memberForm.name" class="input" /></View>
      <View class="field"><Text class="label">会员编号</Text><input v-model="memberForm.memberNo" class="input" /></View>
      <View class="field"><Text class="label">联系方式</Text><input v-model="memberForm.contactText" class="input" /></View>
      <button class="button secondary" :disabled="loading || !memberForm.name || !memberForm.memberNo" @tap="createMember">创建会员</button>
      <View class="field"><Text class="label">本次实收</Text><input v-model.number="issueForm.receivedAmountYuan" class="input" type="digit" /></View>
      <button class="button" :disabled="loading || !readyForIssue" @tap="issueCard">向首位会员发卡</button>
      <button class="button secondary" :disabled="loading || !members.length" @tap="createInvite">生成首位会员邀请码</button>
      <Text v-if="inviteCode" class="status">邀请码 {{ inviteCode }}</Text>
    </View>

    <View class="panel">
      <Text class="panel-title">5. 发布最小时段</Text>
      <Text class="subtitle">默认使用列表中的第一个服务、员工与资源。</Text>
      <View class="field"><Text class="label">开始时间（ISO 8601）</Text><input v-model="slotForm.startAt" class="input" /></View>
      <View class="field"><Text class="label">结束时间（ISO 8601）</Text><input v-model="slotForm.endAt" class="input" /></View>
      <View class="field"><Text class="label">容量</Text><input v-model.number="slotForm.capacity" class="input" type="number" /></View>
      <button class="button" :disabled="loading || !readyForSlot" @tap="publishSlot">发布时段</button>
    </View>

    <View class="panel">
      <Text class="panel-title">会员卡预警</Text>
      <Text v-if="!warnings.length" class="subtitle">当前没有到期、空卡或低余额提醒。</Text>
      <View v-for="(warning, index) in warnings" :key="index" class="list-row">
        <Text class="row-title">{{ warning.memberName }}</Text>
        <Text class="row-meta">{{ warning.cardName }} · {{ warning.displayStatus }}</Text>
      </View>
    </View>

    <Text v-if="message" class="message">{{ message }}</Text>
  </View>
</template>
