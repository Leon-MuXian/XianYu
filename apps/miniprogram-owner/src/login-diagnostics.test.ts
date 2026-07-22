import { describe, expect, it } from 'vitest'
import { loginEnvironmentIssue, loginFailureMessage } from './login-diagnostics'

describe('owner login diagnostics', () => {
  it('blocks loopback API addresses on a real device', () => {
    expect(loginEnvironmentIssue('http://127.0.0.1:8080', 'ios')).toContain('真机无法访问')
    expect(loginEnvironmentIssue('http://localhost:8080', 'android')).toContain('真机无法访问')
  })

  it('allows loopback API addresses inside devtools', () => {
    expect(loginEnvironmentIssue('http://127.0.0.1:8080', 'devtools')).toBe('')
  })

  it('explains request-domain and network failures', () => {
    expect(loginFailureMessage({ errMsg: 'request:fail url not in domain list' })).toContain('request 合法域名')
    expect(loginFailureMessage({ errMsg: 'request:fail timeout' })).toContain('无法连接登录服务')
  })
})
