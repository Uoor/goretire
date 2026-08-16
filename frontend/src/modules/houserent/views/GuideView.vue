<template>
  <div class="guide-page">
    <TopBar back title="避坑指南" />

    <div class="guide-hero">
      <div class="big">📖 避坑指南</div>
      <div class="small">合同模板 · 押金清单 · 骗局案例 · 区域攻略<br />签约前花 2 分钟，避开 90% 的坑</div>
    </div>

    <!-- 板块切换 -->
    <div class="seg-tabs">
      <div
        v-for="s in sections"
        :key="s.id"
        class="seg-tab"
        :class="{ on: active === s.id }"
        @click="active = s.id"
      >
        <i :class="s.icon"></i>{{ s.title }}
      </div>
    </div>

    <!-- 当前板块内容 -->
    <div class="panel">
      <div class="sec-intro">{{ current.intro }}</div>
      <div v-for="(item, i) in current.items" :key="i" class="guide-card">
        <div class="gc-head"><span class="gc-num num">{{ i + 1 }}</span><b>{{ item.t }}</b></div>
        <div class="gc-body">{{ item.d }}</div>
      </div>
    </div>

    <div class="tip-strip">
      <i class="ph ph-lightbulb"></i>
      <span>知识库持续由管理员整理群内优质问答，欢迎补充真实经验</span>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import TopBar from '@/modules/houserent/components/TopBar.vue'
import { GUIDE_SECTIONS } from '@/modules/houserent/data/guide'

const sections = GUIDE_SECTIONS
const active = ref(GUIDE_SECTIONS[0].id)
const current = computed(() => GUIDE_SECTIONS.find((s) => s.id === active.value) || GUIDE_SECTIONS[0])
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
}
.gc-head {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.84rem;
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
  line-height: 1.6;
  margin-top: 8px;
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
