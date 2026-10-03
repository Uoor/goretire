import axios from 'axios'
import { useUserStore } from '@/store/user'
import { showDialog, showToast } from 'vant'

// axios 封装：统一 baseURL / token 注入 / 响应解包 {code,msg,data}
// BASE_URL = Vite base（部署子路径 /ali/house/），API 挂在子路径下
const request = axios.create({
  baseURL: `${import.meta.env.BASE_URL}api`,
  timeout: 10000
})

// 子路径前缀：后端返回的上传图片相对路径 /uploads/xxx → {BASE_URL}uploads/xxx
// （部署在 /ali/house 下时，img src 用相对 /uploads 会丢子路径前缀导致 404）
const uploadsPrefix = `${import.meta.env.BASE_URL}uploads`

/** 递归给响应数据里以 /uploads/ 开头的字符串加子路径前缀（图片 URL 适配） */
function prefixUploads(value) {
  if (typeof value === 'string') {
    return value.startsWith('/uploads/') ? uploadsPrefix + value.slice('/uploads'.length) : value
  }
  if (Array.isArray(value)) {
    return value.map(prefixUploads)
  }
  if (value && typeof value === 'object') {
    for (const k of Object.keys(value)) {
      value[k] = prefixUploads(value[k])
    }
  }
  return value
}

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
      return prefixUploads(body.data)
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
