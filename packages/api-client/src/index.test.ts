import { describe, expect, it, vi } from 'vitest'
import { createApiClient, SerenApiError } from './index'
describe('api client', () => {
  it('surfaces structured field errors and request id', async () => {
    const client = createApiClient({
      baseUrl: '/api',
      getToken: () => 'token',
      transport: async () => ({
        statusCode: 400,
        data: {
          requestId: 'req-1',
          success: false,
          data: null,
          error: { code: 'FIELD_ERROR', message: '请检查表单内容', fieldErrors: { name: '不能为空' } }
        }
      })
    })
    await expect(client.request('POST', '/owner/members', {})).rejects.toMatchObject<Partial<SerenApiError>>({
      code: 'FIELD_ERROR',
      requestId: 'req-1',
      fieldErrors: { name: '不能为空' }
    })
  })

  it('sends authorization and idempotency headers', async () => {
    const transport = vi.fn(async () => ({
      statusCode: 200,
      data: { requestId: 'req-2', success: true, data: { saved: true }, error: null }
    }))
    const client = createApiClient({ baseUrl: '/api', getToken: () => 'secret-token', transport })
    await client.request('POST', '/owner/cards/issue', { memberId: 1 }, 'issue-1')
    expect(transport).toHaveBeenCalledWith(expect.objectContaining({
      url: '/api/owner/cards/issue',
      headers: expect.objectContaining({ Authorization: 'Bearer secret-token', 'Idempotency-Key': 'issue-1' })
    }))
  })

  it('notifies the caller about frozen tenant errors', async () => {
    const onError = vi.fn()
    const client = createApiClient({
      baseUrl: '/api',
      getToken: () => 'token',
      onError,
      transport: async () => ({
        statusCode: 403,
        data: { requestId: 'req-3', success: false, data: null, error: { code: 'TENANT_FROZEN', message: '门店服务已冻结' } }
      })
    })
    await expect(client.request('GET', '/owner/me')).rejects.toMatchObject({ code: 'TENANT_FROZEN' })
    expect(onError).toHaveBeenCalledWith(expect.objectContaining({ code: 'TENANT_FROZEN' }))
  })
})
