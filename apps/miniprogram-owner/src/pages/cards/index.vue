<script setup lang="ts">
import Taro, { useDidShow, useLoad } from '@tarojs/taro'
import { computed, reactive, ref } from 'vue'
import { createIdempotencyKey } from '@serenmeet/api-client'
import OwnerTopbar from '../../components/OwnerTopbar.vue'
import { api } from '../../api'
import { guardOwner, messageOf, type CardTemplate, type Service, type Staff } from '../../owner'

const templates = ref<CardTemplate[]>([])
const services = ref<Service[]>([])
const staff = ref<Staff[]>([])
const mode = ref<'list' | 'count' | 'period'>('list')
const scopeMode = ref<'services' | 'staff' | ''>('')
const form = reactive({ id: 0, name: '', salePriceYuan: 0, totalCount: 20, validDays: 365, lowBalanceThreshold: 2, serviceIds: [] as number[], staffIds: [] as number[] })
const loading = ref(false)
const message = ref('')
const countTotal = computed(() => templates.value.filter((item) => item.cardType === 'count').length)
const periodTotal = computed(() => templates.value.filter((item) => item.cardType === 'period').length)
const selectedServiceNames = computed(() => services.value.filter((item) => form.serviceIds.includes(item.id)).map((item) => item.name).join('、'))
const selectedStaffNames = computed(() => staff.value.filter((item) => form.staffIds.includes(item.id)).map((item) => item.staffName).join('、'))
const canSave = computed(() => form.name && Number(form.salePriceYuan) >= 0 && form.validDays > 0 && form.serviceIds.length > 0 && form.staffIds.length > 0 && (mode.value === 'period' || form.totalCount > 0))

async function load() {
  try {
    if (!await guardOwner()) return
    const [cardRows, optionRows] = await Promise.all([
      api.request<CardTemplate[]>('GET', '/owner/card-templates'),
      api.request<{ services: Service[]; staff: Staff[] }>('GET', '/owner/services/options')
    ])
    templates.value = cardRows
    services.value = optionRows.services.filter((item) => item.status === 'active')
    staff.value = optionRows.staff.filter((item) => item.status === 'active')
  } catch (error) { message.value = messageOf(error, '会员卡模板加载失败') }
}

function begin(type: 'count' | 'period') {
  mode.value = type
  Object.assign(form, { id: 0, name: '', salePriceYuan: 0, totalCount: 20, validDays: type === 'count' ? 365 : 30, lowBalanceThreshold: 2, serviceIds: [], staffIds: [] })
}

function edit(item: CardTemplate) {
  mode.value = item.cardType
  Object.assign(form, { id: item.id, name: item.name, salePriceYuan: item.salePriceYuan, totalCount: item.totalCount || 20, validDays: item.validDays, lowBalanceThreshold: item.lowBalanceThreshold || 2, serviceIds: item.serviceIds || [], staffIds: item.staffIds || [] })
}

function toggleId(target: 'serviceIds' | 'staffIds', id: number) {
  form[target] = form[target].includes(id) ? form[target].filter((value) => value !== id) : [...form[target], id]
}

async function save() {
  if (!canSave.value) { message.value = '请完成所有必填项，售价不能为负数'; return }
  loading.value = true
  message.value = ''
  const payload = { name: form.name, cardType: mode.value, salePriceYuan: Number(form.salePriceYuan), totalCount: mode.value === 'count' ? Number(form.totalCount) : null, validDays: Number(form.validDays), lowBalanceThreshold: mode.value === 'count' ? Number(form.lowBalanceThreshold) : null, serviceIds: form.serviceIds, staffIds: form.staffIds }
  try {
    if (form.id) await api.request('PUT', `/owner/card-templates/${form.id}`, payload, createIdempotencyKey('card-update'))
    else await api.request('POST', '/owner/card-templates', payload, createIdempotencyKey('card-create'))
    await Taro.showToast({ title: '会员卡模板已保存', icon: 'success' })
    mode.value = 'list'
    await load()
  } catch (error) { message.value = messageOf(error, '会员卡模板保存失败') } finally { loading.value = false }
}

useLoad((params) => { if (params.type === 'count' || params.type === 'period') begin(params.type) })
useDidShow(load)
</script>

