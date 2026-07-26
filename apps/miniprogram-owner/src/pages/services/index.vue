<script setup lang="ts">
import Taro, { useDidShow } from '@tarojs/taro'
import { computed, nextTick, reactive, ref } from 'vue'
import { createIdempotencyKey } from '@serenmeet/api-client'
import OwnerTopbar from '../../components/OwnerTopbar.vue'
import { api } from '../../api'
import { guardOwner, messageOf, type Resource, type Service, type Staff } from '../../owner'
import {
  formatSelectionSummary,
  hasDuplicateServiceName,
  isServiceNameTaken,
  normalizeServiceName,
  SERVICE_NAME_TAKEN_MESSAGE
} from '../../service-form'

type ServiceFilter = 'all' | 'active' | 'attention'

const services = ref<Service[]>([])
const resources = ref<Resource[]>([])
const staff = ref<Staff[]>([])
const form = reactive({
  id: 0,
  name: '',
  serviceType: '一对一服务',
  durationMin: 60,
  defaultCapacity: 1,
  deductCount: 1,
  resourceIds: [] as number[],
  staffIds: [] as number[],
  status: 'active'
})
const loading = ref(false)
const message = ref('')
const serviceNameError = ref('')
const showForm = ref(false)
const editMenuOpen = ref(false)
const scopeMode = ref<'resources' | 'staff' | ''>('')
const editorScrollTop = ref(0)
const serviceFilter = ref<ServiceFilter>('all')
let editorScrollPosition = 0
let scopeReturnScrollTop = 0
const editing = computed(() => Boolean(form.id))
const activeCount = computed(() => services.value.filter((item) => item.status === 'active').length)
const attentionCount = computed(() => services.value.length - activeCount.value)
const filteredServices = computed(() => {
  if (serviceFilter.value === 'active') return services.value.filter((item) => item.status === 'active')
  if (serviceFilter.value === 'attention') return services.value.filter((item) => item.status !== 'active')
  return services.value
})
const canActivate = computed(() => Boolean(
  form.name
  && form.durationMin > 0
  && form.defaultCapacity > 0
  && form.deductCount > 0
  && form.resourceIds.length > 0
  && form.staffIds.length > 0
))
const selectedResourceNames = computed(() => resources.value
  .filter((item) => item.id && form.resourceIds.includes(item.id))
  .map((item) => item.name))
const selectedStaffNames = computed(() => staff.value
  .filter((item) => form.staffIds.includes(item.id))
  .map((item) => item.staffName))
const selectedResourceSummary = computed(() => formatSelectionSummary(
  selectedResourceNames.value,
  '资源',
  '请选择至少 1 个启用资源'
))
const selectedStaffSummary = computed(() => formatSelectionSummary(
  selectedStaffNames.value,
  '员工',
  '请选择至少 1 个启用员工'
))

async function load() {
  message.value = ''
  try {
    if (!await guardOwner()) return
    const options = await api.request<{ services: Service[]; resources: Resource[]; staff: Staff[] }>(
      'GET',
      '/owner/services/options'
    )
    services.value = options.services
    resources.value = options.resources
    staff.value = options.staff
  } catch (error) {
    message.value = messageOf(error, '服务项目加载失败')
  }
}

function resetForm() {
  Object.assign(form, {
    id: 0,
    name: '',
    serviceType: '一对一服务',
    durationMin: 60,
    defaultCapacity: 1,
    deductCount: 1,
    resourceIds: [],
    staffIds: [],
    status: 'active'
  })
  serviceNameError.value = ''
  message.value = ''
  scopeMode.value = ''
  editorScrollTop.value = 0
  editorScrollPosition = 0
  scopeReturnScrollTop = 0
}

function openCreate() {
  resetForm()
  editMenuOpen.value = false
  showForm.value = true
}

function closeForm() {
  resetForm()
  editMenuOpen.value = false
  showForm.value = false
}

function edit(service: Service) {
  Object.assign(form, {
    id: service.id,
    name: service.name,
    serviceType: service.serviceType,
    durationMin: service.durationMin,
    defaultCapacity: service.defaultCapacity,
    deductCount: service.deductCount,
    resourceIds: service.resourceIds || [],
    staffIds: service.staffIds || [],
    status: service.status
  })
  serviceNameError.value = ''
  message.value = ''
  editMenuOpen.value = false
  showForm.value = true
}

function toggleEditMenu() {
  if (!editing.value || loading.value) return
  editMenuOpen.value = !editMenuOpen.value
}

