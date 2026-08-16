<template>
  <div class="guide-page">
    <TopBar back title="避坑指南" />

    <div class="guide-hero">
      <div class="big">📖 避坑指南</div>
      <div class="small">合同模板 · 押金清单 · 骗局案例 · 区域攻略<br />签约前花 2 分钟，避开 90% 的坑</div>
    </div>

    <!-- 板块切换（钉钉知识库） -->
    <div class="seg-tabs" v-if="sections.length">
      <div
        v-for="s in sections"
        :key="s.nodeId"
        class="seg-tab"
        :class="{ on: active === s.nodeId }"
        @click="switchSection(s)"
      >
        {{ s.name }}
      </div>
    </div>

    <!-- 当前板块条目 -->
    <div class="panel">
      <div v-if="loading" class="sec-intro">加载中…</div>
      <div v-else-if="items.length === 0" class="sec-intro">该板块暂无内容</div>
      <div
        v-for="(item, i) in items"
        :key="item.nodeId"
        class="guide-card"
        :class="{ open: isOpen(item.nodeId) }"
        @click="toggle(item)"
      >
        <div class="gc-head">
          <span class="gc-num num">{{ i + 1 }}</span>
          <b>{{ item.name }}</b>
          <i class="ph ph-caret-down gc-arrow" :class="{ up: isOpen(item.nodeId) }"></i>
        </div>
        <div v-if="isOpen(item.nodeId)" class="gc-body">
          <template v-if="item.content !== undefined">{{ item.content }}</template>
          <template v-else>加载中…</template>
        </div>
      </div>
    </div>

    <div class="tip-strip">
      <i class="ph ph-lightbulb"></i>
      <span>知识库由管理员在钉钉中维护，本站自动同步</span>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import TopBar from '@/modules/houserent/components/TopBar.vue'
import { guideApi } from '@/modules/houserent/api'

const sections = ref([])
const active = ref('')
const items = ref([])
const loading = ref(true)
// 展开的条目 nodeId → 正文（懒加载）
const openMap = ref({})

async function switchSection(s) {
  active.value = s.nodeId
  items.value = []
  loading.value = true
  try {
    items.value = await guideApi.items(s.nodeId)
  } finally {
    loading.value = false
  }
}

function isOpen(nodeId) {
  return openMap.value[nodeId] != null
}

async function toggle(item) {
  if (isOpen(item.nodeId)) {
    delete openMap.value[item.nodeId]
    openMap.value = { ...openMap.value }
    return
  }
  // 首次展开：拉正文
  if (item.content === undefined) {
    try {
      const { content } = await guideApi.content(item.nodeId)
      item.content = content || '（暂无内容）'
    } catch (e) {
      item.content = '内容加载失败'
    }
  }
  openMap.value = { ...openMap.value, [item.nodeId]: true }
}

onMounted(async () => {
  try {
    sections.value = await guideApi.sections()
    if (sections.value.length) {
      await switchSection(sections.value[0])
    }
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.guide-page {
  padding-bottom: 40px;
  min-height: 100vh;
}
.guide-hero {
  background: linear-gradient(135deg, var(--primary-soft), #ffebd6);
  padding: 20px 16px;
  border-bottom: 1px solid var(--border);
}
.guide-hero .big {
  font-size: 1rem;
  font-weight: 700;
}
.guide-hero .small {
  font-size: 0.72rem;
  color: var(--fg2);
  margin-top: 4px;
  line-height: 1.6;
}
.seg-tabs {
  display: flex;
  gap: 6px;
  padding: 10px 16px;
  overflow-x: auto;
  background: var(--card);
  border-bottom: 1px solid var(--border);
  scrollbar-width: none;
}
.seg-tabs::-webkit-scrollbar {
  display: none;
}
.seg-tab {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 0.74rem;
  padding: 6px 12px;
  border-radius: 999px;
  border: 1px solid var(--border);
  color: var(--fg2);
  background: #fff;
  white-space: nowrap;
  cursor: pointer;
  flex-shrink: 0;
  transition: all 0.15s ease;
}
.seg-tab.on {
  background: var(--primary-soft);
  border-color: var(--primary);
  color: var(--primary-deep);
  font-weight: 600;
}
.panel {
  padding: 12px 16px;
}
.sec-intro {
  font-size: 0.78rem;
  color: var(--fg2);
  margin-bottom: 12px;
  background: var(--bg);
  border-radius: 10px;
  padding: 10px 12px;
}
.guide-card {
  background: var(--card);
  border-radius: 14px;
  padding: 14px;
  border: 1px solid var(--border);
  margin-bottom: 10px;
  cursor: pointer;
  transition: all 0.15s ease;
}
.guide-card:active {
  opacity: 0.85;
}
.gc-head {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.84rem;
}
.gc-arrow {
  margin-left: auto;
  color: var(--fg3);
  font-size: 0.9rem;
  transition: transform 0.2s ease;
}
.gc-arrow.up {
  transform: rotate(180deg);
}
.gc-num {
  width: 20px;
  height: 20px;
  border-radius: 6px;
  background: var(--primary-soft);
  color: var(--primary-deep);
  font-size: 0.72rem;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.gc-body {
  font-size: 0.76rem;
  color: var(--fg2);
  line-height: 1.7;
  margin-top: 8px;
  white-space: pre-wrap;
}
.tip-strip {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 16px;
  padding: 12px;
  background: #fffbe6;
  border: 1px solid #ffe58f;
  border-radius: 12px;
  font-size: 0.7rem;
  color: #874d00;
  line-height: 1.5;
}
.tip-strip i {
  color: var(--warning);
  font-size: 1.2rem;
  flex-shrink: 0;
}
</style>
