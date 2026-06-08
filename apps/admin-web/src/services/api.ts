import type {
  ApiResponse,
  AuditLogItem,
  LoginResponse,
  PageResponse,
  PlatformConfig,
  SupportWechat,
  TenantDetail,
  TenantListResponse
} from '@/types/admin'

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://127.0.0.1:8080'
const TOKEN_KEY = 'serenmeet_admin_token'

export function getToken() {
  return window.localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string) {
  window.localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken() {
  window.localStorage.removeItem(TOKEN_KEY)
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers)
  headers.set('Content-Type', 'application/json')
  const token = getToken()
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  const response = await fetch(`${API_BASE}${path}`, { ...options, headers })
  const text = await response.text()
  let payload: ApiResponse<T> | null = null
  if (text) {
    try {
      payload = JSON.parse(text) as ApiResponse<T>
    } catch {
      payload = null
    }
  }
  if (!response.ok || !payload || !payload.success) {
    const fallback = response.status ? `请求失败：HTTP ${response.status}` : '请求失败：服务无响应'
    throw new Error(payload?.error?.message || (payload?.requestId ? `请求失败：${payload.requestId}` : fallback))
  }
  return payload.data
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
    const data = await request<LoginResponse>('/admin/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password })
    })
    setToken(data.token)
    return data
  },

  listTenants(params: {
    keyword?: string
    status?: string
    expiry?: string
    page?: number
    pageSize?: number
  }) {
    return request<TenantListResponse>(`/admin/tenants${toQuery(params)}`)
  },

  getTenant(id: number) {
    return request<TenantDetail>(`/admin/tenants/${id}`)
  },

  freezeTenant(id: number, reason: string, confirmed: boolean) {
    return request<TenantDetail>(`/admin/tenants/${id}/freeze`, {
      method: 'POST',
      body: JSON.stringify({ reason, confirmed })
    })
  },

  extendTenant(id: number, payload: { newTrialEndAt: string; reason: string; internalNote?: string }) {
    return request<TenantDetail>(`/admin/tenants/${id}/extend-trial`, {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  },

  getSupportWechat() {
    return request<SupportWechat>('/admin/support-wechat')
  },

  updateSupportWechat(payload: {
    wechatId: string
    displayText: string
    displayScope: string
    enabled: boolean
    reason: string
  }) {
    return request<SupportWechat>('/admin/support-wechat', {
      method: 'PUT',
      body: JSON.stringify(payload)
    })
  },

  listConfigs(params: { keyword?: string; editableOnly?: boolean }) {
    return request<PlatformConfig[]>(`/admin/configs${toQuery(params)}`)
  },

  updateConfig(configKey: string, payload: { newValue: string; reason: string }) {
    return request<PlatformConfig>(`/admin/configs/${encodeURIComponent(configKey)}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    })
  },

  listAuditLogs(params: {
    keyword?: string
    action?: string
    range?: string
    page?: number
    pageSize?: number
  }) {
    return request<PageResponse<AuditLogItem>>(`/admin/audit-logs${toQuery(params)}`)
  }
}
