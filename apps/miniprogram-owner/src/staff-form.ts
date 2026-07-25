import { SerenApiError } from '@serenmeet/api-client'

export const STAFF_LOGIN_NAME_TAKEN_CODE = 'STAFF_LOGIN_NAME_TAKEN'
export const STAFF_LOGIN_NAME_TAKEN_MESSAGE = '该登录账号已被使用，请更换账号'

export function formatStaffCredentialCopy(
  staffName: string,
  loginName: string,
  password: string
) {
  return `${staffName}\n登录账号：${loginName}\n登录密码：${password}`
}

export function hasStaffLoginPasswordChanged(initialPassword: string, nextPassword: string) {
  return initialPassword !== nextPassword
}

export function normalizeStaffLoginName(value: string) {
  const compatibleValue = typeof value.normalize === 'function' ? value.normalize('NFKC') : value
  return compatibleValue.trim().toLowerCase()
}

export function isStaffLoginNameTaken(error: unknown) {
  return error instanceof SerenApiError && error.code === STAFF_LOGIN_NAME_TAKEN_CODE
}
