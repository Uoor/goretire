<template>
  <div class="page-home">
    <div class="searchbar" @click="showSearch = true">
      <div class="search-input" :class="{ active: showSearch }">
        <i class="ph ph-magnifying-glass"></i>
        <span v-if="!showSearch">一句话找房：西溪附近 6000 以内两居</span>
        <input
          v-else
          v-model="query"
          ref="searchRef"
          placeholder="预算 6000 以内，西溪附近，能养猫…"
          @keyup.enter="doSearch"
          @blur="onSearchBlur"
        />
      </div>
      <!-- mousedown.prevent：在 input blur 触发前执行，避免点击按钮时按钮因 blur 消失导致 click 丢失 -->
      <button v-if="showSearch" class="search-go" :disabled="matching" @mousedown.prevent="doSearch">
        {{ matching ? '匹配中…' : '找房' }}
      </button>
    </div>

    <!-- AI 匹配中：解析需求需要几秒，给用户明确反馈 -->
    <div v-if="matching" class="match-loading">
      <van-loading size="16" color="#FF6A00">AI 正在理解你的需求，匹配合适房源…</van-loading>
    </div>

    <!-- 一句话找房结果 -->
    <div v-if="matchResult" class="match-panel">
      <div class="match-head">
        <span>🤖 AI 匹配结果</span>
        <span v-if="matchResult.degraded" class="deg-tag">本地降级</span>
        <i class="ph ph-x" @click="matchResult = null"></i>
      </div>
      <div v-if="matchResult.matches.length === 0" class="match-empty">
        没有找到合适的房源，换个说法试试
        <button class="to-demand" @click="goDemand">把需求挂到求租墙，有新房源提醒我</button>
      </div>
      <div v-for="m in matchResult.matches" :key="m.houseId" class="match-card" @click="openHouse(m.houseId)">
        <!-- 房源摘要（产品 4.4.1：3-5 套 + 匹配理由） -->
        <template v-if="houseOf(m)">
          <div class="mc-head">
            <b>{{ houseOf(m).community }}</b>
            <span class="mc-price num">{{ formatMoney(houseOf(m).rent) }} 元/月</span>
          </div>
          <div class="mc-meta">{{ houseOf(m).houseType }} {{ houseOf(m).area }}㎡ · {{ houseOf(m).region }}</div>
        </template>
        <div class="why">{{ m.reason }}</div>
      </div>
    </div>

    <FilterChips :chips="chips" v-model="filters" />

    <!-- 避坑指南引导条 -->
    <div class="home-guide" @click="router.push({ name: 'guide' })">
      <i class="ph ph-book-open-text"></i>
      <span>签约前必读：合同模板 · 押金清单 · 骗局案例</span>
      <i class="ph ph-caret-right"></i>
    </div>

    <!-- 租金周报入口 -->
    <div class="home-guide report-strip" @click="router.push({ name: 'report' })">
      <i class="ph ph-chart-line-up"></i>
      <span>区域租金行情 · 本周新上架与安居故事</span>
      <i class="ph ph-caret-right"></i>
    </div>

    <div class="feed">
      <HouseCard
        v-for="(h, i) in houses"
        :key="h.id"
        :house="h"
        :index="i"
        @click="openHouse(h.id)"
      />
    </div>

    <!-- 触底加载更多 / 加载中 / 已加载完 -->
    <div v-if="loadingMore" class="load-more"><van-loading size="18">加载中…</van-loading></div>
    <div v-else-if="!loading && houses.length && !hasMore" class="load-more end">— 已加载全部 —</div>

    <div v-if="loading" class="feed">
      <van-skeleton v-for="i in 3" :key="i" title :row="2" class="sk" />
    </div>
    <EmptyState v-else-if="houses.length === 0 && !loading" text="暂无房源，先去发布一套吧" />
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import HouseCard from '@/modules/houserent/components/HouseCard.vue'
import FilterChips from '@/modules/houserent/components/FilterChips.vue'
import EmptyState from '@/modules/houserent/components/EmptyState.vue'
import { houseApi, matchApi } from '@/modules/houserent/api'
import { formatMoney } from '@/utils/format'

const router = useRouter()
const PAGE_SIZE = 10
const houses = ref([])
const loading = ref(true)
const loadingMore = ref(false)
const page = ref(1)
const total = ref(0)
const hasMore = computed(() => houses.value.length < total.value)
const query = ref('')
const showSearch = ref(false)
const searchRef = ref(null)
const matching = ref(false)
const matchResult = ref(null)
// 分组筛选：{ group: selectedKey }，见 FilterChips
const filters = ref({})

