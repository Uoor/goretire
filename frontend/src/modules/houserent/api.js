import request from '@/utils/request'

// ============================================================
// 租房模块接口（modules/houserent）
// 对接后端统一响应 {code,msg,data}，request 已解包 data
// ============================================================

export const houseApi = {
  list: (params) => request.get('/houses', { params }),
  detail: (id) => request.get(`/houses/${id}`),
  mine: () => request.get('/houses/mine'),
  publish: (data) => request.post('/houses', data),
  offRack: (id) => request.post(`/houses/${id}/off-rack`),
  feedback: (id, answer) => request.post(`/houses/${id}/feedback`, { answer }),
  report: (id, reason) => request.post(`/houses/${id}/reports`, { reason })
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
  remove: (id) => request.delete(`/subscriptions/${id}`)
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
