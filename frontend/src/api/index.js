import request from '@/utils/request'

// 跨模块基础接口（壳层）
// 业务模块接口见 src/modules/<module>/api.js

export const authApi = {
  login: (code) => request.post('/auth', { code })
}
