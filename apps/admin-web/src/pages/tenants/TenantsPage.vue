<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { EditPen, View } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import StatusTag from '@/components/StatusTag.vue'
import { adminApi } from '@/services/api'
import type { TenantDetail, TenantListItem, TenantListResponse } from '@/types/admin'

const loading = ref(false)
const detailLoading = ref(false)
const submitting = ref(false)
const data = ref<TenantListResponse | null>(null)
const selectedTenant = ref<TenantDetail | null>(null)
const detailOpen = ref(false)
const adjustOpen = ref(false)
const filters = reactive({
  keyword: '',
  status: 'ALL',
  expiry: 'ALL',
  page: 1,
  pageSize: 10
})
const extendForm = reactive({
  newTrialEndAt: '',
  reason: '',
  internalNote: ''
})
const freezeForm = reactive({
  reason: '',
  confirmed: false
})

const rows = computed(() => data.value?.page.items || [])
const stats = computed(() => data.value?.stats)
const canExtend = computed(() => !!selectedTenant.value && selectedTenant.value.status === 'frozen' && !!extendForm.newTrialEndAt && !!extendForm.reason.trim() && !submitting.value)
const canFreeze = computed(() => !!selectedTenant.value && selectedTenant.value.status !== 'frozen' && !!freezeForm.reason.trim() && freezeForm.confirmed && !submitting.value)

async function load() {
  loading.value = true
  try {
    data.value = await adminApi.listTenants(filters)
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '租户列表加载失败')
  } finally {
    loading.value = false
  }
}

function query() {
  filters.page = 1
  load()
}

async function fetchTenant(id: number) {
  detailLoading.value = true
  try {
    selectedTenant.value = await adminApi.getTenant(id)
    return selectedTenant.value
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '租户详情加载失败')
    return null
  } finally {
    detailLoading.value = false
  }
}

async function openDetail(row: TenantListItem) {
  detailOpen.value = true
  await fetchTenant(row.id)
}

function resetAdjustForms(tenant: TenantDetail) {
  extendForm.newTrialEndAt = tenant.trialEndAt
  extendForm.reason = ''
  extendForm.internalNote = ''
  freezeForm.reason = ''
  freezeForm.confirmed = false
}

async function openAdjust(row: TenantListItem) {
  adjustOpen.value = true
  const tenant = await fetchTenant(row.id)
  if (tenant) {
    resetAdjustForms(tenant)
  }
}

async function submitFreeze() {
  if (!selectedTenant.value || !canFreeze.value) return
  submitting.value = true
  try {
    const updated = await adminApi.freezeTenant(selectedTenant.value.id, freezeForm.reason, freezeForm.confirmed)
    selectedTenant.value = updated
    resetAdjustForms(updated)
    ElMessage.success('租户已冻结，审计记录已写入')
    load()
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '冻结失败')
  } finally {
    submitting.value = false
  }
}

