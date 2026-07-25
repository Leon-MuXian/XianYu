<script setup lang="ts">
import Taro, { useDidShow } from '@tarojs/taro'
import { computed, reactive, ref } from 'vue'
import { createIdempotencyKey } from '@serenmeet/api-client'
import OwnerTabbar from '../../components/OwnerTabbar.vue'
import OwnerTopbar from '../../components/OwnerTopbar.vue'
import { api } from '../../api'
import { guardOwner, messageOf, type Staff, type StaffCredential } from '../../owner'
import {
  formatStaffCredentialCopy,
  hasStaffLoginPasswordChanged,
  isStaffLoginNameTaken,
  normalizeStaffLoginName,
  STAFF_LOGIN_NAME_TAKEN_MESSAGE
} from '../../staff-form'

const rows = ref<Staff[]>([])
const filter = ref<'all' | 'active' | 'disabled'>('all')
const sheetMode = ref<'create' | 'edit' | ''>('')
const current = ref<Staff | null>(null)
const form = reactive({ staffName: '', roleLabel: '', loginName: '', password: '', confirmPassword: '', status: 'active' })
const initialPassword = ref('')
const loading = ref(false)
const message = ref('')
const loginNameError = ref('')
const credentialLoading = ref(false)
const credentialError = ref('')
const credentialReady = ref(false)
const credentialMigrationRequired = ref(false)
const passwordVisible = ref(false)
const copyingStaffId = ref<number | null>(null)
const editMenuOpen = ref(false)
let credentialRequestId = 0
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
  credentialRequestId += 1
  editMenuOpen.value = false
  current.value = null
  initialPassword.value = ''
  Object.assign(form, { staffName: '', roleLabel: '', loginName: '', password: '', confirmPassword: '', status: 'active' })
  sheetMode.value = 'create'
  message.value = ''
  loginNameError.value = ''
  credentialLoading.value = false
  credentialError.value = ''
  credentialReady.value = false
  credentialMigrationRequired.value = false
  passwordVisible.value = false
}

async function openEdit(staff: Staff) {
  const requestId = credentialRequestId + 1
  credentialRequestId = requestId
  editMenuOpen.value = false
  current.value = staff
  initialPassword.value = ''
  Object.assign(form, {
    staffName: staff.staffName,
    roleLabel: staff.roleLabel,
    loginName: staff.loginName,
    password: '',
    confirmPassword: '',
    status: staff.status
  })
  sheetMode.value = 'edit'
  message.value = ''
  loginNameError.value = ''
  credentialLoading.value = true
  credentialError.value = ''
  credentialReady.value = false
  credentialMigrationRequired.value = !staff.credentialAvailable
  passwordVisible.value = false
  if (!staff.credentialAvailable) {
    credentialLoading.value = false
    credentialReady.value = true
    return
  }
  try {
    const credential = await api.request<StaffCredential>(
      'POST',
      `/owner/staff/${staff.id}/credential/reveal`,
      {}
    )
    if (requestId !== credentialRequestId || sheetMode.value !== 'edit' || current.value?.id !== staff.id) return
    initialPassword.value = credential.loginPassword
    Object.assign(form, {
      staffName: credential.staffName,
      loginName: credential.loginName,
      password: credential.loginPassword,
      confirmPassword: credential.loginPassword
    })
    passwordVisible.value = true
    credentialReady.value = true
  } catch (error) {
    if (requestId !== credentialRequestId || sheetMode.value !== 'edit' || current.value?.id !== staff.id) return
    credentialError.value = messageOf(error, '员工登录信息加载失败')
  } finally {
    if (requestId === credentialRequestId) credentialLoading.value = false
  }
}

function closeSheet() {
  credentialRequestId += 1
  editMenuOpen.value = false
  sheetMode.value = ''
  initialPassword.value = ''
  form.password = ''
  form.confirmPassword = ''
  loginNameError.value = ''
  credentialLoading.value = false
  credentialError.value = ''
  credentialReady.value = false
  credentialMigrationRequired.value = false
  passwordVisible.value = false
}

function clearLoginNameError() {
  loginNameError.value = ''
}

function toggleEditMenu() {
  if (sheetMode.value !== 'edit' || loading.value) return
  editMenuOpen.value = !editMenuOpen.value
}

