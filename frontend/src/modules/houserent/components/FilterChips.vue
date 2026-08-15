<template>
  <div class="chips">
    <div
      v-for="chip in chips"
      :key="chip.key"
      class="chip"
      :class="{ on: modelValue === chip.key }"
      @click="$emit('update:modelValue', modelValue === chip.key ? '' : chip.key)"
    >
      {{ chip.label }}
    </div>
  </div>
</template>

<script setup>
defineProps({
  chips: { type: Array, required: true }, // [{key,label}]
  modelValue: { type: String, default: '' }
})
defineEmits(['update:modelValue'])
</script>

<style scoped>
.chips {
  display: flex;
  gap: 8px;
  padding: 10px 16px;
  overflow-x: auto;
  background: var(--card);
  border-bottom: 1px solid var(--border);
  scrollbar-width: none;
}
.chips::-webkit-scrollbar {
  display: none;
}
.chip {
  font-size: 0.74rem;
  padding: 5px 12px;
  border-radius: 999px;
  border: 1px solid var(--border);
  background: #fff;
  color: var(--fg2);
  white-space: nowrap;
  cursor: pointer;
  flex-shrink: 0;
  transition: all 0.15s ease;
}
.chip.on {
  background: var(--primary-soft);
  border-color: var(--primary);
  color: var(--primary-deep);
  font-weight: 600;
}
</style>
