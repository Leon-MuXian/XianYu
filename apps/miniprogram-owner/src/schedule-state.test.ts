import { describe, expect, it } from 'vitest'
import { scheduleDisplayState } from './schedule-state'

describe('schedule display state', () => {
  it('distinguishes active, ended and upcoming published schedules', () => {
    const now = Date.parse('2026-07-27T01:33:00Z')
    expect(scheduleDisplayState({ status: 'published', startAt: '2026-07-27T01:00:00Z', endAt: '2026-07-27T02:00:00Z' }, now))
      .toEqual({ state: 'active', label: '进行中' })
    expect(scheduleDisplayState({ status: 'published', startAt: '2026-07-26T10:30:00Z', endAt: '2026-07-26T11:30:00Z' }, now))
      .toEqual({ state: 'ended', label: '已结束' })
    expect(scheduleDisplayState({ status: 'published', startAt: '2026-07-28T10:00:00Z', endAt: '2026-07-28T11:00:00Z' }, now))
      .toEqual({ state: 'published', label: '会员可预约' })
  })
})
