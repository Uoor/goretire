<template>
  <div class="me-page">
    <div class="me-hero">
      <div class="avatar num">{{ (store.nickname || '校')[0] }}</div>
      <div>
        <div class="nm">{{ store.nickname || '校友' }}</div>
        <div class="sub">已通过校友身份认证 · 可信租房网络</div>
      </div>
    </div>

    <div class="me-grid">
      <div class="me-cell"><div class="v num">{{ myHouses.length }}</div><div class="k">我的发布</div></div>
      <div class="me-cell"><div class="v num">{{ activeCount }}</div><div class="k">在租/审核中</div></div>
      <div class="me-cell"><div class="v num">{{ store.isAdmin ? '管理员' : '校友' }}</div><div class="k">角色</div></div>
    </div>

    <div class="me-list">
      <div class="me-row" @click="router.push({ name: 'admin' })" v-if="store.isAdmin">
        <i class="ph ph-shield-check"></i><span>管理后台</span>
        <span class="badge">运营</span>
        <i class="ph ph-caret-right arr"></i>
      </div>
      <div class="me-row" @click="router.push({ name: 'subscribe' })">
        <i class="ph ph-bell"></i><span>我的订阅</span>
        <i class="ph ph-caret-right arr"></i>
      </div>
      <div class="me-row" @click="router.push({ name: 'demand' })">
        <i class="ph ph-magnifying-glass"></i><span>我的求租需求</span>
        <i class="ph ph-caret-right arr"></i>
      </div>
      <div class="me-row" @click="router.push({ name: 'guide' })">
        <i class="ph ph-book-open-text"></i><span>避坑指南</span>
        <i class="ph ph-caret-right arr"></i>
      </div>
    </div>

    <div class="me-list me-houses">
      <div class="list-title">我的发布</div>
      <div v-if="myHouses.length === 0" class="me-none">还没有发布过房源</div>
      <div v-for="h in myHouses" :key="h.id" class="my-house" @click="openMyHouse(h)">
        <div class="mh-main">
          <div class="mh-title">{{ h.community }} · {{ h.houseType }} {{ h.area }}㎡</div>
          <div class="mh-meta">
            <span class="st-tag" :class="statusClass(h)">{{ statusText(h) }}</span>
            <span v-if="h.auditReason" class="reason">驳回：{{ h.auditReason }}</span>
          </div>
        </div>
        <div class="mh-ops">
          <button v-if="canOffRack(h)" class="op-btn" @click.stop="offRack(h)">标记已租出</button>
          <button v-else-if="h.auditStatus === AUDIT_STATUS.ONLINE && h.rackStatus !== RACK_STATUS.RENTING" class="op-btn ghost-op" @click.stop="relist(h)">重新出租</button>
          <button v-if="h.auditStatus === AUDIT_STATUS.REJECTED" class="op-btn ghost-op" @click.stop="editRejected(h)">修改重新提交</button>
          <button v-if="canDelete(h)" class="op-btn del-op" @click.stop="removeHouse(h)">删除</button>
        </div>
      </div>
    </div>

    <!-- 轻问句弹层 -->
    <van-popup v-model:show="showFeedback" position="bottom" round>
      <div class="feedback-panel">
        <h4>房子租出去了？</h4>
        <p class="fb-sub">
          标记后房源将从广场下架。<br />
          顺便告诉我们：这房子是租给通过「校友直租」认识的人吗？<br />
          每周「安居故事」会统计有多少校友通过安居找到新家。
        </p>
        <div class="fb-btns">
          <button class="btn-ghost" @click="feedback(0)">跳过，不回答</button>
          <button class="btn-ghost" @click="feedback(2)">不是（其他渠道）</button>
          <button class="btn-primary fb-main" @click="feedback(1)">是的，通过安居找到的</button>
        </div>
      </div>
    </van-popup>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog, showToast, showSuccessToast } from 'vant'
import { useUserStore } from '@/store/user'
import { houseApi } from '@/modules/houserent/api'
import { AUDIT_STATUS, RACK_STATUS, getHouseStatusText, getHouseStatusClass } from '@/constants/status'

const router = useRouter()
const store = useUserStore()
const myHouses = ref([])
const showFeedback = ref(false)
const pendingOffRack = ref(null)

const activeCount = computed(() => myHouses.value.filter((h) => h.auditStatus === AUDIT_STATUS.ONLINE && h.rackStatus === RACK_STATUS.RENTING).length)

function statusText(h) {
  return getHouseStatusText(h.auditStatus, h.rackStatus)
}
function statusClass(h) {
  return getHouseStatusClass(h.auditStatus, h.rackStatus)
}
function canOffRack(h) {
  return h.auditStatus === AUDIT_STATUS.ONLINE && h.rackStatus === RACK_STATUS.RENTING
}

/** 点击我的房源：仅已上架可进详情；待审核/驳回给状态提示（详情接口对未上架返回 404） */
function openMyHouse(h) {
  if (h.auditStatus !== AUDIT_STATUS.ONLINE) {
    showToast(h.auditStatus === AUDIT_STATUS.PENDING ? '该房源待审核，上架后可查看' : `已驳回：${h.auditReason || '未通过审核'}`)
    return
  }
  router.push({ name: 'house-detail', params: { id: h.id } })
}

async function load() {
  try {
    myHouses.value = await houseApi.mine()
  } catch (e) {
    showToast(e.message || '加载失败')
  }
}

function offRack(h) {
  pendingOffRack.value = h
  showFeedback.value = true
}

/** 可删除：任意状态均可直接删除（房东完全处置权，删除前有二次确认） */
function canDelete() {
  return true
}

