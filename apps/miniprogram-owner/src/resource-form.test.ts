import { describe, expect, it } from 'vitest'
import {
  CUSTOM_RESOURCE_TYPE,
  resourceTypeStateFromValue,
  resolveResourceType
} from './resource-form'

describe('owner resource form', () => {
  it('keeps fixed resource types in shortcut mode', () => {
    expect(resourceTypeStateFromValue(' 设备 ')).toEqual({ selection: '设备', customValue: '' })
    expect(resolveResourceType('房间', '不会提交')).toBe('房间')
  })

  it('opens custom mode and restores a saved custom type', () => {
    expect(resourceTypeStateFromValue(' 咨询舱 ')).toEqual({
      selection: CUSTOM_RESOURCE_TYPE,
      customValue: '咨询舱'
    })
  })

  it('does not treat the custom placeholder as a real resource type', () => {
    expect(resourceTypeStateFromValue(CUSTOM_RESOURCE_TYPE)).toEqual({
      selection: CUSTOM_RESOURCE_TYPE,
      customValue: ''
    })
    expect(resolveResourceType(CUSTOM_RESOURCE_TYPE, '  静语舱  ')).toBe('静语舱')
  })
})
