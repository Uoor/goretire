<template>
  <div class="demand-page">
    <div class="prompt-hero">
      <div class="big">🔍 求租需求墙</div>
      <div class="small">暂时没找到合适的？挂上需求，新房源自动匹配提醒你</div>
      <div class="prompt-input" @click="openCreate">
        <i class="ph ph-plus-circle"></i>
        <span>{{ createForm.region ? '已填写需求，点击修改' : '发布我的求租需求…' }}</span>
      </div>
    </div>

    <div class="seg-tabs">
      <div class="seg-tab" :class="{ on: tab === 'wall' }" @click="tab = 'wall'">求租墙</div>
      <div class="seg-tab" :class="{ on: tab === 'mine' }" @click="tab = 'mine'">我发布的需求</div>
    </div>

    <div class="wall" v-if="tab === 'wall'">
      <van-skeleton v-if="loading" v-for="i in 3" :key="i" title :row="2" class="sk" />
      <template v-else>
        <div v-for="d in wall" :key="d.id" class="match-card" @click="openDetail(d)">
        <div class="mc-top">
          <b>{{ d.region }}</b>
          <span v-if="d.houseType" class="mc-tag">{{ d.houseType }}</span>
          <span v-if="d.budget" class="mc-tag">{{ budgetText(d.budget) }}</span>
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
        <div v-for="d in mine" :key="d.id" class="match-card" @click="openMyDetail(d)">
        <div class="mc-top">
          <b>{{ d.region }}</b>
          <span v-if="d.houseType" class="mc-tag">{{ d.houseType }}</span>
          <span class="mc-status" :class="statusClass(d.matchStatus)">{{ statusText(d.matchStatus) }}</span>
        </div>
        <div v-if="d.description" class="mc-desc">{{ d.description }}</div>
        <div class="mc-ops">
          <button v-if="d.matchStatus !== 2" class="op-btn" @click.stop="editDemand(d)">
            <i class="ph ph-pencil-simple"></i>编辑
          </button>
        </div>
      </div>
      <EmptyState v-if="mine.length === 0" text="还没有发布过求租需求" />
      </template>
    </div>

    <!-- 发布/编辑需求弹层 -->
    <van-popup v-model:show="showCreate" position="bottom" round :safe-area-inset-bottom="true">
      <div class="create-panel">
        <div class="dp-head">
          <h4>{{ editingId ? '编辑求租需求' : '发布求租需求' }}</h4>
          <i class="ph ph-x" @click="showCreate = false"></i>
        </div>
        <div class="form-field"><input v-model="createForm.region" placeholder="目标区域 *（如：杭州西溪）" /></div>
        <div class="form-field"><input v-model="createForm.houseType" placeholder="期望户型（如：2室1厅）" /></div>
        <div class="form-field"><input v-model="createForm.budget" placeholder="预算区间（如：4000-6000）" /></div>
        <div class="form-field"><input v-model="createForm.moveInDate" type="date" /></div>
        <div class="form-field"><input v-model="createForm.leaseTerm" placeholder="期望租期（如：一年）" /></div>
        <div class="form-field"><input v-model="createForm.requirements" placeholder="特殊要求（可空，逗号分隔：如 可养宠,带车位,南向）" /></div>
        <div class="form-field"><input v-model="createForm.description" placeholder="一句话描述（可空）" /></div>
        <!-- 创建订阅勾选：需求合并订阅入口（新房源自动提醒）；编辑时隐藏 -->
        <div v-if="!editingId" class="sub-check" @click="createForm.createSubscription = !createForm.createSubscription">
          <span class="ck" :class="{ on: createForm.createSubscription }">
            <i v-if="createForm.createSubscription" class="ph ph-check"></i>
          </span>
          <span class="ck-tx">
            <b>同时创建订阅</b>
            <small>新房源上架自动提醒你（可在「订阅」页管理）</small>
          </span>
        </div>
        <button class="btn-primary" :disabled="creating" @click="create">
          {{ creating ? '保存中…' : editingId ? '保存修改' : '发布到求租墙' }}
        </button>
      </div>
    </van-popup>

    <!-- 需求详情弹层：求租墙打开=联系租客；我的需求打开=管理（编辑/成交/重匹配/撤回） -->
    <van-popup v-model:show="showDetail" position="bottom" round :safe-area-inset-bottom="true">
      <div class="create-panel" v-if="detailData">
        <div class="dp-head">
          <h4>求租需求</h4>
          <i class="ph ph-x" @click="showDetail = false"></i>
        </div>
        <div class="dd-row"><b class="dd-label">区域</b><span>{{ detailData.region }}</span></div>
        <div v-if="detailData.houseType" class="dd-row"><b class="dd-label">户型</b><span>{{ detailData.houseType }}</span></div>
        <div v-if="detailData.budget" class="dd-row"><b class="dd-label">预算</b><span>{{ budgetText(detailData.budget) }}</span></div>
        <div v-if="detailData.moveInDate" class="dd-row"><b class="dd-label">入住</b><span>{{ detailData.moveInDate }}</span></div>
        <div v-if="detailData.leaseTerm" class="dd-row"><b class="dd-label">租期</b><span>{{ detailData.leaseTerm }}</span></div>
        <div v-if="detailData.description" class="dd-row dd-desc"><b class="dd-label">描述</b><span>{{ detailData.description }}</span></div>
        <div v-if="detailData.requirements" class="dd-reqs">
          <span v-for="r in reqs(detailData.requirements)" :key="r" class="req-tag">{{ r }}</span>
        </div>
        <div class="mc-meta" v-if="detailMode === 'mine'">
          状态：{{ statusText(detailData.matchStatus) }}
        </div>

        <!-- 求租墙视角：房东联系租客 -->
        <template v-if="detailMode === 'wall'">
          <button class="btn-primary" :disabled="contacting" @click="contactRenter(detailData)">
            {{ contacting ? '唤起中…' : '钉钉内联系租客' }}
          </button>
        </template>

        <!-- 我的需求视角：管理操作（编辑在卡片上，这里只留低频操作） -->
        <template v-else>
          <div class="dd-ops">
            <button v-if="detailData.matchStatus !== 2" class="op-btn ghost" @click="complete(detailData)">标记已成交</button>
            <button class="op-btn ghost danger" @click="withdraw(detailData)">撤回</button>
          </div>
        </template>
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
import EmptyState from '@/modules/houserent/components/EmptyState.vue'
import { demandApi } from '@/modules/houserent/api'
import { openSingleChat } from '@/utils/dd'
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
// 需求详情弹层：点求租墙卡片打开（wall 视角=联系租客）；点我的需求打开（mine 视角=管理）
const showDetail = ref(false)
const detailData = ref(null)
const detailMode = ref('wall')
const contacting = ref(false)
const createForm = reactive({ region: '', houseType: '', budget: '', moveInDate: '', leaseTerm: '', requirements: '', description: '', createSubscription: false })
// 编辑模式：null=新建，有值=编辑该 id 的需求
const editingId = ref(null)

