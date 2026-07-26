import { describe, expect, it } from 'vitest'
import { SerenApiError } from '@serenmeet/api-client'
import {
  CARD_TEMPLATE_NAME_TAKEN_CODE,
  cardTemplateNameKey,
  formatCardSelectionSummary,
  hasDuplicateCardTemplateName,
  isCardTemplateNameTaken,
  normalizeCardTemplateName
} from './card-form'

describe('card form helpers', () => {
  it('normalizes compatibility characters and whitespace', () => {
    expect(normalizeCardTemplateName('  Ｐose　 Card  ')).toBe('Pose Card')
    expect(cardTemplateNameKey('ＰＯＳＥ　ＣＡＲＤ')).toBe('pose card')
  })

  it('detects duplicates while excluding the current card', () => {
    const templates = [{ id: 1, name: 'Pose Card' }, { id: 2, name: '月度卡' }]
    expect(hasDuplicateCardTemplateName(templates, ' pose  card ')).toBe(true)
    expect(hasDuplicateCardTemplateName(templates, 'ＰＯＳＥ　ＣＡＲＤ', 1)).toBe(false)
  })

  it('collapses long multi-select values to a stable count', () => {
    expect(formatCardSelectionSummary([], '服务', '请选择服务')).toBe('请选择服务')
    expect(formatCardSelectionSummary(['评估', '训练'], '服务', '请选择服务')).toBe('评估、训练')
    expect(formatCardSelectionSummary(['评估', '训练', '咨询'], '服务', '请选择服务')).toBe('已选 3 个服务')
  })

  it('recognizes the tenant name conflict response', () => {
    expect(isCardTemplateNameTaken(new SerenApiError(CARD_TEMPLATE_NAME_TAKEN_CODE, 'duplicate'))).toBe(true)
  })
})
