<template>
  <div class="landing-page">
    <van-loading size="24" color="#FF6A00">正在进入…</van-loading>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const store = useUserStore()

/**
 * 群卡片/推送落地页（浏览器免登）：
 * 1. 已有登录态（localStorage token 有效）→ 直接跳 redirect 目标页；
 * 2. 无登录态 → 跳登录页并携带 redirect（登录页展示信任/授权说明后由用户发起扫码，
 *    扫码成功后端回跳 oauth-callback 携带 redirect，实现"从卡片进 → 登录 → 回原目标页"）。
 * 首次登录一次，之后 localStorage 有 token，点卡片直接免登。
 */
onMounted(() => {
  const redirect = (route.query.redirect || '').toString()
  const target = redirect && redirect.startsWith('/') ? redirect : '/'
  if (store.isLoggedIn) {
    router.replace({ path: target })
    return
  }
  // 无登录态：跳登录页（保留 redirect，登录后回跳目标页）
  router.replace({ path: '/login', query: target === '/' ? {} : { redirect: target } })
})
</script>

<style scoped>
.landing-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg);
  font-size: 0.85rem;
  color: var(--fg2);
}
</style>
