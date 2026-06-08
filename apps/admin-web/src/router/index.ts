import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/services/api'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/tenants' },
    { path: '/login', component: () => import('@/pages/login/LoginPage.vue') },
    {
      path: '/',
      component: () => import('@/layouts/AdminLayout.vue'),
      children: [
        { path: 'tenants', component: () => import('@/pages/tenants/TenantsPage.vue') },
        { path: 'tenants/:id', component: () => import('@/pages/tenant-detail/TenantDetailPage.vue') },
        { path: 'tenants/:id/freeze', component: () => import('@/pages/freeze/FreezePage.vue') },
        { path: 'support-wechat', component: () => import('@/pages/support-wechat/SupportWechatPage.vue') },
        { path: 'config', component: () => import('@/pages/config/ConfigPage.vue') },
        { path: 'audit', component: () => import('@/pages/audit/AuditPage.vue') }
      ]
    }
  ]
})

router.beforeEach((to) => {
  if (to.path !== '/login' && !getToken()) {
    return '/login'
  }
  if (to.path === '/login' && getToken()) {
    return '/tenants'
  }
  return true
})

export default router
