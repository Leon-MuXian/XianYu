import { describe, expect, it } from 'vitest'
import { SerenApiError } from '@serenmeet/api-client'
import {
  formatStaffCredentialCopy,
  hasStaffLoginPasswordChanged,
  isStaffLoginNameTaken,
  normalizeStaffLoginName,
  STAFF_LOGIN_NAME_TAKEN_CODE
} from './staff-form'

describe('staff login name form rules', () => {
  it('normalizes case, surrounding whitespace and NFKC variants', () => {
    expect(normalizeStaffLoginName('  Ｓtaff_Nora  ')).toBe('staff_nora')
  })

  it('recognizes the dedicated duplicate login error', () => {
    expect(isStaffLoginNameTaken(new SerenApiError(
      STAFF_LOGIN_NAME_TAKEN_CODE,
      '该登录账号已被使用，请更换账号'
    ))).toBe(true)
    expect(isStaffLoginNameTaken(new SerenApiError('CONFLICT', '其他冲突'))).toBe(false)
  })

  it('formats a complete credential copy without dropping the password', () => {
    expect(formatStaffCredentialCopy('李在明', 'zaiming.li', 'lms@123456')).toBe(
      '李在明\n登录账号：zaiming.li\n登录密码：lms@123456'
    )
  })

  it('only treats a different password as a password update', () => {
    expect(hasStaffLoginPasswordChanged('lms@123456', 'lms@123456')).toBe(false)
    expect(hasStaffLoginPasswordChanged('lms@123456', 'new@123456')).toBe(true)
  })
})