async function submitExtend() {
  if (!selectedTenant.value || !canExtend.value) return
  submitting.value = true
  try {
    const updated = await adminApi.extendTenant(selectedTenant.value.id, extendForm)
    selectedTenant.value = updated
    resetAdjustForms(updated)
    ElMessage.success('期限已保存并解冻，审计记录已写入')
    load()
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '保存失败')
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<template>
  <section>
    <div class="page-title">
      <div>
        <h1>租户管理</h1>
        <p>租户只由店长首次微信登录自动开通。后台用于观察试点活跃度、处理冻结和人工续期。</p>
      </div>
    </div>

    <div v-if="stats" class="metric-grid">
      <div class="metric-card"><span>全部租户</span><strong>{{ stats.total }}</strong></div>
      <div class="metric-card"><span>试用中</span><strong>{{ stats.trialing }}</strong></div>
      <div class="metric-card"><span>即将到期</span><strong>{{ stats.expiring }}</strong></div>
      <div class="metric-card frozen-metric"><span>已冻结</span><strong>{{ stats.frozen }}</strong></div>
      <div class="metric-card extended-metric"><span>已延长</span><strong>{{ stats.extended }}</strong></div>
    </div>

    <div class="filter-bar">
      <el-input v-model="filters.keyword" class="filter-input" placeholder="搜索租户名称或城市" clearable @keyup.enter="query" />
      <el-select v-model="filters.status" class="filter-select">
        <el-option label="状态：全部" value="ALL" />
        <el-option label="试用中" value="trialing" />
        <el-option label="即将到期" value="expiring" />
        <el-option label="已冻结" value="frozen" />
        <el-option label="已延长" value="extended" />
      </el-select>
      <el-select v-model="filters.expiry" class="filter-select wide">
        <el-option label="到期：全部" value="ALL" />
        <el-option label="已过期" value="EXPIRED" />
        <el-option label="7 天内到期" value="WITHIN_7_DAYS" />
        <el-option label="7 天后到期" value="AFTER_7_DAYS" />
      </el-select>
      <el-button class="filter-submit" type="primary" @click="query">查询</el-button>
    </div>

    <div class="admin-card table-shell">
      <el-table v-loading="loading" :data="rows" border class="tenant-table">
        <el-table-column label="租户" min-width="170" fixed show-overflow-tooltip>
          <template #default="{ row }">
            <strong class="tenant-name-cell">{{ row.name }}</strong>
          </template>
        </el-table-column>
        <el-table-column prop="city" label="城市" width="80" show-overflow-tooltip />
        <el-table-column prop="trialStartAt" label="开通时间" width="116" />
        <el-table-column prop="trialEndAt" label="到期时间" width="116" />
        <el-table-column label="状态" width="112">
          <template #default="{ row }">
            <StatusTag :status="row.status" :text="row.statusText" />
          </template>
        </el-table-column>
        <el-table-column label="客服微信" min-width="170" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="wechat-id-cell" :title="row.supportWechatId || '未配置'">{{ row.supportWechatId || '未配置' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="本月线下售卡" width="138" align="left" header-align="left">
          <template #default="{ row }">
            <span class="sales-amount-cell">¥{{ Number(row.monthlySalesYuan).toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="178" align="left" header-align="left">
          <template #default="{ row }">
            <div class="tenant-row-actions">
              <el-button class="tenant-action-button detail-action" :icon="View" @click="openDetail(row)">详情</el-button>
              <el-button class="tenant-action-button adjust-action" :icon="EditPen" @click="openAdjust(row)">调整</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="tenant-card-list">
      <article v-for="row in rows" :key="row.id" class="tenant-mobile-card">
        <div>
          <strong>{{ row.name }}</strong>
          <small>{{ row.city }} · 到期 {{ row.trialEndAt }}</small>
        </div>
        <StatusTag :status="row.status" :text="row.statusText" />
        <p>发卡 {{ row.issuedCards }} · 核销 {{ row.deductions }} · 预警 {{ row.warningCount }} · ¥{{ Number(row.monthlySalesYuan).toFixed(0) }}</p>
        <div class="mobile-actions">
          <el-button class="tenant-action-button detail-action" :icon="View" @click="openDetail(row)">详情</el-button>
          <el-button class="tenant-action-button adjust-action" :icon="EditPen" @click="openAdjust(row)">调整</el-button>
        </div>
      </article>
    </div>

    <div v-if="!loading && rows.length === 0" class="empty-state">
      <div>
        <h3>没有匹配的租户</h3>
        <p>当前筛选条件下没有自动开通的租户。可以清空搜索词或切换状态/到期筛选；后台不会提供新建租户入口。</p>
        <el-button type="primary" @click="filters.keyword = ''; filters.status = 'ALL'; filters.expiry = 'ALL'; query()">查看全部租户</el-button>
      </div>
    </div>

    <div v-if="data" class="pagination-row">
      <div class="pagination-meta">
        <strong>{{ data.page.total }}</strong>
        <span>个租户 · 第 {{ data.page.page }} 页</span>
      </div>
      <el-pagination
        v-model:current-page="filters.page"
        v-model:page-size="filters.pageSize"
        layout="prev, pager, next"
        :total="data.page.total"
        @current-change="load"
      />
    </div>

    <el-drawer v-model="detailOpen" title="租户详情" size="min(720px, 92vw)">
      <div v-loading="detailLoading">
        <template v-if="selectedTenant">
          <div class="tenant-drawer-hero">
            <div class="tenant-avatar">{{ selectedTenant.name.slice(0, 1) }}</div>
            <div>
              <h2>{{ selectedTenant.name }}</h2>
              <p>{{ selectedTenant.city }} · {{ selectedTenant.store.name }}</p>
            </div>
          </div>

          <div class="tenant-summary-grid">
            <div class="summary-cell state-card" :class="`state-${selectedTenant.status.toLowerCase()}`">
              <span>当前状态</span>
              <div class="state-line">
                <i aria-hidden="true"></i>
                <strong>{{ selectedTenant.statusText }}</strong>
              </div>
              <small>{{ selectedTenant.status === 'frozen' ? '三端业务入口已暂停' : '运营状态正常追踪' }}</small>
            </div>
            <div class="summary-cell"><span>试用到期</span><strong>{{ selectedTenant.trialEndAt }}</strong></div>
            <div class="summary-cell"><span>客服微信</span><strong>{{ selectedTenant.supportWechatId || '未配置' }}</strong></div>
          </div>

          <section class="drawer-section">
            <h3>门店资料</h3>
            <div class="field-grid drawer-fields">
              <div class="field-box"><span>联系电话</span><strong>{{ selectedTenant.store.contactPhone }}</strong></div>
              <div class="field-box"><span>营业时间</span><strong>{{ selectedTenant.store.businessHours }}</strong></div>
              <div class="field-box wide"><span>门店地址</span><strong>{{ selectedTenant.store.address }}</strong></div>
              <div class="field-box wide"><span>经营项目 / 服务标签</span><strong>{{ selectedTenant.store.businessCategories }} / {{ selectedTenant.store.serviceTags }}</strong></div>
            </div>
          </section>

          <section class="drawer-section">
            <h3>试点信号</h3>
            <div class="drawer-metric-grid">
              <div class="drawer-metric"><span>发卡</span><strong>{{ selectedTenant.snapshot.issuedCards }}</strong></div>
              <div class="drawer-metric"><span>到店转核销</span><strong>{{ Math.round((selectedTenant.snapshot.deductions / Math.max(selectedTenant.snapshot.checkins, 1)) * 100) }}%</strong></div>
              <div class="drawer-metric"><span>预警未处理</span><strong>{{ selectedTenant.snapshot.warningCount }}</strong></div>
              <div class="drawer-metric"><span>线下售卡</span><strong>¥{{ Number(selectedTenant.snapshot.monthlySalesYuan).toFixed(0) }}</strong></div>
            </div>
          </section>

          <section v-if="selectedTenant.status === 'frozen' && selectedTenant.frozenReason" class="frozen-reason-card">
            <div class="frozen-reason-title">
              <span>冻结原因</span>
              <StatusTag status="frozen" text="已冻结" />
            </div>
            <p>{{ selectedTenant.frozenReason }}</p>
          </section>
        </template>
      </div>
    </el-drawer>

    <el-drawer v-model="adjustOpen" title="租户调整" size="min(760px, 94vw)">
      <div v-loading="detailLoading">
        <template v-if="selectedTenant">
          <div class="tenant-drawer-hero adjust-hero">
            <div class="tenant-avatar">{{ selectedTenant.name.slice(0, 1) }}</div>
            <div>
              <h2>{{ selectedTenant.name }}</h2>
              <p>冻结账户、延期并解冻都会写入平台审计记录。</p>
            </div>
          </div>

          <div class="tenant-summary-grid adjust-summary">
            <div class="summary-cell state-card" :class="`state-${selectedTenant.status.toLowerCase()}`">
              <span>当前状态</span>
              <div class="state-line">
                <i aria-hidden="true"></i>
                <strong>{{ selectedTenant.statusText }}</strong>
              </div>
              <small>{{ selectedTenant.status === 'frozen' ? '仅可延期并解冻' : '可冻结账户' }}</small>
            </div>
            <div class="summary-cell"><span>当前到期日</span><strong>{{ selectedTenant.trialEndAt }}</strong></div>
            <div class="summary-cell"><span>客服微信</span><strong>{{ selectedTenant.supportWechatId || '未配置' }}</strong></div>
          </div>

          <section v-if="selectedTenant.status !== 'frozen'" class="adjust-section freeze-section">
            <div class="adjust-section-title">
              <div>
                <h3>冻结账户</h3>
                <p>暂停店长、教练和会员端业务操作，历史数据保留。</p>
              </div>
              <StatusTag status="trialing" text="可操作" />
            </div>
            <el-form label-position="top">
              <el-form-item label="冻结原因（必填）">
                <el-input v-model="freezeForm.reason" placeholder="例如：试用到期未完成人工续期确认" />
              </el-form-item>
              <el-checkbox v-model="freezeForm.confirmed">确认暂停三端业务入口，历史预约、售卡和核销记录保留</el-checkbox>
            </el-form>
            <div class="adjust-actions">
              <span>提交后写入冻结审计记录。</span>
              <el-button type="danger" :loading="submitting" :disabled="!canFreeze" @click="submitFreeze">确认冻结</el-button>
            </div>
          </section>

          <section v-if="selectedTenant.status === 'frozen'" class="adjust-section extend-section">
            <div class="adjust-section-title">
              <div>
                <h3>延期并解冻</h3>
                <p>调整试用到期日，可同时恢复冻结租户的业务入口。</p>
              </div>
              <StatusTag status="extended" text="写入审计" />
            </div>
            <div v-if="selectedTenant.frozenReason" class="frozen-reason-mini">
              <span>当前冻结原因</span>
              <strong>{{ selectedTenant.frozenReason }}</strong>
            </div>
            <el-form label-position="top">
              <el-form-item label="新到期日（必填）">
                <el-date-picker
                  v-model="extendForm.newTrialEndAt"
                  type="date"
                  value-format="YYYY-MM-DD"
                  format="YYYY-MM-DD"
                  placeholder="选择新到期日"
                  popper-class="tenant-date-popper"
                  :teleported="false"
                  style="width: 100%"
                />
              </el-form-item>
              <el-form-item label="操作原因（必填）">
                <el-input v-model="extendForm.reason" placeholder="例如：客服确认人工续期，延长 30 天" />
              </el-form-item>
              <el-form-item label="内部备注">
                <el-input v-model="extendForm.internalNote" type="textarea" :rows="3" placeholder="例如：已通过客服微信确认，不涉及平台收款记录" />
              </el-form-item>
            </el-form>
            <div class="adjust-actions">
              <span>新到期日不得早于今天；保存后刷新列表并清除冻结原因。</span>
              <el-button type="primary" :loading="submitting" :disabled="!canExtend" @click="submitExtend">延期并解冻</el-button>
            </div>
          </section>
        </template>
      </div>
    </el-drawer>
  </section>
</template>

<style scoped>
.filter-input {
  width: 260px;
}

.filter-select {
  width: 160px;
}

.filter-select.wide {
  width: 190px;
}

.filter-submit {
  margin-left: auto;
  min-width: 86px;
}

.metric-grid {
  grid-template-columns: repeat(auto-fit, minmax(168px, 1fr));
}

.metric-card.frozen-metric::before {
  background: var(--coral);
}

.metric-card.extended-metric::before {
  background: var(--green);
}

.tenant-table {
  width: 100%;
  border-radius: 8px;
}

.tenant-table :deep(.el-table__header th) {
  background: #f8fbf9;
  color: var(--muted);
  font-size: 12px;
  font-weight: 950;
}

.tenant-table :deep(.el-table__cell) {
  padding: 11px 0;
}

.tenant-table :deep(.cell) {
  min-width: 0;
}

.tenant-name-cell {
  display: block;
  overflow: hidden;
  color: var(--ink);
  font-weight: 900;
  line-height: 1.45;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.wechat-id-cell {
  display: block;
  max-width: 100%;
  overflow: hidden;
  color: var(--ink);
  font-size: 13px;
  font-weight: 850;
  line-height: 1.45;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sales-amount-cell {
  display: inline-flex;
  min-width: 112px;
  min-height: 30px;
  align-items: center;
  justify-content: flex-start;
  border: 1px solid #f0d8b7;
  border-radius: 8px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.5), rgba(255, 246, 221, 0.78)),
    var(--gold-soft);
  color: #7d5516;
  padding: 0 10px;
  font-size: 13px;
  font-variant-numeric: tabular-nums;
  font-weight: 950;
  white-space: nowrap;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

.tenant-card-list {
  display: none;
}

.tenant-row-actions {
  display: inline-flex;
  align-items: center;
  justify-content: flex-start;
  gap: 8px;
  width: 100%;
}

.tenant-action-button.el-button {
  min-width: 74px;
  min-height: 34px;
  height: 34px;
  margin-left: 0;
  border-radius: 8px;
  padding: 0 11px;
  font-size: 13px;
  font-weight: 900;
  letter-spacing: 0;
}

.tenant-action-button :deep(.el-icon) {
  margin-right: 4px;
  font-size: 14px;
}

.tenant-action-button.el-button:not(.is-disabled):hover {
  transform: none;
}

.detail-action.el-button {
  --el-button-bg-color: #ffffff;
  --el-button-border-color: var(--line);
  --el-button-text-color: var(--muted);
  --el-button-hover-bg-color: var(--gray-soft);
  --el-button-hover-border-color: #b7cbc2;
  --el-button-hover-text-color: var(--green-dark);
  --el-button-active-bg-color: var(--green-soft);
  --el-button-active-border-color: var(--green);
  box-shadow: none;
}

.adjust-action.el-button {
  --el-button-bg-color: var(--green-soft);
  --el-button-border-color: #cfe3da;
  --el-button-text-color: var(--green-dark);
  --el-button-hover-bg-color: var(--green);
  --el-button-hover-border-color: var(--green);
  --el-button-hover-text-color: #ffffff;
  --el-button-active-bg-color: var(--green-dark);
  --el-button-active-border-color: var(--green-dark);
  box-shadow: 0 8px 18px rgba(47, 125, 110, 0.08);
}

.pagination-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 16px;
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.92), rgba(248, 250, 247, 0.78)),
    var(--surface);
  padding: 12px 14px;
  color: var(--muted);
  box-shadow: 0 10px 28px rgba(23, 33, 27, 0.04);
}

.pagination-meta {
  display: inline-flex;
  align-items: baseline;
  gap: 7px;
}

.pagination-meta strong {
  color: var(--green-dark);
  font-size: 20px;
  font-variant-numeric: tabular-nums;
}

.pagination-meta span {
  font-size: 13px;
  font-weight: 800;
}

.tenant-drawer-hero {
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  gap: 14px;
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background:
    linear-gradient(135deg, rgba(228, 241, 236, 0.92), rgba(255, 255, 255, 0.82)),
    var(--surface);
  padding: 18px;
  margin-bottom: 16px;
}

.tenant-drawer-hero::after {
  content: "";
  position: absolute;
  right: -42px;
  bottom: -74px;
  width: 180px;
  height: 180px;
  border-radius: 999px;
  background: rgba(47, 125, 110, 0.1);
}

.adjust-hero {
  background:
    linear-gradient(135deg, rgba(255, 246, 221, 0.72), rgba(255, 255, 255, 0.84)),
    var(--surface);
}

.tenant-avatar {
  position: relative;
  z-index: 1;
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  border-radius: 8px;
  background: var(--green);
  color: #fff;
  font-size: 20px;
  font-weight: 760;
  box-shadow: 0 12px 26px rgba(47, 125, 110, 0.2);
}

.tenant-drawer-hero > div:not(.tenant-avatar) {
  position: relative;
  z-index: 1;
  min-width: 0;
}

.tenant-drawer-hero h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 720;
  line-height: 1.3;
}

.tenant-drawer-hero p {
  margin: 6px 0 0;
  color: var(--muted);
  line-height: 1.5;
}

.tenant-summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.summary-cell {
  position: relative;
  overflow: hidden;
  display: grid;
  align-content: start;
  justify-items: start;
  min-height: 70px;
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 250, 247, 0.88)),
    var(--surface);
  padding: 12px 14px;
  box-shadow: 0 12px 28px rgba(23, 33, 27, 0.045);
}

