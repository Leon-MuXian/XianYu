export const FIXED_RESOURCE_TYPES = ['房间', '场地', '床位', '设备', '工位'] as const
export const CUSTOM_RESOURCE_TYPE = '自定义'

export type ResourceTypeSelection = (typeof FIXED_RESOURCE_TYPES)[number] | typeof CUSTOM_RESOURCE_TYPE

export interface ResourceTypeState {
  selection: ResourceTypeSelection
  customValue: string
}

export function resourceTypeStateFromValue(value: string): ResourceTypeState {
  const normalized = value.trim()
  if (FIXED_RESOURCE_TYPES.includes(normalized as (typeof FIXED_RESOURCE_TYPES)[number])) {
    return { selection: normalized as (typeof FIXED_RESOURCE_TYPES)[number], customValue: '' }
  }
  return {
    selection: CUSTOM_RESOURCE_TYPE,
    customValue: normalized === CUSTOM_RESOURCE_TYPE ? '' : normalized
  }
}

export function resolveResourceType(selection: ResourceTypeSelection, customValue: string): string {
  return selection === CUSTOM_RESOURCE_TYPE ? customValue.trim() : selection
}
