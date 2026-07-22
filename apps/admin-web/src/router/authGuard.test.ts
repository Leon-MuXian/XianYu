import { describe, expect, it } from 'vitest'
import { resolveAdminRoute } from './authGuard'

describe('admin route guard', () => {
  it('redirects anonymous users to login', () => {
    expect(resolveAdminRoute('/tenants', null)).toBe('/login')
  })

  it('keeps authenticated users out of login', () => {
    expect(resolveAdminRoute('/login', 'token')).toBe('/tenants')
  })

  it('allows the intended destination', () => {
    expect(resolveAdminRoute('/login', null)).toBe(true)
    expect(resolveAdminRoute('/audit', 'token')).toBe(true)
  })
})
