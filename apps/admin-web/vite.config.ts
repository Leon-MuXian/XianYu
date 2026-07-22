import { fileURLToPath, URL } from 'node:url'
import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  build: {
    rollupOptions: {
      onwarn(warning, warn) {
        if (warning.code === 'INVALID_ANNOTATION' && warning.id?.includes('/@vueuse/core/')) return
        warn(warning)
      },
      output: {
        manualChunks(id) {
          const componentMarker = '/node_modules/element-plus/es/components/'
          if (id.includes(componentMarker)) {
            const component = id.split(componentMarker)[1].split('/')[0]
            return `element-${component === 'select-v2' ? 'select' : component}`
          }
          if (id.includes('/node_modules/@element-plus/icons-vue/')) return 'element-icons'
          if (id.includes('/node_modules/vue/') || id.includes('/node_modules/vue-router/')) return 'vue-runtime'
          return undefined
        }
      }
    }
  },
  server: {
    port: 5173
  }
})
