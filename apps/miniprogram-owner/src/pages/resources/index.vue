<script setup lang="ts">
import Taro, { useDidShow } from '@tarojs/taro'
import { computed, reactive, ref } from 'vue'
import { createIdempotencyKey } from '@serenmeet/api-client'
import OwnerTopbar from '../../components/OwnerTopbar.vue'
import { api } from '../../api'
import { guardOwner, messageOf, type OnboardingDraft, type Resource, ownerRoutes } from '../../owner'
import {
  CUSTOM_RESOURCE_TYPE,
  FIXED_RESOURCE_TYPES,
  resolveResourceType,
  resourceTypeStateFromValue,
  type ResourceTypeSelection
} from '../../resource-form'

const types: ResourceTypeSelection[] = [...FIXED_RESOURCE_TYPES, CUSTOM_RESOURCE_TYPE]
const resources = ref<Resource[]>([])
const storeReady = ref(false)
const sheetOpen = ref(false)
const editingIndex = ref(-1)
const form = reactive<Resource>({ name: '', resourceType: '房间', capacity: 1, enabled: true, sortOrder: 1 })
const selectedResourceType = ref<ResourceTypeSelection>('房间')
const customResourceType = ref('')
const saving = ref(false)
const message = ref('')
const enabledCount = computed(() => resources.value.filter((item) => item.enabled !== false).length)
const customTypeSelected = computed(() => selectedResourceType.value === CUSTOM_RESOURCE_TYPE)

async function load() {
  message.value = ''
  try {
    const owner = await guardOwner()
    if (!owner) return
    storeReady.value = Boolean(owner.storeId)
    if (storeReady.value) resources.value = await api.request<Resource[]>('GET', '/owner/resources')
    else {
      const draft = await api.request<OnboardingDraft>('GET', '/owner/onboarding/draft')
      resources.value = (draft.resources || []).map((item, index) => ({
        ...item,
        enabled: item.enabled !== false,
        sortOrder: item.sortOrder ?? index + 1
      }))
    }
    if (!resources.value.length) openCreate()
  } catch (error) { message.value = messageOf(error, '资源列表加载失败') }
}

function resetResourceType(value: string) {
  const state = resourceTypeStateFromValue(value)
  selectedResourceType.value = state.selection
  customResourceType.value = state.customValue
}

function resetForm() {
  Object.assign(form, { id: undefined, name: '', resourceType: '房间', capacity: 1, enabled: true, sortOrder: resources.value.length + 1 })
  resetResourceType('房间')
}
function closeSheet() { sheetOpen.value = false; message.value = '' }
function openCreate() { editingIndex.value = -1; message.value = ''; resetForm(); sheetOpen.value = true }
function openEdit(index: number) {
  editingIndex.value = index
  message.value = ''
  const resource = resources.value[index]
  Object.assign(form, {
    ...resource,
    enabled: resource.enabled !== false,
    sortOrder: Math.max(1, resource.sortOrder ?? index + 1)
  })
  resetResourceType(resource.resourceType)
  sheetOpen.value = true
}
function selectResourceType(type: ResourceTypeSelection) { selectedResourceType.value = type }
function changeCapacity(delta: number) { form.capacity = Math.max(1, form.capacity + delta) }
function changeSortOrder(delta: number) { form.sortOrder = Math.max(1, (form.sortOrder ?? 1) + delta) }

async function saveDraftList(next: Resource[]) {
  await api.request('PUT', '/owner/onboarding/resources', {
    resources: next.map(({ name, resourceType, capacity, enabled, sortOrder }, index) => ({
      name,
      resourceType,
      capacity,
      enabled: enabled !== false,
      sortOrder: sortOrder ?? index + 1
    }))
  })
}

async function saveResource() {
  const name = form.name.trim()
  const resourceType = resolveResourceType(selectedResourceType.value, customResourceType.value)
  if (!name) { message.value = '请填写资源名称'; return }
  if (!resourceType) { message.value = '请填写自定义资源类型'; return }
  saving.value = true
  message.value = ''
  try {
    const payload: Resource = {
      ...form,
      name,
      resourceType,
      enabled: form.enabled !== false,
      sortOrder: Math.max(1, form.sortOrder ?? 1)
    }
    if (storeReady.value) {
      if (editingIndex.value >= 0 && form.id) await api.request('PUT', `/owner/resources/${form.id}`, payload, createIdempotencyKey('resource-update'))
      else await api.request('POST', '/owner/resources', payload, createIdempotencyKey('resource-create'))
    } else {
      const next = [...resources.value]
      const value = { ...payload }
      if (editingIndex.value >= 0) next[editingIndex.value] = value
      else next.push(value)
      await saveDraftList(next)
    }
    sheetOpen.value = false
    await Taro.showToast({ title: '资源已保存', icon: 'success' })
    await load()
  } catch (error) { message.value = messageOf(error, '资源保存失败') } finally { saving.value = false }
}

async function removeResource(resource: Resource) {
  const confirm = await Taro.showModal({ title: `确认删除${resource.name}？`, content: '有关联未来排期或删除后没有启用资源时，系统会阻止删除。', confirmText: '确认删除', confirmColor: '#c64a46' })
  if (!confirm.confirm) return
  try {
    if (storeReady.value && resource.id) await api.request('DELETE', `/owner/resources/${resource.id}`, {}, createIdempotencyKey('resource-delete'))
    else await saveDraftList(resources.value.filter((item) => item !== resource))
    await Taro.showToast({ title: '资源已删除', icon: 'success' })
    await load()
  } catch (error) { message.value = messageOf(error, '资源删除失败') }
}

