<template>
  <div class="empty">
    <i :class="icon"></i>
    <p>{{ text }}</p>
    <slot />
  </div>
</template>

<script setup>
defineProps({
  icon: { type: String, default: 'ph ph-house-simple' },
  text: { type: String, default: '暂无内容' }
})
</script>

<style scoped>
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 48px 16px;
  color: var(--fg3);
}
.empty i {
  font-size: 2rem;
}
.empty p {
  font-size: 0.85rem;
}
</style>
