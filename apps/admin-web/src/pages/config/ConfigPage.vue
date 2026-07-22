<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { QuestionFilled } from '@element-plus/icons-vue'
import StatusTag from '@/components/StatusTag.vue'
import { adminApi } from '@/services/api'
import type { PlatformConfig } from '@/types/admin'

const loading = ref(false)
const saving = ref(false)
const configs = ref<PlatformConfig[]>([])
const keyword = ref('')
const editableFilter = ref('ALL')
const editing = ref<PlatformConfig | null>(null)
const drawerOpen = ref(false)
const rulesOpen = ref(false)
const editForm = reactive({
  newValue: '',
  reason: ''
})

const canSave = computed(() => !!editing.value && !!editForm.newValue.trim() && !!editForm.reason.trim() && !saving.value)

async function load() {
  loading.value = true
  try {
    configs.value = await adminApi.listConfigs({ keyword: keyword.value, editableOnly: editableFilter.value === 'EDITABLE' })
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '平台配置加载失败')
  } finally {
    loading.value = false
  }
}

function startEdit(row: PlatformConfig) {
  editing.value = row
  editForm.newValue = row.valueText
  editForm.reason = ''
  drawerOpen.value = true
}

async function save() {
  if (!editing.value || !canSave.value) return
  saving.value = true
  try {
    const updated = await adminApi.updateConfig(editing.value.configKey, editForm)
    const index = configs.value.findIndex((item) => item.configKey === updated.configKey)
    if (index >= 0) configs.value[index] = updated
    ElMessage.success('配置已保存，列表当前行已刷新')
    drawerOpen.value = false
    editing.value = null
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <section>
    <div class="page-title">
      <div>
        <h1>平台配置</h1>
        <p>配置新租户默认试用天数、到期提醒、邀请码有效期、取消截止和会员卡提醒阈值。修改只影响之后的新业务流程。</p>
      </div>
      <el-tooltip content="查看配置生效规则" placement="bottom">
        <el-button class="help-button" circle :icon="QuestionFilled" @click="rulesOpen = true" />
      </el-tooltip>
    </div>

    <div class="filter-bar">
      <el-input v-model="keyword" class="config-keyword" placeholder="搜索配置键或名称" clearable @keyup.enter="load" />
      <el-select v-model="editableFilter" class="config-filter">
        <el-option label="可编辑：全部" value="ALL" />
        <el-option label="只看可编辑" value="EDITABLE" />
      </el-select>
      <el-button class="config-search" type="primary" @click="load">查询</el-button>
    </div>

    <div class="admin-card table-shell">
      <el-table v-loading="loading" :data="configs" border>
        <el-table-column prop="name" label="配置项" min-width="190" />
        <el-table-column prop="configKey" label="配置键" min-width="250" />
        <el-table-column label="当前值" width="120">
          <template #default="{ row }">{{ row.valueText }} {{ row.unit }}</template>
        </el-table-column>
        <el-table-column prop="impactScope" label="影响范围" min-width="210" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }"><StatusTag :status="row.enabled ? 'trialing' : 'frozen'" :text="row.enabled ? '启用' : '停用'" /></template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button type="primary" :disabled="!row.editable" @click="startEdit(row)">修改</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div v-if="!loading && configs.length === 0" class="empty-state">
      <div>
        <h3>没有匹配的配置项</h3>
        <p>当前搜索条件没有配置结果。可以清空搜索词或切换可编辑筛选；平台配置不会批量改写已有租户、邀请码或预约时段。</p>
      </div>
    </div>

    <el-drawer v-model="drawerOpen" title="修改配置" size="min(640px, 92vw)" :close-on-click-modal="false">
      <template v-if="editing">
        <div class="config-edit-summary">
          <div>
            <span class="summary-label">正在修改</span>
            <h3>{{ editing.name }}</h3>
            <p>{{ editing.impactScope }}</p>
          </div>
          <div class="summary-value">
            <span>当前值</span>
            <strong>{{ editing.valueText }}{{ editing.unit ? ` ${editing.unit}` : '' }}</strong>
          </div>
        </div>
        <div class="config-meta-list">
          <div class="config-meta-item">
            <span>配置键</span>
            <code>{{ editing.configKey }}</code>
          </div>
          <div class="config-meta-item">
            <span>类型</span>
            <p>{{ editing.valueType }}</p>
          </div>
          <div class="config-meta-item">
            <span>单位</span>
            <p>{{ editing.unit || '无' }}</p>
          </div>
        </div>
        <div class="edit-panel">
          <div class="edit-panel-title">
            <span></span>
            <strong>填写变更内容</strong>
          </div>
          <el-form label-position="top" class="edit-form">
            <el-form-item>
              <template #label><span class="required-label">新值</span></template>
              <el-input v-model="editForm.newValue" />
            </el-form-item>
            <el-form-item>
              <template #label><span class="required-label">操作原因</span></template>
              <el-input v-model="editForm.reason" type="textarea" :rows="3" placeholder="每次修改必须填写可追溯原因" />
            </el-form-item>
          </el-form>
        </div>
      </template>
      <template #footer>
        <div class="drawer-actions">
          <el-button @click="drawerOpen = false">取消</el-button>
          <el-button type="primary" :loading="saving" :disabled="!canSave" @click="save">保存配置</el-button>
        </div>
      </template>
    </el-drawer>

    <el-dialog v-model="rulesOpen" title="生效规则" width="560">
      <div class="rules-list">
        <div class="step-card"><b>新</b><div><strong>只影响后续流程</strong><small>不会批量改写已有租户到期日或已生成邀请码。</small></div><StatusTag status="extended" text="后续" /></div>
        <div class="step-card"><b>验</b><div><strong>值校验</strong><small>整数、分钟、天数等配置按后端规则校验。</small></div><StatusTag status="expiring" text="必检" /></div>
        <div class="step-card"><b>因</b><div><strong>原因必填</strong><small>每次修改必须填写可追溯原因。</small></div><StatusTag status="expiring" text="必填" /></div>
        <div class="step-card"><b>审</b><div><strong>审计记录</strong><small>记录配置键、旧值、新值、操作人和原因。</small></div><StatusTag status="extended" text="写入" /></div>
      </div>
    </el-dialog>
  </section>
</template>

<style scoped>
.help-button {
  justify-self: end;
  width: 34px;
  height: 34px;
  min-height: 34px;
  color: var(--green-dark);
  background: var(--green-soft);
  border-color: #cfe3da;
}

.config-keyword {
  width: 280px;
}

.config-filter {
  width: 170px;
}

.config-search {
  margin-left: auto;
  min-width: 86px;
}

.config-edit-summary {
  position: relative;
  overflow: hidden;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 18px;
  align-items: start;
  padding: 18px 18px 18px 20px;
  border: 1px solid #dcebe4;
  border-radius: 8px;
  background:
    radial-gradient(circle at 100% 0%, rgba(72, 145, 127, 0.12), transparent 34%),
    linear-gradient(135deg, #f6fbf8 0%, #ffffff 78%);
  box-shadow: 0 12px 34px rgba(32, 80, 67, 0.08);
}

.config-edit-summary::before {
  position: absolute;
  inset: 14px auto 14px 0;
  width: 4px;
  border-radius: 999px;
  background: linear-gradient(180deg, var(--green) 0%, #9fd2c4 100%);
  content: "";
}

.summary-label,
.summary-value span,
.config-meta-item span {
  display: block;
  color: var(--muted);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0;
}

.config-edit-summary h3 {
  margin: 8px 0 8px;
  color: var(--ink);
  font-size: 20px;
  font-weight: 700;
  line-height: 1.35;
}

.config-edit-summary p {
  margin: 0;
  color: var(--muted);
  font-size: 14px;
  line-height: 1.65;
}

.summary-value {
  min-width: 132px;
  padding: 12px 14px;
  border: 1px solid #cfe3da;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.76);
}

.summary-value strong {
  display: block;
  margin-top: 8px;
  color: var(--green-dark);
  font-size: 20px;
  font-weight: 680;
  line-height: 1.2;
  word-break: break-word;
}

.config-meta-list {
  display: grid;
  grid-template-columns: minmax(0, 1.8fr) minmax(0, 0.8fr) minmax(0, 0.7fr);
  gap: 10px;
  margin-top: 12px;
}

.config-meta-item {
  min-width: 0;
  padding: 12px 14px;
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.72);
  box-shadow: 0 8px 22px rgba(37, 76, 65, 0.05);
}

.config-meta-item:hover {
  border-color: #cfe3da;
  background: #fff;
}

.config-meta-item code,
.config-meta-item p {
  display: block;
  margin: 7px 0 0;
  color: var(--ink);
  font-size: 14px;
  font-weight: 500;
  line-height: 1.45;
  word-break: break-word;
}

.config-meta-item code {
  font-family: "SFMono-Regular", Consolas, "Liberation Mono", monospace;
}

.edit-panel {
  margin-top: 18px;
  padding: 16px;
  border: 1px solid #dcebe4;
  border-radius: 8px;
  background: linear-gradient(180deg, rgba(247, 251, 248, 0.78), rgba(255, 255, 255, 0.94));
}

.edit-panel-title {
  display: flex;
  align-items: center;
  gap: 9px;
  margin-bottom: 14px;
  color: var(--ink);
}

.edit-panel-title span {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: var(--green);
  box-shadow: 0 0 0 5px rgba(72, 145, 127, 0.12);
}

.edit-panel-title strong {
  font-size: 14px;
  font-weight: 720;
}

.edit-form :deep(.el-form-item:last-child) {
  margin-bottom: 0;
}

.edit-form :deep(.el-input__wrapper),
.edit-form :deep(.el-textarea__inner) {
  border-radius: 8px;
  box-shadow: 0 0 0 1px #dbe8e2 inset;
}

.edit-form :deep(.el-input__wrapper.is-focus),
.edit-form :deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 1px var(--green) inset, 0 0 0 4px rgba(72, 145, 127, 0.12);
}

.required-label::before {
  content: "*";
  margin-right: 4px;
  color: var(--coral);
  font-weight: 950;
}

.rules-list {
  display: grid;
  gap: 10px;
}

.drawer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

@media (max-width: 760px) {
  .help-button {
    justify-self: start;
  }

  .config-keyword,
  .config-filter {
    width: 100%;
  }

  .config-search {
    margin-left: 0;
  }

  .config-edit-summary,
  .config-meta-list {
    grid-template-columns: 1fr;
  }

  .summary-value {
    width: 100%;
  }
}
</style>
