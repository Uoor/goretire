<template>
  <div class="app-shell">
    <router-view />
    <TabBar v-if="route.meta.tab" />
  </div>
</template>

<script setup>
import { useRoute } from 'vue-router'
import TabBar from '@/modules/houserent/components/TabBar.vue'

const route = useRoute()
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
}
</style>