.summary-cell:not(.state-card)::before {
  content: "";
  position: absolute;
  inset: 0 auto 0 0;
  width: 3px;
  background: var(--green);
}

.summary-cell span {
  position: relative;
  z-index: 1;
  display: block;
  margin-bottom: 8px;
  color: var(--muted);
  font-size: 12px;
  font-weight: 680;
}

.summary-cell strong {
  position: relative;
  z-index: 1;
  color: var(--ink);
  font-size: 15px;
  font-weight: 560;
  line-height: 1.45;
}

.state-card {
  position: relative;
  overflow: hidden;
  border-color: rgba(47, 125, 110, 0.18);
  background:
    linear-gradient(135deg, rgba(244, 249, 246, 0.98), rgba(255, 255, 255, 0.92)),
    var(--surface);
}

.state-card::after {
  content: "";
  position: absolute;
  right: -18px;
  top: -26px;
  width: 76px;
  height: 76px;
  border-radius: 50%;
  background: rgba(47, 125, 110, 0.08);
}

.state-card.state-frozen {
  border-color: rgba(232, 112, 82, 0.28);
  background:
    linear-gradient(135deg, rgba(255, 248, 244, 0.98), rgba(255, 255, 255, 0.94)),
    var(--surface);
}

.state-card.state-frozen::after {
  background: rgba(232, 112, 82, 0.1);
}

