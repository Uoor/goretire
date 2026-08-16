<template>
  <div class="me-page">
    <TopBar />
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
      <div v-for="h in myHouses" :key="h.id" class="my-house" @click="router.push({ name: 'house-detail', params: { id: h.id } })">
        <div class="mh-main">
          <div class="mh-title">{{ h.community }} · {{ h.houseType }} {{ h.area }}㎡</div>
          <div class="mh-meta">
            <span class="st-tag" :class="statusClass(h)">{{ statusText(h) }}</span>
            <span v-if="h.auditReason" class="reason">驳回：{{ h.auditReason }}</span>
          </div>
        </div>
        <div class="mh-ops">
          <button v-if="canOffRack(h)" class="op-btn" @click.stop="offRack(h)">已租出下架</button>
        </div>
      </div>
    </div>

    <!-- 轻问句弹层 -->
    <van-popup v-model:show="showFeedback" position="bottom" round>
      <div class="feedback-panel">
        <h4>恭喜🎉 房子租出去了！</h4>
        <p class="fb-sub">在安居找到新家了吗？（用于每周安居故事统计）</p>
        <div class="fb-btns">
          <button class="btn-ghost" @click="feedback(0)">跳过</button>
          <button class="btn-ghost" @click="feedback(2)">还没找到</button>
          <button class="btn-primary fb-main" @click="feedback(1)">找到新家啦</button>
        </div>
      </div>
    </van-popup>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showSuccessToast } from 'vant'
import TopBar from '@/modules/houserent/components/TopBar.vue'
import { useUserStore } from '@/store/user'
import { houseApi } from '@/modules/houserent/api'

const router = useRouter()
const store = useUserStore()
const myHouses = ref([])
const showFeedback = ref(false)
const pendingOffRack = ref(null)

const activeCount = computed(() => myHouses.value.filter((h) => h.auditStatus === 1 && h.rackStatus === 0).length)

function statusText(h) {
  if (h.rackStatus === 1) return '已租出'
  if (h.rackStatus === 2) return '已下架'
  if (h.auditStatus === 2) return '已驳回'
  if (h.auditStatus === 0) return '待审核'
  return '已上架'
}
function statusClass(h) {
  if (h.auditStatus === 2) return 'st-rejected'
  if (h.auditStatus === 0) return 'st-pending'
  if (h.rackStatus === 1) return 'st-rented'
  return 'st-online'
}
function canOffRack(h) {
  return h.auditStatus === 1 && h.rackStatus === 0
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

async function feedback(answer) {
  const h = pendingOffRack.value
  showFeedback.value = false
  if (!h) return
  try {
    await houseApi.offRack(h.id)
    await houseApi.feedback(h.id, answer)
    showSuccessToast('已下架')
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
  background: #fee2e2;
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
.btn-primary,
.btn-ghost {
  border-radius: 10px;
  padding: 12px;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}
.btn-primary {
  background: var(--primary);
  color: #fff;
  border: none;
  box-shadow: 0 2px 0 var(--primary-deep);
}
.btn-primary:active {
  transform: translateY(1px);
  box-shadow: none;
}
.btn-ghost {
  background: #fff;
  color: var(--fg);
  border: 1px solid var(--border);
}
.btn-ghost:active {
  background: var(--bg);
}
</style>
