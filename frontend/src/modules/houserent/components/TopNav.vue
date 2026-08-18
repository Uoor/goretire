<template>
  <header class="topnav">
    <div class="topnav-inner">
      <div class="topnav-brand" @click="go('home')">
        <span class="topnav-logo">校友安居</span>
      </div>
      <nav class="topnav-tabs">
        <a
          v-for="item in tabs"
          :key="item.name"
          class="topnav-tab"
          :class="{ on: route.name === item.name }"
          @click="go(item.name)"
        >
          <i :class="item.icon"></i>
          <span>{{ item.label }}</span>
        </a>
      </nav>
      <div class="topnav-actions">
        <button class="publish-btn" @click="go('publish')">
          <i class="ph ph-plus"></i> 发布房源
        </button>
      </div>
    </div>
  </header>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const tabs = [
  { name: 'home', label: '房源', icon: 'ph ph-house' },
  { name: 'demand', label: '求租', icon: 'ph ph-magnifying-glass' },
  { name: 'subscribe', label: '订阅', icon: 'ph ph-bell' },
  { name: 'me', label: '我的', icon: 'ph ph-user' },
]

function go(name) {
  if (route.name === name) return
  router.push({ name })
}
</script>

<style scoped>
.topnav {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 200;
  background: rgba(255, 255, 255, 0.97);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--border);
  height: 60px;
  display: flex;
  align-items: center;
}
.topnav-inner {
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  align-items: center;
  gap: 32px;
}
.topnav-brand {
  cursor: pointer;
  flex-shrink: 0;
}
.topnav-logo {
  font-size: 18px;
  font-weight: 700;
  color: var(--primary);
  letter-spacing: -0.3px;
}
.topnav-tabs {
  display: flex;
  gap: 4px;
  flex: 1;
}
.topnav-tab {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 14px;
  color: var(--fg2);
  cursor: pointer;
  transition: background 0.15s ease, color 0.15s ease;
  text-decoration: none;
  user-select: none;
}
.topnav-tab:hover {
  background: var(--card);
  color: var(--fg);
}
.topnav-tab.on {
  background: rgba(255, 106, 0, 0.08);
  color: var(--primary);
  font-weight: 600;
}
.topnav-tab i {
  font-size: 16px;
}
.topnav-actions {
  flex-shrink: 0;
}
.publish-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 18px;
  border-radius: 10px;
  background: var(--primary);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  border: none;
  cursor: pointer;
  transition: opacity 0.15s ease;
  user-select: none;
}
.publish-btn:hover {
  opacity: 0.9;
}
</style>
