export function resolveAdminRoute(path: string, token: string | null) {
  if (path !== '/login' && !token) return '/login'
  if (path === '/login' && token) return '/tenants'
  return true
}
