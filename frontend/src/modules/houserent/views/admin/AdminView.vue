<template>
  <div class="admin-page">
    <TopBar back title="管理后台" />

    <div class="seg-tabs">
      <div class="seg-tab" :class="{ on: tab === 'audit' }" @click="switchTab('audit')">
        待审核<template v-if="pending.length"> ({{ pending.length }})</template>
      </div>
      <div class="seg-tab" :class="{ on: tab === 'reports' }" @click="switchTab('reports')">
        举报<template v-if="reportPending"> ({{ reportPending }})</template>
      </div>
      <div class="seg-tab" :class="{ on: tab === 'stats' }" @click="switchTab('stats')">看板</div>
    </div>

    <!-- 待审核 -->
    <div v-if="tab === 'audit'" class="panel">
      <div v-for="h in pending" :key="h.id" class="audit-card">
        <div class="ac-top">
          <b>{{ h.community }} · {{ h.houseType }} {{ h.area }}㎡</b>
          <span class="ac-price num">{{ h.rent }} 元/月</span>
        </div>
        <div class="ac-meta">{{ h.region }} · {{ h.depositPay }} · 房号 {{ h.roomNo || '—' }} · 发布人#{{ h.publisherId }}</div>
        <div v-if="h.description" class="ac-desc">{{ h.description }}</div>
        <div class="ac-ops">
          <button class="op pass" @click="audit(h, true, '')">通过</button>
          <button class="op reject" @click="openReject(h)">驳回</button>
        </div>
      </div>
      <EmptyState v-if="pending.length === 0" text="没有待审核房源" />
    </div>

    <!-- 举报 -->
    <div v-else-if="tab === 'reports'" class="panel">
      <div v-for="r in reports" :key="r.id" class="audit-card">
        <div class="ac-top"><b>举报 #{{ r.id }} · {{ targetText(r.targetType) }}#{{ r.targetId }}</b></div>
        <div class="ac-desc">{{ r.reason }}</div>
        <div class="ac-meta">举报人 #{{ r.reporterId }} · {{ r.status === 1 ? '已处理' : '待处理' }}</div>
        <div v-if="r.status === 0" class="ac-ops">
          <button class="op pass" @click="openHandleReport(r)">处理</button>
        </div>
        <div v-if="r.result" class="ac-result">处理结果：{{ r.result }}</div>
      </div>
      <EmptyState v-if="reports.length === 0" text="暂无举报" />
    </div>

    <!-- 看板 -->
    <div v-else class="panel">
      <div class="stats-grid" v-if="stats">
        <div class="stat"><div class="v num">{{ stats.total }}</div><div class="k">房源总数</div></div>
        <div class="stat"><div class="v num">{{ stats.pending }}</div><div class="k">待审核</div></div>
        <div class="stat"><div class="v num">{{ stats.online }}</div><div class="k">已上架</div></div>
        <div class="stat"><div class="v num">{{ stats.rented }}</div><div class="k">已租出</div></div>
        <div class="stat"><div class="v num">{{ stats.todayNew }}</div><div class="k">今日新增</div></div>
      </div>
    </div>

    <!-- 驳回原因 -->
    <van-popup v-model:show="showReject" position="bottom" round>
      <div class="mini-panel">
        <h4>驳回原因</h4>
        <van-field v-model="rejectReason" rows="2" autosize type="textarea" placeholder="必填，将展示给发布人" />
        <button class="btn-primary" :disabled="!rejectReason.trim()" @click="doReject">确认驳回</button>
      </div>
    </van-popup>

    <!-- 举报处理 -->
    <van-popup v-model:show="showHandle" position="bottom" round>
      <div class="mini-panel">
        <h4>处理举报</h4>
        <van-field v-model="handleResult" rows="2" autosize type="textarea" placeholder="处理结果（必填，如：已核实，房源下架）" />
        <button class="btn-primary" :disabled="!handleResult.trim()" @click="doHandleReport">确认处理</button>
      </div>
    </van-popup>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { showToast, showSuccessToast } from 'vant'
import TopBar from '@/modules/houserent/components/TopBar.vue'
import EmptyState from '@/modules/houserent/components/EmptyState.vue'
import { adminApi } from '@/modules/houserent/api'

