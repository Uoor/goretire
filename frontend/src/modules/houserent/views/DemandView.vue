<template>
  <div class="demand-page">
    <TopBar />
    <div class="prompt-hero">
      <div class="big">🔍 求租需求墙</div>
      <div class="small">暂时没找到合适的？挂上需求，新房源自动匹配提醒你</div>
      <div class="prompt-input" @click="showCreate = true">
        <i class="ph ph-plus-circle"></i>
        <span>{{ createForm.region ? '已填写需求，点击修改' : '发布我的求租需求…' }}</span>
      </div>
    </div>

    <div class="seg-tabs">
      <div class="seg-tab" :class="{ on: tab === 'wall' }" @click="tab = 'wall'">求租墙</div>
      <div class="seg-tab" :class="{ on: tab === 'mine' }" @click="tab = 'mine'">我的需求</div>
    </div>

    <div class="wall" v-if="tab === 'wall'">
      <van-skeleton v-if="loading" v-for="i in 3" :key="i" title :row="2" class="sk" />
      <template v-else>
        <div v-for="d in wall" :key="d.id" class="match-card">
        <div class="mc-top">
          <b>{{ d.region }}</b>
          <span v-if="d.houseType" class="mc-tag">{{ d.houseType }}</span>
          <span v-if="d.budget" class="mc-tag">{{ budgetText(d.budget) }}</span>
          <span class="mc-status" :class="d.matchStatus === 1 ? 's-matched' : ''">
            {{ d.matchStatus === 1 ? '已匹配' : '待匹配' }}
          </span>
        </div>
        <div v-if="d.description" class="mc-desc">{{ d.description }}</div>
        <div v-if="d.requirements" class="mc-reqs">
          <span v-for="r in reqs(d.requirements)" :key="r" class="req-tag">{{ r }}</span>
        </div>
        <div class="mc-meta">入住：{{ d.moveInDate || '时间灵活' }} · 租期：{{ d.leaseTerm || '面议' }}</div>
      </div>
      <EmptyState v-if="wall.length === 0" text="求租墙还空着，来发布第一个需求吧" />
      </template>
    </div>

    <div class="wall" v-else>
      <van-skeleton v-if="loading" v-for="i in 3" :key="i" title :row="2" class="sk" />
      <template v-else>
        <div v-for="d in mine" :key="d.id" class="match-card">
        <div class="mc-top">
          <b>{{ d.region }}</b>
          <span v-if="d.houseType" class="mc-tag">{{ d.houseType }}</span>
          <span class="mc-status" :class="statusClass(d.matchStatus)">{{ statusText(d.matchStatus) }}</span>
        </div>
        <div v-if="d.description" class="mc-desc">{{ d.description }}</div>
        <div class="mc-ops">
          <button v-if="d.matchStatus !== 2" class="op-btn ghost" @click="complete(d)">标记已成交</button>
          <button class="op-btn" @click="rematch(d)">重新匹配</button>
          <button class="op-btn" @click="withdraw(d)">撤回</button>
        </div>
      </div>
      <EmptyState v-if="mine.length === 0" text="还没有发布过求租需求" />
      </template>
    </div>

    <!-- 发布/编辑需求弹层 -->
    <van-popup v-model:show="showCreate" position="bottom" round>
      <div class="create-panel">
        <h4>发布求租需求</h4>
        <div class="form-field"><input v-model="createForm.region" placeholder="目标区域 *（如：杭州西溪）" /></div>
        <div class="form-field"><input v-model="createForm.houseType" placeholder="期望户型（如：2室1厅）" /></div>
        <div class="form-field"><input v-model="createForm.budget" placeholder="预算区间（如：4000-6000）" /></div>
        <div class="form-field"><input v-model="createForm.moveInDate" type="date" /></div>
        <div class="form-field"><input v-model="createForm.leaseTerm" placeholder="期望租期（如：一年）" /></div>
        <div class="form-field"><input v-model="createForm.requirements" placeholder="特殊要求（可空，逗号分隔：如 可养宠,带车位,南向）" /></div>
        <div class="form-field"><input v-model="createForm.description" placeholder="一句话描述（可空）" /></div>
        <button class="btn-primary" :disabled="creating" @click="create">
          {{ creating ? '发布中…' : '发布到求租墙' }}
        </button>
      </div>
    </van-popup>

    <!-- 发布后的即时匹配结果 -->
    <van-popup v-model:show="showMatchResult" position="bottom" round :safe-area-inset-bottom="true">
      <div class="match-panel">
        <div class="mp-head">
          <b>{{ immediateMatches.length ? '🎯 现在就有一套适合你' : '📡 已挂上求租墙' }}</b>
          <i class="ph ph-x" @click="showMatchResult = false"></i>
        </div>
        <div v-if="immediateMatches.length" class="mp-sub">根据你的需求，以下在租房源当前即可联系房东：</div>
        <div v-else class="mp-sub">暂时没有完全匹配的在租房源，新房源上架会自动提醒你</div>
        <div
          v-for="m in immediateMatches"
          :key="m.houseId"
          class="mp-card"
          @click="goHouse(m.houseId)"
        >
          <div class="mp-line">
            <b>{{ m.community || ('房源 ' + m.houseId) }}</b>
            <span class="mp-meta">{{ m.houseType || '' }}{{ m.area ? ' ' + m.area + '㎡' : '' }} · {{ m.rent ? formatMoney(m.rent) + ' 元/月' : '' }}</span>
            <span class="mp-why">{{ m.reason }}</span>
          </div>
        </div>
        <div v-if="immediateDegraded" class="mp-deg">（本地降级匹配）</div>
        <button class="btn-primary mp-done" @click="showMatchResult = false">知道了</button>
      </div>
    </van-popup>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog, showToast, showSuccessToast } from 'vant'
