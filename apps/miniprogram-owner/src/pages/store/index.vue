<script setup lang="ts">
import Taro, { useLoad } from '@tarojs/taro'
import { computed, reactive, ref } from 'vue'
import { createIdempotencyKey, SerenApiError } from '@serenmeet/api-client'
import OwnerTopbar from '../../components/OwnerTopbar.vue'
import { api } from '../../api'
import { messageOf, type OnboardingDraft, type RegionOption, type StoreProfileDraft, ownerRoutes } from '../../owner'

type NameStatus = 'idle' | 'checking' | 'available' | 'taken'

const form = reactive({
  name: '',
  cityCode: '',
  city: '',
  districtCode: '',
  district: '',
  detailAddress: '',
  address: '',
  contactPhone: '',
  serviceScopes: [] as string[]
})
const addressDraft = reactive({ cityCode: '', districtCode: '', detailAddress: '' })
const serviceScopeInput = ref('')
const cities = ref<RegionOption[]>([])
const districts = ref<RegionOption[]>([])
const addressSheetOpen = ref(false)
const regionLoading = ref(false)
const nameStatus = ref<NameStatus>('idle')
const saving = ref(false)
const message = ref('')
const storeMode = ref(false)
let nameCheckSequence = 0

const addressSummary = computed(() => form.address || `${form.city}${form.district}${form.detailAddress}`)
const nameStatusText = computed(() => ({
  idle: '',
  checking: '正在检查',
  available: '名称可用',
  taken: '名称已被使用'
})[nameStatus.value])

async function loadCities() {
  if (cities.value.length) return
  cities.value = await api.request<RegionOption[]>('GET', '/owner/regions/cities')
}

async function loadDistricts(cityCode: string) {
  districts.value = cityCode
    ? await api.request<RegionOption[]>('GET', `/owner/regions/cities/${cityCode}/districts`)
    : []
}

async function load(params?: Record<string, string>) {
  storeMode.value = params?.mode === 'store'
  try {
    let profile: StoreProfileDraft
    if (storeMode.value) {
      profile = await api.request<StoreProfileDraft>('GET', '/owner/store')
    } else {
      const draft = await api.request<OnboardingDraft>('GET', '/owner/onboarding/draft')
      profile = draft.storeProfile || {}
    }
    Object.assign(form, profile)
    await loadCities()
    if (form.cityCode) await loadDistricts(form.cityCode)
  } catch (error) { message.value = messageOf(error, '门店资料加载失败') }
}

function resetNameStatus() {
  nameCheckSequence += 1
  nameStatus.value = 'idle'
}

async function checkStoreName() {
  const name = form.name.trim()
  if (!name) {
    nameStatus.value = 'idle'
    return false
  }
  const sequence = ++nameCheckSequence
  nameStatus.value = 'checking'
  try {
    const result = await api.request<{ available: boolean }>('POST', '/owner/store/name-availability', { name })
    if (sequence !== nameCheckSequence) return true
    nameStatus.value = result.available ? 'available' : 'taken'
    return result.available
  } catch {
    if (sequence === nameCheckSequence) nameStatus.value = 'idle'
    return true
  }
}

async function openAddressSheet() {
  message.value = ''
  regionLoading.value = true
  try {
    await loadCities()
    Object.assign(addressDraft, {
      cityCode: form.cityCode,
      districtCode: form.districtCode,
      detailAddress: form.detailAddress
    })
    await loadDistricts(addressDraft.cityCode)
    addressSheetOpen.value = true
  } catch (error) {
    message.value = messageOf(error, '行政区划加载失败')
  } finally {
    regionLoading.value = false
  }
}

async function selectCity(event: { detail: { value: string | number } }) {
  const city = cities.value[Number(event.detail.value)]
  if (!city || city.code === addressDraft.cityCode) return
  addressDraft.cityCode = city.code
  addressDraft.districtCode = ''
  regionLoading.value = true
  try {
    await loadDistricts(city.code)
  } catch (error) {
    message.value = messageOf(error, '区县列表加载失败')
  } finally {
    regionLoading.value = false
  }
}

function selectDistrict(event: { detail: { value: string | number } }) {
  const district = districts.value[Number(event.detail.value)]
  if (district) addressDraft.districtCode = district.code
}

function confirmAddress() {
  const city = cities.value.find((item) => item.code === addressDraft.cityCode)
  const district = districts.value.find((item) => item.code === addressDraft.districtCode)
  if (!city) {
    message.value = '请选择城市'
    return
  }
  if (!district) {
    message.value = '请选择区/县'
    return
  }
  if (!addressDraft.detailAddress.trim()) {
    message.value = '请填写详细地址'
    return
  }
  Object.assign(form, {
    cityCode: city.code,
    city: city.name,
    districtCode: district.code,
    district: district.name,
    detailAddress: addressDraft.detailAddress.trim(),
    address: `${city.name}${district.name}${addressDraft.detailAddress.trim()}`
  })
  message.value = ''
  addressSheetOpen.value = false
}

function addServiceScopes(input: string) {
  const values = input.split(/[、,，]/).map((value) => value.trim()).filter(Boolean)
  for (const value of values) {
    if (form.serviceScopes.includes(value)) continue
    if (form.serviceScopes.length >= 6) {
      message.value = '服务范围最多填写 6 项'
      break
    }
    form.serviceScopes.push(value)
  }
  serviceScopeInput.value = ''
}

function removeServiceScope(value: string) {
  form.serviceScopes = form.serviceScopes.filter((item) => item !== value)
}