.state-card.state-expiring {
  border-color: rgba(208, 145, 42, 0.3);
  background:
    linear-gradient(135deg, rgba(255, 250, 239, 0.98), rgba(255, 255, 255, 0.94)),
    var(--surface);
}

.state-card.state-expiring::after {
  background: rgba(208, 145, 42, 0.1);
}

.state-line {
  position: relative;
  z-index: 1;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.state-line i {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: var(--green);
  box-shadow: 0 0 0 4px rgba(47, 125, 110, 0.12);
}

.state-frozen .state-line i {
  background: var(--coral);
  box-shadow: 0 0 0 4px rgba(232, 112, 82, 0.12);
}

.state-expiring .state-line i {
  background: #d0912a;
  box-shadow: 0 0 0 4px rgba(208, 145, 42, 0.13);
}

.state-line strong {
  font-size: 18px;
  font-weight: 720;
  letter-spacing: 0;
}

.state-card small {
  position: relative;
  z-index: 1;
  display: block;
  margin-top: 7px;
  color: var(--muted);
  font-size: 12px;
  font-weight: 500;
  line-height: 1.4;
}

.drawer-section {
  margin-top: 16px;
}

.drawer-section h3 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 12px;
  font-size: 16px;
  font-weight: 720;
}

.drawer-section h3::before {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: var(--green);
  box-shadow: 0 0 0 5px rgba(47, 125, 110, 0.1);
  content: "";
}

