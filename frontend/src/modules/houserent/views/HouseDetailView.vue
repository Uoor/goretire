<template>
  <div class="detail-page">
    <TopBar back :title="house?.community || '房源详情'" />

    <div v-if="house">
      <!-- 移动端：单图轮播（产品 4.2 图集，最多 9 张，首图为主图） -->
      <div v-if="!isPc" class="detail-hero">
        <div class="hero-img" :class="thumbClass">
          <img v-if="images.length" :src="images[imgIndex]" alt="" @click="previewImage" />
          <!-- 多图：左右切换 + 指示点 -->
          <template v-if="images.length > 1">
            <div class="hero-nav prev" @click="switchImg(-1)"><i class="ph ph-caret-left"></i></div>
            <div class="hero-nav next" @click="switchImg(1)"><i class="ph ph-caret-right"></i></div>
          </template>
          <div class="dots" v-if="images.length > 1">
            <i v-for="(_, i) in images.length" :key="i" :class="{ on: i === imgIndex }"></i>
          </div>
        </div>
      </div>

      <!-- PC：宫格照片墙，一次看全；点任意一张开灯箱 -->
      <div v-else class="detail-hero detail-hero--wall">
        <div class="photo-wall" :class="`wall-${wall.kind}`">
          <div v-if="!wall.visible" class="hero-img" :class="thumbClass"></div>
          <template v-else>
            <button
              v-for="i in wall.visible"
              :key="i"
              type="button"
              class="wall-tile"
              :aria-label="`查看第 ${i} 张照片，共 ${images.length} 张`"
              @click="openLightbox(i - 1)"
            >
              <img :src="images[i - 1]" alt="" />
              <span v-if="wall.overflow && i === wall.visible" class="wall-more num">+{{ wall.overflow }}</span>
            </button>
          </template>
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
        <div class="d-title">{{ house.title || (house.community + ' · ' + house.houseType + ' ' + house.area + '㎡') }}</div>
        <div class="d-specs">
          <div class="d-spec"><div class="v num">{{ house.area }}㎡</div><div class="k">面积</div></div>
          <div class="d-spec"><div class="v">{{ house.depositPay }}</div><div class="k">押付</div></div>
          <div class="d-spec"><div class="v">{{ house.leaseTerm || '面议' }}</div><div class="k">租期</div></div>
        </div>

        <!-- 通勤为自由文本，不用 label 卡片：独立区块整行展示（长文本自动换行，不受卡片宽度限制） -->
        <div v-if="house.commute" class="d-sec d-commute-sec">
          <h5>通勤</h5>
          <div class="d-commute">
            <span class="c-ico">🚲</span>
            <span class="c-txt">{{ house.commute }}</span>
          </div>
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

    <!-- PC 灯箱：深色遮罩 + contain 看全貌 + 44px 箭头 + 计数 + 键盘 ←/→/Esc -->
    <Teleport to="body">
      <div
        v-if="lightboxOpen"
        ref="lightboxEl"
        class="pc-lightbox"
        role="dialog"
        aria-modal="true"
        tabindex="-1"
        :aria-label="`房源照片，第 ${lightboxIndex + 1} 张，共 ${images.length} 张`"
        @click.self="closeLightbox"
      >
        <button type="button" class="lb-btn lb-close" aria-label="关闭" @click="closeLightbox">
          <i class="ph ph-x"></i>
        </button>
        <button
          v-if="images.length > 1"
          type="button"
          class="lb-btn lb-prev"
          aria-label="上一张"
          @click="stepLightbox(-1)"
        >
          <i class="ph ph-caret-left"></i>
        </button>
        <img class="lb-img" :src="images[lightboxIndex]" alt="" />
        <button
          v-if="images.length > 1"
          type="button"
          class="lb-btn lb-next"
          aria-label="下一张"
          @click="stepLightbox(1)"
        >
          <i class="ph ph-caret-right"></i>
        </button>
        <div class="lb-count num">{{ lightboxIndex + 1 }} / {{ images.length }}</div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast, showSuccessToast, showImagePreview } from 'vant'
import TopBar from '@/modules/houserent/components/TopBar.vue'
import EmptyState from '@/modules/houserent/components/EmptyState.vue'
import { houseApi } from '@/modules/houserent/api'
import { formatMoney, parseImages } from '@/utils/format'
import { openSingleChat } from '@/utils/dd'
import { useIsPc } from '@/composables/useIsPc'
import { photoWallLayout } from '@/modules/houserent/photoWall'
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

/* PC 端：宫格照片墙 + 灯箱；移动端仍走单图轮播 + Vant 预览 */
const isPc = useIsPc()
const wall = computed(() => photoWallLayout(images.value.length))
const lightboxOpen = ref(false)
const lightboxIndex = ref(0)
const lightboxEl = ref(null)
let lastFocused = null

/** 图集切换（产品 4.2：最多 9 张，首图为封面） */
function switchImg(dir) {
  const len = images.value.length
  if (len <= 1) return
  imgIndex.value = (imgIndex.value + dir + len) % len
}

/** 点击图片全屏预览（移动端：Vant 预览支持触摸滑动与双指缩放） */
function previewImage() {
  showImagePreview({
    images: images.value,
    startPosition: imgIndex.value,
    closeable: true
  })
}

// ---- PC 灯箱 ----
// 不用 Vant 预览：它的 Swipe 只绑了 touchstart/touchend，PC 上鼠标拖不动；
// 而 ImagePreview 没有内置箭头、也没暴露 zIndex（叠不了自己的箭头）——
// 等于进入全屏后根本无法翻到下一张。

function openLightbox(index) {
  lightboxIndex.value = index
  lightboxOpen.value = true
}

