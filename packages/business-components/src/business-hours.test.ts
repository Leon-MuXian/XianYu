import { describe, expect, it } from 'vitest'
import { presentBusinessHours } from './index'

describe('presentBusinessHours', () => {
  it('keeps the Monday-to-Sunday order and groups equal time ranges', () => {
    const result = presentBusinessHours({
      days: {
        monday: { open: true, start: '09:00', end: '18:00' },
        tuesday: { open: true, start: '10:00', end: '20:00' },
        wednesday: { open: true, start: '09:00', end: '18:00' },
        thursday: { open: true, start: '10:00', end: '20:00' },
        friday: { open: true, start: '09:00', end: '18:00' },
        saturday: { open: true, start: '08:00', end: '12:00' },
        sunday: { open: false, start: '', end: '' }
      }
    })

    expect(result.week.map((day) => day.key)).toEqual([
      'monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday', 'sunday'
    ])
    expect(result.openDayCount).toBe(6)
    expect(result.periodCount).toBe(3)
    expect(result.summary).toBe('每周营业 6 天 · 3 个营业时段')
    expect(result.groups.map((group) => [group.dayLabels, group.open, group.start, group.end])).toEqual([
      [['周一', '周三', '周五'], true, '09:00', '18:00'],
      [['周二', '周四'], true, '10:00', '20:00'],
      [['周六'], true, '08:00', '12:00'],
      [['周日'], false, '', '']
    ])
  })

  it('treats missing days as closed and provides an empty summary', () => {
    const result = presentBusinessHours()

    expect(result.week).toHaveLength(7)
    expect(result.week.every((day) => !day.open)).toBe(true)
    expect(result.groups).toHaveLength(1)
    expect(result.groups[0].dayLabels).toEqual(['周一', '周二', '周三', '周四', '周五', '周六', '周日'])
    expect(result.summary).toBe('暂未设置营业时间')
  })
})
