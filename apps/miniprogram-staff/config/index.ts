import { defineConfig } from '@tarojs/cli'

const apiBaseUrl = process.env.TARO_APP_API_BASE_URL || 'http://127.0.0.1:8080'

export default defineConfig({
  projectName: 'seren-meet-staff',
  date: '2026-07-21',
  designWidth: 375,
  sourceRoot: 'src',
  outputRoot: 'dist',
  framework: 'vue3',
  compiler: 'webpack5',
  plugins: ['@tarojs/plugin-platform-weapp'],
  env: { TARO_APP_API_BASE_URL: JSON.stringify(apiBaseUrl) },
  mini: { postcss: { pxtransform: { enable: true } } }
})
