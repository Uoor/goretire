<template>
  <div class="chips">
    <div
      v-for="chip in chips"
      :key="chip.key"
      class="chip"
      :class="{ on: modelValue[chip.group] === chip.key }"
      @click="toggle(chip)"
    >
      {{ chip.label }}
    </div>
  </div>
</template>

<script setup>
// 分组筛选条：每组内单选（互斥），不同组独立（如区域 西溪 + 价格 <3000 + 可养宠 可同时选中）。
// modelValue: { [group]: selectedKey }，选中/取消通过 update:modelValue 回传新对象。
const props = defineProps({
  chips: { type: Array, required: true }, // [{ key, label, group }]
  modelValue: { type: Object, default: () => ({}) }
})
const emit = defineEmits(['update:modelValue'])

function toggle(chip) {
  const next = { ...props.modelValue }
  if (next[chip.group] === chip.key) {
    delete next[chip.group]
  } else {
    next[chip.group] = chip.key
  }
  emit('update:modelValue', next)
}
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