function closeLightbox() {
  lightboxOpen.value = false
}

/** 循环切换，与移动端轮播的取模一致 */
function stepLightbox(dir) {
  const len = images.value.length
  if (len <= 1) return
  lightboxIndex.value = (lightboxIndex.value + dir + len) % len
}

function onLightboxKeydown(event) {
  if (event.key === 'Escape') closeLightbox()
  else if (event.key === 'ArrowLeft') stepLightbox(-1)
  else if (event.key === 'ArrowRight') stepLightbox(1)
}

// 打开时接管键盘、锁住页面滚动（否则滚轮会把背后的详情页滚走），并把焦点移进灯箱；
// 关闭时把焦点还给触发它的那块瓦片。
watch(lightboxOpen, (open) => {
  if (open) {
    lastFocused = document.activeElement
    window.addEventListener('keydown', onLightboxKeydown)
    document.body.style.overflow = 'hidden'
    nextTick(() => lightboxEl.value?.focus())
  } else {
    window.removeEventListener('keydown', onLightboxKeydown)
    document.body.style.overflow = ''
    if (lastFocused instanceof HTMLElement) lastFocused.focus()
    lastFocused = null
  }
})

// 缩到移动端宽度就收起灯箱，避免与移动端轮播两套结构并存
watch(isPc, (pc) => {
  if (!pc) closeLightbox()
})

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
  /* PC 端仍需给 fixed 操作栏让位，否则底部内容被盖住 */
  .detail-page {
    padding-bottom: 76px;
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
  /* 保留多行输入（textarea）里的换行，长词/长 URL 自动折行 */
  white-space: pre-line;
  overflow-wrap: anywhere;
}
/* 通勤：自由长文本，整行展示，自动换行 */
.d-commute {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  background: var(--bg);
  border-radius: 12px;
  padding: 10px 12px;
}
.d-commute .c-ico {
  font-size: 1rem;
  line-height: 1.6;
  flex-shrink: 0;
}
.d-commute .c-txt {
  font-size: 0.8rem;
  color: var(--fg2);
  line-height: 1.6;
  white-space: pre-line;
  overflow-wrap: anywhere;
  flex: 1;
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

/* ===== PC（≥768px）：宫格照片墙 =====
   移动端那条 210px 高的横条在 1152px 宽下接近 5.5:1，配 object-fit: cover
   会把 4:3 的照片裁掉六成；PC 改为宫格一次看全。 */
@media (min-width: 768px) {
  .detail-hero--wall {
    height: auto;
  }
  .photo-wall {
    display: grid;
    gap: 4px;
    /* 固定宽高比避免图片加载时跳动（CLS），再用 max-height 封顶 */
    aspect-ratio: 16 / 9;
    max-height: 460px;
    overflow: hidden;
    border-radius: var(--radius-lg);
    background: var(--bg);
  }
  .wall-empty,
  .wall-single {
    grid-template-columns: 1fr;
  }
  .wall-two {
    grid-template-columns: repeat(2, 1fr);
  }
  .wall-three {
    grid-template-columns: 2fr 1fr;
    grid-template-rows: repeat(2, 1fr);
  }
  .wall-three .wall-tile:first-child {
    grid-row: span 2;
  }
  .wall-four {
    grid-template-columns: repeat(2, 1fr);
    grid-template-rows: repeat(2, 1fr);
  }
  .wall-mosaic {
    grid-template-columns: 2fr 1fr 1fr;
    grid-template-rows: repeat(2, 1fr);
  }
  .wall-mosaic .wall-tile:first-child {
    grid-row: span 2;
  }
  .wall-tile {
    position: relative;
    padding: 0;
    border: 0;
    background: var(--bg);
    overflow: hidden;
    cursor: pointer;
    transition: filter 0.15s ease;
  }
  .wall-tile:hover {
    filter: brightness(0.94);
  }
  .wall-tile:active {
    filter: brightness(0.88);
  }
  .wall-tile:focus-visible {
    outline: 2px solid var(--primary);
    outline-offset: -2px;
  }
  .wall-tile img {
    display: block;
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
  /* 超过 5 张时，末格盖 +N */
  .wall-more {
    position: absolute;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    background: rgba(0, 0, 0, 0.45);
    color: #fff;
    font-size: 1.5rem;
    font-weight: 600;
  }
}

/* ===== PC 灯箱（只在 PC 打开；Vant 预览在 PC 上翻不了图）===== */
.pc-lightbox {
  position: fixed;
  inset: 0;
  z-index: 3000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.88);
}
.lb-img {
  max-width: calc(100vw - 200px);
  max-height: calc(100vh - 140px);
  object-fit: contain;
  border-radius: var(--radius-md);
}
/* 44px 命中区，符合 DESIGN.md §8「图标钮 ≥40px、触控目标 ≥44px」 */
.lb-btn {
  position: absolute;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border: 0;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.16);
  color: #fff;
  font-size: 1.25rem;
  cursor: pointer;
  transition: background 0.15s ease;
}
.lb-btn:hover {
  background: rgba(255, 255, 255, 0.3);
}
.lb-btn:focus-visible {
  outline: 2px solid var(--primary);
  outline-offset: 2px;
}
.lb-close {
  top: 24px;
  right: 32px;
}
.lb-prev {
  top: 50%;
  left: 32px;
  transform: translateY(-50%);
}
.lb-next {
  top: 50%;
  right: 32px;
  transform: translateY(-50%);
}
.lb-count {
  position: absolute;
  bottom: 28px;
  left: 50%;
  transform: translateX(-50%);
  color: #fff;
  font-size: 0.9rem;
  letter-spacing: 0.05em;
}
</style>
