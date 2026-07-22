<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ChatDotRound, Check, Close, DocumentChecked, Phone, User } from '@element-plus/icons-vue'
import StatusTag from '@/components/StatusTag.vue'
import { adminApi } from '@/services/api'

const loading = ref(false)
const saving = ref(false)
const form = reactive({
  wechatId: '',
  displayText: '',
  displayScope: '',
  enabled: true,
  reason: ''
})

const canSave = computed(() => !!form.wechatId.trim() && !!form.displayText.trim() && !!form.displayScope.trim() && !!form.reason.trim())
const statusText = computed(() => (form.enabled ? '启用展示' : '停用展示'))
const saveText = computed(() => (form.enabled ? '保存客服微信' : '保存停用状态'))

async function load() {
  loading.value = true
  try {
    const data = await adminApi.getSupportWechat()
    form.wechatId = data.wechatId
    form.displayText = data.displayText
    form.displayScope = data.displayScope
    form.enabled = data.enabled
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '客服微信加载失败')
  } finally {
    loading.value = false
  }
}

async function save(enabled = form.enabled) {
  if (!canSave.value) return
  saving.value = true
  try {
    const data = await adminApi.updateSupportWechat({ ...form, enabled })
    form.enabled = data.enabled
    ElMessage.success('客服微信配置已保存，操作记录已写入')
    form.reason = ''
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <section v-loading="loading" class="support-page">
    <div class="page-title">
      <div>
        <h1>客服微信配置</h1>
        <p>维护冻结页的默认客服微信文本，运营只需要确认三件事：展示什么、展示给谁、为什么修改。</p>
      </div>
    </div>

    <div class="admin-card support-workbench">
      <div class="workbench-head">
        <div>
          <h2>客服微信配置</h2>
          <p>未配置租户客服时，冻结页会读取这里的默认值。</p>
        </div>
      </div>

      <section class="editor-pane">
        <el-form label-position="top" class="support-form">
          <div class="config-grid">
            <el-form-item class="wechat-id-field">
              <template #label><span class="required-label">客服微信 ID</span></template>
              <el-input v-model="form.wechatId" placeholder="例如：SerenMeet-CS">
                <template #prefix><el-icon><ChatDotRound /></el-icon></template>
              </el-input>
            </el-form-item>

            <el-form-item label="展示状态" class="status-field">
              <div class="inline-status" :class="{ disabled: !form.enabled }">
                <el-icon><component :is="form.enabled ? Check : Close" /></el-icon>
                <strong>{{ statusText }}</strong>
                <el-switch v-model="form.enabled" active-text="启用" inactive-text="停用" />
              </div>
            </el-form-item>

            <el-form-item class="copy-field">
              <template #label><span class="required-label">展示文案</span></template>
              <el-input
                v-model="form.displayText"
                type="textarea"
                :rows="2"
                maxlength="80"
                show-word-limit
                placeholder="例如：添加客服后，由平台人工调整使用期限并解冻。"
              />
            </el-form-item>

            <el-form-item class="scope-field">
              <template #label><span class="required-label">展示范围</span></template>
              <el-input
                v-model="form.displayScope"
                type="textarea"
                :rows="2"
                maxlength="120"
                show-word-limit
                placeholder="例如：店长冻结页展示客服微信；教练和会员端提示联系门店。"
              />
            </el-form-item>
          </div>
        </el-form>

        <div class="audit-row">
          <div class="audit-copy">
            <el-icon><DocumentChecked /></el-icon>
            <div>
                <strong><span class="required-label">操作原因</span></strong>
              <span>保存后写入操作记录，用于追溯配置变更。</span>
            </div>
          </div>
          <el-input v-model="form.reason" placeholder="例如：更新冻结页默认客服微信文本" />
        </div>
      </section>

      <div class="reference-row">
        <section class="reference-panel preview-section">
          <div class="side-card-head">
            <h2>冻结页预览</h2>
            <StatusTag :status="form.enabled ? 'extended' : 'frozen'" :text="form.enabled ? '展示中' : '已停用'" />
          </div>
          <div class="phone-preview" :class="{ muted: !form.enabled }">
            <div class="phone-status"><span>9:41</span><span>5G 100%</span></div>
            <div class="frozen-banner">
              <b>已冻结</b>
              <strong>门店暂不可使用</strong>
              <p>{{ form.displayText || '添加客服后，由平台人工调整使用期限并解冻。' }}</p>
            </div>
            <div class="wechat-card">
              <span>客服微信</span>
              <strong>{{ form.enabled ? (form.wechatId || 'SerenMeet-CS') : '已停用展示' }}</strong>
            </div>
            <div class="scope-text">{{ form.displayScope || '店长冻结页展示客服微信；教练和会员端提示联系门店。' }}</div>
          </div>
        </section>

        <section class="reference-panel rules-section">
          <div class="side-card-head">
            <h2>三端展示规则</h2>
          </div>
          <div class="step-list compact">
            <div class="step-card"><b><el-icon><Phone /></el-icon></b><div><strong>店长冻结页</strong><small>展示客服微信 {{ form.wechatId || 'SerenMeet-CS' }} 和续期说明。</small></div><StatusTag status="extended" text="展示" /></div>
            <div class="step-card"><b><el-icon><User /></el-icon></b><div><strong>员工冻结页</strong><small>提示联系门店，不展示平台续期操作。</small></div><StatusTag status="expiring" text="提示" /></div>
            <div class="step-card"><b><el-icon><User /></el-icon></b><div><strong>会员冻结页</strong><small>提示联系门店，卡包保持只读。</small></div><StatusTag status="expiring" text="提示" /></div>
          </div>
        </section>
      </div>

      <div class="workbench-footer">
        <el-button type="primary" :loading="saving" :disabled="!canSave" @click="save(form.enabled)">{{ saveText }}</el-button>
      </div>
    </div>
  </section>
</template>

<style scoped>
.support-page {
  padding-bottom: 24px;
}

.support-workbench {
  overflow: hidden;
  padding: 0;
}

.workbench-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  border-bottom: 1px solid var(--line-soft);
  background:
    linear-gradient(90deg, rgba(228, 241, 236, 0.88), rgba(255, 255, 255, 0.32)),
    #fff;
  padding: 18px 20px 15px;
}

.workbench-head h2 {
  margin: 0 0 6px;
  color: var(--ink);
  font-size: 18px;
}

.workbench-head p {
  margin: 0;
  color: var(--muted);
  font-size: 13px;
  font-weight: 750;
  line-height: 1.6;
}

.editor-pane {
  border-bottom: 1px solid var(--line-soft);
  background: #fff;
  padding: 18px 20px;
}

.support-form {
  flex: 0 0 auto;
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background:
    linear-gradient(180deg, rgba(248, 251, 249, 0.92), rgba(255, 255, 255, 0.96)),
    #fff;
  padding: 14px;
}

.config-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(250px, 0.72fr);
  gap: 16px;
  align-items: start;
}

