<script setup lang="ts">
import { useDidShow } from '@tarojs/taro'
import { computed, ref } from 'vue'
import { presentBusinessHours, type BusinessHoursValue } from '@serenmeet/business-components'
import OwnerTabbar from '../../components/OwnerTabbar.vue'
import OwnerTopbar from '../../components/OwnerTopbar.vue'
import { api } from '../../api'
import { formatDate, guardOwner, messageOf, navigate, type OwnerProfile, ownerRoutes, remainingDays } from '../../owner'

interface StoreDetail {
  name?: string
  address?: string
  contactPhone?: string
  businessHours?: BusinessHoursValue
  serviceScopes?: string[]
}
const profile = ref<OwnerProfile | null>(null)
const store = ref<StoreDetail>({})
const message = ref('')
const days = computed(() => remainingDays(profile.value?.trialEndAt))
const businessHours = computed(() => presentBusinessHours(store.value.businessHours))
const storeInitial = computed(() => (store.value.name || profile.value?.name || '店').slice(0, 1))

async function load() {
  message.value = ''
  try {
    profile.value = await guardOwner()
    if (!profile.value) return
    store.value = await api.request<StoreDetail>('GET', '/owner/store')
  } catch (error) { message.value = messageOf(error, '门店资料加载失败') }
}

useDidShow(load)
</script>

<template>
  <View class="screen">
    <OwnerTopbar title="我的" />
    <View class="content owner-dense">
      <View class="owner-paper-profile">
        <View class="owner-paper-accent" aria-hidden="true" />
        <View class="owner-paper-head">
          <Text class="owner-paper-stamp">{{ storeInitial }}</Text>
          <View class="owner-paper-title">
            <Text class="owner-paper-name">{{ store.name || profile?.name || '门店' }}</Text>
            <Text class="owner-paper-account">店长账号 · 微信已登录</Text>
          </View>
          <Text class="owner-paper-role">店长</Text>
        </View>
        <View class="owner-paper-notes">
          <View class="owner-paper-note address-note"><Text>门店地址</Text><Text>{{ store.address || profile?.address || '--' }}</Text></View>
          <View class="owner-paper-note phone-note"><Text>联系电话</Text><Text>{{ store.contactPhone || profile?.contactPhone || '--' }}</Text></View>
        </View>
        <View class="owner-paper-hours">
          <View class="owner-paper-hours-head"><Text>本周营业</Text><Text>{{ businessHours.openDayCount }} 天营业</Text></View>
          <View class="owner-paper-week">
            <View v-for="day in businessHours.week" :key="day.key" class="owner-paper-day" :class="{ closed: !day.open, short: day.key === 'saturday' && day.open }">
              <Text class="owner-paper-day-label">{{ day.label }}</Text>
              <View v-if="day.open" class="owner-paper-day-time"><Text>{{ day.start || '--:--' }}</Text><View /><Text>{{ day.end || '--:--' }}</Text></View>
              <Text v-else class="owner-paper-day-rest">休息</Text>
            </View>
          </View>
        </View>
        <View class="owner-paper-services">
          <Text class="owner-paper-services-label">服务范围</Text>
          <View class="owner-paper-service-list">
            <Text v-for="scope in store.serviceScopes || []" :key="scope">{{ scope }}</Text>
            <Text v-if="!store.serviceScopes?.length" class="owner-paper-service-empty">暂无服务范围</Text>
          </View>
        </View>
      </View>
      <Text class="section-title">使用状态</Text><View class="owner-status-strip two-col"><View class="owner-status"><Text>剩余天数</Text><Text class="status-value">{{ days }}</Text><Text class="status-note gold-text">天</Text></View><View class="owner-status"><Text>到期日</Text><Text class="status-value">{{ formatDate(profile?.trialEndAt) }}</Text><Text class="status-note blue-text">{{ profile?.trialEndAt?.slice(0, 4) }}</Text></View></View>
      <Text class="section-title">门店设置</Text><View class="owner-progress"><View class="owner-step" @tap="navigate(ownerRoutes.store + '?mode=store')"><Text class="step-glyph">店</Text><View><Text class="step-title">门店资料</Text><Text class="step-copy">服务范围、地址、电话</Text></View><Text class="tag blue">管理</Text></View><View class="owner-step" @tap="navigate(ownerRoutes.hours + '?mode=store')"><Text class="step-glyph">时</Text><View><Text class="step-title">营业时间</Text><Text class="step-copy">按星期设置营业和休息</Text></View><Text class="tag blue">设置</Text></View><View class="owner-step" @tap="navigate(ownerRoutes.resources)"><Text class="step-glyph">资</Text><View><Text class="step-title">履约资源</Text><Text class="step-copy">房间、床位、设备或工位</Text></View><Text class="tag blue">管理</Text></View></View>
      <Text class="section-title">运营配置</Text><View class="owner-progress"><View class="owner-step" @tap="navigate(ownerRoutes.staff)"><Text class="step-glyph">员</Text><View><Text class="step-title">员工账号</Text><Text class="step-copy">新增、修改账号/密码、启停和删除校验</Text></View><Text class="tag blue">管理</Text></View><View class="owner-step" @tap="navigate(ownerRoutes.services + '?mode=list')"><Text class="step-glyph">服</Text><View><Text class="step-title">服务项目</Text><Text class="step-copy">时长、容量、适用资源和员工</Text></View><Text class="tag blue">管理</Text></View><View class="owner-step" @tap="navigate(ownerRoutes.cards)"><Text class="step-glyph">卡</Text><View><Text class="step-title">会员卡管理</Text><Text class="step-copy">售价、次数、有效期和适用范围</Text></View><Text class="tag blue">管理</Text></View></View>
      <View v-if="message" class="error-banner">{{ message }}</View>
    </View>
    <OwnerTabbar active="my" />
  </View>
</template>
