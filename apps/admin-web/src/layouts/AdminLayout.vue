<script setup lang="ts">
import { onMounted, ref } from 'vue'
import {
  ChatDotRound,
  DocumentChecked,
  OfficeBuilding,
  Setting,
  SwitchButton
} from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'
import { adminApi, clearToken, getStoredAdminUser } from '@/services/api'
import { useRouter } from 'vue-router'

const router = useRouter()
const currentUsername = ref(getStoredAdminUser()?.username || '账号加载中')

const nav = [
  { path: '/tenants', label: '租户管理', icon: OfficeBuilding },
  { path: '/support-wechat', label: '客服微信', icon: ChatDotRound },
  { path: '/config', label: '平台配置', icon: Setting },
  { path: '/audit', label: '操作记录', icon: DocumentChecked }
]

onMounted(async () => {
  try {
    currentUsername.value = (await adminApi.getCurrentUser()).username
  } catch {
    if (!getStoredAdminUser()) currentUsername.value = '账号信息不可用'
  }
})

async function confirmLogout() {
  try {
    await ElMessageBox.confirm('确认退出当前平台后台账号吗？', '退出登录', {
      confirmButtonText: '确认退出',
      cancelButtonText: '取消',
      type: 'warning',
      autofocus: false,
      closeOnClickModal: true
    })
    clearToken()
    router.push('/login')
  } catch {
    // 用户取消退出，无需处理。
  }
}
</script>

<template>
  <div class="admin-layout">
    <aside class="sidebar">
      <div class="brand">
        <span class="brand-mark">闲</span>
        <div class="brand-copy">
          <strong>闲遇</strong>
          <small>PLATFORM OPS</small>
        </div>
      </div>

      <nav class="sidebar-nav" aria-label="平台后台导航">
        <RouterLink
          v-for="item in nav"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :title="item.label"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>

      <div class="sidebar-session">
        <span class="session-avatar">AD</span>
        <div class="session-copy">
          <strong>平台管理员</strong>
          <small :title="currentUsername">{{ currentUsername }}</small>
        </div>
        <button type="button" class="session-logout" aria-label="退出登录" title="退出登录" @click="confirmLogout">
          <el-icon><SwitchButton /></el-icon>
        </button>
      </div>
    </aside>

    <section class="workspace">
      <main class="main">
        <RouterView />
      </main>
    </section>
  </div>
</template>

<style scoped>
.admin-layout {
  display: grid;
  grid-template-columns: 224px minmax(0, 1fr);
  grid-template-rows: minmax(0, 1fr);
  grid-template-areas: "side workspace";
  min-height: 100vh;
  background: var(--bg);
}

.sidebar {
  grid-area: side;
  position: sticky;
  top: 0;
  display: flex;
  flex-direction: column;
  height: 100vh;
  min-width: 0;
  border-right: 1px solid #0e1c2b;
  background: var(--navy);
  color: #fff;
  padding: 0 14px 18px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 11px;
  min-height: 72px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  padding: 0 10px;
}

.brand-mark {
  display: grid;
  place-items: center;
  flex: 0 0 32px;
  width: 32px;
  height: 32px;
  border: 1px solid rgba(255, 255, 255, 0.28);
  border-radius: 4px;
  background: #fff;
  color: var(--navy);
  font-size: 17px;
  font-weight: 900;
}

.brand-copy strong,
.brand-copy small {
  display: block;
}

.brand-copy strong {
  font-size: 15px;
  line-height: 1.2;
}

.brand-copy small {
  margin-top: 4px;
  color: #91a1b2;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 9px;
  line-height: 1;
}

.sidebar-nav {
  display: grid;
  gap: 4px;
  margin-top: 24px;
}

.nav-item {
  display: grid;
  grid-template-columns: 30px minmax(0, 1fr);
  gap: 8px;
  align-items: center;
  min-height: 46px;
  border: 1px solid transparent;
  border-radius: 4px;
  padding: 0 12px 0 9px;
  color: #aab7c4;
  font-size: 13px;
  font-weight: 700;
  transition: background-color 0.18s ease, border-color 0.18s ease, color 0.18s ease;
}

.nav-item .el-icon {
  justify-self: center;
  color: #7f91a4;
  font-size: 18px;
  transition: color 0.18s ease;
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.035);
  color: #d6e0e9;
}

.nav-item.router-link-active {
  border-color: rgba(121, 173, 219, 0.14);
  background: #21364b;
  color: #f3f7fa;
}

.nav-item.router-link-active .el-icon {
  color: #79addb;
}

.nav-item:focus {
  outline: none;
}

.nav-item:focus-visible {
  box-shadow: inset 0 0 0 2px #79addb;
}

.sidebar-session {
  display: grid;
  grid-template-columns: 32px minmax(0, 1fr) 34px;
  gap: 9px;
  align-items: center;
  margin-top: auto;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  padding: 17px 10px 0;
}

.session-avatar {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border-radius: 4px;
  background: #2b4057;
  color: #dce7f1;
  font-size: 10px;
  font-weight: 900;
}

.session-copy {
  min-width: 0;
}

.session-copy strong,
.session-copy small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-copy strong {
  font-size: 11px;
}

.session-copy small {
  margin-top: 3px;
  color: #8192a4;
  font-size: 9px;
}

.session-logout {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border: 1px solid transparent;
  border-radius: 4px;
  background: transparent;
  color: #8192a4;
  cursor: pointer;
  transition: background-color 0.18s ease, border-color 0.18s ease, color 0.18s ease;
}

.session-logout:hover {
  border-color: rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.06);
  color: #fff;
}

.workspace {
  grid-area: workspace;
  min-width: 0;
}

.main {
  width: 100%;
  min-width: 0;
  padding: 24px 28px 30px;
}

@media (min-width: 1680px) {
  .main {
    padding-right: 36px;
    padding-left: 36px;
  }
}

@media (max-width: 1100px) {
  .admin-layout {
    grid-template-columns: 76px minmax(0, 1fr);
  }

  .sidebar {
    padding: 0 10px 16px;
  }

  .brand {
    justify-content: center;
    padding: 0;
  }

  .brand-copy,
  .nav-item span,
  .session-copy {
    display: none;
  }

  .nav-item {
    grid-template-columns: 1fr;
    padding: 0;
  }

  .sidebar-session {
    grid-template-columns: 1fr;
    justify-items: center;
    padding-right: 0;
    padding-left: 0;
  }

  .session-avatar {
    display: none;
  }
}

@media (max-width: 720px) {
  .admin-layout {
    display: block;
    padding-bottom: 72px;
  }

  .sidebar {
    position: fixed;
    inset: auto 10px 10px;
    z-index: 20;
    display: grid;
    grid-template-columns: minmax(0, 1fr) 44px;
    height: 58px;
    border: 1px solid rgba(255, 255, 255, 0.12);
    border-radius: 5px;
    padding: 6px;
    box-shadow: 0 18px 42px rgba(23, 36, 51, 0.24);
  }

  .brand {
    display: none;
  }

  .sidebar-nav {
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 4px;
    margin: 0;
  }

  .nav-item {
    min-height: 44px;
  }

  .sidebar-session {
    margin: 0;
    border: 0;
    padding: 0;
  }

  .session-logout {
    width: 44px;
    height: 44px;
  }

  .main {
    padding: 18px 12px 24px;
  }
}
</style>
