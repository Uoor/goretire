import request from '@/utils/request'
import { isHuaweiUws } from '@/utils/ua'

// 跨模块基础接口（壳层）
// 业务模块接口见 src/modules/<module>/api.js

export const authApi = {
  login: (code) => request.post('/auth', { code })
}

/**
 * 原生 XHR 上传（华为 UWS 内核最兼容路径）：
 * 绕过 axios，直接 XMLHttpRequest + FormData，响应同样做 /uploads → 子路径前缀适配。
 */
function uploadViaXHR(file) {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest()
    xhr.open('POST', `${import.meta.env.BASE_URL}api/upload`)
    xhr.timeout = 15000
    const token = localStorage.getItem('aliren_token')
    if (token) {
      xhr.setRequestHeader('Authorization', `Bearer ${token}`)
    }
    xhr.onload = () => {
      try {
        const body = JSON.parse(xhr.responseText)
        if (body && body.code === 0) {
          const data = body.data
          resolve(
            typeof data === 'string' && data.startsWith('/uploads/')
              ? `${import.meta.env.BASE_URL}uploads${data.slice('/uploads'.length)}`
              : data
          )
        } else {
          reject(new Error(body?.msg || `上传失败(${xhr.status})`))
        }
      } catch {
        reject(new Error('上传响应解析失败'))
      }
    }
    xhr.onerror = () => reject(new Error('网络异常，请重试'))
    xhr.ontimeout = () => reject(new Error('上传超时，请重试'))
    const fd = new FormData()
    fd.append('file', file)
    xhr.send(fd)
  })
}

// 图片上传（跨模块基础能力）：返回 /uploads/xxx 相对 URL（经拦截器加子路径前缀）
export const uploadApi = {
  image: (file) => {
    // 华为 UWS 内核优先走原生 XHR（最兼容，规避 axios/FormData 兼容坑）
    if (isHuaweiUws) {
      return uploadViaXHR(file)
    }
    const fd = new FormData()
    fd.append('file', file)
    return request.post('/upload', fd, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}
