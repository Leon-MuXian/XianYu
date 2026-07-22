export const cardStatusText = {
  suspended: '已暂停',
  expired: '已过期',
  empty: '已用完',
  expiring: '即将到期',
  low_balance: '余额不足',
  active: '可使用'
} as const

export const tenantStatusText = {
  trialing: '试用中',
  expiring: '即将到期',
  frozen: '已冻结',
  extended: '已延长'
} as const

export interface FeedbackState {
  loading: boolean
  message: string
  fieldErrors: Record<string, string>
}

export const emptyFeedback = (): FeedbackState => ({ loading: false, message: '', fieldErrors: {} })
