import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// 钉钉 H5 微应用：hash 路由由 Vue Router 处理；开发期代理 /api 到本地后端
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
      '/api': {
        // 本机 8080 被占用，后端以 --server.port=8081 启动
        target: 'http://localhost:8081',
        changeOrigin: true
      }
    }
  }
})
