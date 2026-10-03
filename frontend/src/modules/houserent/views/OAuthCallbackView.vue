<template>
  <div class="oauth-callback">
    <van-loading size="24" color="#FF6A00">登录中…</van-loading>
    <p v-if="error" class="error">{{ error }}</p>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'

const router = useRouter()
const route = useRoute()
const store = useUserStore()
const error = ref('')

onMounted(() => {
  // 从 URL query 参数读取 token 和用户信息（后端回调 302 重定向带过来的）
  const token = route.query.token
  const userJson = route.query.user
  if (!token || !userJson) {
    // 参数缺失：登录未完成，回到登录页重试
    error.value = '登录参数缺失，请重新扫码'
    setTimeout(() => router.replace({ name: 'login' }), 2000)
    return
  }
  try {
    const user = JSON.parse(decodeURIComponent(userJson))
    store.setSession(token, user)
    // 登录成功：优先跳回原目标页（群卡片落地页扫码场景，redirect 由后端从 state 带回），否则首页
    const redirect = (route.query.redirect || '').toString()
    if (redirect && redirect.startsWith('/')) {
      router.replace({ path: redirect })
    } else {
      router.replace({ name: 'home' })
    }
  } catch (e) {
    error.value = '登录信息解析失败，请重新扫码'
    setTimeout(() => router.replace({ name: 'home' }), 2000)
  }
})
</script>

<style scoped>
.oauth-callback {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  gap: 16px;
}
.error {
  color: #ee0a24;
  font-size: 0.85rem;
}
</style>
