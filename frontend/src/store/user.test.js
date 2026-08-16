import { beforeEach, describe, expect, it } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useUserStore } from '@/store/user'

describe('user store', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
  })

  it('setSession 持久化 token 与用户', () => {
    const store = useUserStore()
    store.setSession('tok-1', { userId: 1, nickname: '小明', role: 1 })
    expect(store.isLoggedIn).toBe(true)
    expect(store.userId).toBe(1)
    expect(store.nickname).toBe('小明')
    expect(store.isAdmin).toBe(true)
    expect(localStorage.getItem('aliren_token')).toBe('tok-1')
  })

  it('普通用户 isAdmin 为 false', () => {
    const store = useUserStore()
    store.setSession('tok-2', { userId: 2, nickname: '校友', role: 0 })
    expect(store.isAdmin).toBe(false)
  })

  it('clear 清空会话', () => {
    const store = useUserStore()
    store.setSession('tok-1', { userId: 1, nickname: '小明', role: 0 })
    store.clear()
    expect(store.isLoggedIn).toBe(false)
    expect(localStorage.getItem('aliren_token')).toBeNull()
  })

  it('无 token 时未登录', () => {
    const store = useUserStore()
    expect(store.isLoggedIn).toBe(false)
  })
})
