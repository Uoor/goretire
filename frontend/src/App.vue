<template>
  <!-- plain 页面（如 PC 扫码引导页）全屏独立渲染，不进 App 壳 -->
  <div v-if="route.meta.plain" class="app-shell plain">
    <router-view />
  </div>
  <div v-else class="app-shell" :class="{ 'is-pc': isPc }">
    <!-- 手机浏览器（非钉钉）：引导用钉钉扫码，体验更完整 -->
    <MobileDingTalkTip v-if="!isPc && !inDingTalk" />
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
import MobileDingTalkTip from '@/modules/houserent/components/MobileDingTalkTip.vue'
import { isDingTalk } from '@/utils/dd'
import { ref, onMounted, onBeforeUnmount } from 'vue'

const route = useRoute()
const isPc = ref(false)
const inDingTalk = ref(false)

function checkViewport() {
  isPc.value = window.innerWidth >= 768
}

onMounted(() => {
  checkViewport()
  window.addEventListener('resize', checkViewport)
  inDingTalk.value = isDingTalk()
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
  padding-top: 56px; /* 顶部导航高度 */
}
.app-shell.is-pc .app-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
  min-height: calc(100vh - 56px);
}

/* 移动端：无额外 padding */
.app-shell:not(.is-pc) .app-content {
  min-height: 100vh;
}

/* plain 页面（引导页）：无壳、无 padding，全屏由页面自己控制 */
.app-shell.plain {
  padding-top: 0;
}
.app-shell.plain .app-content {
  max-width: none;
  margin: 0;
  padding: 0;
  min-height: 100vh;
}
</style>