.config-grid :deep(.el-form-item) {
  margin-bottom: 0;
}

.config-grid :deep(.el-form-item__label) {
  margin-bottom: 7px;
  color: var(--ink);
  font-size: 13px;
  font-weight: 900;
  line-height: 1.2;
}

.required-label::before {
  content: "*";
  margin-right: 4px;
  color: var(--coral);
  font-weight: 950;
}

.wechat-id-field,
.status-field {
  min-width: 0;
}

.copy-field :deep(.el-textarea__inner),
.scope-field :deep(.el-textarea__inner) {
  resize: none;
}

.inline-status {
  display: grid;
  grid-template-columns: 28px minmax(72px, 1fr) auto;
  align-items: center;
  gap: 10px;
  min-height: 40px;
  border: 1px solid #cfe3da;
  border-radius: 8px;
  background: var(--green-soft);
  color: var(--green-dark);
  padding: 0 12px;
}

.inline-status.disabled {
  border-color: #f1c8be;
  background: var(--coral-soft);
  color: var(--coral);
}

.inline-status .el-icon {
  font-size: 17px;
}

.inline-status strong {
  font-size: 14px;
  font-weight: 950;
  white-space: nowrap;
}

.audit-row {
  display: grid;
  grid-template-columns: minmax(220px, 0.46fr) minmax(0, 1fr);
  align-items: center;
  gap: 14px;
  flex: 0 0 auto;
  border: 1px solid #f0d8b7;
  border-radius: 8px;
  background: var(--gold-soft);
  margin: 12px 0 0;
  padding: 12px;
}

