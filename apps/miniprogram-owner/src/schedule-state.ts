export type ScheduleDisplayState = 'draft' | 'published' | 'active' | 'ended' | 'cancelled' | 'disabled'

export interface ScheduleDisplayInput {
  status: string
  startAt: string
  endAt: string
}

export function scheduleDisplayState(schedule: ScheduleDisplayInput, now = Date.now()) {
  if (schedule.status === 'draft') {
    return { state: 'draft' as ScheduleDisplayState, label: '草稿 · 点击继续' }
  }
  if (schedule.status === 'cancelled') {
    return { state: 'cancelled' as ScheduleDisplayState, label: '已取消' }
  }
  if (schedule.status === 'disabled') {
    return { state: 'disabled' as ScheduleDisplayState, label: '已停用' }
  }
  if (schedule.status !== 'published') {
    return { state: 'disabled' as ScheduleDisplayState, label: '不可用' }
  }
  const startAt = Date.parse(schedule.startAt)
  const endAt = Date.parse(schedule.endAt)
  if (!Number.isNaN(endAt) && endAt <= now) {
    return { state: 'ended' as ScheduleDisplayState, label: '已结束' }
  }
  if (!Number.isNaN(startAt) && startAt <= now) {
    return { state: 'active' as ScheduleDisplayState, label: '进行中' }
  }
  return { state: 'published' as ScheduleDisplayState, label: '会员可预约' }
}
