<script setup lang="ts">
import Taro, { useDidShow, useLoad } from '@tarojs/taro'
import { computed, reactive, ref } from 'vue'
import { createIdempotencyKey } from '@serenmeet/api-client'
import OwnerTopbar from '../../components/OwnerTopbar.vue'
import { api } from '../../api'
import { guardOwner, messageOf, type Resource, type Service, type Staff } from '../../owner'

const services = ref<Service[]>([])
const resources = ref<Resource[]>([])
const staff = ref<Staff[]>([])
const form = reactive({ id: 0, name: '', serviceType: '一对一服务', durationMin: 60, defaultCapacity: 1, deductCount: 1, resourceIds: [] as number[], staffIds: [] as number[], status: 'active' })
const loading = ref(false)
const message = ref('')
const showForm = ref(true)
const scopeMode = ref<'resources' | 'staff' | ''>('')
const editing = computed(() => Boolean(form.id))
const enabledResources = computed(() => resources.value.filter((item) => item.enabled !== false))
const enabledStaff = computed(() => staff.value.filter((item) => item.status === 'active'))
const canActivate = computed(() => form.name && form.durationMin > 0 && form.defaultCapacity > 0 && form.deductCount > 0 && form.resourceIds.length > 0 && form.staffIds.length > 0)
const selectedResourceNames = computed(() => resources.value.filter((item) => item.id && form.resourceIds.includes(item.id)).map((item) => item.name).join('、'))
const selectedStaffNames = computed(() => staff.value.filter((item) => form.staffIds.includes(item.id)).map((item) => item.staffName).join('、'))

async function load() {
  message.value = ''
  try {
    if (!await guardOwner()) return
    const options = await api.request<{ services: Service[]; resources: Resource[]; staff: Staff[] }>('GET', '/owner/services/options')
    services.value = options.services
    resources.value = options.resources
    staff.value = options.staff
  } catch (error) { message.value = messageOf(error, '服务项目加载失败') }
}

function reset() {
  Object.assign(form, { id: 0, name: '', serviceType: '一对一服务', durationMin: 60, defaultCapacity: 1, deductCount: 1, resourceIds: [], staffIds: [], status: 'active' })
  showForm.value = true
}

function edit(service: Service) {
  Object.assign(form, { id: service.id, name: service.name, serviceType: service.serviceType, durationMin: service.durationMin, defaultCapacity: service.defaultCapacity, deductCount: service.deductCount, resourceIds: service.resourceIds || [], staffIds: service.staffIds || [], status: service.status })
  showForm.value = true
}

function toggleId(target: 'resourceIds' | 'staffIds', id: number, selectable: boolean) {
  if (!selectable) return
  const values = form[target]
  form[target] = values.includes(id) ? values.filter((value) => value !== id) : [...values, id]
}

async function submit(status: 'draft' | 'active') {
  if (!form.name || !form.resourceIds.length) { message.value = '请填写服务名称并选择至少 1 个启用资源'; return }
  if (status === 'active' && !canActivate.value) { message.value = '启用服务前，请选择至少 1 个可履约员工'; return }
  loading.value = true
  message.value = ''
  const payload = { name: form.name, serviceType: form.serviceType, durationMin: Number(form.durationMin), defaultCapacity: Number(form.defaultCapacity), deductCount: Number(form.deductCount), resourceIds: form.resourceIds, staffIds: form.staffIds, status }
  try {
    if (editing.value) await api.request('PUT', `/owner/services/${form.id}`, payload, createIdempotencyKey('service-update'))
    else await api.request('POST', '/owner/services', payload, createIdempotencyKey('service-create'))
    await Taro.showToast({ title: status === 'active' ? '服务已启用' : '草稿已保存', icon: 'success' })
    reset()
    await load()
  } catch (error) { message.value = messageOf(error, '服务项目保存失败') } finally { loading.value = false }
}

async function changeStatus(service: Service) {
  const nextStatus = service.status === 'active' ? 'disabled' : 'active'
  try {
    await api.request('PUT', `/owner/services/${service.id}/status`, { status: nextStatus }, createIdempotencyKey('service-status'))
    await Taro.showToast({ title: nextStatus === 'active' ? '服务已启用' : '服务已停用', icon: 'success' })
    await load()
  } catch (error) { message.value = messageOf(error, '服务状态更新失败') }
}

useLoad((params) => { if (params.mode === 'list') showForm.value = false })
useDidShow(load)
</script>

