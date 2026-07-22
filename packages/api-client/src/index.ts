import type { ApiEnvelope } from '@serenmeet/shared-types'

export interface TransportRequest {
  url: string
  method: 'GET' | 'POST' | 'PUT' | 'DELETE'
  headers: Record<string, string>
  data?: unknown
}

export interface TransportResponse {
  statusCode: number
  data: unknown
}

export type ApiTransport = (request: TransportRequest) => Promise<TransportResponse>

export class SerenApiError extends Error {
  constructor(
    public readonly code: string,
    message: string,
    public readonly requestId?: string,
    public readonly fieldErrors?: Record<string, string> | null
  ) {
    super(message)
  }
}

export function createApiClient(options: {
  baseUrl: string
  transport: ApiTransport
  getToken: () => string | null
  onError?: (error: SerenApiError) => void
}) {
  return {
    async request<T>(
      method: TransportRequest['method'],
      path: string,
      data?: unknown,
      idempotencyKey?: string
    ): Promise<T> {
      const headers: Record<string, string> = { 'Content-Type': 'application/json' }
      const token = options.getToken()
      if (token) headers.Authorization = `Bearer ${token}`
      if (idempotencyKey) headers['Idempotency-Key'] = idempotencyKey
      const response = await options.transport({
        url: `${options.baseUrl}${path}`,
        method,
        headers,
        data
      })
      const payload = response.data as ApiEnvelope<T> | null
      if (response.statusCode < 200 || response.statusCode >= 300 || !payload?.success) {
        const error = new SerenApiError(
          payload?.error?.code || `HTTP_${response.statusCode}`,
          payload?.error?.message || '服务暂时不可用，请稍后重试',
          payload?.requestId,
          payload?.error?.fieldErrors
        )
        options.onError?.(error)
        throw error
      }
      return payload.data as T
    }
  }
}

export function createIdempotencyKey(scope: string) {
  return `${scope}-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
}