function clearServiceNameError() {
  serviceNameError.value = ''
  if (message.value === SERVICE_NAME_TAKEN_MESSAGE) message.value = ''
}

function rememberEditorScroll(event: { detail: { scrollTop: number } }) {
  if (!scopeMode.value) editorScrollPosition = event.detail.scrollTop
}

function openScope(mode: 'resources' | 'staff') {
  scopeReturnScrollTop = editorScrollPosition
  editorScrollTop.value = scopeReturnScrollTop
  scopeMode.value = mode
}

async function waitForViewUpdate() {
  await nextTick()
  await new Promise<void>((resolve) => Taro.nextTick(resolve))
}

async function closeScope() {
  const returnScrollTop = scopeReturnScrollTop
  scopeMode.value = ''
  await waitForViewUpdate()
  editorScrollTop.value = returnScrollTop > 0 ? returnScrollTop - 1 : 1
  await waitForViewUpdate()
  editorScrollTop.value = returnScrollTop
  editorScrollPosition = returnScrollTop
}

function toggleId(target: 'resourceIds' | 'staffIds', id: number, selectable: boolean) {
  if (!selectable) return
  const values = form[target]
  form[target] = values.includes(id) ? values.filter((value) => value !== id) : [...values, id]
}

function resourceCount(service: Service) {
  return service.resourceIds?.length || 0
}

function staffCount(service: Service) {
  return service.staffIds?.length || 0
}

function serviceStatusLabel(service: Service) {
  if (service.status === 'active') return '可排期'
  if (service.status === 'disabled') return '已停用'
  const missingScopes = []
  if (!resourceCount(service)) missingScopes.push('资源')
  if (!staffCount(service)) missingScopes.push('员工')
  return missingScopes.length ? `草稿 · 缺少${missingScopes.join('和')}` : '草稿 · 待启用'
}

async function submit(status: 'draft' | 'active') {
  const normalizedName = normalizeServiceName(form.name)
  form.name = normalizedName
  serviceNameError.value = ''
  if (!normalizedName) {
    serviceNameError.value = '请填写服务名称'
    message.value = serviceNameError.value
    return
  }
  if (hasDuplicateServiceName(services.value, normalizedName, form.id || undefined)) {
    serviceNameError.value = SERVICE_NAME_TAKEN_MESSAGE
    message.value = SERVICE_NAME_TAKEN_MESSAGE
    return
  }
  if (!form.resourceIds.length) {
    message.value = '请选择至少 1 个启用资源'
    return
  }
  if (status === 'active' && !canActivate.value) {
    message.value = '启用服务前，请选择至少 1 个可履约员工'
    return
  }
  loading.value = true
  message.value = ''
  const payload = {
    name: normalizedName,
    serviceType: form.serviceType,
    durationMin: Number(form.durationMin),
    defaultCapacity: Number(form.defaultCapacity),
    deductCount: Number(form.deductCount),
    resourceIds: form.resourceIds,
    staffIds: form.staffIds,
    status
  }
  try {
    if (editing.value) {
      await api.request(
        'PUT',
        `/owner/services/${form.id}`,
        payload,
        createIdempotencyKey('service-update')
      )
    } else {
      await api.request('POST', '/owner/services', payload, createIdempotencyKey('service-create'))
    }
    const title = editing.value && form.status === 'active' && status === 'active'
      ? '修改已保存'
      : status === 'active' ? '服务已启用' : '草稿已保存'
    await Taro.showToast({ title, icon: 'success' })
    closeForm()
    await load()
  } catch (error) {
    if (isServiceNameTaken(error)) {
      serviceNameError.value = SERVICE_NAME_TAKEN_MESSAGE
      message.value = SERVICE_NAME_TAKEN_MESSAGE
    } else {
      message.value = messageOf(error, '服务项目保存失败')
    }
  } finally {
    loading.value = false
  }
}

async function changeStatus(service: Service) {
  const nextStatus = service.status === 'active' ? 'disabled' : 'active'
  try {
    await api.request(
      'PUT',
      `/owner/services/${service.id}/status`,
      { status: nextStatus },
      createIdempotencyKey('service-status')
    )
    await Taro.showToast({
      title: nextStatus === 'active' ? '服务已启用' : '服务已停用',
      icon: 'success'
    })
    await load()
    return true
  } catch (error) {
    message.value = messageOf(error, '服务状态更新失败')
    return false
  }
}

