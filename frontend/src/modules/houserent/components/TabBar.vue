<template>
  <nav class="tabbar fixed-shell">
    <div class="tab" :class="{ on: route.name === 'home' }" @click="go('home')">
      <i class="ph ph-house"></i><span>首页</span>
    </div>
    <div class="tab" :class="{ on: route.name === 'demand' }" @click="go('demand')">
      <i class="ph ph-magnifying-glass"></i><span>求租</span>
    </div>
    <div class="tab publish-tab" @click="go('publish')">
      <span class="pub"><i class="ph ph-plus"></i></span><span>发布</span>
    </div>
    <div class="tab" :class="{ on: route.name === 'subscribe' }" @click="go('subscribe')">
      <i class="ph ph-bell"></i><span>订阅</span>
    </div>
    <div class="tab" :class="{ on: route.name === 'me' }" @click="go('me')">
      <i class="ph ph-user"></i><span>我的</span>
    </div>
  </nav>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

function go(name) {
  if (route.name === name) return
  router.push({ name })
}
</script>

<style scoped>
.tabbar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  max-width: 480px;
  margin: 0 auto;
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(10px);
  border-top: 1px solid var(--border);
  display: flex;
  height: 64px;
  padding-bottom: 8px;
  z-index: 100;
}

@media (min-width: 768px) {
  .tabbar {
    max-width: 1200px;
  }
}
.tab {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  font-size: 0.62rem;
  color: var(--fg3);
  cursor: pointer;
  background: none;
  border: none;
  user-select: none;
}
.tab i {
  font-size: 1.25rem;
}
.tab.on {
  color: var(--primary);
  font-weight: 600;
}
.tab.publish-tab {
  position: relative;
}
.tab .pub {
  position: absolute;
  top: -18px;
  width: 52px;
  height: 52px;
  border-radius: 16px;
  background: var(--primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.4rem;
  box-shadow: 0 6px 16px rgba(255, 106, 0, 0.4);
  border: 4px solid var(--bg);
  transition: transform 0.15s ease;
}
.tab.publish-tab:active .pub {
  transform: scale(0.95);
}
</style>
