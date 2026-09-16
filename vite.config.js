import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    proxy: {
      // Axios의 /api 요청을 Spring Boot(8080) 포트로 자동 프록시 라우팅
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      }
    }
  }
})