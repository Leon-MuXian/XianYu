import { SerenApiError } from '@serenmeet/api-client'

export const CARD_TEMPLATE_NAME_TAKEN_CODE = 'CARD_TEMPLATE_NAME_TAKEN'
export const CARD_TEMPLATE_NAME_TAKEN_MESSAGE = '当前租户已存在同名会员卡，请更换名称'

interface CardTemplateNameCandidate {
  id: number
  name: string
}

export function normalizeCardTemplateName(value: string) {
  const compatibleValue = typeof value.normalize === 'function' ? value.normalize('NFKC') : value
  return compatibleValue.trim().replace(/[\s\u00a0\u1680\u2000-\u200a\u2028\u2029\u202f\u205f\u3000]+/g, ' ')
}

export function cardTemplateNameKey(value: string) {
  return normalizeCardTemplateName(value).toLowerCase()
}

export function hasDuplicateCardTemplateName(
  templates: CardTemplateNameCandidate[],
  value: string,
  currentTemplateId?: number
) {
  const candidateKey = cardTemplateNameKey(value)
  return Boolean(candidateKey) && templates.some((template) => (
    template.id !== currentTemplateId && cardTemplateNameKey(template.name) === candidateKey
  ))
}

export function formatCardSelectionSummary(names: string[], noun: '服务' | '员工', placeholder: string) {
  if (!names.length) return placeholder
  if (names.length <= 2) return names.join('、')
  return `已选 ${names.length} 个${noun}`
}

export function isCardTemplateNameTaken(error: unknown) {
  return error instanceof SerenApiError && error.code === CARD_TEMPLATE_NAME_TAKEN_CODE
}
