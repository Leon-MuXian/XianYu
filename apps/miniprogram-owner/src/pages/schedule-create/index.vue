<script setup lang="ts">
import Taro, { useLoad } from '@tarojs/taro'
import { computed, reactive, ref } from 'vue'
import { createIdempotencyKey } from '@serenmeet/api-client'
import OwnerTopbar from '../../components/OwnerTopbar.vue'
import { api } from '../../api'
import { guardOwner, messageOf, type CardTemplate, type Resource, type Schedule, type Service, type Staff, ownerRoutes } from '../../owner'

const services = ref<Service[]>([])
const staff = ref<Staff[]>([])
const resources = ref<Resource[]>([])
const templates = ref<CardTemplate[]>([])
const form = reactive({ serviceId: 0, staffId: 0, resourceId: 0, date: new Date().toISOString().slice(0, 10), startTime: '10:30', endTime: '11:30', capacity: 1 })
const loading = ref(false)
const message = ref('')
const precheckErrors = ref<string[]>([])
const success = ref<Schedule | null>(null)
const selectedService = computed(() => services.value.find((item) => item.id === form.serviceId))
const selectedStaff = computed(() => staff.value.find((item) => item.id === form.staffId))
const selectedResource = computed(() => resources.value.find((item) => item.id === form.resourceId))
const applicableCards = computed(() => templates.value.filter((item) => item.status === 'active'))
const ready = computed(() => form.serviceId && form.staffId && form.resourceId && form.date && form.startTime && form.endTime && form.capacity > 0)

async function load(params?: Record<string, string>) {
  try {
    if (!await guardOwner()) return
    if (params?.date) form.date = params.date
    const [options, cardRows] = await Promise.all([
      api.request<{ services: Service[]; staff: Staff[]; resources: Resource[] }>('GET', '/owner/services/options'),
      api.request<CardTemplate[]>('GET', '/owner/card-templates')
    ])
    services.value = options.services.filter((item) => item.status === 'active')
    staff.value = options.staff.filter((item) => item.status === 'active')
    resources.value = options.resources.filter((item) => item.enabled !== false)
    templates.value = cardRows
    if (services.value.length) { form.serviceId = services.value[0].id; form.capacity = services.value[0].defaultCapacity }
    if (staff.value.length) form.staffId = staff.value[0].id
    if (resources.value[0]?.id) form.resourceId = resources.value[0].id
  } catch (error) { message.value = messageOf(error, '排期配置加载失败') }
}

function selectFrom<T extends { id?: number }>(rows: T[], target: 'serviceId' | 'staffId' | 'resourceId', event: { detail: { value: string } }) {
  const row = rows[Number(event.detail.value)]
  if (row?.id) form[target] = row.id
  if (target === 'serviceId' && 'defaultCapacity' in (row || {})) form.capacity = Number((row as unknown as Service).defaultCapacity)
}

function setTime(field: 'startTime' | 'endTime', event: { detail: { value: string } }) { form[field] = event.detail.value }
function iso(date: string, time: string) { return new Date(`${date}T${time}:00`).toISOString() }
function validate() {
  const errors: string[] = []
  if (!selectedService.value) errors.push('请选择启用的服务项目')
  if (!selectedStaff.value) errors.push('请选择启用员工')
  if (!selectedResource.value) errors.push('请选择启用资源')
  if (form.endTime <= form.startTime) errors.push('结束时间必须晚于开始时间')
  if (!applicableCards.value.length) errors.push('至少需要 1 个启用会员卡模板')
  precheckErrors.value = errors
  return !errors.length
}

async function save(kind: 'draft' | 'publish') {
  if (!ready.value || !validate()) return
  loading.value = true
  message.value = ''
  const payload = { serviceId: form.serviceId, staffId: form.staffId, resourceId: form.resourceId, startAt: iso(form.date, form.startTime), endAt: iso(form.date, form.endTime), capacity: Number(form.capacity) }
  try {
    if (kind === 'draft') success.value = await api.request<Schedule>('POST', '/owner/schedules/drafts', payload, createIdempotencyKey('schedule-draft'))
    else success.value = await api.request<Schedule>('POST', '/owner/schedules/publish', payload, createIdempotencyKey('schedule-publish'))
    await Taro.showToast({ title: kind === 'draft' ? '草稿已保存' : '时段已发布', icon: 'success' })
  } catch (error) {
    message.value = messageOf(error, '排期保存失败')
    precheckErrors.value = [message.value]
  } finally { loading.value = false }
}

function restart() { success.value = null; precheckErrors.value = [] }
useLoad((params) => { void load(params) })
</script>

