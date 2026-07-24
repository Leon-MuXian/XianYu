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
        <p>查看租户试用状态、客服微信、线下售卡和到期风险，及时处理即将到期与冻结租户。</p>
      </div>
    </div>

    <div v-if="stats" class="metric-grid tenant-metrics" aria-label="租户状态概览">
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
      <el-table v-loading="loading" :data="rows" border class="tenant-table" empty-text="暂无数据">
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
      <p v-if="!loading && rows.length === 0" class="tenant-mobile-empty">暂无数据</p>
    </div>

    <div v-if="data && data.page.total > 0" class="pagination-row">
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

    <el-drawer
      v-model="detailOpen"
      title="租户详情"
      size="min(720px, 92vw)"
      :close-on-click-modal="true"
      :close-on-press-escape="true"
    >
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

    <el-drawer
      v-model="adjustOpen"
      title="租户调整"
      size="min(760px, 94vw)"
      :close-on-click-modal="true"
      :close-on-press-escape="true"
    >
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
.tenant-metrics .metric-card {
  min-height: 78px;
  background: #fff;
  padding: 14px 18px;
}

.tenant-metrics .metric-card::before {
  display: none;
}

.tenant-metrics .metric-card span {
  color: #637286;
  font-weight: 650;
}

.tenant-metrics .metric-card strong {
  margin-top: 9px;
  font-size: 23px;
  font-weight: 750;
}

.filter-input {
  flex: 1 1 360px;
  min-width: 280px;
}

.filter-select {
  width: 160px;
}

.filter-select.wide {
  width: 180px;
}

.filter-submit {
  min-width: 84px;
  margin-left: auto;
}

.tenant-table {
  min-width: 1030px;
}

.tenant-name-cell {
  display: block;
  overflow: hidden;
  color: var(--ink);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.wechat-id-cell {
  display: block;
  overflow: hidden;
  color: #405469;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sales-amount-cell {
  color: var(--ink);
  font-weight: 800;
  font-variant-numeric: tabular-nums;
}

.tenant-row-actions,
.mobile-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.tenant-action-button {
  min-height: 34px;
  margin: 0;
  padding: 0 10px;
}

.detail-action {
  border-color: #b8cde0;
  background: var(--blue-soft);
  color: var(--green-dark);
}

.adjust-action {
  background: #fff;
  color: var(--ink);
}

.tenant-card-list {
  display: none;
}

.tenant-mobile-empty {
  margin: 0;
  border: 1px solid var(--line-soft);
  border-radius: 5px;
  background: #fff;
  padding: 28px 16px;
  color: var(--muted);
  text-align: center;
}

.pagination-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  min-height: 58px;
  margin-top: 12px;
  color: var(--muted);
  font-size: 12px;
}

.pagination-meta {
  display: flex;
  align-items: baseline;
  gap: 5px;
}

.pagination-meta strong {
  color: var(--ink);
  font-size: 17px;
}

.tenant-drawer-hero {
  display: grid;
  grid-template-columns: 46px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
  border: 1px solid var(--line-soft);
  border-radius: 4px;
  background: #fff;
  padding: 14px;
}

.tenant-avatar {
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  border: 1px solid #b8cde0;
  border-radius: 4px;
  background: var(--blue-soft);
  color: var(--green-dark);
  font-size: 18px;
  font-weight: 900;
}

.tenant-drawer-hero h2 {
  margin: 0;
  color: var(--ink);
  font-size: 18px;
}

.tenant-drawer-hero p {
  margin: 5px 0 0;
  color: var(--muted);
  font-size: 12px;
  line-height: 1.5;
}

.tenant-summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  overflow: hidden;
  border: 1px solid var(--line-soft);
  border-radius: 4px;
  background: #fff;
  margin-top: 12px;
}

.summary-cell {
  min-width: 0;
  min-height: 88px;
  border-right: 1px solid var(--line-soft);
  padding: 14px;
}

.summary-cell:last-child {
  border-right: 0;
}

.summary-cell > span,
.summary-cell > strong,
.summary-cell > small {
  display: block;
}

.summary-cell > span {
  color: var(--muted);
  font-size: 11px;
  font-weight: 700;
}

.summary-cell > strong {
  margin-top: 10px;
  overflow: hidden;
  color: var(--ink);
  font-size: 16px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.summary-cell > small {
  margin-top: 7px;
  color: var(--muted);
  font-size: 10px;
}

.state-line {
  display: flex;
  align-items: center;
  gap: 7px;
  margin-top: 10px;
}

.state-line i {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--positive);
}

.state-line strong {
  color: var(--positive);
  font-size: 16px;
}

.state-frozen .state-line i {
  background: var(--coral);
}

.state-frozen .state-line strong {
  color: var(--coral);
}

.state-expiring .state-line i {
  background: var(--gold);
}

.state-expiring .state-line strong {
  color: var(--gold);
}

