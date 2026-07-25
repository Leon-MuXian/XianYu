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

export const businessDayMeta = [
  { key: 'monday', label: '一', fullLabel: '周一' },
  { key: 'tuesday', label: '二', fullLabel: '周二' },
  { key: 'wednesday', label: '三', fullLabel: '周三' },
  { key: 'thursday', label: '四', fullLabel: '周四' },
  { key: 'friday', label: '五', fullLabel: '周五' },
  { key: 'saturday', label: '六', fullLabel: '周六' },
  { key: 'sunday', label: '日', fullLabel: '周日' }
] as const

export type BusinessDayKey = typeof businessDayMeta[number]['key']

export interface BusinessDayHours {
  open?: boolean
  start?: string
  end?: string
}

export interface BusinessHoursValue {
  days?: Partial<Record<BusinessDayKey, BusinessDayHours>>
}

export interface BusinessWeekDay {
  key: BusinessDayKey
  label: string
  fullLabel: string
  open: boolean
  start: string
  end: string
}

export interface BusinessHoursGroup {
  key: string
  open: boolean
  start: string
  end: string
  dayKeys: BusinessDayKey[]
  dayLabels: string[]
}

export interface BusinessHoursPresentation {
  week: BusinessWeekDay[]
  groups: BusinessHoursGroup[]
  openDayCount: number
  periodCount: number
  summary: string
}

export function presentBusinessHours(value?: BusinessHoursValue | null): BusinessHoursPresentation {
  const source = value?.days || {}
  const week = businessDayMeta.map(({ key, label, fullLabel }) => {
    const day = source[key]
    return {
      key,
      label,
      fullLabel,
      open: day?.open === true,
      start: day?.start || '',
      end: day?.end || ''
    }
  })
  const grouped = new Map<string, BusinessHoursGroup>()
  week.forEach((day) => {
    const groupKey = day.open ? `${day.start}|${day.end}` : 'closed'
    const current = grouped.get(groupKey)
    if (current) {
      current.dayKeys.push(day.key)
      current.dayLabels.push(day.fullLabel)
      return
    }
    grouped.set(groupKey, {
      key: groupKey,
      open: day.open,
      start: day.start,
      end: day.end,
      dayKeys: [day.key],
      dayLabels: [day.fullLabel]
    })
  })
  const groups = [...grouped.values()]
  const openDayCount = week.filter((day) => day.open).length
  const periodCount = groups.filter((group) => group.open).length
  return {
    week,
    groups,
    openDayCount,
    periodCount,
    summary: openDayCount ? `每周营业 ${openDayCount} 天 · ${periodCount} 个营业时段` : '暂未设置营业时间'
  }
}
