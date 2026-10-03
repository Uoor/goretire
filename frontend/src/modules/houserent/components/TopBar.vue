<template>
  <div class="topbar sticky-shell">
    <button v-if="back" class="back" @click="goBack"><i class="ph ph-arrow-left"></i></button>
    <div v-if="title" class="title-center">{{ title }}</div>
    <div v-else class="brand">🏡 校友<span>安居</span></div>
    <div class="spacer"></div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

defineProps({
  back: { type: Boolean, default: false },
  title: { type: String, default: '' }
})

const router = useRouter()
function goBack() {
  if (window.history.length > 1) router.back()
  else router.push({ name: 'home' })
}
</script>

<style scoped>
.topbar {
  background: var(--card);
  padding: 14px 16px 10px;
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: center;
  gap: 10px;
  position: sticky;
  top: 0;
  z-index: 10;
}
.brand {
  font-weight: 700;
  font-size: 0.98rem;
}
.brand span {
  color: var(--primary);
}
.back {
  font-size: 1.1rem;
  color: var(--fg2);
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px;
  display: flex;
}
.title-center {
  flex: 1;
  text-align: center;
  font-weight: 600;
  font-size: 0.9rem;
  margin-right: 28px;
}
.spacer {
  flex: 1;
}
</style>
