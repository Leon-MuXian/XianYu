export type { components, operations, paths } from './generated.js'
export type * from './admin.js'

export type ActorType = 'admin' | 'owner' | 'staff' | 'member'
export type MemberCardStatus = 'suspended' | 'expired' | 'empty' | 'expiring' | 'low_balance' | 'active'

export interface ApiEnvelope<T> {
  requestId: string
  success: boolean
  data: T | null
  error: {
    code: string
    message: string
    fieldErrors?: Record<string, string> | null
  } | null
}
