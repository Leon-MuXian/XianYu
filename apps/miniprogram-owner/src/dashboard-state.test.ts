import { describe, expect, it } from 'vitest'
import { hasCompletedScheduleTask, isDashboardOperational, pendingDashboardTaskCount } from './dashboard-state'

const completeMetrics = {
  storeReady: true,
  staffCount: 1,
  serviceCount: 1,
  cardTemplateCount: 1,
  memberCount: 0,
  operationalSlotCount: 1,
  scheduleReadiness: { ready: true }
}

describe('owner dashboard state', () => {
  it('enters operational mode for an active or upcoming schedule without members', () => {
    expect(hasCompletedScheduleTask(completeMetrics)).toBe(true)
    expect(isDashboardOperational(completeMetrics)).toBe(true)
    expect(pendingDashboardTaskCount(completeMetrics)).toBe(1)
  })

  it('keeps the setup task when there is no active or upcoming published schedule', () => {
    const metrics = { ...completeMetrics, operationalSlotCount: 0 }
    expect(isDashboardOperational(metrics)).toBe(false)
    expect(pendingDashboardTaskCount(metrics)).toBe(2)
  })

  it('keeps the setup state when the schedule scope is incomplete', () => {
    const metrics = {
      ...completeMetrics,
      scheduleReadiness: { ready: false }
    }
    expect(isDashboardOperational(metrics)).toBe(false)
    expect(pendingDashboardTaskCount(metrics)).toBe(2)
  })
})
