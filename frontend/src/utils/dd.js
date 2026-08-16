// 钉钉 JSAPI 封装（免登）
// 参考: 钉钉开放平台 H5 免登流程（requestAuthCode → 后端换 userid → JWT）
// CorpId 从 frontend/.env 的 VITE_DING_CORP_ID 读取（钉钉容器内使用）

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
