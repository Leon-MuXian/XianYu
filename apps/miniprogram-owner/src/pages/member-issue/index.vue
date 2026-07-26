<script setup lang="ts">
import Taro, { useLoad } from '@tarojs/taro'
import { computed, reactive, ref } from 'vue'
import { createIdempotencyKey } from '@serenmeet/api-client'
import OwnerTopbar from '../../components/OwnerTopbar.vue'
import { api } from '../../api'
import { guardOwner, messageOf, type CardTemplate, type Member, ownerRoutes } from '../../owner'

interface InviteResult { code: string; expiresAt: string; status: string }
interface IssueResult { receivedAmountYuan: number; validUntil: string }

const templates = ref<CardTemplate[]>([])
const form = reactive({ name: '', memberNo: '', contactText: '', templateId: 0, receivedAmountYuan: 0, saleDate: new Date().toISOString().slice(0, 10), payMethodLabel: '门店已收' })
const createdMember = ref<Member | null>(null)
const issueResult = ref<IssueResult | null>(null)
const invite = ref<InviteResult | null>(null)
const loading = ref(false)
const message = ref('')
const selectedTemplate = computed(() => templates.value.find((item) => item.id === form.templateId))
const canSubmit = computed(() => form.name && form.memberNo && form.templateId && Number(form.receivedAmountYuan) >= 0)

async function load() {
  try {
    if (!await guardOwner()) return
    templates.value = (await api.request<CardTemplate[]>('GET', '/owner/card-templates')).filter((item) => item.status === 'active')
    if (templates.value.length) {
      form.templateId = templates.value[0].id
      form.receivedAmountYuan = templates.value[0].salePriceYuan
    }
  } catch (error) { message.value = messageOf(error, '发卡页面加载失败') }
}

function selectTemplate(event: { detail: { value: string } }) {
  const item = templates.value[Number(event.detail.value)]
  if (!item) return
  form.templateId = item.id
  form.receivedAmountYuan = item.salePriceYuan
}

async function submit() {
  if (!canSubmit.value || loading.value) { message.value = '请完成所有必填项，实收金额不能为负数'; return }
  loading.value = true
  message.value = ''
  try {
    if (!createdMember.value) createdMember.value = await api.request<Member>('POST', '/owner/members', { name: form.name, memberNo: form.memberNo, contactText: form.contactText }, createIdempotencyKey('member-create'))
    if (!issueResult.value) issueResult.value = await api.request<IssueResult>('POST', `/owner/members/${createdMember.value.id}/cards`, { templateId: form.templateId, receivedAmountYuan: Number(form.receivedAmountYuan), saleDate: form.saleDate, payMethodLabel: form.payMethodLabel }, createIdempotencyKey('member-issue-card'))
    invite.value = await api.request<InviteResult>('POST', `/owner/members/${createdMember.value.id}/invite-codes`, {}, createIdempotencyKey('member-invite'))
    await Taro.showToast({ title: '会员已添加并发卡', icon: 'success' })
  } catch (error) { message.value = messageOf(error, '添加会员或发卡失败，可重试当前步骤') } finally { loading.value = false }
}

async function regenerate() {
  if (!createdMember.value) return
  loading.value = true
  try {
    invite.value = await api.request<InviteResult>('POST', `/owner/members/${createdMember.value.id}/invite-codes`, {}, createIdempotencyKey('member-invite-regenerate'))
    await Taro.showToast({ title: '邀请码已更新', icon: 'success' })
  } catch (error) { message.value = messageOf(error, '邀请码生成失败') } finally { loading.value = false }
}

async function copyInvite() {
  if (!invite.value) return
  await Taro.setClipboardData({ data: invite.value.code })
  await Taro.showToast({ title: '邀请码已复制', icon: 'success' })
}

useLoad(load)
</script>

