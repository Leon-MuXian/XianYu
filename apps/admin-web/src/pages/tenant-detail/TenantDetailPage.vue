<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import StatusTag from '@/components/StatusTag.vue'
import { adminApi } from '@/services/api'
import type { TenantDetail } from '@/types/admin'

const route = useRoute()
const router = useRouter()
const tenant = ref<TenantDetail | null>(null)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    tenant.value = await adminApi.getTenant(Number(route.params.id))
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '租户详情加载失败')
    router.push('/tenants')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <section v-loading="loading">
    <template v-if="tenant">
      <div class="page-title">
        <div>
          <h1>{{ tenant.name }}</h1>
          <p>租户状态、门店资料和试点健康度统一在详情页确认。</p>
        </div>
        <div class="title-actions">
          <el-button @click="router.push('/tenants')">返回列表</el-button>
          <el-button type="danger" :disabled="tenant.status === 'frozen'" @click="router.push(`/tenants/${tenant.id}/freeze`)">冻结租户</el-button>
        </div>
      </div>

      <div class="detail-grid">
        <div class="admin-card">
          <h2>租户与门店资料</h2>
          <div class="tenant-status-strip">
            <div>
              <span>租户状态</span>
              <StatusTag :status="tenant.status" :text="tenant.statusText" />
            </div>
            <div>
              <span>试用到期</span>
              <strong>{{ tenant.trialEndAt }}</strong>
            </div>
          </div>
          <div class="field-grid">
            <div class="field-box"><span>门店名称</span><strong>{{ tenant.store.name }}</strong></div>
            <div class="field-box"><span>联系电话</span><strong>{{ tenant.store.contactPhone }}</strong></div>
            <div class="field-box wide"><span>门店地址</span><strong>{{ tenant.store.address }}</strong></div>
            <div class="field-box wide"><span>经营项目 / 服务标签</span><strong>{{ tenant.store.businessCategories }} / {{ tenant.store.serviceTags }}</strong></div>
            <div class="field-box wide"><span>营业时间</span><strong>{{ tenant.store.businessHours }}</strong></div>
            <div v-if="tenant.status === 'frozen' && tenant.frozenReason" class="field-box wide"><span>冻结原因</span><strong>{{ tenant.frozenReason }}</strong></div>
          </div>

          <h2 class="section-heading">试点观测</h2>
          <div class="metric-grid compact">
            <div class="metric-card"><span>发卡</span><strong>{{ tenant.snapshot.issuedCards }}</strong><em>本月线下录入</em></div>
            <div class="metric-card"><span>到店转核销</span><strong>{{ Math.round((tenant.snapshot.deductions / Math.max(tenant.snapshot.checkins, 1)) * 100) }}%</strong><em>{{ tenant.snapshot.checkins }} 到店 / {{ tenant.snapshot.deductions }} 核销</em></div>
            <div class="metric-card"><span>预警未处理</span><strong>{{ tenant.snapshot.warningCount }}</strong><em>低余额 {{ tenant.snapshot.lowBalanceCount }} / 到期 {{ tenant.snapshot.expiringCount }}</em></div>
            <div class="metric-card"><span>线下售卡</span><strong>¥{{ Number(tenant.snapshot.monthlySalesYuan).toFixed(0) }}</strong><em>本月录入</em></div>
          </div>

          <h2 class="section-heading">试点健康信号</h2>
          <div class="step-list">
            <div class="step-card"><b>账</b><div><strong>权益台账已接入</strong><small>发卡、绑定、核销后余额变化均由后端数据沉淀。</small></div><StatusTag status="trialing" text="正常" /></div>
            <div class="step-card"><b>用</b><div><strong>员工核销活跃</strong><small>通过到店和核销数据判断门店是否真正使用。</small></div><StatusTag status="extended" text="观察" /></div>
            <div class="step-card"><b>险</b><div><strong>预警处理节奏</strong><small>低余额和到期提醒影响客服续期判断。</small></div><StatusTag status="expiring" text="关注" /></div>
          </div>
        </div>

        <div class="admin-card">
          <h2>冻结和续期影响</h2>
          <div class="step-list">
            <div class="step-card"><b>店</b><div><strong>店长端</strong><small>冻结后只允许进入冻结页和客服信息。</small></div><StatusTag :status="tenant.status === 'frozen' ? 'frozen' : 'trialing'" :text="tenant.status === 'frozen' ? '暂停' : '可用'" /></div>
            <div class="step-card"><b>教</b><div><strong>员工端</strong><small>冻结后名单、到店、核销和记录不可用。</small></div><StatusTag :status="tenant.status === 'frozen' ? 'frozen' : 'extended'" :text="tenant.status === 'frozen' ? '暂停' : '活跃'" /></div>
            <div class="step-card"><b>会</b><div><strong>会员端</strong><small>冻结后约课、取消和候补禁用，只读卡包摘要。</small></div><StatusTag :status="tenant.status === 'frozen' ? 'frozen' : 'extended'" :text="tenant.status === 'frozen' ? '暂停' : '可见'" /></div>
            <div class="step-card"><b>数</b><div><strong>历史数据</strong><small>冻结和解冻都不删除会员卡、到店、核销和预警记录。</small></div><StatusTag status="trialing" text="保留" /></div>
          </div>
        </div>
      </div>
    </template>
  </section>
</template>

<style scoped>
.title-actions {
  display: flex;
  gap: 10px;
}

.section-heading {
  margin-top: 24px;
}

.tenant-status-strip {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 12px;
}

.tenant-status-strip > div {
  display: grid;
  align-content: start;
  justify-items: start;
  min-height: 64px;
  border: 1px solid var(--line-soft);
  border-radius: 4px;
  background: var(--gray-soft);
  padding: 12px;
}

.tenant-status-strip > div > span {
  display: block;
  margin-bottom: 8px;
  color: var(--muted);
  font-size: 12px;
  font-weight: 900;
}

.tenant-status-strip strong {
  display: block;
  color: var(--ink);
  font-size: 16px;
}

.metric-grid.compact {
  margin-bottom: 0;
}

.metric-card em {
  display: block;
  margin-top: 6px;
  color: var(--muted);
  font-style: normal;
  font-size: 12px;
}
</style>