async function copyAccount(staff: Staff) {
  if (copyingStaffId.value !== null) return
  copyingStaffId.value = staff.id
  try {
    if (!staff.credentialAvailable) {
      const result = await Taro.showModal({
        title: '需要设置一次新密码',
        content: '该账号创建于可管理密码功能启用前，请设置一次新密码；之后可直接查看和复制。',
        confirmText: '去设置',
        confirmColor: '#176b5c'
      })
      if (result.confirm) await openEdit(staff)
      return
    }
    const credential = await api.request<StaffCredential>(
      'POST',
      `/owner/staff/${staff.id}/credential/reveal`,
      {}
    )
    await Taro.setClipboardData({
      data: formatStaffCredentialCopy(
        credential.staffName,
        credential.loginName,
        credential.loginPassword
      )
    })
    await Taro.showToast({ title: '登录信息已复制', icon: 'success' })
  } catch (error) {
    message.value = messageOf(error, '登录信息复制失败')
  } finally {
    copyingStaffId.value = null
  }
}

async function submit() {
  const loginName = normalizeStaffLoginName(form.loginName)
  form.loginName = loginName
  loginNameError.value = ''
  if (sheetMode.value === 'edit' && !credentialReady.value) {
    message.value = credentialError.value || '员工登录信息正在加载，请稍候'
    return
  }
  if (!form.staffName || !form.roleLabel || !loginName) { message.value = '请完成所有必填项'; return }
  const passwordChanged = sheetMode.value === 'edit'
    && hasStaffLoginPasswordChanged(initialPassword.value, form.password)
  if ((sheetMode.value === 'create' || credentialMigrationRequired.value || passwordChanged) && form.password.length < 8) { message.value = '员工密码至少 8 位'; return }
  if (form.password !== form.confirmPassword) { message.value = '两次输入的密码不一致'; return }
  loading.value = true
  message.value = ''
  try {
    if (sheetMode.value === 'create') {
      await api.request('POST', '/owner/staff', { loginName, password: form.password, staffName: form.staffName, roleLabel: form.roleLabel }, createIdempotencyKey('staff-create'))
    } else if (current.value) {
      const staffId = current.value.id
      await api.request('PUT', `/owner/staff/${staffId}`, { loginName, staffName: form.staffName, roleLabel: form.roleLabel }, createIdempotencyKey('staff-update'))
      if (passwordChanged) {
        await api.request('PUT', `/owner/staff/${staffId}/password`, { password: form.password }, createIdempotencyKey('staff-password'))
      }
      if (form.status !== current.value.status) await api.request('PUT', `/owner/staff/${staffId}/status`, { status: form.status }, createIdempotencyKey('staff-status'))
    }
    await Taro.showToast({ title: sheetMode.value === 'create' ? '员工已创建' : '员工已更新', icon: 'success' })
    closeSheet()
    await load()
  } catch (error) {
    if (isStaffLoginNameTaken(error)) {
      loginNameError.value = STAFF_LOGIN_NAME_TAKEN_MESSAGE
      message.value = ''
    } else {
      message.value = messageOf(error, '员工账号保存失败')
    }
  } finally { loading.value = false }
}

async function remove() {
  editMenuOpen.value = false
  if (!current.value) return
  const result = await Taro.showModal({ title: `删除${current.value.staffName}账号？`, content: '系统会先校验服务项目、会员卡范围和未来排期关联。', confirmText: '确认删除', confirmColor: '#c64a46' })
  if (!result.confirm) return
  loading.value = true
  try {
    await api.request('DELETE', `/owner/staff/${current.value.id}`, {}, createIdempotencyKey('staff-delete'))
    closeSheet()
    await Taro.showToast({ title: '员工账号已删除', icon: 'success' })
    await load()
  } catch (error) { message.value = messageOf(error, '员工账号暂不能删除') } finally { loading.value = false }
}

useDidShow(load)
</script>

