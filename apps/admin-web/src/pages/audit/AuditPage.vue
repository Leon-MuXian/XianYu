<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '@/services/api'
import type { AuditLogItem, PageResponse } from '@/types/admin'

const loading = ref(false)
const data = ref<PageResponse<AuditLogItem> | null>(null)
const filters = reactive({
  keyword: '',
  action: 'ALL',
  range: 'THIS_MONTH',
  page: 1,
  pageSize: 10
})

const rows = computed(() => data.value?.items || [])

const actionLabels: Record<string, string> = {
  AUTO_TRIAL_CREATED: '自动试用开通',
  FREEZE_TENANT: '冻结租户',
  EXTEND_TRIAL: '调整期限并解冻',
  UPDATE_SUPPORT_WECHAT: '保存客服微信',
  UPDATE_CONFIG: '修改平台配置'
}

async function load() {
  loading.value = true
  try {
    data.value = await adminApi.listAuditLogs(filters)
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '操作记录加载失败')
  } finally {
    loading.value = false
  }
}

function query() {
  filters.page = 1
  load()
}

function formatDateTime(value: string) {
  if (!value) return '-'
  return value.replace('T', ' ').replace(/\.\d+$/, '').slice(0, 19)
}

onMounted(load)
</script>

<template>
  <section>
    <div class="page-title">
      <div>
        <h1>操作记录</h1>
        <p>记录自动试用开通、冻结、延期并解冻、客服微信和平台配置变更。</p>
      </div>
    </div>

    <div class="filter-bar">
      <el-input v-model="filters.keyword" class="audit-keyword" placeholder="搜索操作人、租户名称" clearable @keyup.enter="query" />
      <el-select v-model="filters.action" class="audit-action">
        <el-option label="动作：全部" value="ALL" />
        <el-option v-for="(label, value) in actionLabels" :key="value" :label="label" :value="value" />
      </el-select>
      <el-select v-model="filters.range" class="audit-range">
        <el-option label="本月" value="THIS_MONTH" />
        <el-option label="近 7 天" value="LAST_7_DAYS" />
        <el-option label="全部" value="ALL" />
      </el-select>
      <el-button class="audit-search-button" type="primary" @click="query">查询</el-button>
    </div>

    <div class="admin-card table-card">
      <h2>记录列表</h2>
      <el-table v-loading="loading" :data="rows" border empty-text=" ">
        <el-table-column label="动作" min-width="210">
          <template #default="{ row }"><strong>{{ actionLabels[row.action] || row.action }} · {{ row.targetName }}</strong></template>
        </el-table-column>
        <el-table-column prop="actorName" label="操作人/来源" width="190" />
        <el-table-column label="时间" width="190">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="旧值 → 新值" min-width="220">
          <template #default="{ row }">{{ row.oldValue || '无' }} → {{ row.newValue || '无' }}</template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" min-width="260" />
      </el-table>

      <div v-if="!loading && rows.length === 0" class="empty-state">
        <div>
          <h3>没有匹配的操作记录</h3>
          <p>当前筛选条件下没有审计结果。可以切换动作或时间范围；冻结、延期并解冻和配置修改都会写入这里。</p>
        </div>
      </div>

      <div v-if="data" class="pagination-row">
        <span>共 {{ data.total }} 条记录，第 {{ data.page }} 页</span>
        <el-pagination
          v-model:current-page="filters.page"
          layout="prev, pager, next"
          :page-size="filters.pageSize"
          :total="data.total"
          @current-change="load"
        />
      </div>
    </div>
  </section>
</template>

<style scoped>
.audit-keyword {
  width: 280px;
}

.audit-action {
  width: 190px;
}

.audit-range {
  width: 150px;
}

.audit-search-button {
  margin-left: auto;
  min-width: 86px;
}

.pagination-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  color: var(--muted);
  font-size: 12px;
}

.table-card {
  overflow: hidden;
  padding: 0;
}

.table-card > h2 {
  display: flex;
  align-items: center;
  min-height: 52px;
  border-bottom: 1px solid var(--line-soft);
  margin: 0;
  padding: 0 16px;
}

.table-card :deep(.el-table) {
  border: 0;
  border-radius: 0;
}

.table-card .empty-state {
  border: 0;
  border-radius: 0;
}

@media (max-width: 760px) {
  .audit-keyword,
  .audit-action,
  .audit-range {
    width: 100%;
  }

  .audit-search-button {
    margin-left: 0;
  }
}
</style>