<template>
  <View class="screen">
    <OwnerTopbar title="服务项目" back />
    <View class="content owner-dense">
      <View class="owner-page-head">
        <View class="owner-page-head-copy">
          <Text class="page-title">{{ showForm ? editing ? '修改服务项目' : '创建可预约服务' : '服务项目' }}</Text>
          <Text class="page-copy">{{ showForm ? '先写清楚会员看到的服务，再选择哪些资源和员工可以承接。' : `共 ${services.length} 项，查看服务状态或进入修改。` }}</Text>
        </View>
        <button v-if="!showForm" class="owner-add-action" aria-label="新增服务" hover-class="owner-add-action-pressed" @tap="reset"><View class="owner-add-action-icon" /></button>
      </View>
      <View v-if="services.length" class="service-summary-list">
        <View v-for="service in services" :key="service.id" class="owner-list-card" @tap="edit(service)"><Text class="list-card-title">{{ service.name }}</Text><Text class="list-card-copy">{{ service.serviceType }} · {{ service.durationMin }} 分钟 · 容量 {{ service.defaultCapacity }} · 每次核销 {{ service.deductCount }} 次</Text><Text class="tag" :class="service.status === 'active' ? '' : 'warn'">{{ service.status === 'active' ? '启用' : '草稿/停用' }}</Text><View class="inline-actions"><Text @tap.stop="edit(service)">修改</Text><Text @tap.stop="changeStatus(service)">{{ service.status === 'active' ? '停用' : '启用' }}</Text></View></View>
      </View>
      <View v-if="showForm" class="service-form">
        <View class="owner-form-grid">
          <View class="field wide"><Text>服务名称 <Text class="required-mark">必填</Text></Text><input v-model="form.name" class="field-input" placeholder="如：姿态评估服务" /></View>
          <View class="field"><Text>服务类型 <Text class="required-mark">必填</Text></Text><input v-model="form.serviceType" class="field-input" /></View>
          <View class="field"><Text>服务时长 <Text class="required-mark">必填</Text></Text><input v-model.number="form.durationMin" class="field-input" type="number" /></View>
          <View class="field"><Text>默认容量 <Text class="required-mark">必填</Text></Text><input v-model.number="form.defaultCapacity" class="field-input" type="number" /></View>
          <View class="field"><Text>每次核销 <Text class="required-mark">必填</Text></Text><input v-model.number="form.deductCount" class="field-input" type="number" /></View>
          <View class="field"><Text>服务状态</Text><Text class="field-value">{{ canActivate ? '可启用' : '仅可保存草稿' }}</Text></View>
          <View class="field wide selector-field" @tap="scopeMode = 'resources'"><Text>适用资源（下拉多选） <Text class="required-mark">必填</Text></Text><Text class="field-value">{{ selectedResourceNames || '请选择至少 1 个启用资源' }} ▾</Text></View>
          <View class="field wide selector-field" @tap="scopeMode = 'staff'"><Text>可履约员工（下拉多选） <Text class="required-mark">必填</Text></Text><Text class="field-value" :class="{ 'gold-text': !form.staffIds.length }">{{ selectedStaffNames || '请选择至少 1 个启用员工' }} ▾</Text></View>
        </View>
        <View class="owner-list-card"><Text class="list-card-title">保存后会影响哪里</Text><Text class="list-card-copy">会员卡选择适用服务、排期选择服务和员工时都会用到这份配置。</Text><Text class="tag blue">说明</Text></View>
        <View class="owner-list-card"><Text class="list-card-title">启用前检查</Text><Text class="list-card-copy">服务名称、时长、容量、核销次数、至少 1 个启用资源和 1 个启用员工都完整后才可启用。</Text><Text class="tag" :class="canActivate ? '' : 'red'">{{ canActivate ? '可启用' : '缺员工' }}</Text></View>
        <View v-if="message" class="error-banner">{{ message }}</View>
        <View class="form-footer"><button class="button secondary" :loading="loading" :disabled="loading" @tap="submit('draft')">保存草稿</button><button class="button" :loading="loading" :disabled="loading || !canActivate" @tap="submit('active')">保存并启用</button></View>
      </View>
    </View>
    <View v-if="scopeMode" class="modal-backdrop" @tap.self="scopeMode = ''">
      <View class="sheet">
        <View class="sheet-grip" /><View class="sheet-title"><View><Text class="sheet-heading">{{ scopeMode === 'resources' ? '选择适用资源' : '选择可履约员工' }}</Text><Text class="sheet-copy">支持多选；停用项不可选。</Text></View></View>
        <View class="owner-progress" v-if="scopeMode === 'resources'">
          <View v-for="resource in resources" :key="resource.id" class="owner-step" @tap="resource.id && toggleId('resourceIds', resource.id, resource.enabled !== false)"><Text class="step-glyph">{{ resource.id && form.resourceIds.includes(resource.id) ? '✓' : '-' }}</Text><View><Text class="step-title">{{ resource.name }}</Text><Text class="step-copy">{{ resource.resourceType }} · {{ resource.enabled === false ? '停用' : '启用' }} · 容量 {{ resource.capacity }}</Text></View><Text class="tag" :class="resource.enabled === false ? 'red' : resource.id && form.resourceIds.includes(resource.id) ? '' : 'blue'">{{ resource.enabled === false ? '不可选' : resource.id && form.resourceIds.includes(resource.id) ? '已选' : '选择' }}</Text></View>
        </View>
        <View class="owner-progress" v-else>
          <View v-for="item in staff" :key="item.id" class="owner-step" @tap="toggleId('staffIds', item.id, item.status === 'active')"><Text class="step-glyph">{{ form.staffIds.includes(item.id) ? '✓' : '-' }}</Text><View><Text class="step-title">{{ item.staffName }}</Text><Text class="step-copy">{{ item.roleLabel }} · {{ item.status === 'active' ? '启用 · 可排期' : '停用 · 不可排期' }}</Text></View><Text class="tag" :class="item.status !== 'active' ? 'red' : form.staffIds.includes(item.id) ? '' : 'blue'">{{ item.status !== 'active' ? '不可选' : form.staffIds.includes(item.id) ? '已选' : '选择' }}</Text></View>
        </View>
        <View class="form-footer"><button class="button secondary" @tap="scopeMode = ''">取消</button><button class="button" @tap="scopeMode = ''">确认选择</button></View>
      </View>
    </View>
  </View>
</template>