<template>
  <View class="screen staff-screen">
    <OwnerTopbar title="员工" back />
    <scroll-view :scroll-y="true" :enhanced="true" :show-scrollbar="false" class="staff-page-scroll">
      <View class="content owner-dense">
        <View class="owner-page-head">
          <View class="owner-page-head-copy"><Text class="page-title">员工账号</Text><Text class="page-copy">共 {{ rows.length }} 人。查看员工是否可登录、可排期；可复制完整登录信息发给员工。</Text></View>
          <button class="owner-add-action" aria-label="新增员工" hover-class="owner-add-action-pressed" @tap="openCreate"><View class="owner-add-action-icon" /></button>
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
      </View>
    </scroll-view>
    <OwnerTabbar active="my" />
    <View v-if="sheetMode" class="modal-backdrop" @tap.self="closeSheet">
      <View class="sheet resource-sheet" @tap="editMenuOpen = false">
        <View class="sheet-grip" />
        <View class="sheet-title staff-sheet-title">
          <View class="staff-sheet-title-copy"><Text class="sheet-heading">{{ sheetMode === 'create' ? '新增员工账号' : `修改 ${current?.staffName} 账号` }}</Text><Text class="sheet-copy">{{ sheetMode === 'create' ? '创建后员工出现在列表，可继续在列表中打开修改抽屉。' : '修改账号资料、密码和启停状态。' }}</Text></View>
          <View v-if="sheetMode === 'edit'" class="staff-sheet-actions" @tap.stop>
            <button
              class="staff-sheet-more"
              :class="{ active: editMenuOpen }"
              aria-label="更多账号操作"
              hover-class="staff-sheet-more-pressed"
              @tap="toggleEditMenu"
            >
              <View class="staff-sheet-more-dot" />
              <View class="staff-sheet-more-dot" />
              <View class="staff-sheet-more-dot" />
            </button>
            <View v-if="editMenuOpen" class="staff-sheet-menu">
              <button class="staff-sheet-menu-danger" hover-class="staff-sheet-menu-danger-pressed" @tap="remove">删除账号</button>
            </View>
          </View>
        </View>
        <View class="owner-form-grid">
          <View class="field wide"><Text>员工姓名 <Text class="required-mark">必填</Text></Text><input v-model="form.staffName" class="field-input" :disabled="sheetMode === 'edit' && !credentialReady" placeholder="请输入员工姓名" /></View>
          <View class="field"><Text>角色标签 <Text class="required-mark">必填</Text></Text><input v-model="form.roleLabel" class="field-input" :disabled="sheetMode === 'edit' && !credentialReady" placeholder="如：康复师" /></View>
          <View class="field wide"><Text>登录账号 <Text class="required-mark">必填</Text></Text><input v-model="form.loginName" class="field-input" :disabled="sheetMode === 'edit' && !credentialReady" maxlength="80" placeholder="请输入登录账号" @input="clearLoginNameError" /><Text v-if="loginNameError" class="field-error">{{ loginNameError }}</Text></View>
          <View class="field"><View class="field-label-row"><Text>{{ sheetMode === 'create' ? '初始密码' : credentialMigrationRequired ? '新密码' : '登录密码' }} <Text class="required-mark">必填</Text></Text><Text v-if="!credentialLoading && (credentialReady || sheetMode === 'create')" class="field-label-action" @tap.stop="passwordVisible = !passwordVisible">{{ passwordVisible ? '隐藏密码' : '显示密码' }}</Text></View><input v-model="form.password" class="field-input" :disabled="sheetMode === 'edit' && !credentialReady" :password="!passwordVisible" :placeholder="credentialLoading ? '正在加载' : '至少 8 位'" /></View>
          <View class="field"><Text>确认密码 <Text class="required-mark">必填</Text></Text><input v-model="form.confirmPassword" class="field-input" :disabled="sheetMode === 'edit' && !credentialReady" :password="!passwordVisible" :placeholder="credentialLoading ? '正在加载' : '再次输入'" /></View>
        </View>
        <View v-if="credentialLoading" class="resource-sheet-note">正在加载员工登录信息…</View>
        <View v-else-if="credentialError" class="error-banner">{{ credentialError }}</View>
        <View v-if="sheetMode === 'edit'" class="staff-edit-status">
          <View class="staff-edit-status-copy"><Text>账号状态</Text><Text>当前可登录和参与排期</Text></View>
          <View class="staff-status-switch"><Text :class="{ active: form.status === 'active' }" @tap="form.status = 'active'">启用</Text><Text :class="{ active: form.status !== 'active' }" @tap="form.status = 'disabled'">停用</Text></View>
        </View>
        <View v-if="message" class="error-banner">{{ message }}</View>
        <View class="form-footer"><button class="button secondary" @tap="closeSheet">取消</button><button class="button" :loading="loading || credentialLoading" :disabled="loading || credentialLoading || (sheetMode === 'edit' && !credentialReady)" @tap="submit">{{ sheetMode === 'create' ? '创建账号' : '保存修改' }}</button></View>
      </View>
    </View>
  </View>
</template>