const tab = ref('audit')
const pending = ref([])
const reports = ref([])
const reportPending = ref(0)
const stats = ref(null)

const showReject = ref(false)
const rejectTarget = ref(null)
const rejectReason = ref('')
const showHandle = ref(false)
const handleTarget = ref(null)
const handleResult = ref('')

function targetText(t) {
  return t === 1 ? '房源' : '用户'
}

async function loadAudit() {
  try {
    pending.value = await adminApi.auditPending()
  } catch (e) {
    showToast(e.message || '加载失败')
  }
}
async function loadReports() {
  try {
    reports.value = await adminApi.reports(0)
    reportPending.value = reports.value.length
  } catch (e) {
    showToast(e.message || '加载失败')
  }
}
async function loadStats() {
  try {
    stats.value = await adminApi.stats()
  } catch (e) {
    showToast(e.message || '加载失败')
  }
}

function switchTab(t) {
  tab.value = t
  if (t === 'audit') loadAudit()
  if (t === 'reports') loadReports()
  if (t === 'stats') loadStats()
}

async function audit(h, pass, reason) {
  try {
    await adminApi.audit(h.id, pass, reason)
    showSuccessToast(pass ? '已通过' : '已驳回')
    loadAudit()
    loadStats()
  } catch (e) {
    showToast(e.message || '操作失败')
  }
}

function openReject(h) {
  rejectTarget.value = h
  rejectReason.value = ''
  showReject.value = true
}
async function doReject() {
  await audit(rejectTarget.value, false, rejectReason.value.trim())
  showReject.value = false
}

function openHandleReport(r) {
  handleTarget.value = r
  handleResult.value = ''
  showHandle.value = true
}
async function doHandleReport() {
  try {
    await adminApi.handleReport(handleTarget.value.id, handleResult.value.trim())
    showSuccessToast('已处理')
    showHandle.value = false
    loadReports()
  } catch (e) {
    showToast(e.message || '操作失败')
  }
}

onMounted(loadAudit)
</script>

<style scoped>
.admin-page {
  padding-bottom: 40px;
  min-height: 100vh;
}
.seg-tabs {
  display: flex;
  background: var(--card);
  border-bottom: 1px solid var(--border);
  position: sticky;
  top: 0;
  z-index: 10;
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
.panel {
  padding: 12px 16px;
}
.audit-card {
  background: var(--card);
  border-radius: 14px;
  padding: 14px;
  border: 1px solid var(--border);
  margin-bottom: 10px;
}
.ac-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.ac-top b {
  font-size: 0.86rem;
}
.ac-price {
  color: var(--primary-deep);
  font-weight: 700;
  font-size: 0.8rem;
  white-space: nowrap;
}
.ac-meta {
  font-size: 0.68rem;
  color: var(--fg3);
  margin-top: 6px;
}
.ac-desc {
  font-size: 0.76rem;
  color: var(--fg2);
  margin-top: 6px;
  line-height: 1.5;
}
.ac-result {
  font-size: 0.7rem;
  color: var(--accent);
  margin-top: 6px;
  background: var(--accent-soft);
  border-radius: 6px;
  padding: 6px 8px;
}
.ac-ops {
  display: flex;
  gap: 10px;
  margin-top: 10px;
}
.op {
  flex: 1;
  padding: 9px;
  border-radius: 10px;
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
  border: none;
}
.op.pass {
  background: var(--primary);
  color: #fff;
  box-shadow: 0 2px 0 var(--primary-deep);
}
.op.pass:active {
  transform: translateY(1px);
  box-shadow: none;
}
.op.reject {
  background: #fff;
  color: var(--destructive);
  border: 1px solid #fecaca;
}
.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}
.stat {
  background: var(--card);
  border-radius: 14px;
  padding: 18px 14px;
  border: 1px solid var(--border);
  text-align: center;
}
.stat .v {
  font-size: 1.4rem;
  font-weight: 700;
  font-family: var(--num);
  color: var(--primary-deep);
}
.stat .k {
  font-size: 0.68rem;
  color: var(--fg3);
  margin-top: 4px;
}
.mini-panel {
  padding: 20px 16px 28px;
}
.mini-panel h4 {
  font-size: 0.95rem;
  font-weight: 600;
  margin-bottom: 14px;
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
</style>
