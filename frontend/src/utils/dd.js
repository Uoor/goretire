// 钉钉 JSAPI 封装骨架（免登）
// 参考: 钉钉开放平台 H5 免登流程（requestAuthCode → 后端换 userid → JWT）

/**
 * 获取免登 authCode。
 * 浏览器联调环境（无 dd 全局）返回桩值 'dev-code'，由后端开发桩放行。
 */
export function getAuthCode() {
  return new Promise((resolve, reject) => {
    if (typeof dd === 'undefined') {
      resolve('dev-code')
      return
    }
    dd.runtime.permission.requestAuthCode({
      corpId: window.DING_CORP_ID || '',
      onSuccess: (result) => resolve(result.code),
      onFail: (err) => reject(err)
    })
  })
}