<template>
  <View class="screen">
    <OwnerTopbar :title="success ? '发布成功' : '发布预约时段'" back />
    <View class="content owner-dense">
      <template v-if="success">
        <View class="owner-hero"><Text class="hero-title">{{ selectedService?.name }}已{{ success.status === 'draft' ? '保存' : '发布' }}</Text><Text class="hero-copy">{{ form.date }} {{ form.startTime }}-{{ form.endTime }} · {{ selectedStaff?.staffName }} · {{ selectedResource?.name }}</Text><View class="owner-hero-meta"><Text>容量 {{ form.capacity }}</Text><Text>{{ success.status === 'draft' ? '草稿' : '会员可约' }}</Text></View></View>
        <View class="owner-start-note"><View><Text class="note-title">已准备返回 {{ form.date }} 排期</Text><Text>新时段会在排期首页展示，已发布时段同步到员工端和会员端。</Text></View><Text class="tag blue">高亮</Text></View>
        <View class="schedule-lane"><View class="schedule-item new-slot"><Text class="hour">{{ form.startTime }}</Text><View><Text class="schedule-title">{{ selectedService?.name }}</Text><Text class="schedule-copy">{{ selectedStaff?.staffName }} · {{ selectedResource?.name }} · 刚刚{{ success.status === 'draft' ? '保存' : '发布' }}</Text></View><Text class="tag">新时段</Text></View></View>
        <View class="form-footer"><button class="button secondary" @tap="restart">继续新建</button><button class="button" @tap="Taro.redirectTo({ url: `${ownerRoutes.schedules}?date=${form.date}&highlight=${success.id}` })">查看排期</button></View>
      </template>
      <template v-else>
        <View class="owner-hero" :class="{ 'warning-hero': precheckErrors.length }"><Text class="hero-title">{{ selectedService?.name || '新预约时段' }}</Text><Text class="hero-copy">{{ form.date }} {{ form.startTime }}-{{ form.endTime }} · {{ selectedResource?.name || '待选资源' }} · {{ selectedStaff?.staffName || '待选员工' }}</Text><View class="owner-hero-meta"><Text>容量 {{ form.capacity }}</Text><Text>核销 {{ selectedService?.deductCount || 0 }} 次</Text></View></View>
        <View class="owner-form-grid">
          <picker class="field" mode="selector" :range="services" range-key="name" @change="selectFrom(services, 'serviceId', $event)"><View><Text>服务项目 <Text class="required-mark">必填</Text></Text><Text class="field-value">{{ selectedService?.name || '请选择' }} ▾</Text></View></picker>
          <picker class="field" mode="selector" :range="staff" range-key="staffName" @change="selectFrom(staff, 'staffId', $event)"><View><Text>员工 <Text class="required-mark">必填</Text></Text><Text class="field-value">{{ selectedStaff?.staffName || '请选择' }} ▾</Text></View></picker>
          <picker class="field" mode="selector" :range="resources" range-key="name" @change="selectFrom(resources, 'resourceId', $event)"><View><Text>资源 <Text class="required-mark">必填</Text></Text><Text class="field-value">{{ selectedResource?.name || '请选择' }} ▾</Text></View></picker>
          <picker class="field" mode="date" :value="form.date" @change="form.date = $event.detail.value"><View><Text>日期 <Text class="required-mark">必填</Text></Text><Text class="field-value">{{ form.date }} ▾</Text></View></picker>
          <picker class="field" mode="time" :value="form.startTime" @change="setTime('startTime', $event)"><View><Text>开始时间 <Text class="required-mark">必填</Text></Text><Text class="field-value">{{ form.startTime }} ▾</Text></View></picker>
          <picker class="field" mode="time" :value="form.endTime" @change="setTime('endTime', $event)"><View><Text>结束时间 <Text class="required-mark">必填</Text></Text><Text class="field-value">{{ form.endTime }} ▾</Text></View></picker>
          <View class="field"><Text>容量 <Text class="required-mark">必填</Text></Text><input v-model.number="form.capacity" class="field-input" type="number" /></View>
          <View class="field wide"><Text>可用会员卡 <Text class="required-mark">必填</Text></Text><Text class="field-value">{{ applicableCards.map((item) => item.name).join('、') || '没有启用会员卡模板' }}</Text></View>
        </View>
        <Text class="section-title">发布确认</Text>
        <View class="owner-progress" v-if="!precheckErrors.length"><View class="owner-step"><Text class="step-glyph">服</Text><View><Text class="step-title">服务已启用</Text><Text class="step-copy">时长、容量、核销规则完整</Text></View><Text class="tag">通过</Text></View><View class="owner-step"><Text class="step-glyph">员</Text><View><Text class="step-title">员工已安排</Text><Text class="step-copy">已选择启用的可履约员工</Text></View><Text class="tag">通过</Text></View><View class="owner-step"><Text class="step-glyph">卡</Text><View><Text class="step-title">会员卡可使用</Text><Text class="step-copy">会员可使用适用会员卡预约</Text></View><Text class="tag blue">通过</Text></View></View>
        <View class="owner-progress" v-else><View v-for="(error, index) in precheckErrors" :key="error" class="owner-step"><Text class="step-glyph">{{ index + 1 }}</Text><View><Text class="step-title">预检未通过</Text><Text class="step-copy">{{ error }}</Text></View><Text class="tag red">失败</Text></View></View>
        <View v-if="message" class="error-banner">{{ message }}</View>
        <View class="form-footer"><button class="button secondary" :loading="loading" :disabled="loading || !ready" @tap="save('draft')">保存草稿</button><button class="button" :loading="loading" :disabled="loading || !ready || precheckErrors.length > 0" @tap="save('publish')">发布</button></View>
      </template>
    </View>
  </View>
</template>
