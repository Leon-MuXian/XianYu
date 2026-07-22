<script setup lang="ts">
import Taro, { useDidShow } from '@tarojs/taro'
import { computed, reactive, ref } from 'vue'
import { createIdempotencyKey } from '@serenmeet/api-client'
import OwnerTabbar from '../../components/OwnerTabbar.vue'
import OwnerTopbar from '../../components/OwnerTopbar.vue'
import { api } from '../../api'
import { guardOwner, messageOf, type Staff } from '../../owner'

const rows = ref<Staff[]>([])
const filter = ref<'all' | 'active' | 'disabled'>('all')
const sheetMode = ref<'create' | 'edit' | ''>('')
const current = ref<Staff | null>(null)
const form = reactive({ staffName: '', roleLabel: '', loginName: '', password: '', confirmPassword: '', status: 'active' })
const loading = ref(false)
const message = ref('')
const filteredRows = computed(() => filter.value === 'all' ? rows.value : rows.value.filter((item) => item.status === filter.value))
const activeCount = computed(() => rows.value.filter((item) => item.status === 'active').length)
const disabledCount = computed(() => rows.value.length - activeCount.value)

async function load() {
  try {
    if (!await guardOwner()) return
    rows.value = await api.request<Staff[]>('GET', '/owner/staff')
  } catch (error) { message.value = messageOf(error, '员工列表加载失败') }
}

function openCreate() {
  current.value = null
  Object.assign(form, { staffName: '', roleLabel: '', loginName: '', password: '', confirmPassword: '', status: 'active' })
  sheetMode.value = 'create'
  message.value = ''
}

function openEdit(staff: Staff) {
  current.value = staff
  Object.assign(form, { staffName: staff.staffName, roleLabel: staff.roleLabel, loginName: staff.loginName, password: '', confirmPassword: '', status: staff.status })
  sheetMode.value = 'edit'
  message.value = ''
}

async function copyAccount(staff: Staff) {
  try {
    await Taro.setClipboardData({ data: `${staff.staffName}\n登录账号：${staff.loginName}` })
    await Taro.showToast({ title: '账号已复制', icon: 'success' })
  } catch (error) { message.value = messageOf(error, '复制失败') }
}

async function submit() {
  if (!form.staffName || !form.roleLabel || !form.loginName) { message.value = '请完成所有必填项'; return }
  if ((sheetMode.value === 'create' || form.password) && form.password.length < 8) { message.value = '员工密码至少 8 位'; return }
  if (form.password !== form.confirmPassword) { message.value = '两次输入的密码不一致'; return }
  loading.value = true
  message.value = ''
  try {
    if (sheetMode.value === 'create') {
      await api.request('POST', '/owner/staff', { loginName: form.loginName, password: form.password, staffName: form.staffName, roleLabel: form.roleLabel }, createIdempotencyKey('staff-create'))
    } else if (current.value) {
      await api.request('PUT', `/owner/staff/${current.value.id}`, { loginName: form.loginName, staffName: form.staffName, roleLabel: form.roleLabel }, createIdempotencyKey('staff-update'))
      if (form.password) await api.request('PUT', `/owner/staff/${current.value.id}/password`, { password: form.password }, createIdempotencyKey('staff-password'))
      if (form.status !== current.value.status) await api.request('PUT', `/owner/staff/${current.value.id}/status`, { status: form.status }, createIdempotencyKey('staff-status'))
    }
    await Taro.showToast({ title: sheetMode.value === 'create' ? '员工已创建' : '员工已更新', icon: 'success' })
    sheetMode.value = ''
    await load()
  } catch (error) { message.value = messageOf(error, '员工账号保存失败') } finally { loading.value = false }
}

async function remove() {
  if (!current.value) return
  const result = await Taro.showModal({ title: `删除${current.value.staffName}账号？`, content: '系统会先校验服务项目、会员卡范围和未来排期关联。', confirmText: '确认删除', confirmColor: '#c64a46' })
  if (!result.confirm) return
  loading.value = true
  try {
    await api.request('DELETE', `/owner/staff/${current.value.id}`, {}, createIdempotencyKey('staff-delete'))
    sheetMode.value = ''
    await Taro.showToast({ title: '员工账号已删除', icon: 'success' })
    await load()
  } catch (error) { message.value = messageOf(error, '员工账号暂不能删除') } finally { loading.value = false }
}

useDidShow(load)
</script>

