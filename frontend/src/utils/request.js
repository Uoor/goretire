import axios from 'axios'
import { useUserStore } from '@/store/user'
import { showDialog, showToast } from 'vant'

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

/**
 * 401 处理（阻断式）：
 * - /api/auth（免登/换身份）401 → 免登失败：携带 __loginFailed 标记，
 *   由路由层跳「加入组织」引导页（非组织成员等）
 * - 其他业务 401 → token 失效/无权限：清除会话并弹窗阻止（确认后回首页重登）
 */
request.interceptors.response.use(
  (resp) => {
    const body = resp.data
    if (body && body.code === 0) {
      return body.data
    }
    return Promise.reject(new Error(body?.msg || '请求失败'))
  },
  (err) => {
    if (err.response?.status === 401) {
      const store = useUserStore()
      const isLoginApi = err.config?.url?.startsWith('/auth')
      if (isLoginApi) {
        // 免登失败（如非组织成员）：清空会话，标记由调用方跳加入组织页
        store.clear()
        const e = new Error(err.response?.data?.msg || '身份验证失败')
        e.__loginFailed = true
        return Promise.reject(e)
      }
      // 业务请求 401（token 失效/无权）：阻断弹窗，确认后回首页重新登录
      store.clear()
      const msg = err.response?.data?.msg || '登录状态已失效，请重新进入'
      showDialog({
        title: '需要重新登录',
        message: msg,
        confirmButtonText: '我知道了',
        closeOnClickOverlay: false
      }).then(() => {
        location.hash = '#/'
        location.reload()
      }).catch(() => {
        location.hash = '#/'
        location.reload()
      })
    }
    return Promise.reject(err)
  }
)

export default request
