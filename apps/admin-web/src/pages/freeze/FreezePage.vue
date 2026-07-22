<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import StatusTag from '@/components/StatusTag.vue'
import { adminApi } from '@/services/api'
import type { TenantDetail } from '@/types/admin'

const route = useRoute()
const router = useRouter()
const tenant = ref<TenantDetail | null>(null)
const loading = ref(false)
const submitting = ref(false)
const form = reactive({
  reason: '',
  confirmed: false
})

const canSubmit = computed(() => !!form.reason.trim() && form.confirmed && !submitting.value)

async function load() {
  loading.value = true
  try {
    tenant.value = await adminApi.getTenant(Number(route.params.id))
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '租户加载失败')
    router.push('/tenants')
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (!tenant.value || !canSubmit.value) return
  submitting.value = true
  try {
    await adminApi.freezeTenant(tenant.value.id, form.reason, form.confirmed)
    ElMessage.success('租户已冻结，状态和审计记录已刷新')
    router.push(`/tenants/${tenant.value.id}`)
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '冻结失败')
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<template>
  <section v-loading="loading">
    <template v-if="tenant">
      <div class="page-title">
        <div>
          <h1>冻结{{ tenant.name }}</h1>
          <p>冻结会暂停三端业务操作，但历史数据、会员卡和预约记录都保留。</p>
        </div>
        <StatusTag status="frozen" text="高风险" />
      </div>

      <div class="detail-grid">
        <div class="admin-card">
          <h2>冻结确认</h2>
          <div class="field-grid">
            <div class="field-box"><span>租户</span><strong>{{ tenant.name }}</strong></div>
            <div class="field-box"><span>当前状态</span><StatusTag :status="tenant.status" :text="tenant.statusText" /></div>
            <div class="field-box"><span>当前到期</span><strong>{{ tenant.trialEndAt }}</strong></div>
            <div class="field-box"><span>冻结后状态</span><strong>已冻结</strong></div>
          </div>
          <el-form class="freeze-form" label-position="top">
            <el-form-item label="冻结原因（必填）">
              <el-input v-model="form.reason" type="textarea" :rows="4" placeholder="例如：试用到期未续期，客服已完成通知。" />
            </el-form-item>
            <el-checkbox v-model="form.confirmed">已确认暂停店长端、员工端和会员端业务操作</el-checkbox>
          </el-form>
          <div class="form-hint danger-hint">原因缺失或未勾选确认时，确认冻结按钮禁用；提交中禁止重复操作。</div>
          <div class="actions">
            <el-button type="danger" :loading="submitting" :disabled="!canSubmit" @click="submit">确认冻结</el-button>
            <el-button @click="router.push(`/tenants/${tenant.id}`)">取消</el-button>
          </div>
        </div>

        <div class="admin-card">
          <h2>冻结影响</h2>
          <div class="step-list">
            <div class="step-card"><b>店</b><div><strong>店长端</strong><small>只允许进入冻结页和客服信息。</small></div><StatusTag status="frozen" text="暂停" /></div>
            <div class="step-card"><b>教</b><div><strong>员工端</strong><small>名单、到店、核销和记录不可用。</small></div><StatusTag status="frozen" text="暂停" /></div>
            <div class="step-card"><b>会</b><div><strong>会员端</strong><small>约课、取消和候补禁用，只读卡包摘要。</small></div><StatusTag status="frozen" text="暂停" /></div>
            <div class="step-card"><b>数</b><div><strong>历史数据</strong><small>不删除历史预约、售卡和核销记录。</small></div><StatusTag status="trialing" text="保留" /></div>
          </div>
        </div>
      </div>
    </template>
  </section>
</template>

<style scoped>
.freeze-form {
  margin-top: 18px;
}

.actions {
  display: flex;
  gap: 12px;
  margin-top: 20px;
}
</style>
