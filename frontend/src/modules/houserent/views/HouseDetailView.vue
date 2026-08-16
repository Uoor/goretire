<template>
  <div class="detail-page">
    <TopBar back :title="house?.community || '房源详情'" />

    <div v-if="house">
      <div class="detail-hero">
        <div class="hero-img" :class="thumbClass">
          <img v-if="cover" :src="cover" alt="" />
          <div class="dots">
            <i v-for="(_, i) in Math.max(1, images.length)" :key="i" :class="{ on: i === 0 }"></i>
          </div>
        </div>
      </div>

      <div class="detail-body">
        <div class="d-price-row">
          <span class="d-price num">{{ formatMoney(house.rent) }}</span>
          <small>元/月</small>
          <span class="d-tag tag owner">{{ labelText }}</span>
          <span v-if="house.petOk === 1" class="tag verify">可养宠</span>
        </div>
        <div class="d-title">{{ house.community }} · {{ house.houseType }} {{ house.area }}㎡</div>
        <div class="d-specs">
          <div class="d-spec"><div class="v num">{{ house.area }}㎡</div><div class="k">面积</div></div>
          <div class="d-spec"><div class="v">{{ house.depositPay }}</div><div class="k">押付</div></div>
          <div class="d-spec"><div class="v">{{ house.commute || '—' }}</div><div class="k">通勤</div></div>
        </div>

        <div class="d-sec">
          <h5>房源描述</h5>
          <div class="d-desc">{{ house.description || '房东还没有写描述，快联系 TA 问问吧。' }}</div>
        </div>

        <div class="d-sec">
          <h5>房东信息</h5>
          <div class="landlord">
            <div class="avatar num">{{ (house.publisherName || '校')[0] }}</div>
            <div>
              <div class="nm">{{ house.publisherName || '校友' }}</div>
              <div class="sub">已通过校友身份认证</div>
            </div>
            <span class="verify-chip">✅ 已核实</span>
          </div>
        </div>

        <div class="guide-strip" @click="router.push({ name: 'guide' })">
          <i class="ph ph-shield-check"></i>
          <div class="tx">
            <b>避坑提醒</b>：签约前先看《合同模板》与《押金避坑清单》<br />
            <span>所有房源均经管理员核实 · 举报入口在下方</span>
          </div>
          <i class="ph ph-caret-right guide-arr"></i>
        </div>
      </div>

      <div class="action-bar fixed-shell">
        <button class="icon-btn" @click="showReport = true"><i class="ph ph-flag"></i></button>
        <button class="btn-primary" @click="contact">钉钉内联系房东</button>
      </div>
    </div>

    <van-skeleton v-else-if="loading" title :row="6" class="detail-sk" />
    <EmptyState v-else icon="ph ph-warning-circle" text="房源不存在或已下架" />

    <!-- 举报弹层 -->
    <van-popup v-model:show="showReport" position="bottom" round>
      <div class="report-panel">
        <h4>举报房源</h4>
        <van-field
          v-model="reportReason"
          rows="2"
          autosize
          type="textarea"
          maxlength="500"
          show-word-limit
          placeholder="请填写举报原因（如信息不实、疑似中介）"
        />
        <button class="btn-primary report-btn" :disabled="!reportReason.trim()" @click="submitReport">提交举报</button>
      </div>
    </van-popup>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast, showSuccessToast } from 'vant'
import TopBar from '@/modules/houserent/components/TopBar.vue'
import EmptyState from '@/modules/houserent/components/EmptyState.vue'
import { houseApi } from '@/modules/houserent/api'
import { formatMoney, parseImages } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const house = ref(null)
const loading = ref(true)
const showReport = ref(false)
const reportReason = ref('')

const images = computed(() => parseImages(house.value?.images))
const cover = computed(() => images.value[0] || '')
const thumbClass = computed(() => ['thumb-a', 'thumb-b', 'thumb-c'][Number(route.params.id) % 3])
const labelText = computed(() => ({ 1: '房东直租', 2: '校友转租', 3: '合租拼室友' })[house.value?.label] || '')

