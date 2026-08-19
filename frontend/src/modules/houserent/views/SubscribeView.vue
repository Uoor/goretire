<template>
  <div class="subscribe-page">
    <div class="prompt-hero">
      <div class="big">🔔 订阅管理</div>
      <div class="small">新房源会自动提醒你，可随时暂停、编辑或删除订阅</div>
    </div>

    <!-- 引导：创建入口已并入发布需求 -->
    <div class="guide-strip" @click="goPublish">
      <div class="gs-tx">
        <b>想要新房源提醒？</b>
        <span>发布求租需求时勾选「同时创建订阅」即可</span>
      </div>
      <div class="gs-btn">去发布<i class="ph ph-arrow-up-right"></i></div>
    </div>

    <div class="list">
      <van-skeleton v-if="loading" v-for="i in 3" :key="i" title :row="2" class="sk" />
      <template v-else>
        <div v-for="s in subs" :key="s.id" class="sub-item" @click="openDetail(s)">
          <div class="txt">
            <div class="q">{{ s.rawText }}</div>
            <div class="st">{{ typeText(s.type) }} · 已推送 {{ s.pushCount || 0 }} 次</div>
            <div v-if="s.quietHours" class="qh">🔕 免打扰 {{ quietText(s.quietHours) }}</div>
          </div>
          <div class="sw" :class="{ on: s.status === 0 }" @click.stop="toggle(s)"></div>
        </div>
        <EmptyState v-if="subs.length === 0" icon="ph ph-bell" text="还没有订阅，去发布一条求租需求吧">
          <button class="empty-btn" @click="goPublish">去发布需求</button>
        </EmptyState>
      </template>
    </div>

    <!-- 订阅详情：编辑 / 免打扰 / 推送历史 -->
    <van-popup v-model:show="showDetail" position="bottom" round>
      <div class="detail-panel" v-if="current">
        <div class="dp-head">
          <h4>订阅管理</h4>
          <i class="ph ph-x" @click="showDetail = false"></i>
        </div>

        <div class="form-label">订阅内容</div>
        <div class="form-field"><input v-model="edit.rawText" /></div>

        <div v-if="conditionText" class="cond-strip">
          <i class="ph ph-sparkle"></i>
          <span>AI 解析条件：{{ conditionText }}</span>
        </div>

        <div class="form-label">免打扰时段 <span class="hint">该时段内不推送提醒</span></div>
        <div class="qh-row">
          <input v-model="edit.qStart" type="time" />
          <span>至</span>
          <input v-model="edit.qEnd" type="time" />
          <button v-if="current.quietHours" class="qh-clear" @click="clearQuiet">清除</button>
        </div>

        <button class="btn-primary" @click="saveDetail">保存设置</button>

        <div class="form-label pushes-label">推送历史（{{ pushes.length }}）</div>
        <div v-if="pushes.length === 0" class="push-none">暂无推送记录，新房源匹配时会提醒你</div>
        <div v-for="p in pushes" :key="p.id" class="push-item">
          <div class="pi-content">{{ p.content }}</div>
          <div class="pi-time">{{ p.createdAt }}</div>
        </div>

        <button class="del-btn" @click="remove">删除订阅</button>
      </div>
    </van-popup>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showSuccessToast } from 'vant'
import EmptyState from '@/modules/houserent/components/EmptyState.vue'
import { subscribeApi } from '@/modules/houserent/api'

const router = useRouter()

const subs = ref([])
const loading = ref(false)

const showDetail = ref(false)
const current = ref(null)
const pushes = ref([])
const edit = reactive({ rawText: '', qStart: '', qEnd: '' })

/** 引导去发布需求：跳求租页（发布弹层由用户点击 prompt-input 打开） */
function goPublish() {
  router.push({ name: 'demand' })
}

/** 展示 AI 解析条件（structured_condition JSON → 可读文本） */
const conditionText = computed(() => {
  const c = current.value?.structuredCondition
  if (!c) return ''
  try {
    const o = JSON.parse(c)
    const parts = []
    if (o.region) parts.push(o.region)
    if (o.minRent || o.maxRent) parts.push(`${o.minRent || ''}-${o.maxRent || ''} 元`.replace(/^-/, '≤').replace(/-$/, '以内'))
    if (o.houseType) parts.push(o.houseType)
    if (o.petOk === 1) parts.push('可养宠')
    return parts.join(' · ') || '已解析'
  } catch {
    return c
  }
})