<template>
  <View class="screen">
    <OwnerTopbar title="员工" back />
    <View class="content owner-dense">
      <View class="owner-page-head">
        <View class="owner-page-head-copy"><Text class="page-title">员工账号</Text><Text class="page-copy">共 {{ rows.length }} 人。查看员工是否可登录、可排期；可快速复制账号发给教练。</Text></View>
        <button class="owner-add-action" aria-label="新增员工" hover-class="owner-add-action-pressed" @tap="openCreate"><Text class="owner-add-action-icon">+</Text></button>
      </View>
      <View class="owner-status-strip"><View class="owner-status"><Text>启用</Text><Text class="status-value">{{ activeCount }}</Text><Text class="status-note">可排期</Text></View><View class="owner-status"><Text>停用</Text><Text class="status-value">{{ disabledCount }}</Text><Text class="status-note coral-text">不可登录</Text></View><View class="owner-status"><Text>今日服务</Text><Text class="status-value">0</Text><Text class="status-note blue-text">时段</Text></View></View>
      <View class="owner-tabs"><Text :class="{ active: filter === 'all' }" @tap="filter = 'all'">全部</Text><Text :class="{ active: filter === 'active' }" @tap="filter = 'active'">启用</Text><Text :class="{ active: filter === 'disabled' }" @tap="filter = 'disabled'">停用</Text></View>
      <View v-if="message && !sheetMode" class="error-banner">{{ message }}</View>
      <View v-if="!filteredRows.length" class="empty-state" @tap="openCreate"><Text class="empty-title">还没有员工账号</Text><Text>点击页面上方新增按钮，为可履约人员创建登录账号。</Text></View>
      <View v-else class="coach-roster">
        <View v-for="staff in filteredRows" :key="staff.id" class="coach-row">
          <Text class="avatar">{{ staff.staffName.slice(0, 2) }}</Text><View @tap="openEdit(staff)"><Text class="staff-name">{{ staff.staffName }} · {{ staff.roleLabel }}</Text><Text class="staff-meta">{{ staff.loginName }} · {{ staff.status === 'active' ? '启用' : '停用' }} · {{ staff.firstLogin ? '待首次登录' : '已登录' }}</Text></View><View class="resource-row-actions"><Text class="resource-action" @tap="copyAccount(staff)">复制</Text><Text class="resource-action" :class="{ danger: staff.status !== 'active' }" @tap="openEdit(staff)">修改</Text></View>
        </View>
      </View>
      <View class="owner-list-card"><Text class="list-card-title">快速复制账号</Text><Text class="list-card-copy">复制员工姓名和登录账号；店长再通过微信等渠道手动分享给教练。</Text><Text class="tag blue">复制</Text></View>
    </View>
    <OwnerTabbar active="my" />
    <View v-if="sheetMode" class="modal-backdrop" @tap.self="sheetMode = ''">
      <View class="sheet resource-sheet">
        <View class="sheet-grip" />
        <View class="sheet-title"><View><Text class="sheet-heading">{{ sheetMode === 'create' ? '新增员工账号' : `修改 ${current?.staffName} 账号` }}</Text><Text class="sheet-copy">{{ sheetMode === 'create' ? '创建后员工出现在列表，可继续在列表中打开修改抽屉。' : '修改账号资料、密码和启停状态。' }}</Text></View></View>
        <View class="owner-form-grid">
          <View class="field wide"><Text>员工姓名 <Text class="required-mark">必填</Text></Text><input v-model="form.staffName" class="field-input" placeholder="请输入员工姓名" /></View>
          <View class="field"><Text>角色标签 <Text class="required-mark">必填</Text></Text><input v-model="form.roleLabel" class="field-input" placeholder="如：康复师" /></View>
          <View class="field wide"><Text>登录账号 <Text class="required-mark">必填</Text></Text><input v-model="form.loginName" class="field-input" placeholder="请输入登录账号" /></View>
          <View class="field"><Text>{{ sheetMode === 'create' ? '初始密码' : '新密码' }} <Text v-if="sheetMode === 'create'" class="required-mark">必填</Text></Text><input v-model="form.password" class="field-input" password placeholder="至少 8 位" /></View>
          <View class="field"><Text>确认密码 <Text v-if="sheetMode === 'create'" class="required-mark">必填</Text></Text><input v-model="form.confirmPassword" class="field-input" password placeholder="再次输入" /></View>
        </View>
        <View v-if="sheetMode === 'edit'" class="resource-toggle staff-status-toggle"><Text :class="{ selected: form.status === 'active' }" @tap="form.status = 'active'">启用</Text><Text :class="{ selected: form.status !== 'active' }" @tap="form.status = 'disabled'">停用</Text></View>
        <View class="resource-sheet-note">{{ sheetMode === 'create' ? '账号创建后默认为启用；取消或创建成功后回到员工列表。' : '停用后员工不能登录，也不能被新排期选择；删除前会校验关联项目。' }}</View>
        <View v-if="message" class="error-banner">{{ message }}</View>
        <View class="form-footer"><button class="button secondary" @tap="sheetMode = ''">取消</button><button class="button" :loading="loading" :disabled="loading" @tap="submit">{{ sheetMode === 'create' ? '创建账号' : '保存修改' }}</button></View>
        <View v-if="sheetMode === 'edit'" class="form-footer single"><button class="button coral" :disabled="loading" @tap="remove">删除账号</button></View>
      </View>
    </View>
  </View>
</template>
