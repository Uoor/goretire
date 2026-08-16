import request from '@/utils/request'

// 跨模块基础接口（壳层）
// 业务模块接口见 src/modules/<module>/api.js

export const authApi = {
  login: (code) => request.post('/auth', { code })
}

// 图片上传（跨模块基础能力）：返回 /uploads/xxx 相对 URL
export const uploadApi = {
  image: (file) => {
    const fd = new FormData()
    fd.append('file', file)
    return request.post('/upload', fd, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}
