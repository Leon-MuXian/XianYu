export interface DashboardReadiness {
  ready: boolean
}

export interface DashboardStateMetrics {
  storeReady: boolean
  staffCount: number
  serviceCount: number
  cardTemplateCount: number
  memberCount: number
  operationalSlotCount: number
  scheduleReadiness: DashboardReadiness
}

export function hasCompletedScheduleTask(metrics: DashboardStateMetrics) {
  return metrics.scheduleReadiness.ready && metrics.operationalSlotCount > 0
}

export function isDashboardOperational(metrics: DashboardStateMetrics) {
  return metrics.storeReady && hasCompletedScheduleTask(metrics)
}

export function pendingDashboardTaskCount(metrics: DashboardStateMetrics) {
  const configurationGaps = [
    metrics.staffCount,
    metrics.serviceCount,
    metrics.cardTemplateCount,
    metrics.memberCount
  ].filter((value) => !value).length
  return configurationGaps + (hasCompletedScheduleTask(metrics) ? 0 : 1)
}
