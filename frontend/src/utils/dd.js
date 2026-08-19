// 钉钉 JSAPI 封装（免登 / 单聊 / dd.config 授权）
// 参考: 钉钉开放平台 H5 免登流程（requestAuthCode → 后端换 userid → JWT）
// 参考: 钉钉开放平台 JSAPI 鉴权（dd.config，需后端 /api/dingtalk/jsapi-sign 签名）
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

// 后端 jsapi-sign 返回的 corpId（应用所属企业，已通过 dd.config 校验）。
// requestAuthCode 优先用它，避免依赖构建时环境变量（线上构建可能缺失 VITE_DING_CORP_ID）。
let backendCorpId = ''

/** 从后端获取 corpId（浏览器环境用，钉钉容器内由 configDingtalk 设置） */
function fetchBackendCorpId() {
  if (backendCorpId) return Promise.resolve(backendCorpId)
  // 用一个占位 URL 调 jsapi-sign，只取 corpId
  return request
    .post('/dingtalk/jsapi-sign', { url: location.href.split('#')[0] })
    .then((cfg) => {
      backendCorpId = cfg.corpId || import.meta.env.VITE_DING_CORP_ID || ''
      return backendCorpId
    })
    .catch(() => {
      return import.meta.env.VITE_DING_CORP_ID || ''
    })
}

/**
 * 获取钉钉容器当前企业 corpId（dd.runtime.info，免鉴权）。
 * 多组织场景下，容器当前活跃企业可能与应用所属企业不一致，
 * dd.config 校验 corpId 不匹配会报 invalid corpid，因此优先跟随容器。
 * 容器不可用时返回 null，由调用方回退到后端配置的 corpId。
 */