/** 被驳回 → 修改重新提交（跳发布页编辑模式，改完重新送审） */
function editRejected(h) {
  router.push({ name: 'publish', query: { edit: h.id } })
}

/** 重新出租：已租出/已下架 → 在租中（状态反转，二次确认） */
async function relist(h) {
  try {
    await showConfirmDialog({
      title: '重新出租',
      message: `确认将「${h.community}」重新上架出租吗？`,
      confirmButtonText: '确认重新出租',
      confirmButtonColor: '#FF6A00'
    })
  } catch {
    return // 用户取消
  }
  try {
    await houseApi.relist(h.id)
    showSuccessToast('已重新出租，房源重新上架')
    load()
  } catch (e) {
    showToast(e.message || '操作失败')
  }
}

/** 删除房源（二次确认；任意状态可删，在租中删除会立即从列表消失） */
async function removeHouse(h) {
  try {
    await showConfirmDialog({
      title: '删除房源',
      message: `删除后不可恢复；若在出租中将立即从房源列表消失。确认删除「${h.community}」吗？`,
      confirmButtonText: '确认删除',
      confirmButtonColor: '#DC2626'
    })
  } catch {
    return
  }
  try {
    await houseApi.remove(h.id)
    showSuccessToast('已删除')
    load()
  } catch (e) {
    showToast(e.message || '删除失败')
  }
}

async function feedback(answer) {
  const h = pendingOffRack.value
  showFeedback.value = false
  if (!h) return
  try {
    await houseApi.offRack(h.id)
    await houseApi.feedback(h.id, answer)
    showSuccessToast('已标记租出并下架')
    load()
  } catch (e) {
    showToast(e.message || '操作失败')
  }
}

onMounted(load)
</script>

<style scoped>
.me-page {
  padding-bottom: 76px;
}
@media (min-width: 768px) {
  .me-page {
    padding-bottom: 0;
  }
}
.me-hero {
  background: linear-gradient(135deg, var(--primary-soft), #ffe3cc);
  padding: 24px 16px 20px;
  display: flex;
  align-items: center;
  gap: 14px;
}
.avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ffb98a, #ff8a3d);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 700;
  font-size: 1.2rem;
  flex-shrink: 0;
}
.me-hero .nm {
  font-size: 1rem;
  font-weight: 700;
}
.me-hero .sub {
  font-size: 0.72rem;
  color: var(--fg2);
  margin-top: 2px;
}
.me-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  padding: 14px 16px;
  background: var(--card);
  border-bottom: 1px solid var(--border);
}
.me-cell {
  text-align: center;
  padding: 8px 0;
  border-radius: 10px;
  cursor: pointer;
}
.me-cell .v {
  font-size: 1.05rem;
  font-weight: 700;
  font-family: var(--num);
  color: var(--fg);
}
.me-cell .k {
  font-size: 0.66rem;
  color: var(--fg3);
  margin-top: 2px;
}
.me-list {
  background: var(--card);
  padding: 4px 16px;
  border-bottom: 8px solid var(--bg);
}
.me-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 0;
  border-bottom: 1px solid var(--border);
  font-size: 0.84rem;
  cursor: pointer;
}
.me-row:last-child {
  border-bottom: none;
}
.me-row i {
  font-size: 1.15rem;
  color: var(--fg2);
  width: 24px;
}
.me-row .arr {
  margin-left: auto;
  color: var(--fg3);
  font-size: 0.85rem;
}
.me-row .badge {
  margin-left: auto;
  font-size: 0.66rem;
  color: var(--primary-deep);
  background: var(--primary-soft);
  padding: 2px 8px;
  border-radius: 999px;
}
.me-houses {
  border-bottom: none;
  padding-bottom: 16px;
}
.list-title {
  font-size: 0.82rem;
  font-weight: 600;
  padding: 12px 0 8px;
}
.me-none {
  font-size: 0.8rem;
  color: var(--fg3);
  padding: 16px 0;
}
.my-house {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 0;
  border-bottom: 1px solid var(--border);
  cursor: pointer;
}
.my-house:last-child {
  border-bottom: none;
}
.mh-main {
  flex: 1;
  min-width: 0;
}
.mh-title {
  font-size: 0.84rem;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.mh-meta {
  margin-top: 4px;
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.st-tag {
  font-size: 0.62rem;
  padding: 2px 8px;
  border-radius: 999px;
  font-weight: 600;
}
.st-online {
  background: var(--accent-soft);
  color: var(--accent);
}
.st-pending {
  background: var(--primary-soft);
  color: var(--primary-deep);
}
.st-rejected {
  background: var(--destructive-soft);
  color: var(--destructive);
}
.st-rented {
  background: var(--bg);
  color: var(--fg2);
}
.reason {
  font-size: 0.66rem;
  color: var(--destructive);
}
.op-btn {
  font-size: 0.68rem;
  color: var(--primary-deep);
  background: var(--primary-soft);
  border: none;
  padding: 6px 10px;
  border-radius: 8px;
  white-space: nowrap;
}
.op-btn.ghost-op {
  color: var(--fg2);
  background: var(--bg);
}
.op-btn.del-op {
  color: var(--destructive);
  background: var(--destructive-soft);
}
.op-btn:active {
  opacity: 0.8;
}
.feedback-panel {
  padding: 24px 16px 28px;
}
.feedback-panel h4 {
  font-size: 1rem;
  font-weight: 700;
  text-align: center;
}
.fb-sub {
  font-size: 0.72rem;
  color: var(--fg2);
  text-align: center;
  margin: 8px 0 18px;
}
.fb-btns {
  display: flex;
  gap: 10px;
}
.fb-btns .btn-ghost {
  flex: 1;
}
.fb-main {
  flex: 1.4;
}
</style>
