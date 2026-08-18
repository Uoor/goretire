<template>
  <div class="app-shell" :class="{ 'is-pc': isPc }">
    <!-- 移动端：底部 TabBar；PC 端：顶部导航 -->
    <TopNav v-if="isPc" />
    <div class="app-content">
      <router-view />
    </div>
    <TabBar v-if="!isPc && route.meta.tab" />
  </div>
</template>

<script setup>
import { useRoute } from 'vue-router'
import TabBar from '@/modules/houserent/components/TabBar.vue'
import TopNav from '@/modules/houserent/components/TopNav.vue'
import { ref, onMounted, onBeforeUnmount } from 'vue'

const route = useRoute()
const isPc = ref(false)

function checkViewport() {
  isPc.value = window.innerWidth >= 768
}

onMounted(() => {
  checkViewport()
  window.addEventListener('resize', checkViewport)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', checkViewport)
})
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
}

/* PC 端：顶部导航 + 居中宽内容区 */
.app-shell.is-pc {
  padding-top: 60px; /* 顶部导航高度 */
}
.app-shell.is-pc .app-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
  min-height: calc(100vh - 60px);
}

/* 移动端：无额外 padding */
.app-shell:not(.is-pc) .app-content {
  min-height: 100vh;
}
</style>
