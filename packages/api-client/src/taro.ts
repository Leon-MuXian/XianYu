import Taro from '@tarojs/taro'
import { createApiClient } from './index.js'

const TOKEN_KEY = 'seren_meet_session_token'

export function getSessionToken() {
  return Taro.getStorageSync<string>(TOKEN_KEY) || null
}

export function setSessionToken(token: string) {
  Taro.setStorageSync(TOKEN_KEY, token)
}

export function clearSessionToken() {
  Taro.removeStorageSync(TOKEN_KEY)
}

export function createTaroApiClient(baseUrl: string, frozenPath?: string) {
  return createApiClient({
    baseUrl,
    getToken: getSessionToken,
    onError: error => {
      if (error.code === 'TENANT_FROZEN' && frozenPath) {
        void Taro.reLaunch({ url: frozenPath })
      }
    },
    transport: async request => {
      const response = await Taro.request({
        url: request.url,
        method: request.method,
        header: request.headers,
        data: request.data
      })
      return { statusCode: response.statusCode, data: response.data }
    }
  })
}