async function save() {
  if (!form.name || !form.cityCode || !form.districtCode || !form.detailAddress || !form.contactPhone || !form.serviceScopes.length) {
    message.value = '请完成所有标记为必填的门店资料'
    return
  }
  if (!(await checkStoreName())) {
    message.value = '该门店名称已被使用，请更换名称'
    return
  }
  saving.value = true
  message.value = ''
  try {
    const payload = {
      name: form.name,
      cityCode: form.cityCode,
      districtCode: form.districtCode,
      detailAddress: form.detailAddress,
      contactPhone: form.contactPhone,
      serviceScopes: form.serviceScopes
    }
    if (storeMode.value) {
      await api.request('PUT', '/owner/store', payload, createIdempotencyKey('store-update'))
    } else {
      await api.request('PUT', '/owner/onboarding/store-profile', payload)
    }
    await Taro.showToast({ title: '门店资料已保存', icon: 'success' })
    if (storeMode.value) await Taro.navigateBack()
    else await Taro.redirectTo({ url: ownerRoutes.onboarding })
  } catch (error) {
    if (error instanceof SerenApiError && error.code === 'STORE_NAME_TAKEN') nameStatus.value = 'taken'
    message.value = messageOf(error, '门店资料保存失败')
  } finally { saving.value = false }
}

useLoad((params) => { void load(params) })
</script>

<template>
  <View class="screen">
    <OwnerTopbar title="门店资料" back />
    <View class="content ostore-flow">
      <View class="owner-form-grid ostore-profile-fields">
        <View class="field wide ostore-name-field"><View class="ostore-field-head"><Text class="field-label">门店名称 <Text class="required-mark">必填</Text></Text><Text v-if="nameStatusText" class="ostore-name-status" :class="nameStatus">{{ nameStatusText }}</Text></View><input v-model="form.name" class="field-input" maxlength="120" placeholder="请输入门店名称" placeholder-class="field-placeholder" @input="resetNameStatus" @blur="checkStoreName" /><Text v-if="nameStatus === 'taken'" class="field-error">该名称已被其他门店使用</Text></View>
        <View class="ostore-address-card" @tap="openAddressSheet"><Text class="ostore-address-icon">⌖</Text><View class="ostore-address-copy"><Text class="field-label">门店地址 <Text class="required-mark">必填</Text></Text><Text class="ostore-address-value" :class="{ placeholder: !addressSummary }">{{ addressSummary || '选择城市、区县并填写详细地址' }}</Text></View><Text class="ostore-address-action">{{ regionLoading ? '加载中' : addressSummary ? '修改' : '填写' }}</Text></View>
        <View class="field wide"><Text>联系电话 <Text class="required-mark">必填</Text></Text><input v-model="form.contactPhone" class="field-input" type="text" placeholder="请输入联系电话" placeholder-class="field-placeholder" /></View>
      </View>
      <View class="ostore-card">
        <View class="card-title"><Text>服务范围</Text><Text class="required-mark">必填</Text></View>
        <View class="ostore-chip-set"><Text v-for="item in form.serviceScopes" :key="item" @tap="removeServiceScope(item)">{{ item }} ×</Text><input v-model="serviceScopeInput" class="chip-input" confirm-type="done" placeholder="+ 添加服务范围" @confirm="addServiceScopes(serviceScopeInput)" @blur="addServiceScopes(serviceScopeInput)" /></View>
      </View>
      <View v-if="message" class="error-banner">{{ message }}</View>
      <View class="form-footer"><button class="button secondary" @tap="Taro.navigateBack()">返回</button><button class="button" :loading="saving" :disabled="saving" @tap="save">{{ storeMode ? '保存修改' : '保存并返回清单' }}</button></View>
    </View>
    <View v-if="addressSheetOpen" class="modal-backdrop" @tap.self="addressSheetOpen = false">
      <View class="sheet ostore-address-sheet">
        <View class="sheet-grip" />
        <View class="sheet-title"><View><Text class="sheet-heading">填写门店地址</Text></View><Text class="ostore-sheet-close" aria-label="关闭" @tap="addressSheetOpen = false">×</Text></View>
        <View class="ostore-address-levels">
          <picker mode="selector" :range="cities" range-key="name" @change="selectCity"><View class="ostore-address-level"><Text>城市 <Text class="required-mark">必填</Text></Text><Text :class="{ placeholder: !addressDraft.cityCode }">{{ cities.find((item) => item.code === addressDraft.cityCode)?.name || '请选择城市' }}</Text><Text>›</Text></View></picker>
          <picker mode="selector" :disabled="!addressDraft.cityCode || regionLoading" :range="districts" range-key="name" @change="selectDistrict"><View class="ostore-address-level"><Text>区 / 县 <Text class="required-mark">必填</Text></Text><Text :class="{ placeholder: !addressDraft.districtCode }">{{ districts.find((item) => item.code === addressDraft.districtCode)?.name || (addressDraft.cityCode ? '请选择区县' : '请先选择城市') }}</Text><Text>›</Text></View></picker>
        </View>
        <View class="ostore-address-detail"><Text class="field-label">详细地址 <Text class="required-mark">必填</Text></Text><textarea v-model="addressDraft.detailAddress" class="ostore-address-textarea" maxlength="180" auto-height placeholder="街道、门牌号、楼栋、楼层或房间号" placeholder-class="field-placeholder" /></View>
        <View v-if="message" class="error-banner">{{ message }}</View>
        <View class="form-footer"><button class="button secondary" @tap="addressSheetOpen = false">取消</button><button class="button" :disabled="regionLoading" @tap="confirmAddress">确认地址</button></View>
      </View>
    </View>
  </View>
</template>