/** 预算 JSON → 输入框文本（{"min":4000,"max":6000} → "4000-6000"） */
function budgetInput(budget) {
  try {
    const b = JSON.parse(budget)
    const parts = []
    if (b.min != null) parts.push(b.min)
    if (b.max != null) parts.push(b.max)
    return parts.join('-')
  } catch {
    return budget || ''
  }
}

/** 特殊要求 JSON 数组 → 逗号分隔输入框文本 */
function requirementsInput(requirements) {
  try {
    const arr = JSON.parse(requirements)
    return Array.isArray(arr) ? arr.join(',') : ''
  } catch {
    return ''
  }
}

/** 打开发布弹层（新建模式）：重置编辑态 */
function openCreate() {
  editingId.value = null
  showCreate.value = true
}

/** 编辑需求：预填表单并打开弹层 */
function editDemand(d) {
  editingId.value = d.id
  createForm.region = d.region || ''
  createForm.houseType = d.houseType || ''
  createForm.budget = budgetInput(d.budget)
  createForm.moveInDate = d.moveInDate || ''
  createForm.leaseTerm = d.leaseTerm || ''
  createForm.requirements = requirementsInput(d.requirements)
  createForm.description = d.description || ''
  createForm.createSubscription = false
  showCreate.value = true
}

/** 打开需求详情弹层（求租墙卡片点击，房东视角：联系租客） */
async function openDetail(d) {
  detailMode.value = 'wall'
  detailData.value = d
  showDetail.value = true
}

/** 打开我的需求详情弹层（发布者视角：编辑/成交/重匹配/撤回） */
function openMyDetail(d) {
  detailMode.value = 'mine'
  detailData.value = d
  showDetail.value = true
}

