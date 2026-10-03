/**
 * 房源审核状态
 */
export const AUDIT_STATUS = {
  PENDING: 0,    // 待审核
  ONLINE: 1,     // 已上架（审核通过）
  REJECTED: 2    // 已驳回
}

/**
 * 房源上架状态
 */
export const RACK_STATUS = {
  RENTING: 0,    // 在租中
  RENTED: 1,     // 已租出
  OFF: 2         // 已下架
}

/**
 * 房源标签
 */
export const HOUSE_LABEL = {
  DIRECT: 1,     // 房东直租
  TRANSFER: 2,   // 校友转租
  SHARE: 3       // 合租拼室友
}

/**
 * 房源标签文本
 */
export const HOUSE_LABEL_TEXT = {
  [HOUSE_LABEL.DIRECT]: '房东直租',
  [HOUSE_LABEL.TRANSFER]: '校友转租',
  [HOUSE_LABEL.SHARE]: '合租拼室友'
}

/**
 * 订阅类型
 */
export const SUBSCRIBE_TYPE = {
  HOUSE: 1,      // 找房源
  TENANT: 2      // 找租客
}

/**
 * 订阅状态
 */
export const SUBSCRIBE_STATUS = {
  ACTIVE: 0,     // 启用
  PAUSED: 1      // 暂停
}

/**
 * 订阅类型文本
 */
export const SUBSCRIBE_TYPE_TEXT = {
  [SUBSCRIBE_TYPE.HOUSE]: '找房源',
  [SUBSCRIBE_TYPE.TENANT]: '找租客'
}

/**
 * 求租需求状态（与后端 Demand.STATUS_* 一致：0 待匹配 / 1 已匹配 / 2 已成交；撤回为物理删除无状态）
 */
export const DEMAND_STATUS = {
  PENDING: 0,    // 待匹配
  MATCHED: 1,    // 已匹配
  DONE: 2        // 已成交
}

/**
 * 求租需求状态文本
 */
export const DEMAND_STATUS_TEXT = {
  [DEMAND_STATUS.PENDING]: '待匹配',
  [DEMAND_STATUS.MATCHED]: '已匹配',
  [DEMAND_STATUS.DONE]: '已成交'
}

/**
 * 举报状态
 */
export const REPORT_STATUS = {
  PENDING: 0,    // 待处理
  HANDLED: 1     // 已处理
}

/**
 * 用户角色
 */
export const USER_ROLE = {
  USER: 0,       // 普通用户
  ADMIN: 1       // 管理员
}

/**
 * 用户状态
 */
export const USER_STATUS = {
  DISABLED: 0,   // 禁用
  ACTIVE: 1      // 正常
}

/**
 * 获取审核状态文本
 */
export function getAuditStatusText(status) {
  const texts = {
    [AUDIT_STATUS.PENDING]: '待审核',
    [AUDIT_STATUS.ONLINE]: '已上架',
    [AUDIT_STATUS.REJECTED]: '已驳回'
  }
  return texts[status] || '未知'
}

/**
 * 获取上架状态文本
 */
export function getRackStatusText(status) {
  const texts = {
    [RACK_STATUS.RENTING]: '在租中',
    [RACK_STATUS.RENTED]: '已租出',
    [RACK_STATUS.OFF]: '已下架'
  }
  return texts[status] || '未知'
}

/**
 * 获取房源状态组合文本（优先显示上架状态）
 */
export function getHouseStatusText(auditStatus, rackStatus) {
  // 已驳回
  if (auditStatus === AUDIT_STATUS.REJECTED) return '已驳回'
  // 待审核
  if (auditStatus === AUDIT_STATUS.PENDING) return '待审核'
  // 已上架：显示上架状态
  if (auditStatus === AUDIT_STATUS.ONLINE) {
    return getRackStatusText(rackStatus)
  }
  return '未知'
}

/**
 * 获取房源状态样式类
 */
export function getHouseStatusClass(auditStatus, rackStatus) {
  if (auditStatus === AUDIT_STATUS.REJECTED) return 'st-rejected'
  if (auditStatus === AUDIT_STATUS.PENDING) return 'st-pending'
  if (auditStatus === AUDIT_STATUS.ONLINE) {
    if (rackStatus === RACK_STATUS.RENTED) return 'st-rented'
    if (rackStatus === RACK_STATUS.OFF) return 'st-off'
    return 'st-online'
  }
  return ''
}
