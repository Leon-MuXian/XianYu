<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
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

async function save() {
  if (!canSave.value) return
  saving.value = true
  try {
    const data = await adminApi.updateSupportWechat({ ...form })
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
        <p>维护冻结页默认客服微信文本，运营只需确认展示什么、展示给谁、为什么修改。</p>
      </div>
    </div>

    <section class="admin-card support-card">
      <header class="support-card-head">
        <h2>默认客服微信</h2>
      </header>

      <el-form label-position="top" class="support-form">
        <div class="support-form-grid">
          <el-form-item>
            <template #label><span class="required-label">客服微信 ID</span></template>
            <el-input v-model="form.wechatId" placeholder="例如：SerenMeet-CS" />
          </el-form-item>

          <el-form-item label="展示状态">
            <div class="status-control">
              <strong>{{ form.enabled ? '启用展示' : '停用展示' }}</strong>
              <el-switch v-model="form.enabled" active-text="启用" inactive-text="停用" />
            </div>
          </el-form-item>

          <el-form-item class="wide-field">
            <template #label><span class="required-label">展示文案</span></template>
            <el-input
              v-model="form.displayText"
              type="textarea"
              :autosize="{ minRows: 1, maxRows: 3 }"
              maxlength="80"
              show-word-limit
              placeholder="例如：添加客服后，由平台人工调整使用期限并解冻。"
            />
          </el-form-item>

          <el-form-item class="wide-field">
            <template #label><span class="required-label">展示范围</span></template>
            <el-input
              v-model="form.displayScope"
              type="textarea"
              :autosize="{ minRows: 1, maxRows: 3 }"
              maxlength="120"
              show-word-limit
              placeholder="例如：店长冻结页展示客服微信；教练和会员端提示联系门店。"
            />
          </el-form-item>

          <el-form-item class="wide-field">
            <template #label><span class="required-label">操作原因</span></template>
            <el-input v-model="form.reason" placeholder="例如：更新冻结页默认客服微信文本" />
          </el-form-item>
        </div>
      </el-form>

      <footer class="support-actions">
        <span>保存后同步更新冻结页默认文本并写入操作记录。</span>
        <el-button type="primary" :loading="saving" :disabled="!canSave" @click="save">{{ saveText }}</el-button>
      </footer>
    </section>
  </section>
</template>

<style scoped>
.support-card {
  padding: 0;
}

.support-card-head {
  display: flex;
  align-items: center;
  min-height: 54px;
  border-bottom: 1px solid var(--line-soft);
  padding: 0 20px;
}

.support-card-head h2 {
  margin: 0;
}

.support-form {
  padding: 20px;
}

.support-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px 16px;
}

.support-form-grid :deep(.el-form-item) {
  min-width: 0;
  margin-bottom: 0;
}

.wide-field {
  grid-column: 1 / -1;
}

.required-label::after {
  margin-left: 5px;
  color: var(--coral);
  content: "必填";
}

.support-form :deep(.el-textarea__inner) {
  min-height: 42px !important;
  padding: 9px 66px 9px 12px;
  line-height: 22px;
  resize: none;
}

.support-form :deep(.el-textarea .el-input__count) {
  right: 12px;
  bottom: 10px;
  height: 22px;
  background: transparent;
  line-height: 22px;
}

.status-control {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 42px;
  border: 1px solid var(--line);
  border-radius: 4px;
  background: #fff;
  padding: 0 12px;
}

.status-control strong {
  color: var(--ink);
  font-size: 13px;
}

.support-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  min-height: 70px;
  border-top: 1px solid var(--line-soft);
  padding: 12px 20px;
  color: var(--muted);
  font-size: 12px;
}

@media (max-width: 760px) {
  .support-form-grid {
    grid-template-columns: 1fr;
  }

  .wide-field {
    grid-column: auto;
  }

  .support-actions {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