<template>
  <View class="screen">
    <OwnerTopbar :title="mode === 'list' ? '会员卡模板' : mode === 'count' ? '次数卡模板' : '期限卡模板'" back />
    <View class="content owner-dense">
      <template v-if="mode === 'list'">
        <View class="owner-page-head"><View><Text class="page-title">选择卡类型</Text><Text class="page-copy">先决定按次数扣卡，还是按有效期使用；MVP 只提供这两类。</Text></View><Text class="tag blue">2 类</Text></View>
        <View class="owner-status-strip"><View class="owner-status"><Text>次数卡</Text><Text class="status-value">{{ countTotal }}</Text><Text class="status-note">按次核销</Text></View><View class="owner-status"><Text>期限卡</Text><Text class="status-value">{{ periodTotal }}</Text><Text class="status-note blue-text">按期使用</Text></View><View class="owner-status"><Text>可发卡</Text><Text class="status-value">{{ templates.length }}</Text><Text class="status-note">模板</Text></View></View>
        <View class="owner-progress"><View class="owner-step" @tap="begin('count')"><Text class="step-glyph">次</Text><View><Text class="step-title">新建次数卡</Text><Text class="step-copy">适合 10 次、20 次这类每次服务后扣次数的卡。</Text></View><Text class="tag blue">创建</Text></View><View class="owner-step" @tap="begin('period')"><Text class="step-glyph">期</Text><View><Text class="step-title">新建期限卡</Text><Text class="step-copy">适合月卡、季卡这类在有效期内使用的卡。</Text></View><Text class="tag blue">创建</Text></View></View>
        <View v-if="!services.length" class="owner-list-card"><Text class="list-card-title">请先创建服务项目</Text><Text class="list-card-copy">会员卡至少需要覆盖 1 个启用服务。</Text><Text class="tag red">阻断</Text></View>
        <View v-for="item in templates" :key="item.id" class="owner-list-card" @tap="edit(item)"><Text class="list-card-title">{{ item.name }}</Text><Text class="list-card-copy">¥{{ item.salePriceYuan }} · {{ item.cardType === 'count' ? `${item.totalCount} 次 · 低于 ${item.lowBalanceThreshold} 次提醒` : `${item.validDays} 天有效 · 不显示剩余次数` }}</Text><Text class="tag" :class="item.cardType === 'count' ? '' : 'blue'">{{ item.cardType === 'count' ? '次数卡' : '期限卡' }}</Text></View>
      </template>
      <template v-else>
        <View class="member-pass"><Text class="pass-title">{{ form.name || (mode === 'count' ? '评估训练次数卡' : '月度期限卡') }}</Text><Text class="pass-value">¥{{ form.salePriceYuan }}</Text><Text class="pass-copy">{{ mode === 'count' ? '会员每完成一次服务，员工核销后扣减次数。' : '会员在有效期内预约适用服务，不显示剩余次数。' }}</Text></View>
        <View class="owner-tabs"><Text :class="{ active: mode === 'count' }" @tap="begin('count')">次数卡</Text><Text :class="{ active: mode === 'period' }" @tap="begin('period')">期限卡</Text><Text @tap="mode = 'list'">返回列表</Text></View>
        <View class="owner-form-grid">
          <View class="field wide"><Text>卡名 <Text class="required-mark">必填</Text></Text><input v-model="form.name" class="field-input" placeholder="请输入会员卡名称" /></View>
          <View class="field"><Text>售价 <Text class="required-mark">必填</Text></Text><input v-model.number="form.salePriceYuan" class="field-input" type="digit" /></View>
          <View v-if="mode === 'count'" class="field"><Text>总次数 <Text class="required-mark">必填</Text></Text><input v-model.number="form.totalCount" class="field-input" type="number" /></View>
          <View class="field"><Text>有效期 <Text class="required-mark">必填</Text></Text><input v-model.number="form.validDays" class="field-input" type="number" /></View>
          <View v-if="mode === 'count'" class="field"><Text>低余额提醒 <Text class="required-mark">必填</Text></Text><input v-model.number="form.lowBalanceThreshold" class="field-input" type="number" /></View>
          <View v-else class="field"><Text>到期提醒 <Text class="required-mark">必填</Text></Text><Text class="field-value">提前 14 天</Text></View>
          <View class="field wide selector-field" @tap="scopeMode = 'services'"><Text>适用服务（下拉多选） <Text class="required-mark">必填</Text></Text><Text class="field-value">{{ selectedServiceNames || '请选择适用服务' }} ▾</Text></View>
          <View class="field wide selector-field" @tap="scopeMode = 'staff'"><Text>适用员工（下拉多选） <Text class="required-mark">必填</Text></Text><Text class="field-value">{{ selectedStaffNames || '请选择适用员工' }} ▾</Text></View>
        </View>
        <View class="owner-list-card"><Text class="list-card-title">发给会员后怎么用</Text><Text class="list-card-copy">{{ mode === 'count' ? '会员只能预约适用服务；到店后员工确认服务并扣减次数。' : '会员在有效期内可预约适用服务；过期后不能继续预约。' }}</Text><Text class="tag">清楚</Text></View>
        <View class="owner-list-card"><Text class="list-card-title">保存前检查</Text><Text class="list-card-copy">卡名、售价、有效期和适用范围都完整后才能保存；售价不能为负数。</Text><Text class="tag" :class="canSave ? 'blue' : 'red'">{{ canSave ? '可保存' : '待补充' }}</Text></View>
        <View v-if="message" class="error-banner">{{ message }}</View>
        <View class="form-footer"><button class="button secondary" @tap="mode = 'list'">返回列表</button><button class="button" :disabled="loading || !canSave" :loading="loading" @tap="save">保存模板</button></View>
      </template>
    </View>
    <View v-if="scopeMode" class="modal-backdrop" @tap.self="scopeMode = ''"><View class="sheet"><View class="sheet-grip" /><View class="sheet-title"><View><Text class="sheet-heading">{{ scopeMode === 'services' ? '选择适用服务' : '选择适用员工' }}</Text><Text class="sheet-copy">支持多选，停用项不会出现在这里。</Text></View></View><View class="owner-progress"><View v-for="item in scopeMode === 'services' ? services : staff" :key="item.id" class="owner-step" @tap="toggleId(scopeMode === 'services' ? 'serviceIds' : 'staffIds', item.id)"><Text class="step-glyph">{{ (scopeMode === 'services' ? form.serviceIds : form.staffIds).includes(item.id) ? '✓' : '-' }}</Text><View><Text class="step-title">{{ 'staffName' in item ? item.staffName : item.name }}</Text><Text class="step-copy">{{ 'roleLabel' in item ? `${item.roleLabel} · 可履约` : `${item.durationMin} 分钟 · 已启用` }}</Text></View><Text class="tag" :class="(scopeMode === 'services' ? form.serviceIds : form.staffIds).includes(item.id) ? '' : 'blue'">{{ (scopeMode === 'services' ? form.serviceIds : form.staffIds).includes(item.id) ? '已选' : '选择' }}</Text></View></View><View class="form-footer"><button class="button secondary" @tap="scopeMode = ''">取消</button><button class="button" @tap="scopeMode = ''">确认选择</button></View></View></View>
  </View>
</template>
