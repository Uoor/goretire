<template>
  <div class="filter-bar">
    <!-- 顶部筛选栏：按显示组聚合入口；组内有选中即高亮并显示已选值 -->
    <div
      v-for="g in groups"
      :key="g.disp"
      class="f-item"
      :class="{ on: groupActive(g.disp) }"
      @click="openGroup(g.disp)"
    >
      <span class="f-label">{{ g.label }}</span>
      <span v-if="groupActive(g.disp)" class="f-val">{{ labelOf(g.disp) }}</span>
      <i class="ph ph-caret-down" :class="{ up: activeDisp === g.disp }"></i>
    </div>
    <div v-if="activeCount > 0" class="f-clear" @click="clearAll">清空</div>

    <!-- 底部弹层：当前显示组下所有选项（不同内部 group 独立选择） -->
    <van-popup v-model:show="showSheet" position="bottom" round :safe-area-inset-bottom="true">
      <div class="sheet">
        <div class="sheet-head">
          <span class="sheet-title">{{ activeLabel }}</span>
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
          <button class="reset-btn" @click="clearGroup(activeDisp)">重置</button>
          <button class="confirm-btn" @click="showSheet = false">完成</button>
        </div>
      </div>
    </van-popup>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'

// 分组筛选：顶部一行按「显示组」聚合入口，点开底部弹层选择（单选互斥、组间独立）。
// 显示组：多个内部 group 可共用一个入口（如 pet/new → "其他"），弹层里各自独立选择。
// modelValue: { [group]: selectedKey }，交互与旧版一致，HomeView 数据无需改动。
const props = defineProps({
  chips: { type: Array, required: true }, // [{ key, label, group }]
  modelValue: { type: Object, default: () => ({}) }
})
const emit = defineEmits(['update:modelValue'])

const showSheet = ref(false)
const activeDisp = ref('')

/** 内部 group → 显示组名（同名的多个 group 聚合为一个入口） */
const GROUP_NAMES = { region: '区域', price: '价格', type: '户型', label: '标签', pet: '其他', new: '其他' }
const dispOf = (group) => GROUP_NAMES[group] || group

/** 显示组入口：按显示名去重，保持 chips 出现顺序 */
const groups = computed(() => {
  const seen = new Set()
  const list = []
  for (const c of props.chips) {
    const d = dispOf(c.group)
    if (!seen.has(d)) {
      seen.add(d)
      list.push({ disp: d, label: d })
    }
  }
  return list
})

const activeLabel = computed(() => activeDisp.value)
const groupChips = computed(() => props.chips.filter((c) => dispOf(c.group) === activeDisp.value))

/** 已选中 chip 数（不同内部 group 各计一次，pet/new 可同时选中算 2 个筛选） */
const activeCount = computed(() => Object.keys(props.modelValue).length)

/** 显示组下所有已选中值文案（多个用 · 连接） */
function labelOf(disp) {
  return props.chips
    .filter((c) => dispOf(c.group) === disp && props.modelValue[c.group] === c.key)
    .map((c) => c.label)
    .join('·')
}

/** 显示组下是否已有选中（多个内部 group 任一选中即高亮） */
function groupActive(disp) {
  return props.chips.some((c) => dispOf(c.group) === disp && props.modelValue[c.group] != null)
}

function openGroup(disp) {
  activeDisp.value = disp
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

function clearGroup(disp) {
  const next = { ...props.modelValue }
  for (const c of props.chips) {
    if (dispOf(c.group) === disp) delete next[c.group]
  }
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
  /* 防止任何子项把页面撑宽（clip 不建立滚动容器，不破坏 sticky 吸顶） */
  max-width: 100vw;
  overflow-x: clip;
}
.f-item {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 3px;
  flex: 1 1 0;
  min-width: 0;
  padding: 6px 4px;
  border-radius: 8px;
  font-size: 0.74rem;
  color: var(--fg2);
  background: #f5f5f4;
  border: 1px solid transparent;
  cursor: pointer;
  white-space: nowrap;
  overflow: hidden;
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
  white-space: nowrap;
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
