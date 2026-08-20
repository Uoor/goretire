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
    // 允许任意 Host 访问（局域网 IP + cloudflared 隧道域名都可能是来源）
    allowedHosts: true,
    proxy: {
      '/api': {
        // 本地开发连接远程服务器
        target: 'https://test.nekomiao.com',
        changeOrigin: true,
        secure: true
      },
      // 上传图片静态访问
      '/uploads': {
        target: 'https://test.nekomiao.com',
        changeOrigin: true,
        secure: true
      }
    }
  }
})
