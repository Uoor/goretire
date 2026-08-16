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
    // host: true 暴露局域网地址，手机同 WiFi 扫码真机预览（最接近钉钉内体验）
    host: true,
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
