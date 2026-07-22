const rawApiBaseUrl = process.env.TARO_APP_API_BASE_URL?.trim()

if (!rawApiBaseUrl) {
  fail('TARO_APP_API_BASE_URL is required for a real-device preview build.')
}

let apiBaseUrl
try {
  apiBaseUrl = new URL(rawApiBaseUrl)
} catch {
  fail('TARO_APP_API_BASE_URL must be an absolute URL.')
}

if (apiBaseUrl.protocol !== 'https:') {
  fail('TARO_APP_API_BASE_URL must use HTTPS for a real-device preview build.')
}

if (apiBaseUrl.hostname === 'localhost' || apiBaseUrl.hostname.startsWith('127.')) {
  fail('TARO_APP_API_BASE_URL cannot use a loopback host for a real-device preview build.')
}

function fail(message) {
  console.error(`Preview configuration error: ${message}`)
  process.exit(1)
}
