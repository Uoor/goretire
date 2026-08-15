import { defineStore } from 'pinia'

// 用户态：token 持久化到 localStorage；免登接入后由 dd.js + /api/auth 填充
export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('aliren_token') || '',
    userInfo: JSON.parse(localStorage.getItem('aliren_user') || 'null')
  }),
  getters: {
    isLoggedIn: (s) => !!s.token,
    isAdmin: (s) => s.userInfo?.role === 1,
    userId: (s) => s.userInfo?.userId ?? null,
    nickname: (s) => s.userInfo?.nickname ?? ''
  },
  actions: {
    setSession(token, userInfo) {
      this.token = token
      this.userInfo = userInfo
      localStorage.setItem('aliren_token', token)
      localStorage.setItem('aliren_user', JSON.stringify(userInfo))
    },
    clear() {
      this.token = ''
      this.userInfo = null
      localStorage.removeItem('aliren_token')
      localStorage.removeItem('aliren_user')
    }
  }
})
