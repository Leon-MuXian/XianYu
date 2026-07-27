import Taro from '@tarojs/taro'
import type { BusinessHoursValue } from '@serenmeet/business-components'
import { api } from './api'
export { scheduleDisplayState } from './schedule-state'

export const ownerRoutes = {
  login: '/pages/login/index',
  onboarding: '/pages/onboarding/index',
  store: '/pages/store/index',
  hours: '/pages/hours/index',
  resources: '/pages/resources/index',
  dashboard: '/pages/dashboard/index',
  staff: '/pages/staff/index',
  services: '/pages/services/index',
  cards: '/pages/cards/index',
  memberIssue: '/pages/member-issue/index',
  schedules: '/pages/schedules/index',
  members: '/pages/members/index',
  report: '/pages/report/index',
  reportDetail: '/pages/report-detail/index',
  my: '/pages/my/index',
  frozen: '/pages/frozen/index'
} as const

export interface OwnerProfile {
  tenantId: number
  storeId?: number
  name?: string
  city?: string
  address?: string
  contactPhone?: string
  status: string
  trialStartAt?: string
  trialEndAt?: string
  supportWechatId?: string
  supportText?: string
}

export interface StoreProfileDraft {
  name?: string
  cityCode?: string
  city?: string
  districtCode?: string
  district?: string
  detailAddress?: string
  address?: string
  contactPhone?: string
  serviceScopes?: string[]
}

export interface RegionOption {
  code: string
  name: string
}

export interface DayHours {
  open: boolean
  start: string
  end: string
}

export interface OnboardingDraft {
  trialNoticeRequired: boolean
  trialDays: number
  storeProfile?: StoreProfileDraft
  businessHours?: BusinessHoursValue
  resources?: Resource[]
  completion: {
    storeProfileDone: boolean
    businessHoursDone: boolean
    resourceDone: boolean
  }
  status: string
}

export interface Resource {
  id?: number
  name: string
  resourceType: string
  capacity: number
  enabled?: boolean
  sortOrder?: number
}

export interface Staff {
  id: number
  loginName: string
  staffName: string
  roleLabel: string
  status: string
  firstLogin?: boolean
  credentialAvailable: boolean
}

export interface StaffCredential {
  staffName: string
  loginName: string
  loginPassword: string
}

export interface Service {
  id: number
  name: string
  serviceType: string
  durationMin: number
  defaultCapacity: number
  deductCount: number
  status: string
  resourceIds?: number[]
  staffIds?: number[]
}

export interface CardTemplate {
  id: number
  name: string
  cardType: 'count' | 'period'
  salePriceYuan: number
  totalCount?: number
  validDays: number
  lowBalanceThreshold?: number
  status: string
  serviceIds?: number[]
  staffIds?: number[]
}

export interface Member {
  id: number
  name: string
  memberNo: string
  contactText: string
  bindStatus: string
}

export interface Schedule {
  id: number
  serviceId?: number
  staffId?: number
  resourceId?: number
  startAt: string
  endAt: string
  capacity: number
  reservedCount: number
  status: string
  serviceName: string
  staffName: string
  resourceName: string
}

export interface CardWarning {
  id: number
  memberName: string
  cardName: string
  remainCount?: number
  validUntil?: string
  status: string
}

export function messageOf(error: unknown, fallback = '操作失败，请稍后重试') {
  return error instanceof Error && error.message ? error.message : fallback
}

export async function guardOwner() {
  try {
    const profile = await api.request<OwnerProfile>('GET', '/owner/me')
    if (profile.status === 'frozen') {
      await Taro.reLaunch({ url: ownerRoutes.frozen })
      return null
    }
    return profile
  } catch (error) {
    const message = messageOf(error)
    if (message.includes('登录') || message.includes('会话') || message.includes('401')) {
      await Taro.reLaunch({ url: ownerRoutes.login })
      return null
    }
    throw error
  }
}

export function navigate(url: string) {
  return Taro.navigateTo({ url })
}

export function switchOwnerTab(url: string) {
  const current = `/${Taro.getCurrentInstance().router?.path || ''}`
  if (current === url) return Promise.resolve()
  return Taro.redirectTo({ url })
}

export function formatMoney(value: number | string | undefined) {
  return Number(value || 0).toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}

export function formatDate(value?: string) {
  if (!value) return '--'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return `${String(date.getMonth() + 1).padStart(2, '0')}/${String(date.getDate()).padStart(2, '0')}`
}

export function formatTime(value?: string) {
  if (!value) return '--:--'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value.slice(11, 16)
  return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

export function remainingDays(value?: string) {
  if (!value) return 0
  return Math.max(0, Math.ceil((new Date(value).getTime() - Date.now()) / 86400000))
}
