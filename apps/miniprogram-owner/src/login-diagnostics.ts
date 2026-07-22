const LOOPBACK_API_PATTERN = /^https?:\/\/(127(?:\.\d{1,3}){3}|localhost)(?::\d+)?(?:\/|$)/i

export function loginEnvironmentIssue(apiBaseUrl: string, platform: string) {
  if (platform !== 'devtools' && LOOPBACK_API_PATTERN.test(apiBaseUrl)) {
    return '当前预览包仍指向本机地址，真机无法访问。请使用真机可访问的 HTTPS API 地址重新构建。'
  }
  return ''
}

export function loginFailureMessage(error: unknown) {
  const detail = errorDetail(error)
  if (/url not in domain list|request 合法域名|不在以下.*合法域名/i.test(detail)) {
    return 'API 域名尚未加入微信 request 合法域名，请完成平台配置后重试。'
  }
  if (/request:fail|network|unable to resolve host|connection refused|timeout/i.test(detail)) {
    return '无法连接登录服务，请检查预览包 API 地址、HTTPS 证书和手机网络。'
  }
  return detail || '微信登录失败，请重试'
}

function errorDetail(error: unknown) {
  if (error instanceof Error && error.message) return error.message
  if (typeof error === 'object' && error !== null && 'errMsg' in error) {
    const errMsg = (error as { errMsg?: unknown }).errMsg
    return typeof errMsg === 'string' ? errMsg : ''
  }
  return ''
}
