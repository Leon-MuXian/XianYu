<script setup lang="ts">
import Taro, { useLoad } from '@tarojs/taro'
import { reactive, ref } from 'vue'
import { createIdempotencyKey } from '@serenmeet/api-client'
import OwnerTopbar from '../../components/OwnerTopbar.vue'
import { api } from '../../api'
import { messageOf, type OnboardingDraft, type StoreProfileDraft, ownerRoutes } from '../../owner'

const form = reactive({ name: '', city: '', address: '', contactPhone: '', businessCategories: [] as string[], serviceTags: [] as string[] })
const categoryInput = ref('')
const tagInput = ref('')
const saving = ref(false)
const message = ref('')
const storeMode = ref(false)

async function load(params?: Record<string, string>) {
  storeMode.value = params?.mode === 'store'
  try {
    if (storeMode.value) {
      Object.assign(form, await api.request<StoreProfileDraft>('GET', '/owner/store'))
    } else {
      const draft = await api.request<OnboardingDraft>('GET', '/owner/onboarding/draft')
      Object.assign(form, draft.storeProfile || {})
    }
  } catch (error) { message.value = messageOf(error, '门店资料加载失败') }
}

function addValue(target: 'businessCategories' | 'serviceTags', input: string) {
  const values = input.split(/[、,，]/).map((value) => value.trim()).filter(Boolean)
  for (const value of values) if (!form[target].includes(value)) form[target].push(value)
  if (target === 'businessCategories') categoryInput.value = ''
  else tagInput.value = ''
}

function removeValue(target: 'businessCategories' | 'serviceTags', value: string) {
  form[target] = form[target].filter((item) => item !== value)
}

async function save() {
  if (!form.name || !form.address || !form.contactPhone || !form.businessCategories.length || !form.serviceTags.length) {
    message.value = '请完成所有标记为必填的门店资料'
    return
  }
  saving.value = true
  message.value = ''
  try {
    if (storeMode.value) {
      await api.request('PUT', '/owner/store', form, createIdempotencyKey('store-update'))
    } else {
      await api.request('PUT', '/owner/onboarding/store-profile', form)
    }
    await Taro.showToast({ title: '门店资料已保存', icon: 'success' })
    if (storeMode.value) await Taro.navigateBack()
    else await Taro.redirectTo({ url: ownerRoutes.onboarding })
  } catch (error) { message.value = messageOf(error, '门店资料保存失败') } finally { saving.value = false }
}

useLoad((params) => { void load(params) })
</script>

<template>
  <View class="screen">
    <OwnerTopbar title="门店资料" back />
    <View class="content ostore-flow">
      <View class="ostore-guide"><Text class="guide-title">{{ storeMode ? '正在修改门店资料' : '正在填写门店资料' }}</Text><Text class="guide-copy">{{ storeMode ? '保存后立即更新门店展示，并返回“我的”。' : '店长从开始使用清单进入，边填边保存草稿，保存后回到清单。' }}</Text></View>
      <View class="owner-form-grid">
        <View class="field wide"><Text>门店名称 <Text class="required-mark">必填</Text></Text><input v-model="form.name" class="field-input" placeholder="请输入门店名称" placeholder-class="field-placeholder" /></View>
        <View class="field wide"><Text>门店地址 <Text class="required-mark">必填</Text></Text><input v-model="form.address" class="field-input" placeholder="请输入详细地址" placeholder-class="field-placeholder" /></View>
        <View class="field wide"><Text>联系电话 <Text class="required-mark">必填</Text></Text><input v-model="form.contactPhone" class="field-input" type="text" placeholder="请输入联系电话" placeholder-class="field-placeholder" /></View>
      </View>
      <View class="ostore-card">
        <View class="card-title"><Text>经营项目</Text><Text class="required-mark">必填</Text></View>
        <View class="ostore-chip-set"><Text v-for="item in form.businessCategories" :key="item" @tap="removeValue('businessCategories', item)">{{ item }} ×</Text><input v-model="categoryInput" class="chip-input" confirm-type="done" placeholder="+ 添加" @confirm="addValue('businessCategories', categoryInput)" @blur="addValue('businessCategories', categoryInput)" /></View>
        <Text class="card-help">使用会员能理解的词描述门店服务方向，不限制后续服务项目名称。</Text>
      </View>
      <View class="ostore-card">
        <View class="card-title"><Text>服务标签</Text><Text class="required-mark">必填</Text></View>
        <View class="ostore-chip-set"><Text v-for="item in form.serviceTags" :key="item" @tap="removeValue('serviceTags', item)">{{ item }} ×</Text><input v-model="tagInput" class="chip-input" confirm-type="done" placeholder="+ 添加" @confirm="addValue('serviceTags', tagInput)" @blur="addValue('serviceTags', tagInput)" /></View>
        <Text class="card-help">标签由门店自行填写，只用于展示和筛选提示。</Text>
      </View>
      <View class="ostore-next-preview"><Text class="note-title">{{ storeMode ? '保存后更新门店资料' : '保存后返回清单' }}</Text><Text>{{ storeMode ? '门店名称、地址、电话和展示标签会立即生效。' : '营业时间和履约资源完成后，系统会生成创建门店前的完整展示预览。' }}</Text></View>
      <View v-if="message" class="error-banner">{{ message }}</View>
      <View class="form-footer"><button class="button secondary" @tap="Taro.navigateBack()">返回</button><button class="button" :loading="saving" :disabled="saving" @tap="save">{{ storeMode ? '保存修改' : '保存并返回清单' }}</button></View>
    </View>
  </View>
</template>