import TopBar from '@/modules/houserent/components/TopBar.vue'
import EmptyState from '@/modules/houserent/components/EmptyState.vue'
import { demandApi } from '@/modules/houserent/api'
import { formatMoney } from '@/utils/format'

const router = useRouter()

function goHouse(id) {
  showMatchResult.value = false
  router.push({ name: 'house-detail', params: { id } })
}

const tab = ref('wall')
const wall = ref([])
const loading = ref(false)
const mine = ref([])
const showCreate = ref(false)
const showMatchResult = ref(false)
const immediateMatches = ref([])
const immediateDegraded = ref(false)
const creating = ref(false)
const createForm = reactive({ region: '', houseType: '', budget: '', moveInDate: '', leaseTerm: '', requirements: '', description: '' })

function budgetText(budget) {
  try {
    const b = JSON.parse(budget)
    return [b.min, b.max].filter((v) => v != null).join('-') + ' 元'
  } catch {
    return budget
  }
}
/** 特殊要求 JSON 数组 → 标签列表 */
function reqs(requirements) {
  try {
    const arr = JSON.parse(requirements)
    return Array.isArray(arr) ? arr : []
  } catch {
    return []
  }
}
function statusText(s) {
  return { 0: '待匹配', 1: '已匹配', 2: '已成交' }[s] || '待匹配'
}
function statusClass(s) {
  return s === 2 ? 's-done' : s === 1 ? 's-matched' : ''
}

/** 手动重新匹配：用需求原文再匹配一轮现有房源，弹结果 */
async function rematch(d) {
  try {
    const res = await demandApi.rematch(d.id)
    d.matchStatus = res?.matches?.length ? 1 : 0
    immediateMatches.value = res?.matches || []
    immediateDegraded.value = !!res?.degraded
    showMatchResult.value = true
  } catch (e) {
    showToast(e.message || '匹配失败')
  }
}

