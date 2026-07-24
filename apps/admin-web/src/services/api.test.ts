import { beforeEach, describe, expect, it, vi } from 'vitest'

const storage = new Map<string, string>()
const browserLocation = {
  pathname: '/tenants',
  replace: vi.fn()
}

vi.stubGlobal('window', {
  localStorage: {
    getItem: (key: string) => storage.get(key) ?? null,
    setItem: (key: string, value: string) => storage.set(key, value),
    removeItem: (key: string) => storage.delete(key)
  },
  location: browserLocation
})

import { adminApi, clearToken, getStoredAdminUser, getToken } from './api'

function mockResponse(data: unknown) {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
    status: 200,
    text: async () => JSON.stringify({ success: true, data })
  }))
}

function mockErrorResponse(status: number, code: string, message: string) {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
    status,
    text: async () => JSON.stringify({
      success: false,
      data: null,
      error: { code, message }
    })
  }))
}

describe('admin session identity', () => {
  beforeEach(() => {
    storage.clear()
    browserLocation.pathname = '/tenants'
    browserLocation.replace.mockClear()
    vi.restoreAllMocks()
  })

  it('stores the authenticated user returned by login', async () => {
    const user = { id: 7, username: 'ops@serenmeet', displayName: '运营管理员' }
    mockResponse({ token: 'token-1', expiresAt: '2026-07-25T00:00:00+08:00', user })

    await adminApi.login('ops@serenmeet', 'password')

    expect(getToken()).toBe('token-1')
    expect(getStoredAdminUser()).toEqual(user)
  })

  it('refreshes and clears the current admin identity with the session', async () => {
    const user = { id: 8, username: 'admin@example.com', displayName: '平台管理员' }
    mockResponse(user)

    await adminApi.getCurrentUser()
    expect(getStoredAdminUser()).toEqual(user)

    clearToken()
    expect(getToken()).toBeNull()
    expect(getStoredAdminUser()).toBeNull()
  })

  it('clears the expired session and redirects to login', async () => {
    const user = { id: 9, username: 'admin@serenmeet', displayName: '平台管理员' }
    mockResponse({ token: 'expired-token', expiresAt: '2026-07-25T00:00:00+08:00', user })
    await adminApi.login('admin@serenmeet', 'password')

    mockErrorResponse(401, 'LOGIN_REQUIRED', '请先登录平台后台')
    await expect(adminApi.listConfigs({})).rejects.toMatchObject({ code: 'LOGIN_REQUIRED' })

    expect(getToken()).toBeNull()
    expect(getStoredAdminUser()).toBeNull()
    expect(browserLocation.replace).toHaveBeenCalledOnce()
    expect(browserLocation.replace).toHaveBeenCalledWith('/login')
  })

  it('keeps the current session for errors unrelated to expiration', async () => {
    const user = { id: 10, username: 'admin@serenmeet', displayName: '平台管理员' }
    mockResponse({ token: 'active-token', expiresAt: '2026-07-25T00:00:00+08:00', user })
    await adminApi.login('admin@serenmeet', 'password')

    mockErrorResponse(403, 'FORBIDDEN', '当前账号无权执行此操作')
    await expect(adminApi.listConfigs({})).rejects.toMatchObject({ code: 'FORBIDDEN' })

    expect(getToken()).toBe('active-token')
    expect(getStoredAdminUser()).toEqual(user)
    expect(browserLocation.replace).not.toHaveBeenCalled()
  })

  it('does not redirect when login credentials are invalid', async () => {
    browserLocation.pathname = '/login'
    mockErrorResponse(401, 'INVALID_CREDENTIALS', '账号或密码错误')

    await expect(adminApi.login('admin@serenmeet', 'wrong-password')).rejects.toMatchObject({ code: 'INVALID_CREDENTIALS' })

    expect(getToken()).toBeNull()
    expect(browserLocation.replace).not.toHaveBeenCalled()
  })
})
