<template>
  <div class="app-shell">
    <router-view />
    <TabBar v-if="route.meta.tab" />
  </div>
</template>

<script setup>
import { useRoute } from 'vue-router'
import TabBar from '@/modules/houserent/components/TabBar.vue'
import { useDesktopZoom } from '@/utils/viewport'

const route = useRoute()
// 桌面宽视口（PC 钉钉/浏览器）自动缩放到 375 设计稿宽度
useDesktopZoom()
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
}
</style>
