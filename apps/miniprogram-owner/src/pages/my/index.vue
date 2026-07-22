<script setup lang="ts">
import { useDidShow } from '@tarojs/taro'
import { computed, ref } from 'vue'
import OwnerTabbar from '../../components/OwnerTabbar.vue'
import OwnerTopbar from '../../components/OwnerTopbar.vue'
import { api } from '../../api'
import { formatDate, guardOwner, messageOf, navigate, type OwnerProfile, type Resource, ownerRoutes, remainingDays } from '../../owner'

interface StoreDetail { name?: string; address?: string; contactPhone?: string; businessHoursSummary?: string; businessCategories?: string[]; serviceTags?: string[] }
const profile = ref<OwnerProfile | null>(null)
const store = ref<StoreDetail>({})
const resources = ref<Resource[]>([])
const message = ref('')
const days = computed(() => remainingDays(profile.value?.trialEndAt))

async function load() {
  try {
    profile.value = await guardOwner()
    if (!profile.value) return
    const [storeDetail, resourceRows] = await Promise.all([
      api.request<StoreDetail>('GET', '/owner/store'), api.request<Resource[]>('GET', '/owner/resources')
    ])
    store.value = storeDetail
    resources.value = resourceRows
  } catch (error) { message.value = messageOf(error, '门店资料加载失败') }
}

useDidShow(load)
</script>

<template>
  <View class="screen">
    <OwnerTopbar title="我的" />
    <View class="content owner-dense">
      <View class="profile-card detail"><View class="profile-head"><Text class="avatar">店</Text><Text class="profile-title">{{ store.name || profile?.name || '门店' }}</Text><Text class="profile-copy">店长账号 · 微信已登录</Text><Text class="tag">店长</Text></View><View class="profile-detail-grid"><View><Text>门店地址</Text><Text class="profile-value">{{ store.address || profile?.address || '--' }}</Text></View><View><Text>联系电话</Text><Text class="profile-value">{{ store.contactPhone || profile?.contactPhone || '--' }}</Text></View><View><Text>营业时间</Text><Text class="profile-value">{{ store.businessHoursSummary || '查看营业时间设置' }}</Text></View><View><Text>履约资源</Text><Text class="profile-value">{{ resources.filter((item) => item.enabled !== false).length }} 个启用资源</Text></View></View><View class="profile-detail-tags"><Text v-for="tag in [...(store.businessCategories || []), ...(store.serviceTags || [])]" :key="tag">{{ tag }}</Text></View></View>
      <Text class="section-title">使用状态</Text><View class="owner-status-strip two-col"><View class="owner-status"><Text>剩余天数</Text><Text class="status-value">{{ days }}</Text><Text class="status-note gold-text">天</Text></View><View class="owner-status"><Text>到期日</Text><Text class="status-value">{{ formatDate(profile?.trialEndAt) }}</Text><Text class="status-note blue-text">{{ profile?.trialEndAt?.slice(0, 4) }}</Text></View></View>
      <Text class="section-title">门店设置</Text><View class="owner-progress"><View class="owner-step" @tap="navigate(ownerRoutes.store + '?mode=store')"><Text class="step-glyph">店</Text><View><Text class="step-title">门店资料</Text><Text class="step-copy">经营项目、服务标签、地址、电话</Text></View><Text class="tag blue">管理</Text></View><View class="owner-step" @tap="navigate(ownerRoutes.hours + '?mode=store')"><Text class="step-glyph">时</Text><View><Text class="step-title">营业时间</Text><Text class="step-copy">按星期设置营业和休息</Text></View><Text class="tag blue">设置</Text></View><View class="owner-step" @tap="navigate(ownerRoutes.resources)"><Text class="step-glyph">资</Text><View><Text class="step-title">履约资源</Text><Text class="step-copy">房间、床位、设备或工位</Text></View><Text class="tag blue">管理</Text></View></View>
      <Text class="section-title">运营配置</Text><View class="owner-progress"><View class="owner-step" @tap="navigate(ownerRoutes.staff)"><Text class="step-glyph">员</Text><View><Text class="step-title">员工账号</Text><Text class="step-copy">新增、修改账号/密码、启停和删除校验</Text></View><Text class="tag blue">管理</Text></View><View class="owner-step" @tap="navigate(ownerRoutes.services + '?mode=list')"><Text class="step-glyph">服</Text><View><Text class="step-title">服务项目</Text><Text class="step-copy">时长、容量、适用资源和员工</Text></View><Text class="tag blue">管理</Text></View><View class="owner-step" @tap="navigate(ownerRoutes.cards)"><Text class="step-glyph">卡</Text><View><Text class="step-title">会员卡模板</Text><Text class="step-copy">售价、次数、有效期和适用范围</Text></View><Text class="tag blue">管理</Text></View></View>
      <View v-if="message" class="error-banner">{{ message }}</View>
    </View>
    <OwnerTabbar active="my" />
  </View>
</template>
