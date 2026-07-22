<script setup lang="ts">
import Taro, { useDidShow, useLoad } from '@tarojs/taro'
import { computed, ref } from 'vue'
import OwnerTabbar from '../../components/OwnerTabbar.vue'
import OwnerTopbar from '../../components/OwnerTopbar.vue'
import { api } from '../../api'
import { guardOwner, messageOf, navigate, type CardTemplate, type CardWarning, type Member, ownerRoutes } from '../../owner'

const members = ref<Member[]>([])
const templates = ref<CardTemplate[]>([])
const warnings = ref<CardWarning[]>([])
const tab = ref<'members' | 'cards' | 'invites' | 'warnings'>('members')
const message = ref('')
const unboundCount = computed(() => members.value.filter((item) => item.bindStatus !== 'bound').length)
const lowBalanceCount = computed(() => warnings.value.filter((item) => item.status === 'low_balance' || item.status === 'empty').length)

async function load() {
  try {
    if (!await guardOwner()) return
    const [memberRows, templateRows, warningRows] = await Promise.all([
      api.request<Member[]>('GET', '/owner/members'), api.request<CardTemplate[]>('GET', '/owner/card-templates'), api.request<CardWarning[]>('GET', '/owner/warnings/cards')
    ])
    members.value = memberRows
    templates.value = templateRows
    warnings.value = warningRows
  } catch (error) { message.value = messageOf(error, '会员数据加载失败') }
}

useLoad((params) => {
  if (params.tab === 'cards') tab.value = 'cards'
  if (params.tab === 'warnings') tab.value = 'warnings'
})
useDidShow(load)
</script>

<template>
  <View class="screen">
    <OwnerTopbar title="会员" />
    <View class="content owner-dense">
      <View class="owner-status-strip"><View class="owner-status"><Text>会员</Text><Text class="status-value">{{ members.length }}</Text><Text class="status-note">门店录入</Text></View><View class="owner-status"><Text>待绑定</Text><Text class="status-value">{{ unboundCount }}</Text><Text class="status-note gold-text">邀请码</Text></View><View class="owner-status"><Text>低余额</Text><Text class="status-value">{{ lowBalanceCount }}</Text><Text class="status-note coral-text">需跟进</Text></View></View>
      <View class="owner-tabs"><Text :class="{ active: tab === 'members' }" @tap="tab = 'members'">会员</Text><Text :class="{ active: tab === 'cards' || tab === 'warnings' }" @tap="tab = 'cards'">会员卡</Text><Text :class="{ active: tab === 'invites' }" @tap="tab = 'invites'">邀请码</Text></View>
      <View v-if="message" class="error-banner">{{ message }}</View>
      <template v-if="tab === 'members'">
        <View v-if="!members.length" class="empty-state" @tap="navigate(ownerRoutes.memberIssue)"><Text class="empty-title">还没有会员</Text><Text>添加会员、记录线下实收并生成邀请码。</Text></View>
        <View v-for="member in members" :key="member.id" class="owner-list-card"><Text class="list-card-title">{{ member.name }} · {{ member.memberNo }}</Text><Text class="list-card-copy">{{ member.contactText || '无联系备注' }} · {{ member.bindStatus === 'bound' ? '会员已绑定' : '等待会员绑定' }}</Text><Text class="tag" :class="member.bindStatus === 'bound' ? '' : 'warn'">{{ member.bindStatus === 'bound' ? '已绑定' : '待绑定' }}</Text></View>
      </template>
      <template v-else-if="tab === 'cards'">
        <View v-for="item in templates" :key="item.id" class="owner-list-card"><Text class="list-card-title">{{ item.name }}</Text><Text class="list-card-copy">¥{{ item.salePriceYuan }} · {{ item.cardType === 'count' ? `${item.totalCount} 次` : `${item.validDays} 天有效` }} · {{ item.status === 'active' ? '可发卡' : '已停用' }}</Text><Text class="tag" :class="item.cardType === 'count' ? '' : 'blue'">{{ item.cardType === 'count' ? '次数卡' : '期限卡' }}</Text></View>
        <View v-for="warning in warnings" :key="warning.id" class="owner-list-card"><Text class="list-card-title">{{ warning.memberName }} · {{ warning.cardName }}</Text><Text class="list-card-copy">{{ warning.remainCount == null ? `有效期至 ${warning.validUntil}` : `剩余 ${warning.remainCount} 次` }}</Text><Text class="tag" :class="warning.status === 'expired' || warning.status === 'empty' ? 'red' : 'warn'">{{ warning.status === 'low_balance' ? '低余额' : warning.status === 'expiring' ? '到期预警' : warning.status === 'expired' ? '已过期' : warning.status === 'empty' ? '已用完' : warning.status }}</Text></View>
      </template>
      <template v-else-if="tab === 'invites'">
        <View v-for="member in members.filter((item) => item.bindStatus !== 'bound')" :key="member.id" class="owner-list-card"><Text class="list-card-title">{{ member.name }} · 等待绑定</Text><Text class="list-card-copy">邀请码只在发卡成功页展示明文；过期后可从发卡流程重新生成。</Text><Text class="tag warn">待绑定</Text></View>
      </template>
      <template v-else>
        <View v-for="warning in warnings" :key="warning.id" class="owner-list-card"><Text class="list-card-title">{{ warning.memberName }} · {{ warning.cardName }}</Text><Text class="list-card-copy">{{ warning.remainCount == null ? `有效期至 ${warning.validUntil}` : `剩余 ${warning.remainCount} 次` }}</Text><Text class="tag warn">需跟进</Text></View>
      </template>
      <View class="owner-list-card" @tap="navigate(ownerRoutes.memberIssue)"><Text class="list-card-title">添加会员并发卡</Text><Text class="list-card-copy">录入会员档案、选择会员卡并生成邀请码；绑定前门店仍可查看权益台账。</Text><Text class="tag blue">发卡</Text></View>
    </View>
    <OwnerTabbar active="members" />
  </View>
</template>
