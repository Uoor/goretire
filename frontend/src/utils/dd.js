// 钉钉 JSAPI 封装（免登 / 单聊 / dd.config 授权）
// 参考: 钉钉开放平台 H5 免登流程（requestAuthCode → 后端换 userid → JWT）
// 参考: 钉钉开放平台 JSAPI 鉴权（dd.config，需后端 /api/dingtalk/jsapi-sign 签名）
// CorpId 从 frontend/.env 的 VITE_DING_CORP_ID 读取（钉钉容器内使用）
//
// 注意：dd 全局由 index.html 引入的官方 JSAPI SDK 提供，普通浏览器也会注入
// （dd.env.platform === 'notInDingTalk'），因此一律用 isDingTalk() 判断环境，
// 不要用 typeof dd === 'undefined'。
import request from '@/utils/request'

/** 是否运行在钉钉容器内（SDK 在普通浏览器也定义 dd，但 platform 标记为非钉钉环境） */
export function isDingTalk() {
  if (typeof dd === 'undefined') return false
  return Boolean(dd.env && dd.env.platform && dd.env.platform !== 'notInDingTalk')
}

// dd.config 幂等缓存：页面生命周期内只授权一次
let configPromise = null

/**
 * 获取钉钉容器当前企业 corpId。
 * 优先用 dd.runtime.info（容器注入的运行时信息，免鉴权），失败时回退到
 * 后端签名接口返回的 corpId（应用所属企业）。
 * 背景：钉钉多组织场景下，容器当前活跃企业可能与应用所属企业不一致，
 * dd.config 校验 corpId 不匹配会报 invalid corpid。
 */
let lastRuntimeInfo = null
function getContainerCorpId() {
  return new Promise((resolve) => {
    try {
      if (typeof dd !== 'undefined' && dd.runtime && typeof dd.runtime.info === 'function') {
        dd.runtime.info({
          onSuccess: (info) => {
            lastRuntimeInfo = info || null
            const corpId = (info && (info.corpId || info.corpId2)) || ''
            resolve(corpId || null)
          },
          onFail: (err) => {
            lastRuntimeInfo = { error: err?.errorMessage || err?.message || 'onFail' }
            resolve(null)
          }
        })
      } else {
        resolve(null)
      }
    } catch (e) {
      resolve(null)
    }
  })
}

/**
 * 钉钉 JSAPI 授权（dd.config）。
 * - 浏览器联调：非钉钉环境，直接 resolve(false)，一切走桩；
 * - 钉钉容器内：请求后端签名（jsapi_ticket 算法），再 dd.config 授权
 *   runtime.permission.requestAuthCode 与 biz.chat.openSingleChat，
 *   dd.ready 后 resolve(true)。
 * 幂等：多次调用复用同一个 Promise。
 */
export function configDingtalk() {
  if (!isDingTalk()) {
    return Promise.resolve(false)
  }
  if (configPromise) return configPromise
  // 签名 url 与钉钉内实际页面 url 必须一致；hash 路由去掉 # 及之后部分
  const url = location.href.split('#')[0]
  configPromise = getContainerCorpId().then((containerCorpId) =>
    request
      .post('/dingtalk/jsapi-sign', { url })
      .then((cfg) => new Promise((resolve, reject) => {
        // 兜底：dd.config 后 8s 内既无 ready 也无 error（部分钉钉版本静默失败），
        // 超时按失败处理，避免免登流程永久卡死。
        const timer = setTimeout(() => {
          configPromise = null
          reject(new Error('钉钉授权超时（dd.config 无回调）'))
        }, 8000)
        // 容器当前企业优先（多组织场景跟随用户当前活跃企业），否则用应用所属企业
        const corpId = containerCorpId || cfg.corpId
        console.info('[dd.config] corpId 来源:', containerCorpId ? '容器' : '后端配置', corpId)
        dd.config({
          agentId: String(cfg.agentId),
          corpId,
          timeStamp: String(cfg.timeStamp),
          nonceStr: cfg.nonceStr,
          signature: cfg.signature,
          jsApiList: ['runtime.permission.requestAuthCode', 'biz.chat.openSingleChat']
        })
        dd.ready(() => {
          clearTimeout(timer)
          resolve(true)
        })
        dd.error((err) => {
          clearTimeout(timer)
          configPromise = null // 授权失败允许下次重试
          // 钉钉 dd.error 的 err 结构: {errorCode, errorMessage, errorMessageCN?}
          const errorCode = err?.errorCode
          const errorMessage = err?.errorMessageCN || err?.errorMessage || err?.message || JSON.stringify(err)
          // 常见错误码提示
          let friendlyMessage = '钉钉授权失败'
          if (errorCode === 2 || errorCode === 3) {
            friendlyMessage = '钉钉授权签名验证失败，请检查应用配置或联系管理员'
          } else if (errorCode === 4) {
            friendlyMessage = '钉钉应用未授权，请联系管理员开通权限'
          } else if (errorCode === 7) {
            friendlyMessage = '钉钉授权已过期，请刷新页面重试'
          }
          console.error('[dd.config] 授权失败:', { errorCode, errorMessage, corpId, url })
          // 诊断上报：容器 corpId vs 后端配置 corpId 的差异是排查 invalid corpid 的关键
          try {
            request.post('/dingtalk/jsapi-debug', {
              event: 'dd.config.error',
              errorCode,
              errorMessage,
              containerCorpId,
              runtimeInfo: lastRuntimeInfo,
              backendCorpId: cfg.corpId,
              usedCorpId: corpId,
              agentId: cfg.agentId,
              pageUrl: location.href
            }).catch(() => {})
          } catch (e) { /* 上报失败不影响主流程 */ }
          reject(new Error(`${friendlyMessage}（${errorCode}: ${errorMessage}）`))
        })
      }))
      .catch((err) => {
        configPromise = null
        console.error('[dd.config] 签名请求失败:', err)
        throw err
      }))
  return configPromise
}

export function getAuthCode() {
  return new Promise((resolve, reject) => {
    if (!isDingTalk()) {
      // 浏览器联调：返回桩值，后端开发桩放行
      resolve('dev-code')
      return
    }
    // 容器当前企业优先；拿不到时回退构建时配置的 VITE_DING_CORP_ID
    getContainerCorpId().then((containerCorpId) => {
      const corpId = containerCorpId || import.meta.env.VITE_DING_CORP_ID || ''
      dd.runtime.permission.requestAuthCode({
        corpId,
        onSuccess: (result) => resolve(result.code),
        onFail: (err) => {
          const detail = err?.errorMessage || err?.errorCode || err?.message || JSON.stringify(err)
          reject(new Error(`requestAuthCode: ${detail}`))
        }
      })
    })
  })
}

/**
 * 唤起钉钉单聊窗口（联系房东：拿到 staffId 后直接开聊）。
 * 非钉钉环境直接拒绝（调用方降级提示）。
 */
export function openSingleChat(staffId) {
  return new Promise((resolve, reject) => {
    if (!isDingTalk() || !staffId) {
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
