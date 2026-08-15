<template>
  <div class="subscribe-page">
    <TopBar />
    <div class="prompt-hero">
      <div class="big">🔔 订阅你想要的，新房源主动找你</div>
      <div class="small">用一句话订阅，AI 帮你盯着：房源或租客一出现就提醒你</div>
      <div class="prompt-input">
        <i class="ph ph-bell-ring"></i>
        <input
          v-model="rawText"
          placeholder="如：西溪附近 6000 以内两居，能养猫"
          @keyup.enter="create"
        />
      </div>
      <div class="type-seg">
        <div class="seg-item" :class="{ on: type === 1 }" @click="type = 1">找房源</div>
        <div class="seg-item" :class="{ on: type === 2 }" @click="type = 2">找租客</div>
      </div>
      <button class="sub-btn" :disabled="!rawText.trim() || creating" @click="create">
        {{ creating ? '创建中…' : '创建订阅' }}
      </button>
    </div>

    <div class="list">
      <div v-for="s in subs" :key="s.id" class="sub-item">
        <div class="txt">
          <div class="q">{{ s.rawText }}</div>
          <div class="st">{{ typeText(s.type) }} · 已推送 {{ s.pushCount || 0 }} 次</div>
        </div>
        <div class="sw" :class="{ on: s.status === 0 }" @click="toggle(s)"></div>
      </div>
      <EmptyState v-if="subs.length === 0" icon="ph ph-bell" text="还没有订阅，创建一条试试" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { showToast, showSuccessToast } from 'vant'
import TopBar from '@/modules/houserent/components/TopBar.vue'
import EmptyState from '@/modules/houserent/components/EmptyState.vue'
import { subscribeApi } from '@/modules/houserent/api'

const subs = ref([])
const rawText = ref('')
const type = ref(1)
const creating = ref(false)

function typeText(t) {
  return t === 2 ? '找租客' : '找房源'
}

async function load() {
  try {
    subs.value = await subscribeApi.list()
  } catch (e) {
    showToast(e.message || '加载失败')
  }
}

async function create() {
  const text = rawText.value.trim()
  if (!text) return
  creating.value = true
  try {
    await subscribeApi.create({ type: type.value, rawText: text })
    showSuccessToast('订阅已创建，新房源会自动提醒你')
    rawText.value = ''
    load()
  } catch (e) {
    showToast(e.message || '创建失败')
  } finally {
    creating.value = false
  }
}

async function toggle(s) {
  const next = s.status === 0 ? 1 : 0
  try {
    await subscribeApi.update(s.id, { status: next })
    s.status = next
  } catch (e) {
    showToast(e.message || '操作失败')
  }
}

onMounted(load)
</script>

<style scoped>
.subscribe-page {
  padding-bottom: 76px;
}
.prompt-hero {
  background: linear-gradient(135deg, var(--primary-soft), #ffebd6);
  padding: 18px 16px;
  border-bottom: 1px solid var(--border);
}
.prompt-hero .big {
  font-size: 0.98rem;
  font-weight: 700;
  color: var(--fg);
  margin-bottom: 4px;
}
.prompt-hero .small {
  font-size: 0.72rem;
  color: var(--fg2);
  margin-bottom: 12px;
}
.prompt-input {
  background: #fff;
  border-radius: 12px;
  padding: 13px 14px;
  font-size: 0.85rem;
  color: var(--fg3);
  display: flex;
  align-items: center;
  gap: 8px;
  border: 1px solid var(--primary);
  box-shadow: 0 4px 14px rgba(255, 106, 0, 0.12);
}
.prompt-input i {
  color: var(--primary);
}
.prompt-input input {
  border: none;
  outline: none;
  flex: 1;
  font-size: 0.85rem;
  font-family: inherit;
  color: var(--fg);
  background: transparent;
  min-width: 0;
}
.type-seg {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}
.seg-item {
  font-size: 0.74rem;
  padding: 6px 14px;
  border-radius: 8px;
  border: 1px solid rgba(255, 106, 0, 0.35);
  color: var(--fg2);
  background: #fff;
  cursor: pointer;
  transition: all 0.15s ease;
}
.seg-item.on {
  background: var(--primary-soft);
  border-color: var(--primary);
  color: var(--primary-deep);
  font-weight: 600;
}
.sub-btn {
  width: 100%;
  margin-top: 12px;
  background: var(--primary);
  color: #fff;
  border: none;
  border-radius: 10px;
  padding: 12px;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 2px 0 var(--primary-deep);
}
.sub-btn:active {
  transform: translateY(1px);
  box-shadow: none;
}
.sub-btn:disabled {
  opacity: 0.5;
  box-shadow: none;
}
.list {
  padding: 12px 16px;
}
.sub-item {
  background: var(--card);
  border-radius: 12px;
  padding: 12px 14px;
  border: 1px solid var(--border);
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  gap: 10px;
}
.sub-item .txt {
  flex: 1;
  min-width: 0;
}
.sub-item .txt .q {
  font-size: 0.82rem;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.sub-item .txt .st {
  font-size: 0.66rem;
  color: var(--fg3);
  margin-top: 2px;
}
.sw {
  width: 40px;
  height: 22px;
  border-radius: 999px;
  background: var(--border);
  position: relative;
  cursor: pointer;
  flex-shrink: 0;
  transition: background 0.2s;
}
.sw::after {
  content: '';
  position: absolute;
  left: 3px;
  top: 3px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: #fff;
  transition: left 0.2s;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.15);
}
.sw.on {
  background: var(--accent);
}
.sw.on::after {
  left: 21px;
}
</style>
