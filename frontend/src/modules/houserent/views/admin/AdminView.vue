<template>
  <div class="admin-page">
    <TopBar back title="管理后台" />

    <div class="seg-tabs sticky-shell">
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
        <div class="ac-meta">{{ h.region }} · {{ h.depositPay }}{{ h.leaseTerm ? ' · ' + h.leaseTerm : '' }} · 房号 {{ h.roomNo || '—' }} · 发布人#{{ h.publisherId }}</div>
        <!-- 图片预览（审核必须看图） -->
        <div v-if="hImages(h).length" class="ac-imgs">
          <img v-for="(img, i) in hImages(h).slice(0, 6)" :key="i" :src="img" alt="" @click="preview = img" />
        </div>
        <div class="ac-tags">
          <span class="ac-tag">{{ labelText(h.label) }}</span>
          <span v-if="h.petOk === 1" class="ac-tag">可养宠</span>
          <span v-if="h.commute" class="ac-tag">{{ h.commute }}</span>
          <span class="ac-tag time">{{ timeText(h.createdAt) }}</span>
        </div>
        <div v-if="h.description" class="ac-desc">{{ h.description }}</div>
        <div class="ac-ops">
          <button class="op pass" @click="audit(h, true, '')">通过</button>
          <button class="op reject" @click="openReject(h)">驳回</button>
        </div>
      </div>
      <EmptyState v-if="pending.length === 0" text="没有待审核房源" />
    </div>

    <!-- 举报：状态筛选 + 查看被举报房源 -->
    <div v-else-if="tab === 'reports'" class="panel">
      <div class="rp-filter">
        <span class="rp-tab" :class="{ on: reportFilter === 0 }" @click="switchReport(0)">待处理</span>
        <span class="rp-tab" :class="{ on: reportFilter === 1 }" @click="switchReport(1)">已处理</span>
        <span class="rp-tab" :class="{ on: reportFilter === -1 }" @click="switchReport(-1)">全部</span>
      </div>
      <div v-for="r in reports" :key="r.id" class="audit-card">
        <div class="ac-top">
          <b>{{ targetText(r.targetType) }} #{{ r.targetId }}</b>
          <button class="view-house" @click="viewReportedHouse(r)">查看房源</button>
        </div>
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

    <!-- 审核图大图预览 -->
    <van-popup v-model:show="preview" position="center" round>
      <img v-if="preview" :src="preview" class="preview-img" alt="" />
    </van-popup>

    <!-- 被举报房源信息 -->
    <van-popup v-model:show="showReported" position="bottom" round>
      <div class="mini-panel" v-if="reportedHouse">
        <h4>被举报房源</h4>
        <div class="ac-top">
          <b>{{ reportedHouse.community }} · {{ reportedHouse.houseType }} {{ reportedHouse.area }}㎡</b>
          <span class="ac-price num">{{ reportedHouse.rent }} 元/月</span>
        </div>
        <div class="ac-meta">{{ reportedHouse.region }} · {{ reportedHouse.depositPay }} · 状态：{{ statusText(reportedHouse) }}</div>
        <div v-if="reportedHouse.description" class="ac-desc">{{ reportedHouse.description }}</div>
        <div v-if="reportedHouse.auditStatus === 1 && reportedHouse.rackStatus === 0" class="ac-ops">
          <button class="op reject" @click="quickOffRack(reportedHouse)">下架该房源</button>
        </div>
      </div>
      <div class="mini-panel" v-else>
        <h4>被举报房源</h4>
        <p class="ac-desc">{{ reportedError || '房源不存在或已删除' }}</p>
      </div>
    </van-popup>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { showConfirmDialog, showToast, showSuccessToast } from 'vant'
import TopBar from '@/modules/houserent/components/TopBar.vue'
import EmptyState from '@/modules/houserent/components/EmptyState.vue'
import { adminApi, houseApi } from '@/modules/houserent/api'
import { parseImages, timeAgo } from '@/utils/format'

