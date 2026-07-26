import { SerenApiError } from '@serenmeet/api-client'

export const SERVICE_NAME_TAKEN_CODE = 'SERVICE_NAME_TAKEN'
export const SERVICE_NAME_TAKEN_MESSAGE = '当前租户已存在同名服务项目，请更换名称'

interface ServiceNameCandidate {
  id: number
  name: string
}

export function normalizeServiceName(value: string) {
  const compatibleValue = typeof value.normalize === 'function' ? value.normalize('NFKC') : value
  return compatibleValue.trim().replace(/[\s\u00a0\u1680\u2000-\u200a\u2028\u2029\u202f\u205f\u3000]+/g, ' ')
}

export function serviceNameKey(value: string) {
  return normalizeServiceName(value).toLowerCase()
}

export function hasDuplicateServiceName(
  services: ServiceNameCandidate[],
  value: string,
  currentServiceId?: number
) {
  const candidateKey = serviceNameKey(value)
  return Boolean(candidateKey) && services.some((service) => (
    service.id !== currentServiceId && serviceNameKey(service.name) === candidateKey
  ))
}

export function formatSelectionSummary(names: string[], noun: '资源' | '员工', placeholder: string) {
  if (!names.length) return placeholder
  if (names.length <= 2) return names.join('、')
  return `已选 ${names.length} 个${noun}`
}

export function isServiceNameTaken(error: unknown) {
  return error instanceof SerenApiError && error.code === SERVICE_NAME_TAKEN_CODE
}
