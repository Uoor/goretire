<template>
  <div class="filter-bar">
    <!-- 顶部筛选栏：每个入口一行，按分组去重；有选中显示已选标签与徽标 -->
    <div
      v-for="g in groups"
      :key="g.group"
      class="f-item"
      :class="{ on: !!modelValue[g.group] }"
      @click="openGroup(g.group)"
    >
      <span class="f-label">{{ g.label }}</span>
      <span v-if="modelValue[g.group]" class="f-val">{{ labelOf(g.group) }}</span>
      <i class="ph ph-caret-down" :class="{ up: activeGroup === g.group }"></i>
    </div>
    <div v-if="activeCount > 0" class="f-clear" @click="clearAll">清空</div>

    <!-- 底部弹层：当前分组选项单选互斥 -->
    <van-popup v-model:show="showSheet" position="bottom" round :safe-area-inset-bottom="true">
      <div class="sheet">
        <div class="sheet-head">
          <span class="sheet-title">{{ activeGroupLabel }}</span>
          <i class="ph ph-x" @click="showSheet = false"></i>
        </div>
        <div class="sheet-body">
          <div
            v-for="chip in groupChips"
            :key="chip.key"
            class="s-option"
            :class="{ on: modelValue[chip.group] === chip.key }"
            @click="pick(chip)"
          >
            {{ chip.label }}
            <i v-if="modelValue[chip.group] === chip.key" class="ph ph-check"></i>
          </div>
        </div>
        <div class="sheet-foot">
          <button class="reset-btn" @click="clearGroup(activeGroup)">重置</button>
          <button class="confirm-btn" @click="showSheet = false">完成</button>
        </div>
      </div>
    </van-popup>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'

// 分组筛选：顶部一行按组聚合入口，点开底部弹层单选（组内互斥，组间独立）。
// modelValue: { [group]: selectedKey }，交互与旧版一致，HomeView 数据无需改动。
const props = defineProps({
  chips: { type: Array, required: true }, // [{ key, label, group }]
  modelValue: { type: Object, default: () => ({}) }
})
const emit = defineEmits(['update:modelValue'])

const showSheet = ref(false)
const activeGroup = ref('')

/** 分组展示顺序与命名（按 chips 出现顺序 + 覆盖常用中文名） */
const GROUP_NAMES = { region: '区域', price: '价格', type: '户型', label: '标签', pet: '其他', new: '其他' }
const groupLabelOf = (g) => GROUP_NAMES[g] || g

/** 去重分组：同一 group 的 chips 聚合为一个入口（pet/new 归入"其他"） */
const groups = computed(() => {
  const seen = new Set()
  const list = []
  for (const c of props.chips) {
    if (!seen.has(c.group)) {
      seen.add(c.group)
      list.push({ group: c.group, label: groupLabelOf(c.group) })
    }
  }
  return list
})

const activeGroupLabel = computed(() => groupLabelOf(activeGroup.value))
const groupChips = computed(() => props.chips.filter((c) => c.group === activeGroup.value))

const activeCount = computed(() => Object.keys(props.modelValue).length)

function labelOf(group) {
  const chip = props.chips.find((c) => c.key === props.modelValue[group])
  return chip ? chip.label : ''
}

function openGroup(group) {
  activeGroup.value = group
  showSheet.value = true
}

function pick(chip) {
  const next = { ...props.modelValue }
  if (next[chip.group] === chip.key) {
    delete next[chip.group]
  } else {
    next[chip.group] = chip.key
  }
  emit('update:modelValue', next)
}

function clearGroup(group) {
  const next = { ...props.modelValue }
  delete next[group]
  emit('update:modelValue', next)
}

function clearAll() {
  emit('update:modelValue', {})
}
</script>

<style scoped>
.filter-bar {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  background: var(--card);
  border-bottom: 1px solid var(--border);
  position: sticky;
  top: 0;
  z-index: 10;
}
.f-item {
  display: flex;
  align-items: center;
  gap: 3px;
  padding: 6px 10px;
  border-radius: 8px;
  font-size: 0.74rem;
  color: var(--fg2);
  background: #f5f5f4;
  border: 1px solid transparent;
  cursor: pointer;
  white-space: nowrap;
  flex-shrink: 0;
  transition: all 0.15s ease;
}
.f-item.on {
  background: var(--primary-soft);
  border-color: var(--primary);
  color: var(--primary-deep);
}
.f-label {
  font-weight: 500;
}
.f-val {
  max-width: 4.2em;
  overflow: hidden;
  text-overflow: ellipsis;
  font-weight: 600;
}
.f-item i {
  font-size: 0.7rem;
  color: var(--fg3);
}
.f-item i.up {
  transform: rotate(180deg);
}
.f-clear {
  margin-left: auto;
  font-size: 0.7rem;
  color: var(--fg3);
  padding: 6px 4px;
  cursor: pointer;
  white-space: nowrap;
}
.f-clear:active {
  color: var(--primary-deep);
}

.sheet {
  padding: 0 0 env(safe-area-inset-bottom);
}
.sheet-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px 8px;
}
.sheet-title {
  font-size: 0.9rem;
  font-weight: 700;
}
.sheet-head i {
  font-size: 1.1rem;
  color: var(--fg3);
  cursor: pointer;
}
.sheet-body {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding: 8px 16px 16px;
}
.s-option {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 14px;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: #f7f7f6;
  font-size: 0.8rem;
  color: var(--fg2);
  cursor: pointer;
  transition: all 0.15s ease;
}
.s-option.on {
  background: var(--primary-soft);
  border-color: var(--primary);
  color: var(--primary-deep);
  font-weight: 600;
}
.s-option i {
  font-size: 0.8rem;
}
.sheet-foot {
  display: flex;
  gap: 10px;
  padding: 10px 16px calc(12px + env(safe-area-inset-bottom));
  border-top: 1px solid var(--border);
}
.reset-btn {
  flex: 1;
  padding: 11px;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: #fff;
  color: var(--fg2);
  font-size: 0.82rem;
  font-weight: 600;
  cursor: pointer;
}
.confirm-btn {
  flex: 2;
  padding: 11px;
  border-radius: 10px;
  border: none;
  background: var(--primary);
  color: #fff;
  font-size: 0.82rem;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 2px 0 var(--primary-deep);
}
.confirm-btn:active {
  transform: translateY(1px);
  box-shadow: none;
}
</style>