function getContainerCorpId() {
  return new Promise((resolve) => {
    try {
      if (typeof dd !== 'undefined' && dd.runtime && typeof dd.runtime.info === 'function') {
        dd.runtime.info({
          onSuccess: (info) => {
            const corpId = (info && (info.corpId || info.corpId2)) || ''
            resolve(corpId || null)
          },
          onFail: () => resolve(null)
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
 *   runtime.permission.requestAuthCode（免登）；联系房东走统一跳转协议 page/profile，
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
        backendCorpId = cfg.corpId || ''
        // 兜底：dd.config 后 8s 内既无 ready 也无 error（部分钉钉版本静默失败），
        // 超时按失败处理，避免免登流程永久卡死。
        const timer = setTimeout(() => {
          configPromise = null
          reject(new Error('钉钉授权超时（dd.config 无回调）'))
        }, 8000)
        // 容器当前企业优先（多组织场景跟随用户当前活跃企业），否则用应用所属企业
        const corpId = containerCorpId || cfg.corpId
        dd.config({
          agentId: String(cfg.agentId),
          corpId,
          timeStamp: String(cfg.timeStamp),
          nonceStr: cfg.nonceStr,
          signature: cfg.signature,
          jsApiList: ['runtime.permission.requestAuthCode']
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
    // corpId 取值优先级：后端 jsapi-sign 返回（已通过 dd.config 校验）>
    // 容器当前企业 > 构建时 VITE_DING_CORP_ID。
    // 注意：线上构建可能缺失 VITE_DING_CORP_ID，必须优先用后端返回值。
    getContainerCorpId().then((containerCorpId) => {
      const corpId = backendCorpId || containerCorpId || import.meta.env.VITE_DING_CORP_ID || ''
      // 先带 corpId 试一次；失败再不带 corpId 重试（部分钉钉版本带 corpId 会失败，
      // 不带则跟随容器当前企业）
      requestAuthCodeOnce(corpId, resolve, reject)
    })
  })
}

function requestAuthCodeOnce(corpId, resolve, reject, triedWithoutCorpId = false) {
  const options = { onSuccess, onFail }
  if (corpId && !triedWithoutCorpId) {
    options.corpId = corpId
  }
  function onSuccess(result) {
    resolve(result.code)
  }
  function onFail(err) {
    const detail = err?.errorMessage || err?.errorCode || err?.message || JSON.stringify(err)
    console.warn('[authCode] requestAuthCode 失败:', { corpId, detail })
    if (corpId && !triedWithoutCorpId) {
      // 降级重试：不带 corpId（跟随容器当前企业）
      requestAuthCodeOnce(corpId, resolve, reject, true)
    } else {
      reject(new Error(`requestAuthCode: ${detail}`))
    }
  }
  dd.runtime.permission.requestAuthCode(options)
}

/**
 * 打开房东名片页（联系房东：拿到钉钉 userid 后唤起个人名片）。
 * 使用钉钉统一跳转协议 page/profile（官方支持，无需 openSingleChat 的 JSAPI 权限）：
 *   dingtalk://dingtalkclient/page/profile?corp_id={corp_id}&staff_id={staff_id}
 * 用户可在名片页查看资料并自行发起会话。
 * - 钉钉容器内：直接 location.href 跳转（当前页面离开）
 * - 浏览器环境：window.open 新标签页打开协议（唤起本地钉钉客户端）
 */
export function openSingleChat(userId) {
  if (!userId) {
    return Promise.reject(new Error('房东信息缺失'))
  }
  // 获取 corpId（钉钉容器内已有，浏览器环境需从后端获取）
  const corpIdPromise = backendCorpId ? Promise.resolve(backendCorpId) : fetchBackendCorpId()
  return corpIdPromise.then((corpId) => {
    const profileUrl = `dingtalk://dingtalkclient/page/profile?corp_id=${encodeURIComponent(corpId)}&staff_id=${encodeURIComponent(userId)}`
    try {
      if (isDingTalk()) {
        // 钉钉容器内：直接跳转（离开当前页面）
        location.href = profileUrl
      } else {
        // 浏览器环境：新标签页打开协议，唤起本地钉钉客户端
        window.open(profileUrl, '_blank')
      }
      return true
    } catch (e) {
      throw new Error(`无法打开房东名片: ${e?.message || e}`)
    }
  })
}

/**
 * 跳转钉钉 OAuth2 网页扫码登录页（浏览器环境用）。
 * 只申请 openid scope（不传 corpId）：实测带 corpId + "openid corpid" scope 时，
 * 钉钉兑换 authCode 返回"不合法的临时授权码"（应用未开通 corpid 级 OAuth2 授权）；
 * 纯 openid 可正常兑换。非组织成员由后端 getUserIdByUnionId 403 拦截（引导加入社群），
 * 安全边界不变。
 * @param redirect 登录成功后要回跳的前端 hash 路径（如 "/house/123"）；经 state 参数带回，后端解码后拼进回调 URL
 */
export async function redirectToDingTalkOAuth(redirect) {
  const appKey = import.meta.env.VITE_DING_APP_KEY
  if (!appKey) {
    console.error('[dd] VITE_DING_APP_KEY 未配置，无法发起 OAuth2 登录')
    return
  }
  // 回调地址：始终使用生产环境的回调 URL（钉钉只允许已配置的回调地址）
  // 通过 state 参数传递实际的前端 origin，让后端知道最终重定向到哪里
  const callbackUrl = encodeURIComponent('https://test.nekomiao.com/api/auth/dingtalk/callback')
  // state 参数：origin + 可选 redirect（JSON 后 base64），后端解码出 origin 重定向，
  // 并把 redirect 追加到回调 URL，实现扫码后回跳目标页（群卡片落地页免登）
  const statePayload = { origin: location.origin }
  if (redirect) statePayload.redirect = redirect
  const state = encodeURIComponent(btoa(JSON.stringify(statePayload)))
  const params = [
    `client_id=${appKey}`,
    `redirect_uri=${callbackUrl}`,
    'response_type=code',
    'scope=openid',
    'prompt=auto',
    `state=${state}`
  ]
  // prompt=auto：首次授权后不再弹授权页，直接登录
  const oauthUrl = `https://login.dingtalk.com/oauth2/auth?${params.join('&')}`
  // 诊断上报：记录实际跳转的 OAuth URL（定位 authCode 兑换失败的根因）
  try {
    request.post('/dingtalk/jsapi-debug', {
      event: 'oauth.redirect',
      oauthUrl,
      origin: location.origin,
      pageUrl: location.href
    }).catch(() => {})
  } catch (e) { /* 上报失败不影响主流程 */ }
  location.href = oauthUrl
}
