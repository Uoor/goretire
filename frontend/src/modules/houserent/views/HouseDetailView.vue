<template>
  <div class="detail-page">
    <TopBar back :title="house?.community || '房源详情'" />

    <div v-if="house">
      <div class="detail-hero">
        <div class="hero-img" :class="thumbClass">
          <img v-if="images.length" :src="images[imgIndex]" alt="" @click="previewImage" />
          <!-- 多图：左右切换 + 指示点（产品 4.2 图集） -->
          <template v-if="images.length > 1">
            <div class="hero-nav prev" @click="switchImg(-1)"><i class="ph ph-caret-left"></i></div>
            <div class="hero-nav next" @click="switchImg(1)"><i class="ph ph-caret-right"></i></div>
          </template>
          <div class="dots" v-if="images.length > 1">
            <i v-for="(_, i) in images.length" :key="i" :class="{ on: i === imgIndex }"></i>
          </div>
        </div>
      </div>

      <div class="detail-body">
        <div class="d-price-row">
          <span class="d-price num">{{ formatMoney(house.rent) }}</span>
          <small>元/月</small>
          <span class="d-tag tag owner">{{ labelText }}</span>
          <span v-if="house.petOk === 1" class="tag verify">可养宠</span>
          <span v-if="house.rackStatus === RACK_STATUS.RENTED" class="tag status-rented">已租出</span>
          <span v-else-if="house.rackStatus === RACK_STATUS.OFF" class="tag status-off">已下架</span>
        </div>
        <div class="d-title">{{ house.community }} · {{ house.houseType }} {{ house.area }}㎡</div>
        <div class="d-specs">
          <div class="d-spec"><div class="v num">{{ house.area }}㎡</div><div class="k">面积</div></div>
          <div class="d-spec"><div class="v">{{ house.depositPay }}</div><div class="k">押付</div></div>
          <div class="d-spec"><div class="v">{{ house.commute || '—' }}</div><div class="k">通勤</div></div>
        </div>

        <!-- 水电网物业（产品 4.2 价格明细） -->
        <div v-if="house.utilities" class="d-sec d-utils">
          <h5>水电网物业</h5>
          <div class="d-desc">{{ house.utilities }}</div>
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
            <span class="verify-chip"><i class="ph ph-seal-check"></i> 已核实</span>
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
        <!-- 在租中：可联系房东；已租出/已下架：禁用并提示（房源已不可租） -->
        <button v-if="house.rackStatus === RACK_STATUS.RENTING" class="btn-primary" @click="contact">钉钉内联系房东</button>
        <button v-else class="btn-primary btn-disabled" @click="showToast('该房源已' + (house.rackStatus === RACK_STATUS.RENTED ? '租出' : '下架'))">
          已{{ house.rackStatus === RACK_STATUS.RENTED ? '租出' : '下架' }}，暂不可联系
        </button>
      </div>
    </div>

    <van-skeleton v-else-if="loading" title :row="6" class="detail-sk" />
    <EmptyState v-else icon="ph ph-warning-circle" text="房源不存在或已下架" />

    <!-- 举报弹层 -->
    <van-popup v-model:show="showReport" position="bottom" round :safe-area-inset-bottom="true">
      <div class="report-panel">
        <div class="dp-head">
          <h4>举报房源</h4>
          <i class="ph ph-x" @click="showReport = false"></i>
        </div>
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
import { showToast, showSuccessToast, showImagePreview } from 'vant'
import TopBar from '@/modules/houserent/components/TopBar.vue'
import EmptyState from '@/modules/houserent/components/EmptyState.vue'
import { houseApi } from '@/modules/houserent/api'
import { formatMoney, parseImages } from '@/utils/format'
import { openSingleChat } from '@/utils/dd'
import { HOUSE_LABEL_TEXT, RACK_STATUS } from '@/constants/status'

const route = useRoute()
const router = useRouter()
const house = ref(null)
const loading = ref(true)
const showReport = ref(false)
const reportReason = ref('')

const images = computed(() => parseImages(house.value?.images))
const cover = computed(() => images.value[0] || '')
const imgIndex = ref(0)
const thumbClass = computed(() => ['thumb-a', 'thumb-b', 'thumb-c'][Number(route.params.id) % 3])
const labelText = computed(() => HOUSE_LABEL_TEXT[house.value?.label] || '')

/** 图集切换（产品 4.2：最多 9 张，首图为封面） */
function switchImg(dir) {
  const len = images.value.length
  if (len <= 1) return
  imgIndex.value = (imgIndex.value + dir + len) % len
}

/** 点击图片全屏预览 */
function previewImage() {
  showImagePreview({
    images: images.value,
    startPosition: imgIndex.value,
    closeable: true
  })
}

async function load() {
  loading.value = true
  imgIndex.value = 0
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
    const owner = await houseApi.contact(route.params.id)
    // 唤起钉钉单聊（产品 4.1：不留手机号，钉钉内直接开聊）
    await openSingleChat(owner.staffId)
    showToast(`已打开与${owner.nickname || '房东'}的钉钉会话`)
  } catch (e) {
    showToast(e.message || '无法打开钉钉会话，请稍后再试')
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
@media (min-width: 768px) {
  .detail-page {
    padding-bottom: 0;
  }
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
/* 图集左右切换（多图时） */
.hero-nav {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.35);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 2;
}
.hero-nav.prev {
  left: 12px;
}
.hero-nav.next {
  right: 12px;
}
.hero-nav:active {
  background: rgba(0, 0, 0, 0.55);
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
  background: var(--card);
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
.tag.status-rented {
  /* 已租出 = 成交（正向），用成功绿；已下架才是终止灰 */
  background: var(--accent-soft);
  color: var(--accent);
}
.tag.status-off {
  background: var(--bg);
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
  display: inline-flex;
  align-items: center;
  gap: 3px;
}
.verify-chip i {
  font-size: 0.72rem;
}
.guide-arr {
  margin-left: auto;
  color: var(--warning);
  font-size: 1rem;
  flex-shrink: 0;
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
  z-index: 50;
}
@media (min-width: 768px) {
  .action-bar {
    max-width: 1200px;
    margin: 0 auto;
    justify-content: center;
  }
}
.action-bar .btn-primary {
  flex: 1;
  width: auto;          /* 覆盖 .btn-primary 的 width: 100% */
  max-width: 320px;     /* PC 端上限 */
  margin: 0 auto;       /* 居中 */
}
/* 已租出/已下架的禁用态按钮：灰底，无立体边，不可点 */
.action-bar .btn-disabled {
  background: var(--bg);
  color: var(--fg2);
  box-shadow: none;
  font-weight: 500;
}
.detail-sk {
  margin: 16px;
  border-radius: 14px;
}
.report-panel {
  padding: 20px 16px 24px;
}
.report-btn {
  margin-top: 14px;
}
</style>
