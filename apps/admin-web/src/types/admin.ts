export interface ApiError {
  code: string
  message: string
}

export interface ApiResponse<T> {
  requestId: string
  success: boolean
  data: T
  error: ApiError | null
}

export interface AdminUser {
  id: number
  username: string
  displayName: string
}

export interface LoginResponse {
  token: string
  expiresAt: string
  user: AdminUser
}

export interface PageResponse<T> {
  items: T[]
  total: number
  page: number
  pageSize: number
}

export interface TenantStats {
  total: number
  trialing: number
  expiring: number
  frozen: number
  extended: number
}

export interface TenantListItem {
  id: number
  name: string
  city: string
  trialStartAt: string
  trialEndAt: string
  status: TenantStatus
  statusText: string
  supportWechatId: string | null
  issuedCards: number
  deductions: number
  warningCount: number
  monthlySalesYuan: number
}

export type TenantStatus = 'TRIALING' | 'EXPIRING' | 'FROZEN' | 'EXTENDED'

export interface TenantListResponse {
  stats: TenantStats
  page: PageResponse<TenantListItem>
}

export interface StoreView {
  name: string
  businessCategories: string
  serviceTags: string
  address: string
  contactPhone: string
  businessHours: string
}

export interface BusinessSnapshot {
  issuedCards: number
  checkins: number
  deductions: number
  warningCount: number
  lowBalanceCount: number
  expiringCount: number
  monthlySalesYuan: number
  lastActivityAt: string | null
}

export interface TenantDetail {
  id: number
  name: string
  city: string
  status: TenantStatus
  statusText: string
  trialStartAt: string
  trialEndAt: string
  frozenReason: string | null
  supportWechatId: string | null
  store: StoreView
  snapshot: BusinessSnapshot
}

export interface SupportWechat {
  id: number
  wechatId: string
  displayText: string
  displayScope: string
  enabled: boolean
}

export interface PlatformConfig {
  configKey: string
  name: string
  valueText: string
  valueType: string
  unit: string
  minValue: number | null
  maxValue: number | null
  impactScope: string
  editable: boolean
  enabled: boolean
}

export interface AuditLogItem {
  id: number
  tenantId: number | null
  actorType: string
  actorName: string
  action: string
  targetName: string
  oldValue: string | null
  newValue: string | null
  reason: string | null
  createdAt: string
}
