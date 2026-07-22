import type { components } from './generated.js'

type Schemas = components['schemas']

export type TenantStatus = 'trialing' | 'expiring' | 'frozen' | 'extended'
export type AdminUser = Required<Schemas['AdminUserView']>
export type PlatformConfig = Required<Schemas['PlatformConfigResponse']>
export type SupportWechat = Required<Schemas['SupportWechatResponse']>
export type TenantStats = Required<Schemas['TenantStatsResponse']>
export type StoreView = Required<Schemas['StoreView']>

export type LoginResponse = Omit<Required<Schemas['LoginResponse']>, 'user'> & {
  user: AdminUser
}

export interface PageResponse<T> {
  items: T[]
  total: number
  page: number
  pageSize: number
}

export type TenantListItem = Omit<Required<Schemas['TenantListItem']>, 'status' | 'supportWechatId'> & {
  status: TenantStatus
  supportWechatId: string | null
}

export type TenantListResponse = {
  stats: TenantStats
  page: PageResponse<TenantListItem>
}

export type BusinessSnapshot = Omit<Required<Schemas['BusinessSnapshotView']>, 'lastActivityAt'> & {
  lastActivityAt: string | null
}

export type TenantDetail = Omit<
  Required<Schemas['TenantDetailResponse']>,
  'status' | 'frozenReason' | 'supportWechatId' | 'store' | 'snapshot'
> & {
  status: TenantStatus
  frozenReason: string | null
  supportWechatId: string | null
  store: StoreView
  snapshot: BusinessSnapshot
}

export type AuditLogItem = Omit<
  Required<Schemas['AuditLogItem']>,
  'tenantId' | 'oldValue' | 'newValue' | 'reason'
> & {
  tenantId: number | null
  oldValue: string | null
  newValue: string | null
  reason: string | null
}
