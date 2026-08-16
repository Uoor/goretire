// 钉钉 JSAPI 封装（免登 / 单聊 / dd.config 授权）
// 参考: 钉钉开放平台 H5 免登流程（requestAuthCode → 后端换 userid → JWT）
// 参考: 钉钉开放平台 JSAPI 鉴权（dd.config，需后端 /api/dingtalk/jsapi-sign 签名）
// CorpId 从 frontend/.env 的 VITE_DING_CORP_ID 读取（钉钉容器内使用）
import request from '@/utils/request'

// dd.config 幂等缓存：页面生命周期内只授权一次
let configPromise = null

/**
 * 钉钉 JSAPI 授权（dd.config）。
 * - 浏览器联调：无 dd 全局，直接 resolve(false)，一切走桩；
 * - 钉钉容器内：请求后端签名（jsapi_ticket 算法），再 dd.config 授权
 *   runtime.permission.requestAuthCode 与 biz.chat.openSingleChat，
 *   dd.ready 后 resolve(true)。
 * 幂等：多次调用复用同一个 Promise。
 */
export function configDingtalk() {
  if (typeof dd === 'undefined') {
    return Promise.resolve(false)
  }
  if (configPromise) return configPromise
  // 签名 url 与钉钉内实际页面 url 必须一致；hash 路由去掉 # 及之后部分
  const url = location.href.split('#')[0]
  configPromise = request
    .post('/dingtalk/jsapi-sign', { url })
    .then((cfg) => new Promise((resolve, reject) => {
      dd.config({
        agentId: String(cfg.agentId),
        corpId: cfg.corpId,
        timeStamp: String(cfg.timeStamp),
        nonceStr: cfg.nonceStr,
        signature: cfg.signature,
        jsApiList: ['runtime.permission.requestAuthCode', 'biz.chat.openSingleChat']
      })
      dd.ready(() => resolve(true))
      dd.error((err) => {
        configPromise = null // 授权失败允许下次重试
        reject(err)
      })
    }))
    .catch((err) => {
      configPromise = null
      throw err
    })
  return configPromise
}

export function getAuthCode() {
  return new Promise((resolve, reject) => {
    if (typeof dd === 'undefined') {
      // 浏览器联调：返回桩值，后端开发桩放行
      resolve('dev-code')
      return
    }
    dd.runtime.permission.requestAuthCode({
      corpId: import.meta.env.VITE_DING_CORP_ID || '',
      onSuccess: (result) => resolve(result.code),
      onFail: (err) => reject(err)
    })
  })
}

/**
 * 唤起钉钉单聊窗口（联系房东：拿到 staffId 后直接开聊）。
 * 浏览器联调环境无 dd 全局，返回 false（调用方降级提示）。
 */
export function openSingleChat(staffId) {
  return new Promise((resolve, reject) => {
    if (typeof dd === 'undefined' || !staffId) {
      reject(new Error('请在钉钉内使用此功能'))
      return
    }
    dd.biz.chat.openSingleChat({
      staffId,
      onSuccess: () => resolve(true),
      onFail: (err) => reject(err)
    })
  })
}
