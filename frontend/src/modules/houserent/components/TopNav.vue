<template>
  <header class="topnav">
    <div class="topnav-inner">
      <!-- 品牌区 -->
      <div class="topnav-brand" @click="go('home')">
        <span class="brand-icon">🏠</span>
        <span class="brand-text">校友直租</span>
      </div>

      <!-- 导航区 -->
      <nav class="topnav-nav">
        <a
          v-for="item in tabs"
          :key="item.name"
          class="nav-item"
          :class="{ on: route.name === item.name }"
          @click="go(item.name)"
        >
          <i :class="item.icon"></i>
          <span>{{ item.label }}</span>
        </a>
      </nav>

      <!-- 操作区 -->
      <div class="topnav-actions">
        <button class="btn-publish" @click="go('publish')">
          <i class="ph ph-plus"></i>
          <span>发布房源</span>
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

<!-- 全局样式（非 scoped）：postcss-pxtorem 将 px 转为 rem，rootValue=16 即 1rem=16px -->
<style>
.topnav {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 200;
  height: 56px;
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--border);
}
.topnav-inner {
  height: 100%;
  padding: 0 24px;
  display: flex;
  align-items: center;
  gap: 24px;
}
.topnav-brand {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  cursor: pointer;
  user-select: none;
}
.brand-icon {
  font-size: 1.25rem;
  line-height: 1;
}
.brand-text {
  font-size: 1rem;
  font-weight: 700;
  color: var(--primary);
  letter-spacing: -0.3px;
}
.topnav-nav {
  display: flex;
  align-items: center;
  gap: 2px;
  flex: 1;
  min-width: 0;
}
.nav-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: 8px;
  font-size: 0.875rem;
  color: var(--fg2);
  text-decoration: none;
  cursor: pointer;
  user-select: none;
  white-space: nowrap;
  flex-shrink: 1;
  min-width: 0;
  transition: color 0.15s, background 0.15s;
  position: relative;
}
.nav-item i {
  font-size: 1rem;
}
.nav-item:hover {
  color: var(--fg);
  background: var(--bg);
}
.nav-item.on {
  color: var(--primary);
  font-weight: 600;
  background: var(--primary-soft);
}
.nav-item.on::after {
  content: '';
  position: absolute;
  bottom: -8px;
  left: 50%;
  transform: translateX(-50%);
  width: 16px;
  height: 2.5px;
  border-radius: 2px;
  background: var(--primary);
}
.topnav-actions {
  flex-shrink: 1;
}
.btn-publish {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 7px 16px;
  border-radius: 8px;
  background: var(--primary);
  color: #fff;
  font-size: 0.8125rem;
  font-weight: 600;
  border: none;
  cursor: pointer;
  user-select: none;
  transition: opacity 0.15s, transform 0.1s;
  box-shadow: 0 1px 3px rgba(255, 106, 0, 0.25);
}
.btn-publish i {
  font-size: 0.9rem;
}
.btn-publish:hover {
  opacity: 0.92;
}
.btn-publish:active {
  transform: scale(0.97);
}
@media (max-width: 640px) {
  .topnav-inner {
    padding: 0 12px;
    gap: 12px;
  }
  .nav-item {
    padding: 6px 8px;
    font-size: 0.8125rem;
  }
  .nav-item i {
    display: none;
  }
  .brand-icon {
    display: none;
  }
  .btn-publish span {
    display: none;
  }
  .btn-publish {
    padding: 8px;
    border-radius: 50%;
    width: 36px;
    height: 36px;
    justify-content: center;
  }
}
</style>
