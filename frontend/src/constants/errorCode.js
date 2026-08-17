/**
 * 错误码枚举
 * 与后端 ApiResponse.code 对应
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
  SERVICE_UNAVAILABLE: 503,
  
  // 业务错误 (1xxx)
  BUSINESS_ERROR: 1000,
  
  // 房源相关 (1xxx)
  HOUSE_NOT_FOUND: 1001,
  HOUSE_NOT_AVAILABLE: 1002,
  HOUSE_ALREADY_RENTED: 1003,
  HOUSE_NOT_OWNER: 1004,
  HOUSE_AUDIT_PENDING: 1005,
  HOUSE_AUDIT_REJECTED: 1006,
  
  // 求租相关 (2xxx)
  DEMAND_NOT_FOUND: 2001,
  DEMAND_NOT_OWNER: 2002,
  
  // 订阅相关 (3xxx)
  SUBSCRIBE_NOT_FOUND: 3001,
  SUBSCRIBE_NOT_OWNER: 3002,
  
  // 用户相关 (4xxx)
  USER_NOT_FOUND: 4001,
  USER_NOT_ADMIN: 4002,
  USER_NOT_MEMBER: 4003,
  
  // 钉钉相关 (5xxx)
  DINGTALK_AUTH_FAILED: 5001,
  DINGTALK_NOT_MEMBER: 5002,
  DINGTALK_API_ERROR: 5003
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
 * 错误码是否为资源不存在
 */
export function isNotFound(code) {
  return code === ErrorCode.NOT_FOUND || 
         code === ErrorCode.HOUSE_NOT_FOUND ||
         code === ErrorCode.DEMAND_NOT_FOUND ||
         code === ErrorCode.SUBSCRIBE_NOT_FOUND ||
         code === ErrorCode.USER_NOT_FOUND
}

/**
 * 获取错误消息
 */
export function getErrorMessage(code, defaultMsg = '操作失败') {
  const messages = {
    [ErrorCode.UNAUTHORIZED]: '登录状态已失效，请重新进入',
    [ErrorCode.FORBIDDEN]: '没有权限执行此操作',
    [ErrorCode.NOT_FOUND]: '资源不存在',
    [ErrorCode.HOUSE_NOT_FOUND]: '房源不存在或已下架',
    [ErrorCode.HOUSE_NOT_AVAILABLE]: '房源已不可租',
    [ErrorCode.HOUSE_ALREADY_RENTED]: '房源已租出',
    [ErrorCode.HOUSE_NOT_OWNER]: '只能操作自己发布的房源',
    [ErrorCode.DEMAND_NOT_FOUND]: '求租需求不存在',
    [ErrorCode.DEMAND_NOT_OWNER]: '只能操作自己发布的需求',
    [ErrorCode.SUBSCRIBE_NOT_FOUND]: '订阅不存在',
    [ErrorCode.SUBSCRIBE_NOT_OWNER]: '只能操作自己的订阅',
    [ErrorCode.USER_NOT_FOUND]: '用户不存在',
    [ErrorCode.USER_NOT_ADMIN]: '需要管理员权限',
    [ErrorCode.USER_NOT_MEMBER]: '需要加入组织',
    [ErrorCode.DINGTALK_AUTH_FAILED]: '身份验证失败',
    [ErrorCode.DINGTALK_NOT_MEMBER]: '非组织成员',
    [ErrorCode.DINGTALK_API_ERROR]: '钉钉服务异常'
  }
  return messages[code] || defaultMsg
}