.drawer-section,
.adjust-section {
  border: 1px solid var(--line-soft);
  border-radius: 4px;
  background: #fff;
  margin-top: 12px;
  padding: 16px;
}

.drawer-section > h3,
.adjust-section h3 {
  margin: 0 0 13px;
  color: var(--ink);
  font-size: 15px;
}

.drawer-fields .field-box {
  background: var(--surface-soft);
}

.drawer-metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  overflow: hidden;
  border: 1px solid var(--line-soft);
  border-radius: 4px;
}

.drawer-metric {
  min-width: 0;
  border-right: 1px solid var(--line-soft);
  padding: 12px;
}

.drawer-metric:last-child {
  border-right: 0;
}

.drawer-metric span,
.drawer-metric strong {
  display: block;
}

.drawer-metric span {
  color: var(--muted);
  font-size: 11px;
}

.drawer-metric strong {
  margin-top: 7px;
  color: var(--ink);
  font-size: 20px;
  font-variant-numeric: tabular-nums;
}

.frozen-reason-card,
.frozen-reason-mini {
  border-left: 3px solid var(--coral);
  background: var(--coral-soft);
  margin-top: 12px;
  padding: 12px 14px;
}

.frozen-reason-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: var(--coral);
  font-size: 12px;
  font-weight: 800;
}

.frozen-reason-card p {
  margin: 9px 0 0;
  color: #75413f;
  line-height: 1.55;
}

.adjust-hero {
  grid-template-columns: 46px minmax(0, 1fr);
}

.adjust-section-title {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid var(--line-soft);
  margin: -2px 0 16px;
  padding-bottom: 13px;
}

.adjust-section-title h3 {
  margin-bottom: 5px;
}

.adjust-section-title p {
  margin: 0;
  color: var(--muted);
  font-size: 12px;
  line-height: 1.55;
}

.adjust-section :deep(.el-form-item:last-child) {
  margin-bottom: 10px;
}

.adjust-section :deep(.el-checkbox) {
  height: auto;
  align-items: flex-start;
  white-space: normal;
}

.adjust-section :deep(.el-checkbox__label) {
  line-height: 1.55;
}

.adjust-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  border-top: 1px solid var(--line-soft);
  margin-top: 16px;
  padding-top: 14px;
}

.adjust-actions > span {
  color: var(--muted);
  font-size: 11px;
  line-height: 1.55;
}

.frozen-reason-mini {
  margin: 0 0 16px;
}

.frozen-reason-mini span,
.frozen-reason-mini strong {
  display: block;
}

.frozen-reason-mini span {
  color: var(--coral);
  font-size: 11px;
  font-weight: 700;
}

.frozen-reason-mini strong {
  margin-top: 6px;
  color: #75413f;
  line-height: 1.5;
}

@media (max-width: 1180px) {
  .filter-input {
    flex-basis: 100%;
  }

  .filter-submit {
    margin-left: 0;
  }

  .drawer-metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .drawer-metric:nth-child(2) {
    border-right: 0;
  }

  .drawer-metric:nth-child(n + 3) {
    border-top: 1px solid var(--line-soft);
  }
}

@media (max-width: 760px) {
  .filter-input,
  .filter-select,
  .filter-select.wide,
  .filter-submit {
    width: 100%;
    min-width: 0;
  }

  .table-shell {
    display: none;
  }

  .tenant-card-list {
    display: grid;
    gap: 10px;
  }

  .tenant-mobile-card {
    display: grid;
    grid-template-columns: minmax(0, 1fr) auto;
    gap: 10px;
    border: 1px solid var(--line-soft);
    border-radius: 5px;
    background: #fff;
    padding: 14px;
  }

  .tenant-mobile-card strong,
  .tenant-mobile-card small {
    display: block;
  }

  .tenant-mobile-card small {
    margin-top: 5px;
    color: var(--muted);
  }

  .tenant-mobile-card p,
  .tenant-mobile-card .mobile-actions {
    grid-column: 1 / -1;
  }

  .tenant-mobile-card p {
    border-top: 1px solid var(--line-soft);
    margin: 2px 0 0;
    padding-top: 10px;
    color: var(--muted);
    font-size: 12px;
  }

  .tenant-action-button {
    flex: 1;
  }

  .pagination-row {
    align-items: stretch;
    flex-direction: column;
  }

  .pagination-row .el-pagination {
    justify-content: center;
  }

  .tenant-summary-grid,
  .drawer-metric-grid {
    grid-template-columns: 1fr;
  }

  .summary-cell,
  .drawer-metric,
  .drawer-metric:nth-child(2) {
    border-right: 0;
    border-bottom: 1px solid var(--line-soft);
  }

  .summary-cell:last-child,
  .drawer-metric:last-child {
    border-bottom: 0;
  }

  .adjust-section-title,
  .adjust-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .adjust-actions .el-button {
    width: 100%;
  }
}
</style>