function typeText(t) {
  return t === 2 ? '找租客' : '找房源'
}

function quietText(qh) {
  try {
    const q = JSON.parse(qh)
    return `${q.start || '--'} - ${q.end || '--'}`
  } catch {
    return qh
  }
}

function openDetail(s) {
  current.value = s
  edit.rawText = s.rawText
  let q = { start: '', end: '' }
  try {
    q = s.quietHours ? JSON.parse(s.quietHours) : q
  } catch {
    /* ignore */
  }
  edit.qStart = q.start || ''
  edit.qEnd = q.end || ''
  pushes.value = []
  loadPushes(s.id)
  showDetail.value = true
}

async function loadPushes(id) {
  try {
    pushes.value = await subscribeApi.pushes(id)
  } catch (e) {
    pushes.value = []
  }
}

async function saveDetail() {
  const payload = { rawText: edit.rawText.trim() }
  if (edit.qStart && edit.qEnd) {
    payload.quietHours = JSON.stringify({ start: edit.qStart, end: edit.qEnd })
  }
  try {
    const updated = await subscribeApi.update(current.value.id, payload)
    current.value.rawText = updated.rawText
    current.value.quietHours = updated.quietHours
    load()
    showToast('已保存')
    showDetail.value = false
  } catch (e) {
    showToast(e.message || '保存失败')
  }
}

function clearQuiet() {
  edit.qStart = ''
  edit.qEnd = ''
}

async function remove() {
  try {
    await subscribeApi.remove(current.value.id)
    showSuccessToast('已删除订阅')
    showDetail.value = false
    load()
  } catch (e) {
    showToast(e.message || '删除失败')
  }
}

async function load() {
  loading.value = true
  try {
    subs.value = await subscribeApi.list()
  } catch (e) {
    showToast(e.message || '加载失败')
  } finally {
    loading.value = false
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
@media (min-width: 768px) {
  .subscribe-page {
    padding-bottom: 0;
  }
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
/* 引导条：发布需求时勾选创建订阅 */
.guide-strip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: 10px 16px 0;
  padding: 12px 14px;
  background: linear-gradient(90deg, var(--primary-soft), #fff7e6);
  border: 1px solid rgba(255, 106, 0, 0.2);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.15s ease;
}
.guide-strip:active {
  opacity: 0.85;
}
.gs-tx {
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.gs-tx b {
  font-size: 0.8rem;
  color: var(--fg);
}
.gs-tx span {
  font-size: 0.68rem;
  color: var(--fg3);
  line-height: 1.4;
}
.gs-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
  font-size: 0.78rem;
  font-weight: 600;
  color: var(--primary);
}
.gs-btn i {
  font-size: 0.85rem;
}
/* 空态按钮 */
.empty-btn {
  margin-top: 4px;
  padding: 9px 28px;
  border: none;
  border-radius: 10px;
  background: var(--primary);
  color: #fff;
  font-size: 0.82rem;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 2px 0 var(--primary-deep);
}
.empty-btn:active {
  transform: translateY(1px);
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
  cursor: pointer;
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
.sub-item .txt .qh {
  font-size: 0.64rem;
  color: var(--warning);
  margin-top: 3px;
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
  background: var(--card);
  transition: left 0.2s;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.15);
}
.sw.on {
  background: var(--accent);
}
.sw.on::after {
  left: 21px;
}
.detail-panel {
  padding: 20px 16px 28px;
  max-height: 70vh;
  overflow-y: auto;
}
.pushes-label {
  margin-top: 18px;
}
.push-none {
  font-size: 0.74rem;
  color: var(--fg3);
  padding: 10px 0;
}
.push-item {
  background: var(--bg);
  border-radius: 10px;
  padding: 10px 12px;
  margin-bottom: 8px;
}
.pi-content {
  font-size: 0.74rem;
  color: var(--fg2);
  line-height: 1.5;
}
.pi-time {
  font-size: 0.62rem;
  color: var(--fg3);
  margin-top: 4px;
}
/* PC 端主按钮/删除按钮限宽居中（公共样式见 styles/components.css） */
@media (min-width: 768px) {
  .btn-primary,
  .del-btn {
    display: block;
    width: auto;
    max-width: 320px;
    margin: 14px auto 0;
  }
}
</style>