async function disableCurrentService() {
  if (!editing.value || form.status !== 'active') return
  const confirmation = await Taro.showModal({
    title: '停用服务',
    content: '停用后会员不能预约新时段，已有历史记录不受影响。',
    confirmText: '确认停用',
    confirmColor: '#a55345'
  })
  if (!confirmation.confirm) return
  const currentService = services.value.find((service) => service.id === form.id)
  if (!currentService) {
    message.value = '服务项目不存在或已被更新，请返回列表刷新'
    return
  }
  loading.value = true
  const changed = await changeStatus(currentService)
  loading.value = false
  if (changed) closeForm()
}

async function removeCurrentService() {
  editMenuOpen.value = false
  if (!editing.value) return
  if (form.status === 'active') {
    await Taro.showModal({
      title: '请先停用服务',
      content: '启用中的服务不能直接删除。请先停用服务，再执行删除操作。',
      showCancel: false,
      confirmText: '我知道了',
      confirmColor: '#176b5d'
    })
    return
  }
  const serviceId = form.id
  const serviceName = form.name
  const confirmation = await Taro.showModal({
    title: `删除“${serviceName}”？`,
    content: '删除后不可恢复。系统会先校验会员卡和排期关联；已有业务记录的服务只能停用。',
    confirmText: '确认删除',
    confirmColor: '#c64a46'
  })
  if (!confirmation.confirm) return
  loading.value = true
  message.value = ''
  try {
    await api.request(
      'DELETE',
      `/owner/services/${serviceId}`,
      {},
      createIdempotencyKey('service-delete')
    )
    closeForm()
    await Taro.showToast({ title: '服务项目已删除', icon: 'success' })
    await load()
  } catch (error) {
    const deleteErrorMessage = messageOf(error, '服务项目暂不能删除，请稍后重试')
    message.value = deleteErrorMessage
    await Taro.showModal({
      title: '服务项目未删除',
      content: deleteErrorMessage,
      showCancel: false,
      confirmText: '我知道了',
      confirmColor: '#176b5d'
    })
  } finally {
    loading.value = false
  }
}

useDidShow(load)
</script>

