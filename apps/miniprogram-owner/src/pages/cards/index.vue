<script setup lang="ts">
import Taro, { useDidShow, useLoad } from '@tarojs/taro'
import { computed, nextTick, reactive, ref } from 'vue'
import { createIdempotencyKey } from '@serenmeet/api-client'
import OwnerTopbar from '../../components/OwnerTopbar.vue'
import { api } from '../../api'
import {
  CARD_TEMPLATE_NAME_TAKEN_MESSAGE,
  formatCardSelectionSummary,
  hasDuplicateCardTemplateName,
  isCardTemplateNameTaken,
  normalizeCardTemplateName
} from '../../card-form'
import { guardOwner, messageOf, type CardTemplate, type Service, type Staff } from '../../owner'

type CardFilter = 'all' | 'count' | 'period'
type CardType = 'count' | 'period'

const templates = ref<CardTemplate[]>([])
const services = ref<Service[]>([])
const staff = ref<Staff[]>([])
const cardType = ref<CardType>('count')
const cardFilter = ref<CardFilter>('all')
const scopeMode = ref<'services' | 'staff' | ''>('')
const showTypePicker = ref(false)
const showForm = ref(false)
const loading = ref(false)
const message = ref('')
const cardNameError = ref('')
const editorScrollTop = ref(0)
let editorScrollPosition = 0
let scopeReturnScrollTop = 0

const form = reactive({
  id: 0,
  name: '',
  salePriceYuan: 0,
  totalCount: 20,
  validDays: 365,
  lowBalanceThreshold: 2,
  serviceIds: [] as number[],
  staffIds: [] as number[]
})

const editing = computed(() => Boolean(form.id))
const countTotal = computed(() => templates.value.filter((item) => item.cardType === 'count').length)
const periodTotal = computed(() => templates.value.filter((item) => item.cardType === 'period').length)
const filteredTemplates = computed(() => cardFilter.value === 'all'
  ? templates.value
  : templates.value.filter((item) => item.cardType === cardFilter.value))
const selectedServiceNames = computed(() => services.value
  .filter((item) => form.serviceIds.includes(item.id))
  .map((item) => item.name))
const selectedStaffNames = computed(() => staff.value
  .filter((item) => form.staffIds.includes(item.id))
  .map((item) => item.staffName))
const selectedServiceSummary = computed(() => formatCardSelectionSummary(
  selectedServiceNames.value,
  '服务',
  '请选择至少 1 个启用服务'
))
const selectedStaffSummary = computed(() => formatCardSelectionSummary(
  selectedStaffNames.value,
  '员工',
  '请选择至少 1 个启用员工'
))
const canSave = computed(() => Boolean(
  normalizeCardTemplateName(form.name)
  && Number(form.salePriceYuan) >= 0
  && form.validDays > 0
  && form.serviceIds.length > 0
  && form.staffIds.length > 0
  && (cardType.value === 'period' || (form.totalCount > 0 && form.lowBalanceThreshold >= 0))
))

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
  } catch (error) {
    message.value = messageOf(error, '会员卡加载失败')
  }
}

function resetForm(type: CardType = 'count') {
  cardType.value = type
  Object.assign(form, {
    id: 0,
    name: '',
    salePriceYuan: 0,
    totalCount: 20,
    validDays: type === 'count' ? 365 : 30,
    lowBalanceThreshold: 2,
    serviceIds: [],
    staffIds: []
  })
  cardNameError.value = ''
  message.value = ''
  scopeMode.value = ''
  editorScrollTop.value = 0
  editorScrollPosition = 0
  scopeReturnScrollTop = 0
}

function openTypePicker() {
  if (!services.value.length) {
    message.value = '请先创建并启用至少 1 个服务项目'
    return
  }
  message.value = ''
  showTypePicker.value = true
}

function begin(type: CardType) {
  resetForm(type)
  showTypePicker.value = false
  showForm.value = true
}

function closeForm() {
  resetForm()
  showForm.value = false
}

function edit(item: CardTemplate) {
  resetForm(item.cardType)
  Object.assign(form, {
    id: item.id,
    name: item.name,
    salePriceYuan: item.salePriceYuan,
    totalCount: item.totalCount || 20,
    validDays: item.validDays,
    lowBalanceThreshold: item.lowBalanceThreshold ?? 2,
    serviceIds: item.serviceIds || [],
    staffIds: item.staffIds || []
  })
  showForm.value = true
}