.drawer-fields {
  margin-bottom: 0;
}

.drawer-fields .field-box {
  position: relative;
  overflow: hidden;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 250, 247, 0.86)),
    var(--surface);
  box-shadow: 0 10px 24px rgba(23, 33, 27, 0.035);
}

.drawer-fields .field-box::before {
  content: "";
  position: absolute;
  inset: 0 auto 0 0;
  width: 3px;
  background: var(--green);
}

.drawer-fields .field-box span,
.drawer-fields .field-box strong {
  position: relative;
  z-index: 1;
}

.drawer-fields .field-box span {
  font-weight: 680;
}

.drawer-fields .field-box strong {
  color: var(--ink);
  font-weight: 500;
  line-height: 1.62;
}

.drawer-metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.drawer-metric {
  position: relative;
  overflow: hidden;
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 250, 247, 0.86)),
    var(--surface);
  padding: 12px;
  box-shadow: 0 10px 24px rgba(23, 33, 27, 0.035);
}

.drawer-metric::before {
  content: "";
  position: absolute;
  inset: 0 auto 0 0;
  width: 3px;
  background: var(--green);
}

.drawer-metric:nth-child(2)::before {
  background: var(--blue);
}

.drawer-metric:nth-child(3)::before {
  background: var(--gold);
}

