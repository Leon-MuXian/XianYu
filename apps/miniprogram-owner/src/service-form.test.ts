import { SerenApiError } from '@serenmeet/api-client'
import { describe, expect, it } from 'vitest'
import {
  formatSelectionSummary,
  hasDuplicateServiceName,
  isServiceNameTaken,
  normalizeServiceName,
  SERVICE_NAME_TAKEN_CODE,
  serviceNameKey
} from './service-form'

describe('service form rules', () => {
  it('normalizes compatible characters, whitespace and case for comparison', () => {
    expect(normalizeServiceName('  Ｐose　 Care  ')).toBe('Pose Care')
    expect(serviceNameKey('  Ｐose　 Care  ')).toBe('pose care')
  })

  it('detects duplicates while allowing the edited service to keep its own name', () => {
    const services = [
      { id: 1, name: 'Pose Care' },
      { id: 2, name: '运动康复' }
    ]
    expect(hasDuplicateServiceName(services, 'ＰＯＳＥ　ＣＡＲＥ')).toBe(true)
    expect(hasDuplicateServiceName(services, 'ＰＯＳＥ　ＣＡＲＥ', 1)).toBe(false)
  })

  it('recognizes the dedicated duplicate service error', () => {
    expect(isServiceNameTaken(new SerenApiError(
      SERVICE_NAME_TAKEN_CODE,
      '当前租户已存在同名服务项目，请更换名称'
    ))).toBe(true)
    expect(isServiceNameTaken(new SerenApiError('CONFLICT', '其他冲突'))).toBe(false)
  })

  it('collapses long multi-select values into stable counts', () => {
    expect(formatSelectionSummary([], '资源', '请选择资源')).toBe('请选择资源')
    expect(formatSelectionSummary(['评估室 A'], '资源', '请选择资源')).toBe('评估室 A')
    expect(formatSelectionSummary(['评估室 A', '咨询室 B'], '资源', '请选择资源'))
      .toBe('评估室 A、咨询室 B')
    expect(formatSelectionSummary(['Luna', 'Mia', 'Ken'], '员工', '请选择员工'))
      .toBe('已选 3 个员工')
  })
})