<template>
  <View class="screen">
    <OwnerTopbar :title="invite ? '发卡结果' : '添加会员'" back />
    <View class="content owner-dense">
      <template v-if="!invite">
        <View class="owner-page-head"><View><Text class="page-title">{{ form.name || '添加新会员' }}</Text><Text class="page-copy">录入会员档案，发放会员卡并在保存后生成邀请码。</Text></View></View>
        <View v-if="!templates.length" class="owner-list-card" @tap="Taro.redirectTo({ url: ownerRoutes.cards })"><Text class="list-card-title">请先创建会员卡</Text><Text class="list-card-copy">至少需要 1 张启用会员卡，才能给会员发卡。</Text><Text class="tag red">阻断</Text></View>
        <View class="owner-form-grid">
          <View class="field"><Text>会员姓名 <Text class="required-mark">必填</Text></Text><input v-model="form.name" class="field-input" placeholder="请输入姓名" /></View>
          <View class="field"><Text>会员编号 <Text class="required-mark">必填</Text></Text><input v-model="form.memberNo" class="field-input" placeholder="如 SM-0268" /></View>
          <View class="field wide"><Text>联系方式/备注</Text><input v-model="form.contactText" class="field-input" placeholder="只录入必要的联系备注" /></View>
          <picker class="field wide" mode="selector" :range="templates" range-key="name" @change="selectTemplate"><View><Text>发放会员卡 <Text class="required-mark">必填</Text></Text><Text class="field-value">{{ selectedTemplate?.name || '请选择会员卡' }} ▾</Text></View></picker>
          <View class="field"><Text>实收金额 <Text class="required-mark">必填</Text></Text><input v-model.number="form.receivedAmountYuan" class="field-input" type="digit" /></View>
          <View class="field"><Text>实收确认 <Text class="required-mark">必填</Text></Text><Text class="field-value">门店已收</Text></View>
        </View>
        <View class="owner-list-card"><Text class="list-card-title">保存后生成邀请码</Text><Text class="list-card-copy">邀请码 7 天有效，生成后交给会员在会员端绑定身份并查看卡包。</Text><Text class="tag blue">待生成</Text></View>
        <View v-if="message" class="error-banner">{{ message }}</View>
        <View class="member-issue-cta"><button class="button issue-button" :disabled="loading || !canSubmit || !templates.length" :loading="loading" @tap="submit">添加会员并生成邀请码</button></View>
      </template>
      <template v-else>
        <View class="member-pass" @tap="copyInvite"><Text class="pass-title">{{ createdMember?.name }} · {{ selectedTemplate?.name }}</Text><Text class="pass-value">{{ invite.code }}</Text><Text class="pass-copy">邀请码有效至 {{ invite.expiresAt.slice(0, 10) }}，点击复制后发给会员绑定。</Text></View>
        <View class="owner-status-strip two-col"><View class="owner-status"><Text>实收</Text><Text class="status-value">¥{{ issueResult?.receivedAmountYuan }}</Text><Text class="status-note">门店已收</Text></View><View class="owner-status"><Text>绑定</Text><Text class="status-value">待</Text><Text class="status-note gold-text">会员未绑定</Text></View></View>
        <Text class="section-title">接下来做什么</Text>
        <View class="owner-progress"><View class="owner-step" @tap="copyInvite"><Text class="step-glyph">1</Text><View><Text class="step-title">复制邀请码给会员</Text><Text class="step-copy">会员输入邀请码后会先核对门店，再把会员卡加入卡包。</Text></View><Text class="tag blue">复制</Text></View><View class="owner-step"><Text class="step-glyph">2</Text><View><Text class="step-title">提醒 7 天内绑定</Text><Text class="step-copy">过期后可以重新生成一个新的邀请码。</Text></View><Text class="tag warn">提醒</Text></View><View class="owner-step" @tap="Taro.redirectTo({ url: ownerRoutes.members + '?tab=cards' })"><Text class="step-glyph">3</Text><View><Text class="step-title">查看会员卡状态</Text><Text class="step-copy">会员绑定后，卡包会显示剩余次数和有效期。</Text></View><Text class="tag">查看</Text></View></View>
        <View v-if="message" class="error-banner">{{ message }}</View>
        <View class="form-footer"><button class="button secondary" :loading="loading" @tap="regenerate">重新生成</button><button class="button" @tap="Taro.redirectTo({ url: ownerRoutes.members + '?tab=cards' })">查看会员卡</button></View>
      </template>
    </View>
  </View>
</template>
