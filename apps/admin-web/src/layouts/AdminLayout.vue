<script setup lang="ts">
import {
  ChatDotRound,
  DocumentChecked,
  OfficeBuilding,
  Setting,
  SwitchButton,
  User
} from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'
import { clearToken } from '@/services/api'
import { useRouter } from 'vue-router'

const router = useRouter()

const nav = [
  { path: '/tenants', label: '租户管理', icon: OfficeBuilding },
  { path: '/support-wechat', label: '客服微信', icon: ChatDotRound },
  { path: '/config', label: '平台配置', icon: Setting },
  { path: '/audit', label: '操作记录', icon: DocumentChecked }
]

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
        <span>闲</span>
        <strong>闲遇</strong>
      </div>
      <nav>
        <RouterLink v-for="item in nav" :key="item.path" :to="item.path" class="nav-item">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>
      <div class="sidebar-session">
        <div class="session-copy">
          <el-icon><User /></el-icon>
          <div>
            <strong>内部运营</strong>
          </div>
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
  min-height: 100vh;
  grid-template-columns: 232px minmax(0, 1fr);
  background:
    linear-gradient(180deg, rgba(228, 241, 236, 0.82), rgba(238, 243, 239, 0.42) 220px, transparent),
    #eef3ef;
}

.sidebar {
  position: sticky;
  top: 0;
  isolation: isolate;
  display: flex;
  flex-direction: column;
  height: 100vh;
  background:
    radial-gradient(circle at 18% 10%, rgba(47, 125, 110, 0.42), transparent 26%),
    linear-gradient(145deg, rgba(18, 34, 29, 0.98), rgba(18, 34, 29, 0.94)),
    #12221d;
  color: white;
  overflow: hidden;
  padding: 26px 20px 18px;
}

.sidebar::after {
  content: "";
  position: absolute;
  right: -150px;
  bottom: -170px;
  z-index: -1;
  width: 360px;
  height: 360px;
  border-radius: 999px;
  background: rgba(47, 125, 110, 0.22);
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 36px;
  padding: 0 8px;
  font-size: 22px;
  font-weight: 900;
}

.brand span {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--green);
  font-weight: 900;
  box-shadow: 0 10px 24px rgba(47, 125, 110, 0.24);
}

nav {
  display: grid;
  gap: 6px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 48px;
  border: 1px solid transparent;
  border-radius: 8px;
  padding: 0 14px;
  color: rgba(229, 241, 236, 0.72);
  background: transparent;
  font-size: 16px;
  font-weight: 900;
  transition: background 0.16s ease, color 0.16s ease, border-color 0.16s ease;
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.06);
  color: white;
}

.nav-item.router-link-active {
  border-color: rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.09);
  color: white;
  box-shadow: inset 3px 0 0 rgba(82, 166, 149, 0.92);
}

.nav-item .el-icon {
  font-size: 18px;
}

.workspace {
  min-width: 0;
}

.sidebar-session {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: auto;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.045);
  padding: 9px;
}

.session-copy {
  display: flex;
  align-items: center;
  min-width: 0;
  flex: 1;
  gap: 9px;
  color: rgba(229, 241, 236, 0.72);
}

.session-copy > .el-icon {
  font-size: 18px;
}

.session-copy div {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.session-copy strong {
  overflow: hidden;
  color: rgba(255, 255, 255, 0.84);
  font-size: 13px;
  font-weight: 900;
  line-height: 1.2;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-logout {
  display: grid;
  place-items: center;
  flex: 0 0 34px;
  width: 34px;
  height: 34px;
  border: 1px solid transparent;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.055);
  color: rgba(229, 241, 236, 0.72);
  cursor: pointer;
  font: inherit;
  transition: background 0.16s ease, color 0.16s ease, border-color 0.16s ease;
}

.session-logout:hover {
  border-color: rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.1);
  color: white;
}

.main {
  width: min(100%, max(1440px, calc(100vw - 320px)));
  margin: 0 auto;
  padding: clamp(18px, 1.6vw, 34px);
}

@media (max-width: 1100px) {
  .admin-layout {
    grid-template-columns: 76px minmax(0, 1fr);
  }

  .sidebar {
    padding: 16px 10px;
  }

  .brand {
    justify-content: center;
    margin-bottom: 22px;
    padding: 0;
  }

  .brand strong,
  .nav-item span,
  .session-copy {
    display: none;
  }

  .brand span {
    width: 38px;
    height: 38px;
  }

  .nav-item {
    justify-content: center;
    padding: 0;
  }

  .sidebar-session {
    justify-content: center;
    padding: 5px;
  }

  .session-logout {
    flex-basis: 44px;
    width: 44px;
    height: 44px;
  }
}

@media (max-width: 720px) {
  .admin-layout {
    display: block;
    padding-bottom: 76px;
  }

  .sidebar {
    position: fixed;
    inset: auto 12px 12px;
    z-index: 20;
    display: grid;
    grid-template-columns: 1fr 44px;
    align-items: center;
    height: 60px;
    border: 1px solid rgba(255, 255, 255, 0.12);
    border-radius: 8px;
    padding: 8px;
    box-shadow: 0 18px 42px rgba(18, 34, 29, 0.24);
  }

  .brand {
    display: none;
  }

  nav {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 6px;
  }

  .nav-item,
  .session-logout {
    min-height: 44px;
  }

  .nav-item span,
  .session-copy {
    display: none;
  }

  .sidebar-session {
    margin-top: 0;
    border-color: transparent;
    background: transparent;
    padding: 0;
  }

  .session-logout {
    width: 44px;
    height: 44px;
  }

  .main {
    width: 100%;
    padding: 16px 12px;
  }
}
</style>
