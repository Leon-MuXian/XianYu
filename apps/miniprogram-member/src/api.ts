import { createTaroApiClient } from '@serenmeet/api-client/taro'

export const api = createTaroApiClient(
  process.env.TARO_APP_API_BASE_URL || 'http://127.0.0.1:8080',
  '/pages/frozen/index'
)
