import { createTaroApiClient } from '@serenmeet/api-client/taro'

export const apiBaseUrl = process.env.TARO_APP_API_BASE_URL || 'http://127.0.0.1:8080'

export const api = createTaroApiClient(
  apiBaseUrl,
  '/pages/frozen/index'
)