async function load() {
  loading.value = true
  try {
    house.value = await houseApi.detail(route.params.id)
  } catch (e) {
    showToast(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function contact() {
  try {
    const ownerName = await houseApi.contact(route.params.id)
    showSuccessToast(`已通知${ownerName || '房东'}，请留意钉钉消息回复`)
  } catch (e) {
    showToast(e.message || '通知失败，请稍后再试')
  }
}

async function submitReport() {
  try {
    await houseApi.report(route.params.id, reportReason.value.trim())
    showSuccessToast('举报已提交，管理员将尽快处理')
    showReport.value = false
    reportReason.value = ''
  } catch (e) {
    showToast(e.message || '提交失败')
  }
}

onMounted(load)
</script>

<style scoped>
.detail-page {
  padding-bottom: 76px;
  min-height: 100vh;
}
.detail-hero {
  height: 210px;
  position: relative;
}
.hero-img {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #ffd9c2, #ff9e5e);
  position: relative;
}
.hero-img.thumb-b {
  background: linear-gradient(135deg, #c9e4ff, #8fc1ff);
}
.hero-img.thumb-c {
  background: linear-gradient(135deg, #d8f5d8, #a8e6b8);
}
.hero-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.dots {
  position: absolute;
  bottom: 10px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 5px;
}
.dots i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.5);
}
.dots i.on {
  background: #fff;
}
.detail-body {
  background: var(--card);
  border-radius: 20px 20px 0 0;
  margin-top: -16px;
  position: relative;
  padding: 18px 16px;
}
.d-price-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
}
.d-price {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--primary-deep);
  font-family: var(--num);
}
.d-price-row small {
  font-size: 0.8rem;
  font-weight: 500;
  color: var(--fg2);
}
.d-title {
  font-size: 1rem;
  font-weight: 600;
  margin: 4px 0 8px;
}
.d-specs {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin: 12px 0;
}
.d-spec {
  background: var(--bg);
  border-radius: 10px;
  padding: 10px 6px;
  text-align: center;
}
.d-spec .v {
  font-size: 0.86rem;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.d-spec .k {
  font-size: 0.64rem;
  color: var(--fg3);
  margin-top: 2px;
}
.d-sec {
  margin: 14px 0;
}
.d-sec h5 {
  font-size: 0.82rem;
  font-weight: 600;
  margin-bottom: 8px;
}
.d-desc {
  font-size: 0.8rem;
  color: var(--fg2);
  line-height: 1.6;
}
.landlord {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border: 1px solid var(--border);
  border-radius: 12px;
}
.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ffb98a, #ff8a3d);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 700;
  font-size: 0.85rem;
  flex-shrink: 0;
}
.landlord .nm {
  font-size: 0.82rem;
  font-weight: 600;
}
.landlord .sub {
  font-size: 0.68rem;
  color: var(--fg3);
}
.verify-chip {
  margin-left: auto;
  font-size: 0.64rem;
  color: var(--accent);
  background: var(--accent-soft);
  padding: 3px 8px;
  border-radius: 999px;
  font-weight: 600;
  white-space: nowrap;
}
.guide-strip {
  cursor: pointer;
  background: #fffbe6;
  border: 1px solid #ffe58f;
  border-radius: 12px;
  padding: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 14px;
}
.guide-strip > i {
  color: var(--warning);
  font-size: 1.3rem;
  flex-shrink: 0;
}
.guide-arr {
  margin-left: auto;
  color: #d48806;
  font-size: 1rem;
  flex-shrink: 0;
}
.guide-strip .tx {
  font-size: 0.74rem;
  color: #874d00;
  line-height: 1.5;
}
.guide-strip .tx b {
  color: #d48806;
  color: #d48806;
}
.action-bar {
  background: var(--card);
  border-top: 1px solid var(--border);
  padding: 12px 16px;
  display: flex;
  gap: 10px;
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  max-width: 480px;
  margin: 0 auto;
  z-index: 50;
}
.action-bar .btn-primary {
  flex: 1;
}
.icon-btn {
  width: 46px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: #fff;
  color: var(--fg2);
  font-size: 1.1rem;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}
.icon-btn:active {
  background: var(--bg);
}
.detail-sk {
  margin: 16px;
  border-radius: 14px;
}
.report-panel {
  padding: 20px 16px 24px;
}
.report-panel h4 {
  font-size: 0.95rem;
  font-weight: 600;
  margin-bottom: 14px;
}
.report-btn {
  margin-top: 14px;
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
  transition: all 0.15s;
}
.btn-primary:active {
  transform: translateY(1px);
  box-shadow: 0 0 0 var(--primary-deep);
}
.btn-primary:disabled {
  opacity: 0.5;
  box-shadow: none;
}
</style>