const tab = ref('audit')
const pending = ref([])
const reports = ref([])
const reportPending = ref(0)
const reportFilter = ref(0)
const stats = ref(null)

const showReject = ref(false)
const rejectTarget = ref(null)
const rejectReason = ref('')
const showHandle = ref(false)
const handleTarget = ref(null)
const handleResult = ref('')
const preview = ref('')
const showReported = ref(false)
const reportedHouse = ref(null)
const reportedError = ref('')

function targetText(t) {
  return t === 1 ? '房源' : '用户'
}

function hImages(h) {
  return parseImages(h.images)
}
function labelText(label) {
  return { 1: '房东直租', 2: '校友转租', 3: '合租拼室友' }[label] || ''
}
function timeText(t) {
  return t ? `${timeAgo(t)} 发布` : ''
}
function statusText(h) {
  if (h.rackStatus === 1) return '已租出'
  if (h.rackStatus === 2) return '已下架'
  if (h.auditStatus === 2) return '已驳回'
  if (h.auditStatus === 0) return '待审核'
  return '在租中'
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
    reports.value = await adminApi.reports(reportFilter.value === -1 ? null : reportFilter.value)
    reportPending.value = reports.value.filter((r) => r.status === 0).length
  } catch (e) {
    showToast(e.message || '加载失败')
  }
}

/** 举报状态筛选切换 */
function switchReport(status) {
  reportFilter.value = status
  loadReports()
}

/** 查看被举报房源（供举报处理参考上下文） */
async function viewReportedHouse(r) {
  if (r.targetType !== 1) {
    showToast('暂仅支持查看房源类举报')
    return
  }
  reportedHouse.value = null
  reportedError.value = ''
  showReported.value = true
  try {
    reportedHouse.value = await houseApi.detail(r.targetId)
  } catch (e) {
    reportedError.value = e.message || '房源不存在或已删除'
  }
}

/** 举报处理时一键下架该房源（常见处置） */
async function quickOffRack(h) {
  try {
    await houseApi.offRack(h.id)
    showSuccessToast('已下架该房源')
    showReported.value = false
  } catch (e) {
    showToast(e.message || '操作失败')
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
  // 状态变更（审核通过 → 上架并推送）：二次确认
  if (pass) {
    try {
      await showConfirmDialog({
        title: '审核通过',
        message: `确认通过「${h.community}」？通过后将立即上架并推送到群与匹配订阅。`,
        confirmButtonText: '确认通过',
        confirmButtonColor: '#FF6A00'
      })
    } catch {
      return // 用户取消
    }
  }
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
.ac-imgs {
  display: flex;
  gap: 6px;
  margin-top: 8px;
  overflow-x: auto;
}
.ac-imgs img {
  width: 72px;
  height: 72px;
  border-radius: 8px;
  object-fit: cover;
  flex-shrink: 0;
  cursor: pointer;
  background: linear-gradient(135deg, #ffd9c2, #ffb98a);
}
.ac-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}
.ac-tag {
  font-size: 0.62rem;
  color: var(--primary-deep);
  background: var(--primary-soft);
  padding: 2px 8px;
  border-radius: 999px;
}
.ac-tag.time {
  color: var(--fg3);
  background: var(--bg);
}
.rp-filter {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
.rp-tab {
  font-size: 0.74rem;
  padding: 5px 14px;
  border-radius: 999px;
  border: 1px solid var(--border);
  color: var(--fg2);
  background: #fff;
  cursor: pointer;
}
.rp-tab.on {
  background: var(--primary-soft);
  border-color: var(--primary);
  color: var(--primary-deep);
  font-weight: 600;
}
.view-house {
  font-size: 0.66rem;
  color: var(--accent);
  background: var(--accent-soft);
  border: none;
  padding: 4px 10px;
  border-radius: 999px;
  cursor: pointer;
}
.preview-img {
  width: 70vw;
  max-width: 360px;
  border-radius: 12px;
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