<template>
  <View class="screen service-screen">
    <OwnerTopbar title="服务项目" back />
    <scroll-view :scroll-y="true" :enhanced="true" :show-scrollbar="false" class="service-page-scroll">
      <View class="content owner-dense service-page">
        <View class="owner-page-head">
          <View class="owner-page-head-copy">
            <Text class="page-title">服务与排期</Text>
            <Text class="page-copy">快速确认每个项目的资源、员工与服务时长。</Text>
          </View>
          <button
            class="owner-add-action"
            aria-label="新增服务"
            hover-class="owner-add-action-pressed"
            @tap="openCreate"
          ><View class="owner-add-action-icon" /></button>
        </View>

        <View class="owner-status-strip service-status-strip">
          <View class="owner-status">
            <Text>服务项目</Text><Text class="status-value">{{ services.length }}</Text><Text class="status-note">全部</Text>
          </View>
          <View class="owner-status">
            <Text>可排期</Text><Text class="status-value">{{ activeCount }}</Text><Text class="status-note">配置完整</Text>
          </View>
          <View class="owner-status">
            <Text>待处理</Text><Text class="status-value">{{ attentionCount }}</Text><Text class="status-note coral-text">草稿或停用</Text>
          </View>
        </View>

        <View class="owner-tabs service-tabs">
          <Text :class="{ active: serviceFilter === 'all' }" @tap="serviceFilter = 'all'">全部</Text>
          <Text :class="{ active: serviceFilter === 'active' }" @tap="serviceFilter = 'active'">可排期</Text>
          <Text :class="{ active: serviceFilter === 'attention' }" @tap="serviceFilter = 'attention'">待处理</Text>
        </View>

        <View v-if="filteredServices.length" class="service-matrix">
          <View class="service-matrix-head">
            <Text>服务项目</Text><Text>资源</Text><Text>员工</Text><Text>时长</Text>
          </View>
          <View
            v-for="service in filteredServices"
            :key="service.id"
            class="service-matrix-row"
            :class="`status-${service.status}`"
            hover-class="service-matrix-row-pressed"
            role="button"
            :aria-label="`${service.name}，${serviceStatusLabel(service)}，点击修改`"
            @tap="edit(service)"
          >
            <View class="service-matrix-main">
              <Text class="service-matrix-name">{{ service.name }}</Text>
              <Text class="service-matrix-subtitle">{{ service.serviceType }} · 容量 {{ service.defaultCapacity }}</Text>
              <View class="service-matrix-status">
                <View class="service-matrix-status-dot" />
                <Text>{{ serviceStatusLabel(service) }}</Text>
              </View>
            </View>
            <View class="service-matrix-cell" :class="{ missing: !resourceCount(service) }"><Text class="service-matrix-value">{{ resourceCount(service) }}</Text><Text class="service-matrix-unit">个</Text></View>
            <View class="service-matrix-cell" :class="{ missing: !staffCount(service) }"><Text class="service-matrix-value">{{ staffCount(service) }}</Text><Text class="service-matrix-unit">人</Text></View>
            <View class="service-matrix-cell"><Text class="service-matrix-value">{{ service.durationMin }}</Text><Text class="service-matrix-unit">分钟</Text></View>
          </View>
        </View>
        <View v-else class="empty-state service-empty">
          <Text class="empty-title">{{ services.length ? '没有符合筛选的服务' : '还没有服务项目' }}</Text>
          <Text>{{ services.length ? '切换筛选查看其他项目。' : '新增首个服务后，可继续配置会员卡和排期。' }}</Text>
          <button v-if="!services.length" class="button service-empty-action" @tap="openCreate">新增服务</button>
        </View>
        <View v-if="message && !showForm" class="error-banner">{{ message }}</View>
      </View>
    </scroll-view>

    <View v-if="showForm" class="modal-backdrop service-editor-backdrop" @tap.self="closeForm">
      <View class="sheet service-editor-sheet" @tap="editMenuOpen = false">
        <View class="sheet-grip" />
        <View class="sheet-title staff-sheet-title service-sheet-title">
          <View class="staff-sheet-title-copy">
            <Text class="sheet-heading">{{ editing ? '修改服务项目' : '创建可预约服务' }}</Text>
            <Text class="sheet-copy">{{ editing ? '调整会员展示、预约参数和履约范围。' : '完成基础信息后即可保存草稿或启用。' }}</Text>
          </View>
          <View v-if="editing" class="staff-sheet-actions service-sheet-actions" @tap.stop>
            <button
              class="staff-sheet-more"
              :class="{ active: editMenuOpen }"
              aria-label="更多服务操作"
              hover-class="staff-sheet-more-pressed"
              @tap="toggleEditMenu"
            >
              <View class="staff-sheet-more-dot" />
              <View class="staff-sheet-more-dot" />
              <View class="staff-sheet-more-dot" />
            </button>
            <View v-if="editMenuOpen" class="staff-sheet-menu service-sheet-menu">
              <button
                class="staff-sheet-menu-danger"
                hover-class="staff-sheet-menu-danger-pressed"
                :disabled="loading"
                @tap="removeCurrentService"
              >删除服务项目</button>
            </View>
          </View>
        </View>

        <scroll-view
          :scroll-y="true"
          :enhanced="true"
          :show-scrollbar="false"
          :scroll-top="editorScrollTop"
          class="service-editor-body"
          @scroll="rememberEditorScroll"
        >
          <View class="service-form">
            <View class="service-form-band">
              <Text class="service-band-title">会员看到什么</Text>
              <View class="owner-form-grid service-member-grid">
                <View class="field wide" :class="{ 'service-field-invalid': serviceNameError }">
                  <Text>服务名称 <Text class="required-mark">必填</Text></Text>
                  <input
                    v-model="form.name"
                    class="field-input"
                    maxlength="120"
                    placeholder="如：姿态评估服务"
                    @input="clearServiceNameError"
                  />
                  <Text v-if="serviceNameError" class="field-error">{{ serviceNameError }}</Text>
                </View>
                <View class="field wide">
                  <Text>服务类型 <Text class="required-mark">必填</Text></Text>
                  <input v-model="form.serviceType" class="field-input" maxlength="80" />
                </View>
              </View>
            </View>

            <View class="service-form-band">
              <Text class="service-band-title">每次如何预约</Text>
              <View class="service-number-grid">
                <View class="field service-number-field">
                  <Text>时长</Text><input v-model.number="form.durationMin" class="field-input" type="number" />
                  <Text class="service-number-unit">分钟</Text>
                </View>
                <View class="field service-number-field">
                  <Text>容量</Text><input v-model.number="form.defaultCapacity" class="field-input" type="number" />
                  <Text class="service-number-unit">人</Text>
                </View>
                <View class="field service-number-field">
                  <Text>核销</Text><input v-model.number="form.deductCount" class="field-input" type="number" />
                  <Text class="service-number-unit">次</Text>
                </View>
              </View>
            </View>

            <View class="service-form-band">
              <Text class="service-band-title">由谁、在哪里履约</Text>
              <View class="service-selector-list">
                <View class="field selector-field service-selector" @tap="openScope('resources')">
                  <Text>适用资源 <Text class="required-mark">必填</Text></Text>
                  <View class="service-selector-value">
                    <Text class="service-selection-summary">{{ selectedResourceSummary }}</Text><Text class="service-selector-arrow">⌄</Text>
                  </View>
                </View>
                <View class="field selector-field service-selector" @tap="openScope('staff')">
                  <Text>可履约员工 <Text class="required-mark">必填</Text></Text>
                  <View class="service-selector-value">
                    <Text class="service-selection-summary" :class="{ 'gold-text': !form.staffIds.length }">{{ selectedStaffSummary }}</Text><Text class="service-selector-arrow">⌄</Text>
                  </View>
                  <Text v-if="!form.staffIds.length" class="field-error">缺少可履约员工时只能保存草稿。</Text>
                </View>
              </View>
            </View>

            <View v-if="message" class="error-banner">{{ message }}</View>
          </View>
        </scroll-view>

        <View class="form-footer service-form-footer">
          <template v-if="editing && form.status === 'active'">
            <button class="button service-disable-action" :loading="loading" :disabled="loading" @tap="disableCurrentService">停用服务</button>
            <button class="button" :loading="loading" :disabled="loading || !canActivate" @tap="submit('active')">保存修改</button>
          </template>
          <template v-else>
            <button class="button secondary" :loading="loading" :disabled="loading" @tap="submit('draft')">保存草稿</button>
            <button class="button" :loading="loading" :disabled="loading || !canActivate" @tap="submit('active')">保存并启用</button>
          </template>
        </View>
      </View>
    </View>

    <View v-if="scopeMode" class="modal-backdrop service-scope-backdrop" @tap.self="closeScope">
      <View class="sheet service-scope-sheet">
        <View class="sheet-grip" />
        <View class="sheet-title">
          <View>
            <Text class="sheet-heading">{{ scopeMode === 'resources' ? '选择适用资源' : '选择可履约员工' }}</Text>
            <Text class="sheet-copy">支持多选；停用项不可选。</Text>
          </View>
        </View>
        <View v-if="scopeMode === 'resources'" class="owner-progress">
          <View
            v-for="resource in resources"
            :key="resource.id"
            class="owner-step"
            @tap="resource.id && toggleId('resourceIds', resource.id, resource.enabled !== false)"
          >
            <Text class="step-glyph">{{ resource.id && form.resourceIds.includes(resource.id) ? '✓' : '-' }}</Text>
            <View>
              <Text class="step-title">{{ resource.name }}</Text>
              <Text class="step-copy">{{ resource.resourceType }} · {{ resource.enabled === false ? '停用' : '启用' }} · 容量 {{ resource.capacity }}</Text>
            </View>
            <Text class="tag" :class="resource.enabled === false ? 'red' : resource.id && form.resourceIds.includes(resource.id) ? '' : 'blue'">{{ resource.enabled === false ? '不可选' : resource.id && form.resourceIds.includes(resource.id) ? '已选' : '选择' }}</Text>
          </View>
        </View>
        <View v-else class="owner-progress">
          <View
            v-for="item in staff"
            :key="item.id"
            class="owner-step"
            @tap="toggleId('staffIds', item.id, item.status === 'active')"
          >
            <Text class="step-glyph">{{ form.staffIds.includes(item.id) ? '✓' : '-' }}</Text>
            <View>
              <Text class="step-title">{{ item.staffName }}</Text>
              <Text class="step-copy">{{ item.roleLabel }} · {{ item.status === 'active' ? '启用 · 可排期' : '停用 · 不可排期' }}</Text>
            </View>
            <Text class="tag" :class="item.status !== 'active' ? 'red' : form.staffIds.includes(item.id) ? '' : 'blue'">{{ item.status !== 'active' ? '不可选' : form.staffIds.includes(item.id) ? '已选' : '选择' }}</Text>
          </View>
        </View>
        <View class="form-footer"><button class="button secondary" @tap="closeScope">取消</button><button class="button" @tap="closeScope">确认选择</button></View>
      </View>
    </View>
  </View>
</template>
