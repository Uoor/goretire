import request from '@/utils/request'

// ============================================================
// 租房模块接口（modules/houserent）
// 对接后端统一响应 {code,msg,data}，request 已解包 data
// ============================================================

export const houseApi = {
  // 分页列表：返回 { total, list }；兼容旧返回（数组）直接包一层
  list: async (params) => {
    const data = await request.get('/houses', { params })
    return Array.isArray(data) ? { total: data.length, list: data } : data
  },
  detail: (id) => request.get(`/houses/${id}`),
  mine: () => request.get('/houses/mine'),
  publish: (data) => request.post('/houses', data),
  offRack: (id) => request.post(`/houses/${id}/off-rack`),
  relist: (id) => request.post(`/houses/${id}/relist`),
  remove: (id) => request.delete(`/houses/${id}`),
  feedback: (id, answer) => request.post(`/houses/${id}/feedback`, { answer }),
  report: (id, reason) => request.post(`/houses/${id}/reports`, { reason }),
  contact: (id) => request.post(`/houses/${id}/contact`),
  reportWeekly: () => request.get('/houses/report/weekly'),
  regionReport: (region) => request.get('/houses/report/region', { params: { region } })
}

export const demandApi = {
  list: (params) => request.get('/demands', { params }),
  mine: () => request.get('/demands/mine'),
  detail: (id) => request.get(`/demands/${id}`),
  create: (data) => request.post('/demands', data),
  withdraw: (id) => request.post(`/demands/${id}/withdraw`),
  complete: (id) => request.post(`/demands/${id}/complete`)
}

export const subscribeApi = {
  list: () => request.get('/subscriptions'),
  create: (data) => request.post('/subscriptions', data),
  update: (id, data) => request.put(`/subscriptions/${id}`, data),
  remove: (id) => request.delete(`/subscriptions/${id}`),
  pushes: (id) => request.get(`/subscriptions/${id}/pushes`)
}

export const matchApi = {
  search: (text) => request.post('/match/search', { text })
}

export const adminApi = {
  auditPending: () => request.get('/admin/audit/pending'),
  audit: (id, pass, reason) => request.post(`/admin/audit/${id}`, { pass, reason }),
  reports: (status) => request.get('/admin/reports', { params: { status } }),
  handleReport: (id, result) => request.post(`/admin/reports/${id}/handle`, { result }),
  stats: () => request.get('/admin/stats')
}

// 避坑指南知识库（钉钉知识库为数据源）
export const guideApi = {
  sections: () => request.get('/guide/sections'),
  items: (nodeId) => request.get(`/guide/sections/${nodeId}/items`),
  content: (nodeId) => request.get('/guide/items/content', { params: { nodeId } })
}