.audit-copy {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  color: #7d5516;
}

.audit-copy .el-icon {
  margin-top: 2px;
  font-size: 18px;
}

.audit-copy strong,
.audit-copy span {
  display: block;
}

.audit-copy strong {
  font-size: 13px;
  font-weight: 950;
}

.audit-copy span {
  margin-top: 4px;
  color: #8a6423;
  font-size: 12px;
  font-weight: 750;
  line-height: 1.45;
}

.reference-row {
  display: grid;
  grid-template-columns: minmax(320px, 0.86fr) minmax(0, 1.14fr);
  gap: 16px;
  border-bottom: 1px solid var(--line-soft);
  background:
    linear-gradient(180deg, rgba(248, 251, 249, 0.94), rgba(255, 255, 255, 0.7)),
    #fff;
  padding: 18px 20px;
}

.reference-panel {
  min-width: 0;
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: #fff;
  padding: 14px;
}

.preview-section {
  display: flex;
  flex-direction: column;
}

.rules-section {
  display: flex;
  flex-direction: column;
  min-height: 238px;
}

.rules-section .step-list {
  flex: 1;
  display: grid;
  grid-template-rows: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.workbench-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  border-top: 1px solid var(--line-soft);
  background: #fbfdfb;
  padding: 14px 20px;
}

.workbench-footer .el-button {
  flex: 0 0 auto;
}

.phone-preview {
  flex: 1;
  overflow: hidden;
  border: 1px solid #d5e1db;
  border-radius: 8px;
  background: linear-gradient(180deg, #f8faf7, #eef5f1);
  padding: 12px;
}

.phone-preview.muted {
  filter: saturate(0.75);
}

.phone-status {
  display: flex;
  justify-content: space-between;
  color: var(--ink);
  font-size: 12px;
  font-weight: 900;
  padding: 0 4px 10px;
}

.frozen-banner {
  border-radius: 8px;
  background:
    linear-gradient(145deg, rgba(232, 111, 78, 0.94), rgba(183, 72, 48, 0.94)),
    var(--coral);
  color: #fff;
  padding: 16px;
}

.frozen-banner b {
  display: inline-flex;
  min-height: 24px;
  align-items: center;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.18);
  padding: 0 9px;
  font-size: 12px;
}

.frozen-banner strong,
.frozen-banner p {
  display: block;
}

.frozen-banner strong {
  margin-top: 12px;
  font-size: 20px;
  line-height: 1.25;
}

.frozen-banner p {
  margin: 8px 0 0;
  color: rgba(255, 255, 255, 0.86);
  font-size: 13px;
  font-weight: 750;
  line-height: 1.55;
}

.wechat-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 12px;
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: #fff;
  padding: 13px;
}

.wechat-card span {
  color: var(--muted);
  font-size: 12px;
  font-weight: 800;
}

.wechat-card strong {
  color: var(--green-dark);
  font-size: 16px;
  word-break: break-word;
}

.scope-text {
  margin-top: 12px;
  border: 1px dashed #d7c28e;
  border-radius: 8px;
  background: var(--gold-soft);
  color: #7d5516;
  padding: 12px;
  font-size: 12px;
  font-weight: 750;
  line-height: 1.6;
}

.side-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.side-card-head h2 {
  margin: 0;
  font-size: 16px;
}

.compact .step-card {
  grid-template-columns: 34px minmax(0, 1fr) auto;
  align-items: center;
  min-height: 0;
  padding: 12px;
}

.compact .step-card div {
  align-self: center;
}

.compact .step-card b {
  width: 32px;
  height: 32px;
}

@media (max-width: 760px) {
  .reference-row,
  .config-grid,
  .audit-row {
    grid-template-columns: 1fr;
  }

  .reference-row {
    padding: 16px;
  }

  .workbench-head {
    display: grid;
    justify-content: stretch;
  }

  .inline-status,
  .workbench-footer {
    grid-template-columns: 1fr;
  }

  .workbench-footer {
    display: grid;
    justify-content: stretch;
  }
}
</style>
