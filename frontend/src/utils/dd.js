// 钉钉 JSAPI 封装（免登 / 单聊）
// 参考: 钉钉开放平台 H5 免登流程（requestAuthCode → 后端换 userid → JWT）
// CorpId 从 frontend/.env 的 VITE_DING_CORP_ID 读取（钉钉容器内使用）
//
// 注意：真实钉钉容器内使用这些 JSAPI 前需 dd.config 授权（jsApiList 含
// runtime.permission.requestAuthCode、biz.chat.openSingleChat），
// 签名由后端生成（部署时接入 /api/dingtalk/jsapi-sign）；浏览器联调走桩。

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