async function finish() {
  if (!enabledCount.value) { message.value = '至少保留 1 个启用资源'; return }
  if (storeReady.value) await Taro.navigateBack()
  else await Taro.redirectTo({ url: ownerRoutes.onboarding })
}

useDidShow(load)
</script>

<template>
  <View class="screen">
    <OwnerTopbar title="资源管理" back />
    <View class="content owner-dense">
      <View class="owner-page-head">
        <View class="owner-page-head-copy"><Text class="page-title">履约资源</Text><Text class="page-copy">共 {{ resources.length }} 个，资源可以是房间、场地、床位、设备或工位。</Text></View>
        <button class="owner-add-action" aria-label="新增资源" hover-class="owner-add-action-pressed" @tap="openCreate"><View class="owner-add-action-icon" /></button>
      </View>
      <View v-if="message && !sheetOpen" class="error-banner">{{ message }}</View>
      <View v-if="!resources.length && !sheetOpen" class="empty-state"><Text class="empty-title">还没有履约资源</Text><Text>新增房间、场地、床位、设备或工位后，才能继续开店。</Text></View>
      <View v-else class="owner-progress">
        <View v-for="(resource, index) in resources" :key="resource.id || `${resource.name}-${index}`" class="owner-step">
          <Text class="step-glyph">{{ String.fromCharCode(65 + index) }}</Text>
          <View><View class="resource-name-line"><Text class="step-title">{{ resource.name }}</Text><Text class="resource-status" :class="{ off: resource.enabled === false }">{{ resource.enabled === false ? '停用' : '启用' }}</Text></View><Text class="step-copy">{{ resource.resourceType }} · 容量 {{ resource.capacity }} · 排序 {{ resource.sortOrder || index + 1 }}</Text></View>
          <View class="resource-row-actions"><Text class="resource-action" @tap="openEdit(index)">修改</Text><Text class="resource-action danger" @tap="removeResource(resource)">删除</Text></View>
        </View>
      </View>
      <View class="owner-card-grid"><View class="tile"><Text>启用资源</Text><Text class="tile-value">{{ enabledCount }} 个</Text></View><View class="tile"><Text>可预约资源</Text><Text class="tile-value">{{ enabledCount }} 个</Text></View></View>
      <View class="form-footer single"><button class="button" :disabled="!enabledCount" @tap="finish">{{ storeReady ? '完成' : '完成并返回清单' }}</button></View>
    </View>
    <View v-if="sheetOpen" class="modal-backdrop" @tap.self="closeSheet">
      <View class="sheet resource-sheet clear-resource-sheet">
        <View class="sheet-grip" />
        <View class="sheet-title"><View><Text class="sheet-heading">{{ editingIndex >= 0 ? '修改资源' : '新增资源' }}</Text><Text class="sheet-copy">填写资源信息，保存后回到资源列表。</Text></View></View>
        <scroll-view :scroll-y="true" :enhanced="true" :show-scrollbar="false" class="clear-resource-body">
          <View class="resource-section">
            <View class="resource-section-head"><Text class="resource-section-title">资源名称 <Text class="required-mark">必填</Text></Text><Text class="resource-section-meta">列表与排期中展示</Text></View>
            <View class="resource-line-field"><input v-model="form.name" maxlength="80" class="resource-line-input" placeholder="如：静语间 C" placeholder-class="field-placeholder" /><Text>门店内部能快速识别的具体空间或设备名称。</Text></View>
          </View>
          <View class="resource-section">
            <View class="resource-section-head"><Text class="resource-section-title">资源类型 <Text class="required-mark">必填</Text></Text><Text class="resource-section-meta">选择一项</Text></View>
            <View class="resource-type-grid"><Text v-for="type in types" :key="type" :class="{ selected: selectedResourceType === type }" @tap="selectResourceType(type)">{{ type }}</Text></View>
            <View v-if="customTypeSelected" class="resource-custom-entry"><Text>自定义类型 <Text class="required-mark">必填</Text></Text><input v-model="customResourceType" maxlength="80" class="resource-custom-input" placeholder="如：咨询舱" placeholder-class="field-placeholder" /></View>
          </View>
          <View class="resource-section">
            <View class="resource-section-head"><Text class="resource-section-title">容量与排序 <Text class="required-mark">必填</Text></Text><Text class="resource-section-meta">用于排期</Text></View>
            <View class="resource-parameter-pair">
              <View class="resource-parameter"><Text>默认容量</Text><View class="resource-stepper"><button @tap="changeCapacity(-1)">-</button><Text>{{ form.capacity }} 人</Text><button @tap="changeCapacity(1)">+</button></View></View>
              <View class="resource-parameter"><Text>列表排序</Text><View class="resource-stepper"><button @tap="changeSortOrder(-1)">-</button><Text>{{ form.sortOrder }} 位</Text><button @tap="changeSortOrder(1)">+</button></View></View>
            </View>
          </View>
          <View class="resource-section">
            <View class="resource-status-row"><View class="resource-status-copy"><Text>启用资源</Text><Text>启用后可用于服务项目和新排期。</Text></View><View class="resource-toggle"><Text :class="{ selected: form.enabled }" @tap="form.enabled = true">启用</Text><Text :class="{ selected: !form.enabled }" @tap="form.enabled = false">停用</Text></View></View>
          </View>
        </scroll-view>
        <View v-if="message" class="error-banner">{{ message }}</View>
        <View class="form-footer"><button class="button secondary" @tap="closeSheet">取消</button><button class="button" :loading="saving" :disabled="saving" @tap="saveResource">保存资源</button></View>
      </View>
    </View>
  </View>
</template>