.drawer-metric:nth-child(4)::before {
  background: var(--coral);
}

.drawer-metric span {
  position: relative;
  z-index: 1;
  color: var(--muted);
  font-size: 12px;
  font-weight: 680;
}

.drawer-metric strong {
  position: relative;
  z-index: 1;
  display: block;
  margin-top: 8px;
  color: var(--ink);
  font-size: 22px;
  font-weight: 680;
  font-variant-numeric: tabular-nums;
}

.drawer-note {
  margin-top: 16px;
}

.frozen-reason-card {
  position: relative;
  overflow: hidden;
  margin-top: 16px;
  border: 1px solid rgba(232, 112, 82, 0.22);
  border-radius: 8px;
  background:
    linear-gradient(135deg, rgba(255, 240, 234, 0.92), rgba(255, 255, 255, 0.92)),
    var(--surface);
  padding: 16px;
  box-shadow: 0 12px 34px rgba(232, 112, 82, 0.08);
}

.frozen-reason-card::before {
  content: "";
  position: absolute;
  inset: 0 auto 0 0;
  width: 4px;
  background: var(--coral);
}

.frozen-reason-card::after {
  content: "";
  position: absolute;
  right: -34px;
  top: -42px;
  width: 112px;
  height: 112px;
  border-radius: 999px;
  background: rgba(232, 112, 82, 0.08);
}

.frozen-reason-title {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.frozen-reason-title span,
.frozen-reason-mini span {
  color: var(--red);
  font-size: 12px;
  font-weight: 700;
}

.frozen-reason-card p {
  position: relative;
  z-index: 1;
  margin: 0;
  color: var(--ink);
  font-size: 15px;
  font-weight: 500;
  line-height: 1.7;
  word-break: break-word;
}

.adjust-section {
  position: relative;
  overflow: hidden;
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 250, 247, 0.9)),
    var(--surface);
  padding: 16px;
  box-shadow: 0 12px 34px rgba(23, 33, 27, 0.05);
}

