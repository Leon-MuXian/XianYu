import type {
  AdminUser,
  AuditLogItem,
  LoginResponse,
  PageResponse,
  PlatformConfig,
  SupportWechat,
  TenantDetail,
  TenantListResponse
} from '@/types/admin'
import { createApiClient, createIdempotencyKey, type SerenApiError } from '@serenmeet/api-client'

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://127.0.0.1:8080'
const TOKEN_KEY = 'serenmeet_admin_token'
const USER_KEY = 'serenmeet_admin_user'
const SESSION_EXPIRED_CODES = new Set(['LOGIN_REQUIRED', 'UNAUTHORIZED'])

export function getToken() {
  return window.localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string) {
  window.localStorage.setItem(TOKEN_KEY, token)
}

export function getStoredAdminUser(): AdminUser | null {
  const value = window.localStorage.getItem(USER_KEY)
  if (!value) return null
  try {
    const user = JSON.parse(value) as Partial<AdminUser>
    return typeof user.id === 'number' && typeof user.username === 'string' && typeof user.displayName === 'string'
      ? user as AdminUser
      : null
  } catch {
    return null
  }
}

function setStoredAdminUser(user: AdminUser) {
  window.localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function clearToken() {
  window.localStorage.removeItem(TOKEN_KEY)
  window.localStorage.removeItem(USER_KEY)
}

function handleApiError(error: SerenApiError) {
  if (!SESSION_EXPIRED_CODES.has(error.code)) return
  clearToken()
  if (window.location.pathname !== '/login') {
    window.location.replace('/login')
  }
}

const client = createApiClient({
  baseUrl: API_BASE,
  getToken,
  onError: handleApiError,
  transport: async request => {
    const response = await fetch(request.url, {
      method: request.method,
      headers: request.headers,
      body: request.data === undefined ? undefined : JSON.stringify(request.data)
    })
    const text = await response.text()
    let data: unknown = null
    if (text) {
      try { data = JSON.parse(text) } catch { data = null }
    }
    return { statusCode: response.status, data }
  }
})

function request<T>(method: 'GET' | 'POST' | 'PUT' | 'DELETE', path: string, data?: unknown, idempotencyKey?: string) {
  return client.request<T>(method, path, data, idempotencyKey)
}

function toQuery(params: Record<string, string | number | boolean | undefined | null>) {
  const query = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      query.set(key, String(value))
    }
  })
  const value = query.toString()
  return value ? `?${value}` : ''
}

export const adminApi = {
  async login(username: string, password: string) {
    const data = await request<LoginResponse>('POST', '/admin/auth/login', { username, password })
    setToken(data.token)
    setStoredAdminUser(data.user)
    return data
  },

  async getCurrentUser() {
    const user = await request<AdminUser>('GET', '/admin/auth/me')
    setStoredAdminUser(user)
    return user
  },

  listTenants(params: {
    keyword?: string
    status?: string
    expiry?: string
    page?: number
    pageSize?: number
  }) {
    return request<TenantListResponse>('GET', `/admin/tenants${toQuery(params)}`)
  },

  getTenant(id: number) {
    return request<TenantDetail>('GET', `/admin/tenants/${id}`)
  },

  freezeTenant(id: number, reason: string, confirmed: boolean) {
    return request<TenantDetail>('POST', `/admin/tenants/${id}/freeze`, { reason, confirmed }, createIdempotencyKey('admin-freeze'))
  },

  extendTenant(id: number, payload: { newTrialEndAt: string; reason: string; internalNote?: string }) {
    return request<TenantDetail>('POST', `/admin/tenants/${id}/extend-trial`, payload, createIdempotencyKey('admin-extend'))
  },

  getSupportWechat() {
    return request<SupportWechat>('GET', '/admin/support-wechat')
  },

  updateSupportWechat(payload: {
    wechatId: string
    displayText: string
    displayScope: string
    enabled: boolean
    reason: string
  }) {
    return request<SupportWechat>('PUT', '/admin/support-wechat', payload, createIdempotencyKey('admin-support-wechat'))
  },

  listConfigs(params: { keyword?: string; editableOnly?: boolean }) {
    return request<PlatformConfig[]>('GET', `/admin/configs${toQuery(params)}`)
  },

  updateConfig(configKey: string, payload: { newValue: string; reason: string }) {
    return request<PlatformConfig>('PUT', `/admin/configs/${encodeURIComponent(configKey)}`, payload, createIdempotencyKey('admin-config'))
  },

  listAuditLogs(params: {
    keyword?: string
    action?: string
    range?: string
    page?: number
    pageSize?: number
  }) {
    return request<PageResponse<AuditLogItem>>('GET', `/admin/audit-logs${toQuery(params)}`)
  }
}
