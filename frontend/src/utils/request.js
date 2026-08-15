import axios from 'axios'
import { useUserStore } from '@/store/user'

// axios 封装：统一 baseURL / token 注入 / 响应解包 {code,msg,data}
const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.request.use((config) => {
  const store = useUserStore()
  if (store.token) {
    config.headers.Authorization = `Bearer ${store.token}`
  }
  return config
})

request.interceptors.response.use(
  (resp) => {
    const body = resp.data
    if (body && body.code === 0) {
      return body.data
    }
    return Promise.reject(new Error(body?.msg || '请求失败'))
  },
  (err) => {
    // TODO(免登接入): 401 → 清除 token → 重新免登 → 重放原请求（用户无感）
    if (err.response?.status === 401) {
      const store = useUserStore()
      store.clear()
    }
    return Promise.reject(err)
  }
)

export default request