// 产品 4.1 筛选：区域/价格区间/户型/标签/可养宠/新上架（通勤为文本描述，暂不筛）
const chips = [
  { group: 'region', key: 'r-xixi', label: '杭州西溪', region: '杭州西溪' },
  { group: 'region', key: 'r-binjiang', label: '杭州滨江', region: '杭州滨江' },
  { group: 'region', key: 'r-xihu', label: '杭州西湖', region: '杭州西湖' },
  { group: 'region', key: 'r-wangjing', label: '北京望京', region: '北京望京' },
  { group: 'region', key: 'r-zhangjiang', label: '上海张江', region: '上海张江' },
  { group: 'price', key: 'p-3000', label: '3000以下', max: 3000 },
  { group: 'price', key: 'p-3000-5000', label: '3000-5000', min: 3000, max: 5000 },
  { group: 'price', key: 'p-5000-8000', label: '5000-8000', min: 5000, max: 8000 },
  { group: 'price', key: 'p-8000', label: '8000以上', min: 8000 },
  { group: 'type', key: 't-zhengzu', label: '整租', type: '整租' },
  { group: 'type', key: 't-hezu', label: '合租', type: '合租' },
  { group: 'type', key: 't-yiju', label: '一居', type: '一居' },
  { group: 'type', key: 't-liangju', label: '两居', type: '两居' },
  { group: 'label', key: 'label1', label: '房东直租', value: 1 },
  { group: 'label', key: 'label2', label: '校友转租', value: 2 },
  { group: 'label', key: 'label3', label: '合租拼室友', value: 3 },
  { group: 'pet', key: 'pet', label: '可养宠' },
  { group: 'new', key: 'new', label: '新上架' }
]

const chipMap = Object.fromEntries(chips.map((c) => [c.key, c]))

/** 组装筛选参数（分页参数由调用方附加） */
function buildParams() {
  const params = {}
  for (const key of Object.values(filters.value)) {
    const chip = chipMap[key]
    if (!chip) continue
    // 严格按分组取值：label 是显示名，筛选值在 value 字段（避免把区域名当 label 传）
    switch (chip.group) {
      case 'region':
        if (chip.region) params.region = chip.region
        break
      case 'price':
        if (chip.min != null) params.minRent = chip.min
        if (chip.max != null) params.maxRent = chip.max
        break
      case 'type':
        if (chip.type) params.houseType = chip.type
        break
      case 'label':
        if (chip.value != null) params.label = chip.value
        break
      case 'pet':
        params.petOk = 1
        break
      case 'new':
        params.newOnly = true
        break
    }
  }
  return params
}

