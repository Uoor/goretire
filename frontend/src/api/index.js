import request from '@/utils/request'
import { isHuaweiUws } from '@/utils/ua'

// 跨模块基础接口（壳层）
// 业务模块接口见 src/modules/<module>/api.js

export const authApi = {
  login: (code) => request.post('/auth', { code })
}

/**
 * FileReader 读文件为 data URL（最基础兼容 API，华为 UWS 内核亦支持）。
 */
function readAsDataURL(file) {
  return new Promise((resolve, reject) => {
    const fr = new FileReader()
    fr.onload = () => resolve(fr.result)
    fr.onerror = () => reject(new Error('图片读取失败'))
    fr.readAsDataURL(file)
  })
}

/**
 * base64 + JSON 上传（华为 UWS 内核最兼容路径）：
 * 社区通行方案（华为官方问答：鸿蒙 WebView 上 axios/multipart 上传不可靠 → 传 base64 让接口支持）。
 * 完全绕开 FormData/File/canvas，只用 FileReader + XHR。
 */
async function uploadBase64(file) {
  const dataUrl = await readAsDataURL(file)
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest()
    xhr.open('POST', `${import.meta.env.BASE_URL}api/upload/base64`)
    xhr.timeout = 20000
    xhr.setRequestHeader('Content-Type', 'application/json')
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
    xhr.send(JSON.stringify({ name: file.name || 'image.jpg', data: dataUrl }))
  })
}

// 图片上传（跨模块基础能力）：返回 /uploads/xxx 相对 URL（经拦截器/原生路径加子路径前缀）
export const uploadApi = {
  image: (file) => {
    // 华为 UWS 内核走 base64 + JSON（最兼容，规避 multipart/FormData 兼容坑）
    if (isHuaweiUws) {
      return uploadBase64(file)
    }
    const fd = new FormData()
    fd.append('file', file)
    return request.post('/upload', fd, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}
