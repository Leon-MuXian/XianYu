<script setup lang="ts">
import Taro, { useDidShow, useLoad } from '@tarojs/taro'
import { createIdempotencyKey } from '@serenmeet/api-client'
import { computed, reactive, ref } from 'vue'
import OwnerTabbar from '../../components/OwnerTabbar.vue'
import OwnerTopbar from '../../components/OwnerTopbar.vue'
import { api } from '../../api'
import {
  formatTime,
  guardOwner,
  messageOf,
  type Schedule,
  type Service
} from '../../owner'

type ScheduleFilter = 'all' | 'published' | 'draft'

interface ScheduleStaffOption {
  id: number
  staffName: string
  roleLabel: string
  available: boolean
  displayName?: string
}

interface ScheduleResourceOption {
  id: number
  name: string
  resourceType: string
  capacity: number
  available: boolean
  displayName?: string
}

interface ScheduleOptions {
  services: Service[]
  staff: ScheduleStaffOption[]
  resources: ScheduleResourceOption[]
  suggestedDate: string
  suggestedStartTime: string
  endAt?: string
}

interface ScheduleCheck {
  code: string
  label: string
  passed: boolean
  message: string
}

interface SchedulePrecheck {
  eligible: boolean
  startAt: string
  endAt: string
  capacity: number
  cardTemplateCount: number
  checks: ScheduleCheck[]
}

const rows = ref<Schedule[]>([])
const dates = ref(buildDates(localDateValue()))
const selectedDate = ref(dates.value[0].value)
const filter = ref<ScheduleFilter>('all')
const message = ref('')
const editorMessage = ref('')
const highlightedId = ref(0)
const showEditor = ref(false)
const loading = ref(false)
const services = ref<Service[]>([])
const staff = ref<ScheduleStaffOption[]>([])
const resources = ref<ScheduleResourceOption[]>([])
const form = reactive({
  id: 0,
  serviceId: 0,
  staffId: 0,
  resourceId: 0,
  date: selectedDate.value,
  startTime: ''
})
let routeIntent = ''
let routeSource = ''
let routeHighlight = 0
let routeConsumed = false

const filteredRows = computed(() =>
  filter.value === 'all' ? rows.value : rows.value.filter((item) => item.status === filter.value)
)
const publishedCount = computed(() => rows.value.filter((item) => item.status === 'published').length)
const draftCount = computed(() => rows.value.filter((item) => item.status === 'draft').length)
const selectedService = computed(() => services.value.find((item) => item.id === form.serviceId))
const selectedStaff = computed(() => staff.value.find((item) => item.id === form.staffId))
const selectedResource = computed(() => resources.value.find((item) => item.id === form.resourceId))
const endTime = computed(() => {
  if (!form.startTime || !selectedService.value) return '--:--'
  const [hour, minute] = form.startTime.split(':').map(Number)
  const total = hour * 60 + minute + selectedService.value.durationMin
  return `${String(Math.floor(total / 60) % 24).padStart(2, '0')}:${String(total % 60).padStart(2, '0')}`
})
const ready = computed(() => Boolean(
  form.serviceId
  && form.staffId
  && form.resourceId
  && form.date
  && form.startTime
))
const editing = computed(() => form.id > 0)
const staffPickerRows = computed(() => staff.value.map((item) => ({
  ...item,
  displayName: `${item.staffName}${item.available ? '' : ' · 该时段已占用'}`
})))
const resourcePickerRows = computed(() => resources.value.map((item) => ({
  ...item,
  displayName: `${item.name} · 容量 ${item.capacity} 人${item.available ? '' : ' · 该时段已占用'}`
})))

function localDateValue(value = new Date()) {
  return `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, '0')}-${String(value.getDate()).padStart(2, '0')}`
}

function buildDates(anchor: string) {
  const [year, month, day] = anchor.split('-').map(Number)
  const start = new Date(year, month - 1, day)
  return Array.from({ length: 5 }, (_, index) => {
    const date = new Date(start)
    date.setDate(start.getDate() + index)
    return {
      value: localDateValue(date),
      label: index === 0 && anchor === localDateValue()
        ? '今天'
        : ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][date.getDay()],
      day: String(date.getDate()).padStart(2, '0')
    }
  })
}

function startAtValue() {
  return `${form.date}T${form.startTime}:00+08:00`
}

