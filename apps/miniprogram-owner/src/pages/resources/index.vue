<script setup lang="ts">
import Taro, { useDidShow } from '@tarojs/taro'
import { computed, reactive, ref } from 'vue'
import { createIdempotencyKey } from '@serenmeet/api-client'
import OwnerTopbar from '../../components/OwnerTopbar.vue'
import { api } from '../../api'
import { guardOwner, messageOf, type OnboardingDraft, type Resource, ownerRoutes } from '../../owner'

const types = ['房间', '场地', '床位', '设备', '工位', '自定义']
const resources = ref<Resource[]>([])
const storeReady = ref(false)
const sheetOpen = ref(false)
const editingIndex = ref(-1)
const form = reactive<Resource>({ name: '', resourceType: '房间', capacity: 1, enabled: true, sortOrder: 1 })
const saving = ref(false)
const message = ref('')
const enabledCount = computed(() => resources.value.filter((item) => item.enabled !== false).length)

async function load() {
  message.value = ''
  try {
    const owner = await guardOwner()
    if (!owner) return
    storeReady.value = Boolean(owner.storeId)
    if (storeReady.value) resources.value = await api.request<Resource[]>('GET', '/owner/resources')
    else {
      const draft = await api.request<OnboardingDraft>('GET', '/owner/onboarding/draft')
      resources.value = (draft.resources || []).map((item, index) => ({ ...item, enabled: true, sortOrder: index + 1 }))
    }
    if (!resources.value.length) openCreate()
  } catch (error) { message.value = messageOf(error, '资源列表加载失败') }
}

function resetForm() { Object.assign(form, { name: '', resourceType: '房间', capacity: 1, enabled: true, sortOrder: resources.value.length + 1 }) }
function openCreate() { editingIndex.value = -1; resetForm(); sheetOpen.value = true }
function openEdit(index: number) { editingIndex.value = index; Object.assign(form, resources.value[index]); sheetOpen.value = true }
function changeCapacity(delta: number) { form.capacity = Math.max(1, form.capacity + delta) }

async function saveDraftList(next: Resource[]) {
  await api.request('PUT', '/owner/onboarding/resources', { resources: next.map(({ name, resourceType, capacity }) => ({ name, resourceType, capacity })) })
}

async function saveResource() {
  if (!form.name.trim()) { message.value = '请填写资源名称'; return }
  saving.value = true
  message.value = ''
  try {
    if (storeReady.value) {
      if (editingIndex.value >= 0 && form.id) await api.request('PUT', `/owner/resources/${form.id}`, form, createIdempotencyKey('resource-update'))
      else await api.request('POST', '/owner/resources', form, createIdempotencyKey('resource-create'))
    } else {
      const next = [...resources.value]
      const value = { ...form, sortOrder: editingIndex.value >= 0 ? form.sortOrder : next.length + 1 }
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
        <button class="owner-add-action" aria-label="新增资源" hover-class="owner-add-action-pressed" @tap="openCreate"><Text class="owner-add-action-icon">+</Text></button>
      </View>
      <View v-if="message" class="error-banner">{{ message }}</View>
      <View v-if="!resources.length && !sheetOpen" class="empty-state"><Text class="empty-title">还没有履约资源</Text><Text>新增房间、场地、床位、设备或工位后，才能继续开店。</Text></View>
      <View v-else class="owner-progress">
        <View v-for="(resource, index) in resources" :key="resource.id || `${resource.name}-${index}`" class="owner-step">
          <Text class="step-glyph">{{ String.fromCharCode(65 + index) }}</Text>
          <View><View class="resource-name-line"><Text class="step-title">{{ resource.name }}</Text><Text class="resource-status" :class="{ off: resource.enabled === false }">{{ resource.enabled === false ? '停用' : '启用' }}</Text></View><Text class="step-copy">{{ resource.resourceType }} · 容量 {{ resource.capacity }} · 排序 {{ resource.sortOrder || index + 1 }}</Text></View>
          <View class="resource-row-actions"><Text class="resource-action" @tap="openEdit(index)">修改</Text><Text class="resource-action danger" @tap="removeResource(resource)">删除</Text></View>
        </View>
      </View>
      <View class="owner-card-grid"><View class="tile"><Text>启用资源</Text><Text class="tile-value">{{ enabledCount }} 个</Text></View><View class="tile"><Text>可预约资源</Text><Text class="tile-value">{{ enabledCount }} 个</Text></View></View>
      <View class="owner-start-note"><View><Text class="note-title">{{ enabledCount ? '已满足开店资源要求' : '还需启用资源' }}</Text><Text>至少 1 个启用资源即可创建门店，停用资源不会进入新排期选择。</Text></View><Text class="tag" :class="enabledCount ? '' : 'warn'">{{ enabledCount ? '可完成' : '未完成' }}</Text></View>
      <View class="form-footer single"><button class="button" :disabled="!enabledCount" @tap="finish">{{ storeReady ? '完成' : '完成并返回清单' }}</button></View>
    </View>
    <View v-if="sheetOpen" class="modal-backdrop" @tap.self="sheetOpen = false">
      <View class="sheet resource-sheet">
        <View class="sheet-grip" />
        <View class="sheet-title"><View><Text class="sheet-heading">{{ editingIndex >= 0 ? `修改${form.name}` : `新增第 ${resources.length + 1} 个资源` }}</Text><Text class="sheet-copy">资源是排期会占用的房间、场地、床位、设备或工位，保存后会回到列表。</Text></View></View>
        <View class="resource-section"><Text class="resource-section-title">基础信息 <Text class="required-mark">必填</Text></Text><View class="field wide"><Text>资源名称 <Text class="required-mark">必填</Text></Text><input v-model="form.name" class="field-input" placeholder="如：评估室 A" placeholder-class="field-placeholder" /></View></View>
        <View class="resource-section"><Text class="resource-section-title">资源类型 <Text class="required-mark">必填</Text></Text><View class="resource-type-grid"><Text v-for="type in types" :key="type" :class="{ selected: form.resourceType === type }" @tap="form.resourceType = type">{{ type }}</Text></View><Text class="resource-type-help">类型用于服务项目和排期筛选，不包含员工。</Text></View>
        <View class="resource-section"><Text class="resource-section-title">默认容量 <Text class="required-mark">必填</Text></Text><View class="resource-setting-row"><View><Text class="setting-title">默认容量</Text><Text class="setting-copy">最小 1 人</Text></View><View class="resource-stepper"><button @tap="changeCapacity(-1)">-</button><Text>{{ form.capacity }} 人</Text><button @tap="changeCapacity(1)">+</button></View></View></View>
        <View class="resource-section"><Text class="resource-section-title">启用状态 <Text class="required-mark">必填</Text></Text><View class="resource-toggle"><Text :class="{ selected: form.enabled }" @tap="form.enabled = true">启用</Text><Text :class="{ selected: !form.enabled }" @tap="form.enabled = false">停用</Text></View><View class="resource-sheet-note">停用后不会进入新排期选择。</View></View>
        <View v-if="message" class="error-banner">{{ message }}</View>
        <View class="form-footer"><button class="button secondary" @tap="sheetOpen = false">取消</button><button class="button" :loading="saving" :disabled="saving" @tap="saveResource">保存资源</button></View>
      </View>
    </View>
  </View>
</template>