/** 加载第一页（筛选变化/首次进入时调用，重置分页） */
async function load() {
  loading.value = true
  try {
    const data = await houseApi.list({ ...buildParams(), page: 1, size: PAGE_SIZE })
    houses.value = data.list
    total.value = data.total
    page.value = 1
  } catch (e) {
    showToast(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

/** 触底加载下一页（追加） */
async function loadMore() {
  if (loading.value || loadingMore.value || !hasMore.value) return
  loadingMore.value = true
  try {
    const data = await houseApi.list({ ...buildParams(), page: page.value + 1, size: PAGE_SIZE })
    houses.value = houses.value.concat(data.list)
    total.value = data.total
    page.value += 1
  } catch (e) {
    showToast(e.message || '加载失败')
  } finally {
    loadingMore.value = false
  }
}

/** 滚动触底自动加载（距底 120px 触发） */
function onScroll() {
  const el = document.documentElement
  if (el.scrollTop + el.clientHeight >= el.scrollHeight - 120) {
    loadMore()
  }
}

/** 匹配结果里取房源摘要：优先用已加载列表里的完整数据；
 *  不在已加载分页内时回退用 HouseMatch 自带摘要字段（community/houseType/area/rent/region） */
function houseOf(match) {
  return houses.value.find((h) => h.id === match.houseId) || match
}

async function doSearch() {
  const text = query.value.trim()
  if (!text || matching.value) return
  showSearch.value = false
  matching.value = true
  try {
    matchResult.value = await matchApi.search(text)
  } catch (e) {
    showToast(e.message || '找房失败')
  } finally {
    matching.value = false
  }
}

/** 点击页面其他区域时收起搜索框（延迟避免与按钮 mousedown 冲突） */
function onSearchBlur() {
  setTimeout(() => {
    showSearch.value = false
  }, 120)
}

function openHouse(id) {
  router.push({ name: 'house-detail', params: { id } })
}

/** 产品 4.4.2：即时找房没结果时，一键把需求挂上求租墙 */
function goDemand() {
  matchResult.value = null
  router.push({ name: 'demand' })
}

watch(filters, load, { deep: true })

onMounted(async () => {
  window.addEventListener('scroll', onScroll, { passive: true })
  await load()
  if (showSearch.value) nextTick(() => searchRef.value?.focus())
})

onUnmounted(() => {
  window.removeEventListener('scroll', onScroll)
})
</script>

<style scoped>
.page-home {
  padding-bottom: 76px;
}
@media (min-width: 768px) {
  .page-home {
    padding-bottom: 0;
  }
}
.searchbar {
  background: var(--card);
  padding: 10px 16px;
  display: flex;
  gap: 8px;
  align-items: center;
}
.search-input {
  background: var(--input-fill);
  border-radius: var(--radius-md);
  padding: 10px 14px;
  font-size: 0.85rem;
  color: var(--fg3);
  display: flex;
  align-items: center;
  gap: 8px;
  border: 1px solid transparent;
  flex: 1;
  transition: all 0.15s ease;
}
.search-input.active {
  border-color: var(--primary);
  color: var(--fg);
  background: var(--card);
}
.search-input input {
  border: none;
  outline: none;
  flex: 1;
  font-size: 0.85rem;
  font-family: inherit;
  color: var(--fg);
  background: transparent;
  min-width: 0;
}
.search-go {
  background: var(--primary);
  color: #fff;
  border: none;
  border-radius: 10px;
  padding: 10px 16px;
  font-size: 0.85rem;
  font-weight: 600;
  box-shadow: 0 2px 0 var(--primary-deep);
}
.search-go:active {
  transform: translateY(1px);
  box-shadow: none;
}
.search-go:disabled {
  opacity: 0.6;
}
.match-loading {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 14px 16px;
  background: var(--primary-soft);
  border-bottom: 1px solid var(--border);
  font-size: 0.75rem;
  color: var(--primary-deep);
}
.feed {
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
/* PC 端：房源卡片网格 */
@media (min-width: 768px) {
  .feed {
    padding: 16px 0;
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
  }
}
@media (min-width: 1024px) {
  .feed {
    grid-template-columns: repeat(3, 1fr);
  }
}
.load-more {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 14px 0 4px;
  font-size: 0.72rem;
  color: var(--fg3);
}
.load-more.end {
  letter-spacing: 0.05em;
}
.match-panel {
  background: var(--card);
  padding: 10px 16px;
  border-bottom: 1px solid var(--border);
}
.match-head {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.8rem;
  font-weight: 600;
  padding: 4px 0 8px;
}
.match-head i {
  margin-left: auto;
  color: var(--fg3);
  cursor: pointer;
}
.deg-tag {
  font-size: 0.62rem;
  color: var(--primary-deep);
  background: var(--primary-soft);
  padding: 2px 8px;
  border-radius: 999px;
  font-weight: 400;
}
.match-empty {
  font-size: 0.8rem;
  color: var(--fg3);
  padding: 12px 0;
}
.to-demand {
  display: block;
  margin-top: 10px;
  width: 100%;
  background: var(--primary-soft);
  color: var(--primary-deep);
  border: 1px solid rgba(255, 106, 0, 0.3);
  border-radius: 10px;
  padding: 10px;
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
}
.to-demand:active {
  opacity: 0.85;
}
.mc-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
}
.mc-head b {
  font-size: 0.86rem;
}
.mc-price {
  color: var(--primary-deep);
  font-weight: 700;
  font-size: 0.8rem;
  white-space: nowrap;
}
.mc-meta {
  font-size: 0.68rem;
  color: var(--fg3);
  margin-top: 3px;
  margin-bottom: 8px;
}
.match-card .why {
  font-size: 0.7rem;
  color: var(--primary-deep);
  background: var(--primary-soft);
  border-radius: 6px;
  padding: 6px 8px;
  line-height: 1.5;
}
.home-guide {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 10px 16px 0;
  padding: 10px 12px;
  background: var(--warning-soft);
  border: 1px solid var(--warning-border);
  border-radius: 12px;
  font-size: 0.72rem;
  color: var(--warning-text);
  cursor: pointer;
}
.home-guide > i:first-child {
  color: var(--warning);
  font-size: 1.1rem;
}
.home-guide > i:last-child {
  margin-left: auto;
  color: var(--warning);
}
.report-strip {
  background: var(--transfer-soft);
  border-color: var(--transfer-border);
  color: var(--transfer-text);
}
.report-strip > i:first-child {
  color: var(--transfer);
}
.report-strip > i:last-child {
  color: var(--transfer);
}
</style>
