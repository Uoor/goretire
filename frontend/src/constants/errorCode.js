/**
 * 错误码枚举
 * 与后端 ApiResponse.code / BusinessException.code 对应：
 * 后端统一使用 HTTP 风格码（400/401/403/404/500），无 1xxx 业务码段。
 */
export const ErrorCode = {
  SUCCESS: 0,

  // 客户端错误 (4xx)
  BAD_REQUEST: 400,
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
  METHOD_NOT_ALLOWED: 405,
  CONFLICT: 409,
  VALIDATION_ERROR: 422,

  // 服务端错误 (5xx)
  INTERNAL_ERROR: 500,
  SERVICE_UNAVAILABLE: 503
}

/**
 * 错误码是否为成功
 */
export function isSuccess(code) {
  return code === ErrorCode.SUCCESS
}

/**
 * 错误码是否为认证错误
 */
export function isAuthError(code) {
  return code === ErrorCode.UNAUTHORIZED
}

/**
 * 错误码是否为权限错误
 */
export function isForbidden(code) {
  return code === ErrorCode.FORBIDDEN
}

/**
 * 错误码是否为资源不存在（后端房源/需求/订阅不存在统一抛 404）
 */
export function isNotFound(code) {
  return code === ErrorCode.NOT_FOUND
}

/**
 * 获取错误消息（后端会带具体 message，仅在缺失时按码兜底）
 */
export function getErrorMessage(code, defaultMsg = '操作失败') {
  const messages = {
    [ErrorCode.BAD_REQUEST]: '请求参数有误',
    [ErrorCode.UNAUTHORIZED]: '登录状态已失效，请重新进入',
    [ErrorCode.FORBIDDEN]: '没有权限执行此操作',
    [ErrorCode.NOT_FOUND]: '资源不存在或已下架',
    [ErrorCode.INTERNAL_ERROR]: '服务开小差了，请稍后重试'
  }
  return messages[code] || defaultMsg
}
