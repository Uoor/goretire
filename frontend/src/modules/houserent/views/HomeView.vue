<template>
  <div class="page-home">
    <TopBar />
    <div class="searchbar" @click="showSearch = true">
      <div class="search-input" :class="{ active: showSearch }">
        <i class="ph ph-magnifying-glass"></i>
        <span v-if="!showSearch">说人话找房：西溪附近 6000 以内两居</span>
        <input
          v-else
          v-model="query"
          ref="searchRef"
          placeholder="预算 6000 以内，西溪附近，能养猫…"
          @keyup.enter="doSearch"
          @blur="showSearch = false"
        />
      </div>
      <button v-if="showSearch" class="search-go" @click="doSearch">找房</button>
    </div>

    <!-- 一句话找房结果 -->
    <div v-if="matchResult" class="match-panel">
      <div class="match-head">
        <span>🤖 AI 匹配结果</span>
        <span v-if="matchResult.degraded" class="deg-tag">本地降级</span>
        <i class="ph ph-x" @click="matchResult = null"></i>
      </div>
      <div v-if="matchResult.matches.length === 0" class="match-empty">没有找到合适的房源，换个说法试试</div>
      <div v-for="m in matchResult.matches" :key="m.houseId" class="match-card" @click="openHouse(m.houseId)">
        <div class="why">{{ m.reason }}</div>
      </div>
    </div>

    <FilterChips :chips="chips" v-model="activeChip" />

    <div class="feed">
      <HouseCard
        v-for="(h, i) in houses"
        :key="h.id"
        :house="h"
        :index="i"
        @click="openHouse(h.id)"
      />
    </div>

    <div v-if="loading" class="feed">
      <van-skeleton v-for="i in 3" :key="i" title :row="2" class="sk" />
    </div>
    <EmptyState v-else-if="houses.length === 0 && !loading" text="暂无房源，先去发布一套吧" />
  </div>
</template>

<script setup>
import { onMounted, ref, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import TopBar from '@/modules/houserent/components/TopBar.vue'
import HouseCard from '@/modules/houserent/components/HouseCard.vue'
import FilterChips from '@/modules/houserent/components/FilterChips.vue'
import EmptyState from '@/modules/houserent/components/EmptyState.vue'
import { houseApi, matchApi } from '@/modules/houserent/api'

const router = useRouter()
const houses = ref([])
const loading = ref(true)
const query = ref('')
const showSearch = ref(false)
const searchRef = ref(null)
const matchResult = ref(null)
const activeChip = ref('')

const chips = [
  { key: 'label1', label: '房东直租' },
  { key: 'label2', label: '校友转租' },
  { key: 'label3', label: '合租拼室友' },
  { key: 'pet', label: '可养宠' },
  { key: 'new', label: '新上架' }
]

async function load() {
  loading.value = true
  try {
    const params = {}
    if (/^label\d$/.test(activeChip.value)) params.label = Number(activeChip.value.slice(5))
    if (activeChip.value === 'pet') params.petOk = 1
    houses.value = await houseApi.list(params)
  } catch (e) {
    showToast(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function doSearch() {
  const text = query.value.trim()
  if (!text) return
  showSearch.value = false
  try {
    matchResult.value = await matchApi.search(text)
  } catch (e) {
    showToast(e.message || '找房失败')
  }
}

function openHouse(id) {
  router.push({ name: 'house-detail', params: { id } })
}

watch(activeChip, load)

onMounted(async () => {
  await load()
  if (showSearch.value) nextTick(() => searchRef.value?.focus())
})
</script>

<style scoped>
.page-home {
  padding-bottom: 76px;
}
.searchbar {
  background: var(--card);
  padding: 10px 16px;
  display: flex;
  gap: 8px;
  align-items: center;
}
.search-input {
  background: #f2f2f1;
  border-radius: 10px;
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
  background: #fff;
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
.feed {
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.sk {
  border-radius: 14px;
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
.match-card {
  background: var(--card);
  border-radius: 14px;
  padding: 14px;
  border: 1px solid var(--border);
  margin-bottom: 10px;
  cursor: pointer;
}
.match-card .why {
  font-size: 0.7rem;
  color: var(--primary-deep);
  background: var(--primary-soft);
  border-radius: 6px;
  padding: 6px 8px;
  line-height: 1.5;
}
</style>
