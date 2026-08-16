<template>
  <div class="guide-page">
    <TopBar back title="避坑指南" />

    <div class="guide-hero">
      <div class="big">📖 避坑指南</div>
      <div class="small">合同模板 · 押金清单 · 骗局案例 · 区域攻略<br />签约前花 2 分钟，避开 90% 的坑</div>
    </div>

    <!-- 去钉钉知识库引导（下载模板/完整内容） -->
    <div v-if="spaceUrl" class="kb-entry" @click="openSpace">
      <i class="ph ph-books"></i>
      <div class="kb-tx">
        <b>需要下载合同模板等文件？</b>
        <span>去钉钉知识库查看完整文档与附件</span>
      </div>
      <i class="ph ph-arrow-up-right"></i>
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
          <div v-if="item.url" class="gc-link" @click.stop="openDoc(item.url)">
            <i class="ph ph-arrow-up-right"></i>在钉钉中查看原文 / 下载附件
          </div>
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
import { showToast } from 'vant'
import TopBar from '@/modules/houserent/components/TopBar.vue'
import { guideApi } from '@/modules/houserent/api'
import { isDingTalk } from '@/utils/dd'

const sections = ref([])
const active = ref('')
const items = ref([])
const loading = ref(true)
const spaceUrl = ref('')
// 展开的条目 nodeId → 正文（懒加载）
const openMap = ref({})

/** 打开钉钉链接：容器内用 JSAPI，浏览器 fallback 新窗口 */
function openDingLink(url) {
  if (!url) return
  if (isDingTalk() && typeof dd !== 'undefined' && dd.biz?.util?.openLink) {
    dd.biz.util.openLink({ url })
  } else {
    window.open(url, '_blank')
  }
}

/** 去钉钉知识库（下载模板/完整内容） */
function openSpace() {
  openDingLink(spaceUrl.value)
}

/** 打开单篇文档原文 */
function openDoc(url) {
  openDingLink(url)
}

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
    const [secs, space] = await Promise.all([
      guideApi.sections(),
      guideApi.spaceUrl().catch(() => ({ url: '' }))
    ])
    sections.value = secs
    spaceUrl.value = space?.url || ''
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
.kb-entry {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 10px 16px 0;
  padding: 12px 14px;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 12px;
  cursor: pointer;
}
.kb-entry > i:first-child {
  color: #2563eb;
  font-size: 1.3rem;
}
.kb-entry > i:last-child {
  margin-left: auto;
  color: #2563eb;
}
.kb-tx b {
  display: block;
  font-size: 0.78rem;
  color: #1e40af;
}
.kb-tx span {
  font-size: 0.68rem;
  color: #64748b;
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
.gc-link {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 10px;
  padding: 8px 10px;
  background: #eff6ff;
  border-radius: 8px;
  font-size: 0.72rem;
  color: #2563eb;
  white-space: nowrap;
  cursor: pointer;
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