/** 钉钉内联系租客：调后端拿发布者 staffId → 唤起钉钉单聊（对齐房源侧联系房东） */
async function contactRenter(d) {
  if (contacting.value) return
  contacting.value = true
  try {
    const renter = await demandApi.contact(d.id)
    await openSingleChat(renter.staffId)
  } catch (e) {
    showToast(e.message || '暂时无法联系，请稍后再试')
  } finally {
    contacting.value = false
  }
}

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
  if (rematching.value) return
  rematching.value = true
  try {
    const res = await demandApi.rematch(d.id)
    d.matchStatus = res?.matches?.length ? 1 : 0
    immediateMatches.value = res?.matches || []
    immediateDegraded.value = !!res?.degraded
    showDetail.value = false
    showMatchResult.value = true
  } catch (e) {
    showToast(e.message || '匹配失败')
  } finally {
    rematching.value = false
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
    const payload = {
      region: createForm.region.trim(),
      houseType: createForm.houseType.trim(),
      budget,
      moveInDate: createForm.moveInDate || null,
      leaseTerm: createForm.leaseTerm.trim(),
      requirements,
      description: createForm.description.trim()
    }
    if (editingId.value) {
      // 编辑：保存并重新匹配（后端返回更新后的需求）
      await demandApi.update(editingId.value, payload)
      showSuccessToast('已保存修改')
      showCreate.value = false
      editingId.value = null
      loadMine()
      loadWall()
    } else {
      // 新建：发布 + 即时匹配
      const res = await demandApi.create({ ...payload, createSubscription: createForm.createSubscription })
      immediateMatches.value = res?.matches || []
      immediateDegraded.value = !!res?.degraded
      showMatchResult.value = true
      showCreate.value = false
      Object.keys(createForm).forEach((k) => (createForm[k] = ''))
      loadWall()
      loadMine()
    }
  } catch (e) {
    showToast(e.message || '操作失败')
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
    showDetail.value = false
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
    showDetail.value = false
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
@media (min-width: 768px) {
  .demand-page {
    padding-bottom: 0;
  }
}
/* prompt-input 可点击（公共样式见 styles/components.css） */
.prompt-input {
  cursor: pointer;
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
/* 匹配卡片 hover（公共基础见 styles/components.css） */
.match-card:hover {
  border-color: rgba(255, 106, 0, 0.35);
  box-shadow: 0 4px 16px rgba(255, 106, 0, 0.12);
}
/* 需求详情弹层 */
.dd-row {
  display: flex;
  gap: 10px;
  padding: 6px 0;
  font-size: 0.8rem;
  line-height: 1.5;
  color: var(--fg2);
}
.dd-row .dd-label {
  flex-shrink: 0;
  width: 44px;
  color: var(--fg3);
  font-weight: 500;
}
.dd-row.dd-desc span {
  flex: 1;
}
.dd-reqs {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin: 6px 0 14px;
}
/* 我的需求管理操作：主按钮下方一行次按钮 */
.dd-ops {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}
/* 操作按钮（卡片 + 详情弹层共用） */
.op-btn {
  flex: 1;
  font-size: 0.74rem;
  padding: 9px 0;
  border-radius: 10px;
  border: none;
  background: var(--primary);
  color: #fff;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}
.op-btn.ghost {
  background: var(--card);
  color: var(--fg2);
  border: 1px solid var(--border);
}
.op-btn.ghost.danger {
  color: var(--destructive);
  border-color: var(--destructive-border);
}
.op-btn:disabled {
  opacity: 0.55;
  cursor: default;
}
.op-btn:active {
  opacity: 0.85;
}
/* 我的需求卡片操作行：单个高频按钮，右对齐窄宽，不占满整行 */
.mc-ops {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
}
.mc-ops .op-btn {
  flex: none;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 7px 14px;
  font-size: 0.76rem;
}
.mc-ops .op-btn i {
  font-size: 0.9rem;
}
.d-cards {
  display: flex;
  flex-direction: column;
  gap: 10px;
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
.create-panel {
  padding: 20px 16px 28px;
}
.create-panel h4 {
  font-size: 0.95rem;
  font-weight: 600;
  margin-bottom: 14px;
}
/* 创建订阅勾选 */
.sub-check {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 12px;
  margin-bottom: 12px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--card);
  cursor: pointer;
}
.sub-check .ck {
  width: 20px;
  height: 20px;
  border-radius: 6px;
  border: 1px solid var(--border);
  background: var(--card);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 1px;
  transition: all 0.15s ease;
}
.sub-check .ck.on {
  background: var(--primary);
  border-color: var(--primary);
}
.sub-check .ck i {
  color: #fff;
  font-size: 0.8rem;
  font-weight: 700;
}
.sub-check .ck-tx {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.sub-check .ck-tx b {
  font-size: 0.8rem;
  color: var(--fg);
}
.sub-check .ck-tx small {
  font-size: 0.66rem;
  color: var(--fg3);
  line-height: 1.4;
}
.seg-item {
  font-size: 0.74rem;
  padding: 6px 14px;
  border-radius: 8px;
  border: 1px solid rgba(255, 106, 0, 0.35);
  color: var(--fg2);
  background: var(--card);
}
/* PC 端主按钮限宽居中（公共样式见 styles/components.css） */
@media (min-width: 768px) {
  .btn-primary {
    display: block;
    width: auto;
    max-width: 320px;
    margin: 6px auto 0;
  }
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
@media (min-width: 768px) {
  .mp-done {
    display: block;
    max-width: 320px;
    margin: 10px auto 0;
  }
}
</style>