function dateAndTime(value: string) {
  const date = new Date(value)
  return {
    date: localDateValue(date),
    time: `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
  }
}

async function load() {
  message.value = ''
  try {
    if (!await guardOwner()) return
    rows.value = await api.request<Schedule[]>('GET', `/owner/schedules?date=${selectedDate.value}`)
  } catch (error) {
    message.value = messageOf(error, '排期加载失败')
  }
}

async function loadOptions(preserveSelection = false) {
  loading.value = true
  editorMessage.value = ''
  try {
    const query: string[] = []
    if (form.serviceId) query.push(`serviceId=${form.serviceId}`)
    if (form.startTime) query.push(`startAt=${encodeURIComponent(startAtValue())}`)
    const options = await api.request<ScheduleOptions>(
      'GET',
      `/owner/schedules/options${query.length ? `?${query.join('&')}` : ''}`
    )
    services.value = options.services
    if (!form.startTime) form.startTime = options.suggestedStartTime.slice(0, 5)
    if (!form.date) form.date = options.suggestedDate
    staff.value = options.staff
    resources.value = options.resources
    if (!preserveSelection) {
      form.staffId = 0
      form.resourceId = 0
    }
    if (form.staffId && !staff.value.some((item) => item.id === form.staffId)) form.staffId = 0
    if (form.resourceId && !resources.value.some((item) => item.id === form.resourceId)) form.resourceId = 0
  } catch (error) {
    editorMessage.value = messageOf(error, '排期配置加载失败')
  } finally {
    loading.value = false
  }
}

function resetForm() {
  Object.assign(form, {
    id: 0,
    serviceId: 0,
    staffId: 0,
    resourceId: 0,
    date: selectedDate.value,
    startTime: ''
  })
  services.value = []
  staff.value = []
  resources.value = []
  editorMessage.value = ''
}

async function openCreate(useSuggestedDate = false) {
  resetForm()
  if (useSuggestedDate) form.date = ''
  showEditor.value = true
  await loadOptions()
  if (useSuggestedDate && form.date !== selectedDate.value) {
    selectedDate.value = form.date
    dates.value = buildDates(selectedDate.value)
    await load()
  }
}

async function openDraft(slot: Schedule) {
  if (slot.status !== 'draft') return
  resetForm()
  const value = dateAndTime(slot.startAt)
  Object.assign(form, {
    id: slot.id,
    serviceId: slot.serviceId || 0,
    staffId: slot.staffId || 0,
    resourceId: slot.resourceId || 0,
    date: value.date,
    startTime: value.time
  })
  showEditor.value = true
  await loadOptions(true)
}

function closeEditor() {
  showEditor.value = false
  resetForm()
}

async function selectService(event: { detail: { value: string } }) {
  const service = services.value[Number(event.detail.value)]
  if (!service) return
  form.serviceId = service.id
  await loadOptions()
}

async function selectStaff(event: { detail: { value: string } }) {
  const option = staffPickerRows.value[Number(event.detail.value)]
  if (!option) return
  if (!option.available) {
    await Taro.showToast({ title: '该员工在此时段已有排期', icon: 'none' })
    return
  }
  form.staffId = option.id
}

async function selectResource(event: { detail: { value: string } }) {
  const option = resourcePickerRows.value[Number(event.detail.value)]
  if (!option) return
  if (!option.available) {
    await Taro.showToast({ title: '该资源在此时段已有排期', icon: 'none' })
    return
  }
  form.resourceId = option.id
}

async function changeDate(event: { detail: { value: string } }) {
  form.date = event.detail.value
  if (form.serviceId) await loadOptions(true)
}

async function changeTime(event: { detail: { value: string } }) {
  form.startTime = event.detail.value
  if (form.serviceId) await loadOptions(true)
}

async function checkBeforeSubmit() {
  if (!ready.value) return null
  try {
    return await api.request<SchedulePrecheck>('POST', '/owner/schedules/precheck', {
      serviceId: form.serviceId,
      staffId: form.staffId,
      resourceId: form.resourceId,
      startAt: startAtValue()
    })
  } catch (error) {
    editorMessage.value = messageOf(error, '发布前检查失败')
    return null
  }
}

function failedCheckMessage(checked: SchedulePrecheck | null) {
  const failed = checked?.checks.find((item) => !item.passed)
  return failed ? `${failed.label}：${failed.message}` : editorMessage.value || '发布前检查失败，请稍后重试'
}

async function save(kind: 'draft' | 'publish') {
  if (!ready.value) {
    editorMessage.value = '请先完整选择服务、日期、员工和资源'
    return
  }
  loading.value = true
  editorMessage.value = ''
  try {
    if (kind === 'publish') {
      const checked = await checkBeforeSubmit()
      if (!checked?.eligible) {
        await Taro.showModal({ title: '暂不能发布', content: failedCheckMessage(checked), showCancel: false })
        return
      }
    }
    const payload = {
      serviceId: form.serviceId,
      staffId: form.staffId,
      resourceId: form.resourceId,
      startAt: startAtValue()
    }
    let result: Schedule
    if (kind === 'draft' && editing.value) {
      result = await api.request<Schedule>(
        'PUT',
        `/owner/schedules/${form.id}`,
        payload,
        createIdempotencyKey('schedule-update')
      )
    } else if (kind === 'draft') {
      result = await api.request<Schedule>(
        'POST',
        '/owner/schedules/drafts',
        payload,
        createIdempotencyKey('schedule-draft')
      )
    } else {
      result = await api.request<Schedule>(
        'POST',
        '/owner/schedules/publish',
        { ...payload, slotId: form.id || undefined },
        createIdempotencyKey('schedule-publish')
      )
    }
    selectedDate.value = form.date
    dates.value = buildDates(selectedDate.value)
    highlightedId.value = result.id
    filter.value = 'all'
    closeEditor()
    await load()
    await Taro.showToast({
      title: kind === 'draft' ? '排期草稿已保存' : '预约时段已发布',
      icon: 'success'
    })
  } catch (error) {
    editorMessage.value = messageOf(error, '排期保存失败')
  } finally {
    loading.value = false
  }
}

async function selectDate(value: string) {
  selectedDate.value = value
  highlightedId.value = 0
  await load()
}

useLoad((params) => {
  if (params.date) {
    selectedDate.value = params.date
    dates.value = buildDates(params.date)
  }
  routeIntent = params.intent || ''
  routeSource = params.source || ''
  routeHighlight = Number(params.highlight || 0)
  highlightedId.value = routeHighlight
})

useDidShow(async () => {
  await load()
  if (routeConsumed) return
  routeConsumed = true
  if (routeIntent === 'create') {
    await openCreate(routeSource === 'dashboard')
  } else if (routeIntent === 'edit' && routeHighlight) {
    const draft = rows.value.find((item) => item.id === routeHighlight)
    if (draft) await openDraft(draft)
  }
})
</script>

<template>
  <View class="screen schedule-screen">
    <OwnerTopbar title="排期" />
    <scroll-view :scroll-y="true" :enhanced="true" :show-scrollbar="false" class="schedule-page-scroll">
      <View class="content owner-dense schedule-page">
        <View class="owner-page-head">
          <View class="owner-page-head-copy">
            <Text class="page-title">预约时段</Text>
            <Text class="page-copy">按日期安排服务，发布前统一检查员工、资源与会员卡范围。</Text>
          </View>
          <button
            class="owner-add-action"
            aria-label="新增预约时段"
            hover-class="owner-add-action-pressed"
            @tap="openCreate()"
          ><View class="owner-add-action-icon" /></button>
        </View>

        <View class="owner-status-strip schedule-status-strip">
          <View class="owner-status"><Text>当天时段</Text><Text class="status-value">{{ rows.length }}</Text><Text class="status-note">全部</Text></View>
          <View class="owner-status"><Text>已发布</Text><Text class="status-value">{{ publishedCount }}</Text><Text class="status-note">会员可约</Text></View>
          <View class="owner-status"><Text>草稿</Text><Text class="status-value">{{ draftCount }}</Text><Text class="status-note coral-text">待处理</Text></View>
        </View>

        <View class="calendar-strip">
          <View
            v-for="date in dates"
            :key="date.value"
            class="day"
            :class="{ active: selectedDate === date.value }"
            @tap="selectDate(date.value)"
          ><Text>{{ date.label }}</Text><Text class="day-number">{{ date.day }}</Text></View>
        </View>

        <View class="owner-tabs schedule-tabs">
          <Text :class="{ active: filter === 'all' }" @tap="filter = 'all'">全部</Text>
          <Text :class="{ active: filter === 'published' }" @tap="filter = 'published'">已发布</Text>
          <Text :class="{ active: filter === 'draft' }" @tap="filter = 'draft'">草稿</Text>
        </View>
        <View v-if="message" class="error-banner">{{ message }}</View>
        <View v-if="!filteredRows.length" class="empty-state schedule-empty" @tap="openCreate()">
          <Text class="empty-title">当天还没有排期</Text>
          <Text>新增服务时段后，已发布时段会同步到会员端和员工端。</Text>
          <button class="button schedule-empty-action" @tap.stop="openCreate()">新增时段</button>
        </View>
        <View v-else class="schedule-lane">
          <View
            v-for="slot in filteredRows"
            :key="slot.id"
            class="schedule-item"
            :class="{ 'new-slot': slot.id === highlightedId, editable: slot.status === 'draft' }"
            @tap="openDraft(slot)"
          >
            <Text class="hour">{{ formatTime(slot.startAt) }}</Text>
            <View class="schedule-item-main">
              <Text class="schedule-title">{{ slot.serviceName }}</Text>
              <Text class="schedule-copy">{{ slot.staffName }} · {{ slot.resourceName }}</Text>
              <Text class="schedule-state" :class="slot.status">{{ slot.status === 'published' ? '会员可预约' : '草稿 · 点击继续' }}</Text>
            </View>
            <Text class="tag" :class="slot.reservedCount >= slot.capacity ? 'warn' : slot.status === 'draft' ? 'red' : ''">{{ slot.id === highlightedId ? '新时段' : slot.reservedCount >= slot.capacity ? '满员' : `${slot.reservedCount}/${slot.capacity}` }}</Text>
          </View>
        </View>
      </View>
    </scroll-view>

    <View v-if="showEditor" class="modal-backdrop schedule-editor-backdrop" @tap.self="closeEditor">
      <View class="sheet schedule-editor-sheet">
        <View class="sheet-grip" />
        <View class="sheet-title schedule-sheet-title">
          <View>
            <Text class="sheet-heading">{{ editing ? '继续排期草稿' : '新增预约时段' }}</Text>
            <Text class="sheet-copy">结束时间由服务时长自动计算，提交发布时会检查可预约条件。</Text>
          </View>
          <Text v-if="editing" class="tag warn">草稿</Text>
        </View>

        <scroll-view :scroll-y="true" :enhanced="true" :show-scrollbar="false" class="schedule-editor-body">
          <View class="schedule-form">
            <Text class="schedule-band-title">服务与时间</Text>
            <picker class="field selector-field wide" mode="selector" :range="services" range-key="name" @change="selectService">
              <View><Text class="field-label">服务项目 <Text class="required-mark">必填</Text></Text><Text class="field-value">{{ selectedService?.name || '请选择服务项目' }} ▾</Text></View>
            </picker>
            <View class="schedule-time-grid">
              <picker class="field" mode="date" :value="form.date" @change="changeDate">
                <View><Text class="field-label">日期</Text><Text class="field-value">{{ form.date }} ▾</Text></View>
              </picker>
              <picker class="field" mode="time" :value="form.startTime" @change="changeTime">
                <View><Text class="field-label">开始</Text><Text class="field-value">{{ form.startTime || '请选择' }} ▾</Text></View>
              </picker>
              <View class="field schedule-readonly"><Text class="field-label">结束</Text><Text class="field-value">{{ endTime }}</Text></View>
            </View>

            <Text class="schedule-band-title">由谁、在哪里履约</Text>
            <View class="schedule-selector-list">
              <picker class="field selector-field" mode="selector" :disabled="!form.serviceId" :range="staffPickerRows" range-key="displayName" @change="selectStaff">
                <View><Text class="field-label">履约员工 <Text class="required-mark">必填</Text></Text><Text class="field-value">{{ selectedStaff?.staffName || (form.serviceId ? '请选择适用员工' : '请先选择服务') }} ▾</Text></View>
              </picker>
              <picker class="field selector-field" mode="selector" :disabled="!form.serviceId" :range="resourcePickerRows" range-key="displayName" @change="selectResource">
                <View><Text class="field-label">履约资源 <Text class="required-mark">必填</Text></Text><Text class="field-value">{{ selectedResource?.name || (form.serviceId ? '请选择适用资源' : '请先选择服务') }}<Text v-if="selectedResource"> · {{ selectedResource.capacity }} 人</Text> ▾</Text></View>
              </picker>
            </View>
            <View v-if="editorMessage" class="error-banner">{{ editorMessage }}</View>
          </View>
        </scroll-view>

        <View class="form-footer schedule-form-footer">
          <button class="button secondary" :loading="loading" :disabled="loading || !ready" @tap="save('draft')">保存草稿</button>
          <button class="button" :loading="loading" :disabled="loading || !ready" @tap="save('publish')">发布时段</button>
        </View>
      </View>
    </View>
    <OwnerTabbar active="schedules" />
  </View>
</template>