.adjust-section::before {
  content: "";
  position: absolute;
  inset: 0 auto 0 0;
  width: 4px;
  background: var(--green);
}

.adjust-section::after {
  content: "";
  position: absolute;
  right: -42px;
  top: -58px;
  width: 136px;
  height: 136px;
  border-radius: 999px;
  background: rgba(47, 125, 110, 0.07);
}

.freeze-section {
  border-color: rgba(232, 112, 82, 0.24);
}

.freeze-section::before {
  background: var(--coral);
}

.freeze-section::after {
  background: rgba(232, 112, 82, 0.08);
}

.extend-section {
  border-color: rgba(47, 125, 110, 0.2);
}

.adjust-section + .adjust-section {
  margin-top: 14px;
}

.adjust-section-title {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 14px;
}

.adjust-section-title h3 {
  margin: 0;
  font-size: 17px;
  font-weight: 720;
}

.adjust-section-title p {
  margin: 6px 0 0;
  color: var(--muted);
  line-height: 1.6;
}

.adjust-section .el-form,
.adjust-section .el-checkbox,
.adjust-actions,
.frozen-reason-mini {
  position: relative;
  z-index: 1;
}

.adjust-section .el-form {
  padding: 14px;
  border: 1px solid rgba(214, 230, 222, 0.82);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.58);
}

.adjust-section :deep(.el-form-item__label) {
  color: var(--ink);
  font-weight: 680;
}

.adjust-section :deep(.el-input__wrapper),
.adjust-section :deep(.el-textarea__inner) {
  border-radius: 8px;
  box-shadow: 0 0 0 1px #dbe8e2 inset;
}

.adjust-section :deep(.el-input__wrapper.is-focus),
.adjust-section :deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 1px var(--green) inset, 0 0 0 4px rgba(47, 125, 110, 0.1);
}

.frozen-reason-mini {
  display: grid;
  gap: 7px;
  margin-bottom: 14px;
  border: 1px solid rgba(232, 112, 82, 0.18);
  border-radius: 8px;
  background: rgba(255, 240, 234, 0.7);
  padding: 12px;
}

.frozen-reason-mini strong {
  color: var(--ink);
  font-weight: 500;
  line-height: 1.55;
  word-break: break-word;
}

.adjust-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 14px;
  color: var(--muted);
  font-size: 13px;
  font-weight: 500;
}

.adjust-actions .el-button {
  flex: 0 0 auto;
}

@media (max-width: 980px) {
  .filter-bar {
    display: grid;
    grid-template-columns: 1fr 1fr;
  }

  .filter-input,
  .filter-select,
  .filter-select.wide {
    width: 100%;
  }

  .filter-submit {
    grid-column: 1 / -1;
    justify-self: end;
    margin-left: 0;
  }

}

@media (max-width: 760px) {
  .table-shell {
    display: none;
  }

  .tenant-card-list {
    display: grid;
    gap: 12px;
  }

  .tenant-mobile-card {
    border: 1px solid var(--line-soft);
    border-radius: 8px;
    background: var(--surface);
    padding: 14px;
    box-shadow: 0 12px 32px rgba(23, 33, 27, 0.06);
  }

  .tenant-mobile-card > div:first-child {
    display: flex;
    flex-direction: column;
    gap: 5px;
    margin-bottom: 10px;
  }

  .tenant-mobile-card strong {
    font-size: 16px;
  }

  .tenant-mobile-card small,
  .tenant-mobile-card p {
    color: var(--muted);
  }

  .tenant-mobile-card p {
    margin: 12px 0;
    line-height: 1.6;
  }

  .mobile-actions {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px;
  }

  .mobile-actions .el-button {
    width: 100%;
    padding: 0 8px;
  }

  .pagination-row {
    display: grid;
    gap: 10px;
  }

  .pagination-row .el-pagination {
    justify-content: center;
  }

  .tenant-summary-grid,
  .drawer-metric-grid {
    grid-template-columns: 1fr;
  }

  .adjust-section-title,
  .adjust-actions {
    display: grid;
    gap: 10px;
  }

  .adjust-actions .el-button {
    width: 100%;
  }
}
</style>