async function loadWall() {
  loading.value = true
  try {
    wall.value = await demandApi.list()
  } catch (e) {
    showToast(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}
async function loadMine() {
  loading.value = true
  try {
    mine.value = await demandApi.mine()
  } catch (e) {
    showToast(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function create() {
  if (!createForm.region.trim()) {
    showToast('目标区域必填')
    return
  }
  creating.value = true
  try {
    const budget = createForm.budget
      ? (() => {
          const [min, max] = createForm.budget.split('-').map((s) => Number(s.trim()))
          return JSON.stringify({ min: min || null, max: max || null })
        })()
      : null
    const requirements = createForm.requirements
      ? JSON.stringify(createForm.requirements.split(/[,，]/).map((s) => s.trim()).filter(Boolean))
      : null
    const res = await demandApi.create({
      region: createForm.region.trim(),
      houseType: createForm.houseType.trim(),
      budget,
      moveInDate: createForm.moveInDate || null,
      leaseTerm: createForm.leaseTerm.trim(),
      requirements,
      description: createForm.description.trim()
    })
    // 发布后即时匹配：有结果展示，无结果提示等待新房源
    immediateMatches.value = res?.matches || []
    immediateDegraded.value = !!res?.degraded
    showMatchResult.value = true
    showCreate.value = false
    Object.keys(createForm).forEach((k) => (createForm[k] = ''))
    loadWall()
    loadMine()
  } catch (e) {
    showToast(e.message || '发布失败')
  } finally {
    creating.value = false
  }
}

async function complete(d) {
  // 状态变更：二次确认
  try {
    await showConfirmDialog({
      title: '标记已成交',
      message: '确认将这条求租需求标记为已成交吗？标记后将从求租墙移除。',
      confirmButtonText: '确认成交',
      confirmButtonColor: '#FF6A00'
    })
  } catch {
    return // 用户取消
  }
  try {
    await demandApi.complete(d.id)
    showSuccessToast('已标记成交')
    loadMine()
    loadWall()
  } catch (e) {
    showToast(e.message || '操作失败')
  }
}

async function withdraw(d) {
  // 状态变更：二次确认
  try {
    await showConfirmDialog({
      title: '撤回需求',
      message: '确认撤回这条求租需求吗？撤回后将从求租墙移除，且不再接收新房源提醒。',
      confirmButtonText: '确认撤回',
      confirmButtonColor: '#FF6A00'
    })
  } catch {
    return // 用户取消
  }
  try {
    await demandApi.withdraw(d.id)
    showSuccessToast('已撤回')
    loadMine()
    loadWall()
  } catch (e) {
    showToast(e.message || '操作失败')
  }
}

onMounted(() => {
  loadWall()
  loadMine()
})
</script>

<style scoped>
.demand-page {
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
  cursor: pointer;
}
.prompt-input i {
  color: var(--primary);
}
.seg-tabs {
  display: flex;
  background: var(--card);
  border-bottom: 1px solid var(--border);
}
.seg-tab {
  flex: 1;
  text-align: center;
  padding: 12px 0;
  font-size: 0.82rem;
  color: var(--fg2);
  cursor: pointer;
  border-bottom: 2px solid transparent;
}
.seg-tab.on {
  color: var(--primary-deep);
  font-weight: 600;
  border-bottom-color: var(--primary);
}
.wall {
  padding: 12px 16px;
}
.sk {
  border-radius: 14px;
}
.match-card {
  background: var(--card);
  border-radius: 14px;
  padding: 14px;
  border: 1px solid var(--border);
  margin-bottom: 10px;
}
.mc-top {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.mc-top b {
  font-size: 0.86rem;
}
.mc-tag {
  font-size: 0.62rem;
  color: var(--primary-deep);
  background: var(--primary-soft);
  padding: 2px 8px;
  border-radius: 999px;
}
.mc-status {
  margin-left: auto;
  font-size: 0.62rem;
  color: var(--fg3);
  background: var(--bg);
  padding: 2px 8px;
  border-radius: 999px;
}
.mc-status.s-matched {
  color: var(--accent);
  background: var(--accent-soft);
}
.mc-status.s-done {
  color: var(--fg2);
}
.mc-desc {
  font-size: 0.78rem;
  color: var(--fg2);
  margin-top: 8px;
  line-height: 1.5;
}
.mc-reqs {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}
.req-tag {
  font-size: 0.64rem;
  color: var(--primary-deep);
  background: var(--primary-soft);
  padding: 2px 8px;
  border-radius: 999px;
}
.mc-meta {
  font-size: 0.68rem;
  color: var(--fg3);
  margin-top: 6px;
}
.mc-ops {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 10px;
}
.op-btn {
  font-size: 0.7rem;
  padding: 6px 12px;
  border-radius: 8px;
  border: none;
  background: var(--primary);
  color: #fff;
  font-weight: 600;
  cursor: pointer;
}
.op-btn.ghost {
  background: #fff;
  color: var(--fg2);
  border: 1px solid var(--border);
}
.op-btn:active {
  opacity: 0.85;
}
.create-panel {
  padding: 20px 16px 28px;
}
.create-panel h4 {
  font-size: 0.95rem;
  font-weight: 600;
  margin-bottom: 14px;
}
.form-field {
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 10px 12px;
  margin-bottom: 10px;
  display: flex;
  align-items: center;
}
.form-field input {
  border: none;
  outline: none;
  flex: 1;
  font-size: 0.8rem;
  font-family: inherit;
  color: var(--fg);
  background: transparent;
}
.btn-primary {
  background: var(--primary);
  color: #fff;
  border: none;
  border-radius: 10px;
  padding: 12px;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  width: 100%;
  box-shadow: 0 2px 0 var(--primary-deep);
  margin-top: 6px;
}
.btn-primary:active {
  transform: translateY(1px);
  box-shadow: none;
}
.btn-primary:disabled {
  opacity: 0.5;
  box-shadow: none;
}
.match-panel {
  padding: 16px;
}
.mp-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 0.92rem;
  margin-bottom: 6px;
}
.mp-head i {
  color: var(--fg3);
  font-size: 1.1rem;
  cursor: pointer;
}
.mp-sub {
  font-size: 0.75rem;
  color: var(--fg3);
  margin-bottom: 12px;
}
.mp-card {
  background: var(--primary-soft);
  border: 1px solid rgba(255, 106, 0, 0.25);
  border-radius: 10px;
  padding: 12px;
  margin-bottom: 8px;
  cursor: pointer;
}
.mp-line {
  display: flex;
  align-items: baseline;
  gap: 8px;
  font-size: 0.82rem;
}
.mp-why {
  font-size: 0.72rem;
  color: var(--primary-deep);
  flex: 1;
  text-align: right;
}
.mp-meta {
  font-size: 0.68rem;
  color: var(--fg3);
  white-space: nowrap;
}
.mp-deg {
  font-size: 0.68rem;
  color: var(--fg3);
  text-align: center;
  margin: 4px 0;
}
.mp-done {
  width: 100%;
  margin-top: 10px;
}
</style>