function clearCardNameError() {
  cardNameError.value = ''
  if (message.value === CARD_TEMPLATE_NAME_TAKEN_MESSAGE) message.value = ''
}

function rememberEditorScroll(event: { detail: { scrollTop: number } }) {
  if (!scopeMode.value) editorScrollPosition = event.detail.scrollTop
}

function openScope(mode: 'services' | 'staff') {
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

function toggleId(target: 'serviceIds' | 'staffIds', id: number) {
  const values = form[target]
  form[target] = values.includes(id) ? values.filter((value) => value !== id) : [...values, id]
}

async function save() {
  const normalizedName = normalizeCardTemplateName(form.name)
  form.name = normalizedName
  cardNameError.value = ''
  if (!normalizedName) {
    cardNameError.value = '请填写会员卡名称'
    message.value = cardNameError.value
    return
  }
  if (hasDuplicateCardTemplateName(templates.value, normalizedName, form.id || undefined)) {
    cardNameError.value = CARD_TEMPLATE_NAME_TAKEN_MESSAGE
    message.value = CARD_TEMPLATE_NAME_TAKEN_MESSAGE
    return
  }
  if (!canSave.value) {
    message.value = '请完成所有必填项，售价不能为负数'
    return
  }

  loading.value = true
  message.value = ''
  const payload = {
    name: normalizedName,
    cardType: cardType.value,
    salePriceYuan: Number(form.salePriceYuan),
    totalCount: cardType.value === 'count' ? Number(form.totalCount) : null,
    validDays: Number(form.validDays),
    lowBalanceThreshold: cardType.value === 'count' ? Number(form.lowBalanceThreshold) : null,
    serviceIds: form.serviceIds,
    staffIds: form.staffIds
  }
  try {
    if (editing.value) {
      await api.request(
        'PUT',
        `/owner/card-templates/${form.id}`,
        payload,
        createIdempotencyKey('card-update')
      )
    } else {
      await api.request('POST', '/owner/card-templates', payload, createIdempotencyKey('card-create'))
    }
    await Taro.showToast({ title: editing.value ? '修改已保存' : '会员卡已创建', icon: 'success' })
    closeForm()
    await load()
  } catch (error) {
    if (isCardTemplateNameTaken(error)) {
      cardNameError.value = CARD_TEMPLATE_NAME_TAKEN_MESSAGE
      message.value = CARD_TEMPLATE_NAME_TAKEN_MESSAGE
    } else {
      message.value = messageOf(error, '会员卡保存失败')
    }
  } finally {
    loading.value = false
  }
}

useLoad((params) => {
  if (params.type === 'count' || params.type === 'period') begin(params.type)
})
useDidShow(load)
</script>

<template>
  <View class="screen card-screen">
    <OwnerTopbar
      :title="showForm ? (editing ? '修改会员卡' : '新增会员卡') : '会员卡管理'"
      back
      :intercept-back="showForm"
      @back="closeForm"
    />
    <scroll-view
      v-if="!showForm"
      :scroll-y="true"
      :enhanced="true"
      :show-scrollbar="false"
      class="card-page-scroll"
    >
      <View class="content owner-dense card-page">
        <View class="owner-page-head">
          <View class="owner-page-head-copy">
            <Text class="page-title">会员卡</Text>
            <Text class="page-copy">统一维护门店可发放的次数卡和期限卡。</Text>
          </View>
          <button
            class="owner-add-action"
            aria-label="新增会员卡"
            hover-class="owner-add-action-pressed"
            @tap="openTypePicker"
          ><View class="owner-add-action-icon" /></button>
        </View>

        <View class="owner-status-strip card-status-strip">
          <View class="owner-status"><Text>会员卡</Text><Text class="status-value">{{ templates.length }}</Text><Text class="status-note">全部</Text></View>
          <View class="owner-status"><Text>次数卡</Text><Text class="status-value">{{ countTotal }}</Text><Text class="status-note">按次核销</Text></View>
          <View class="owner-status"><Text>期限卡</Text><Text class="status-value">{{ periodTotal }}</Text><Text class="status-note blue-text">按期使用</Text></View>
        </View>

        <View class="owner-tabs card-tabs">
          <Text :class="{ active: cardFilter === 'all' }" @tap="cardFilter = 'all'">全部</Text>
          <Text :class="{ active: cardFilter === 'count' }" @tap="cardFilter = 'count'">次数卡</Text>
          <Text :class="{ active: cardFilter === 'period' }" @tap="cardFilter = 'period'">期限卡</Text>
        </View>

        <View v-if="filteredTemplates.length" class="card-template-list">
          <View
            v-for="item in filteredTemplates"
            :key="item.id"
            class="card-template-row"
            hover-class="card-template-row-pressed"
            role="button"
            :aria-label="`${item.name}，${item.cardType === 'count' ? '次数卡' : '期限卡'}，点击修改`"
            @tap="edit(item)"
          >
            <View class="card-template-main">
              <View class="card-template-head">
                <Text class="card-template-name">{{ item.name }}</Text>
                <Text class="tag" :class="{ blue: item.cardType === 'period' }">{{ item.cardType === 'count' ? '次数卡' : '期限卡' }}</Text>
              </View>
              <Text class="card-template-meta">{{ item.cardType === 'count' ? `${item.totalCount} 次 · ${item.validDays} 天有效 · 低于 ${item.lowBalanceThreshold} 次提醒` : `${item.validDays} 天有效 · 有效期内不限次` }}</Text>
              <Text class="card-template-scope">适用 {{ item.serviceIds?.length || 0 }} 个服务 · {{ item.staffIds?.length || 0 }} 名员工</Text>
            </View>
            <View class="card-template-price" :class="{ period: item.cardType === 'period' }">
              <Text class="card-template-price-value">¥{{ item.salePriceYuan }}</Text>
              <Text class="card-template-price-label">售价</Text>
            </View>
          </View>
        </View>
        <View v-else class="empty-state card-empty">
          <Text class="empty-title">{{ templates.length ? '没有符合筛选的会员卡' : '还没有会员卡' }}</Text>
          <Text>{{ templates.length ? '切换筛选查看其他会员卡。' : '新增首张会员卡后，就可以给会员发卡。' }}</Text>
          <button v-if="!templates.length && services.length" class="button card-empty-action" @tap="openTypePicker">新增会员卡</button>
        </View>
        <View v-if="message && !showForm" class="error-banner">{{ message }}</View>
      </View>
    </scroll-view>

    <scroll-view
      v-else
      :scroll-y="true"
      :enhanced="true"
      :show-scrollbar="false"
      :scroll-top="editorScrollTop"
      class="card-page-scroll card-form-page-scroll"
      @scroll="rememberEditorScroll"
    >
      <View class="content owner-dense card-form-page">
        <View class="member-pass">
          <Text class="pass-title">{{ form.name || (cardType === 'count' ? '评估训练次数卡' : '月度期限卡') }}</Text>
          <Text class="pass-value">¥{{ form.salePriceYuan }}</Text>
          <Text class="pass-copy">{{ cardType === 'count' ? '会员每完成一次服务，员工核销后扣减次数。' : '会员在有效期内预约适用服务，不显示剩余次数。' }}</Text>
        </View>
        <View class="owner-form-grid card-page-form-grid">
          <View class="field wide" :class="{ 'service-field-invalid': cardNameError }">
            <Text>会员卡名称 <Text class="required-mark">必填</Text></Text>
            <input
              v-model="form.name"
              class="field-input"
              maxlength="120"
              placeholder="如：评估训练 20 次卡"
              @input="clearCardNameError"
            />
            <Text v-if="cardNameError" class="field-error">{{ cardNameError }}</Text>
          </View>
          <View v-if="editing" class="field wide card-type-display">
            <Text>会员卡类型</Text><Text class="field-value">{{ cardType === 'count' ? '次数卡' : '期限卡' }}</Text>
          </View>
          <View class="field card-number-field"><Text>售价 <Text class="required-mark">必填</Text></Text><input v-model.number="form.salePriceYuan" class="field-input" type="digit" /><Text class="card-number-unit">元</Text></View>
          <View v-if="cardType === 'count'" class="field card-number-field"><Text>总次数 <Text class="required-mark">必填</Text></Text><input v-model.number="form.totalCount" class="field-input" type="number" /><Text class="card-number-unit">次</Text></View>
          <View class="field card-number-field"><Text>有效期 <Text class="required-mark">必填</Text></Text><input v-model.number="form.validDays" class="field-input" type="number" /><Text class="card-number-unit">天</Text></View>
          <View v-if="cardType === 'count'" class="field card-number-field"><Text>低余额提醒 <Text class="required-mark">必填</Text></Text><input v-model.number="form.lowBalanceThreshold" class="field-input" type="number" /><Text class="card-number-unit">次</Text></View>
          <View class="field wide selector-field service-selector" @tap="openScope('services')">
            <Text>适用服务 <Text class="required-mark">必填</Text></Text>
            <View class="service-selector-value"><Text class="service-selection-summary">{{ selectedServiceSummary }}</Text><Text class="service-selector-arrow">⌄</Text></View>
          </View>
          <View class="field wide selector-field service-selector" @tap="openScope('staff')">
            <Text>适用员工 <Text class="required-mark">必填</Text></Text>
            <View class="service-selector-value"><Text class="service-selection-summary">{{ selectedStaffSummary }}</Text><Text class="service-selector-arrow">⌄</Text></View>
          </View>
        </View>
        <View v-if="message" class="error-banner">{{ message }}</View>
        <View class="form-footer card-page-form-footer">
          <button class="button secondary" :disabled="loading" @tap="closeForm">返回列表</button>
          <button class="button" :disabled="loading || !canSave" :loading="loading" @tap="save">{{ editing ? '保存修改' : '保存会员卡' }}</button>
        </View>
      </View>
    </scroll-view>

    <View v-if="showTypePicker" class="modal-backdrop card-type-backdrop" @tap.self="showTypePicker = false">
      <View class="sheet card-type-sheet">
        <View class="sheet-grip" />
        <View class="sheet-title">
          <View><Text class="sheet-heading">新增会员卡</Text><Text class="sheet-copy">选择权益计算方式，下一步填写会员卡信息。</Text></View>
        </View>
        <View class="card-type-options">
          <button class="card-type-option" hover-class="card-type-option-pressed" @tap="begin('count')">
            <Text class="card-type-glyph">次</Text><View><Text class="card-type-title">次数卡</Text><Text class="card-type-copy">按总次数核销，支持低余额提醒</Text></View><Text class="card-type-arrow">›</Text>
          </button>
          <button class="card-type-option period" hover-class="card-type-option-pressed" @tap="begin('period')">
            <Text class="card-type-glyph">期</Text><View><Text class="card-type-title">期限卡</Text><Text class="card-type-copy">在有效期内使用，不显示剩余次数</Text></View><Text class="card-type-arrow">›</Text>
          </button>
        </View>
        <View class="form-footer single card-type-footer"><button class="button secondary" @tap="showTypePicker = false">取消</button></View>
      </View>
    </View>

    <View v-if="scopeMode" class="modal-backdrop card-scope-backdrop" @tap.self="closeScope">
      <View class="sheet service-scope-sheet card-scope-sheet">
        <View class="sheet-grip" />
        <View class="sheet-title"><View><Text class="sheet-heading">{{ scopeMode === 'services' ? '选择适用服务' : '选择适用员工' }}</Text><Text class="sheet-copy">支持多选，停用项不会出现在这里。</Text></View></View>
        <View class="owner-progress">
          <View
            v-for="item in scopeMode === 'services' ? services : staff"
            :key="item.id"
            class="owner-step"
            @tap="toggleId(scopeMode === 'services' ? 'serviceIds' : 'staffIds', item.id)"
          >
            <Text class="step-glyph">{{ (scopeMode === 'services' ? form.serviceIds : form.staffIds).includes(item.id) ? '✓' : '-' }}</Text>
            <View><Text class="step-title">{{ 'staffName' in item ? item.staffName : item.name }}</Text><Text class="step-copy">{{ 'roleLabel' in item ? `${item.roleLabel} · 可履约` : `${item.durationMin} 分钟 · 已启用` }}</Text></View>
            <Text class="tag" :class="(scopeMode === 'services' ? form.serviceIds : form.staffIds).includes(item.id) ? '' : 'blue'">{{ (scopeMode === 'services' ? form.serviceIds : form.staffIds).includes(item.id) ? '已选' : '选择' }}</Text>
          </View>
        </View>
        <View class="form-footer"><button class="button secondary" @tap="closeScope">取消</button><button class="button" @tap="closeScope">确认选择</button></View>
      </View>
    </View>
  </View>
</template>
