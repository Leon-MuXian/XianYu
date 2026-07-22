<script setup lang="ts">
import Taro, { useLoad } from '@tarojs/taro'
import { ref } from 'vue'
import OwnerTopbar from '../../components/OwnerTopbar.vue'
import { api } from '../../api'
import { messageOf, type OwnerProfile } from '../../owner'

const info = ref<OwnerProfile | null>(null)
const message = ref('')
useLoad(async () => {
  try { info.value = await api.request<OwnerProfile>('GET', '/owner/frozen') }
  catch (error) { message.value = messageOf(error, '冻结信息加载失败') }
})

async function copyWechat() {
  if (!info.value?.supportWechatId) return
  await Taro.setClipboardData({ data: info.value.supportWechatId })
  await Taro.showToast({ title: '客服微信已复制', icon: 'success' })
}
</script>

<template>
  <View class="screen">
    <OwnerTopbar title="服务已冻结" />
    <View class="content owner-dense">
      <View class="owner-hero frozen-hero"><Text class="hero-title">试用已结束</Text><Text class="hero-copy">当前服务暂时暂停，历史会员、会员卡、预约、核销和报表数据都会保留，恢复后可继续使用。</Text><View class="owner-hero-meta"><Text>服务暂停</Text><Text>数据保留</Text></View></View>
      <View class="owner-status-strip"><View class="owner-status"><Text>门店管理</Text><Text class="status-value">停</Text><Text class="status-note coral-text">暂停编辑</Text></View><View class="owner-status"><Text>员工服务</Text><Text class="status-value">停</Text><Text class="status-note coral-text">暂停核销</Text></View><View class="owner-status"><Text>会员预约</Text><Text class="status-value">停</Text><Text class="status-note coral-text">暂停预约</Text></View></View>
      <View class="store-info-card"><View class="store-info-head"><Text class="store-info-icon">店</Text><View><Text class="preview-title">{{ info?.name || '门店' }}</Text><Text class="page-copy">{{ info?.address || '历史资料已保留' }}</Text></View><Text class="tag hot">只读</Text></View><View class="store-info-grid"><View><Text>联系电话</Text><Text class="info-value">{{ info?.contactPhone || '--' }}</Text></View><View><Text>状态</Text><Text class="info-value">服务已暂停</Text></View></View></View>
      <View class="owner-list-card"><Text class="list-card-title">客服微信 {{ info?.supportWechatId || '暂未配置' }}</Text><Text class="list-card-copy">{{ info?.supportText || '添加客服后，我们会协助确认使用期限并恢复服务。' }}</Text><Text class="tag blue">客服协助</Text></View>
      <View v-if="message" class="error-banner">{{ message }}</View>
      <button class="button coral frozen-action" :disabled="!info?.supportWechatId" @tap="copyWechat">添加客服微信</button>
    </View>
  </View>
</template>
