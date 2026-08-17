<template>
  <div class="report-page">
    <TopBar back title="租金周报" />

    <div class="report-hero">
      <div class="big">📊 本周租金行情</div>
      <div class="small">区域均价来自在租已上架房源 · 每周五更新</div>
    </div>

    <div v-if="report">
      <div class="overview">
        <div class="ov-card">
          <div class="ov-v num">{{ report.weekNew }}</div>
          <div class="ov-k">本周新上架</div>
        </div>
        <div class="ov-card">
          <div class="ov-v num">{{ report.weekRented }}</div>
          <div class="ov-k">本周找到新家</div>
        </div>
        <div class="ov-card">
          <div class="ov-v num">{{ report.totalOnline }}</div>
          <div class="ov-k">在租房源</div>
        </div>
      </div>

      <div class="sec-title">区域均价（元/月）</div>
      <div class="region-list">
        <div v-for="r in report.regions" :key="r.region" class="region-card">
          <div class="rg-left">
            <div class="rg-name">{{ r.region }}</div>
            <div class="rg-count">{{ r.count }} 套在租</div>
          </div>
          <div class="rg-price">
            <span class="num">{{ formatMoney(r.avgRent) }}</span>
            <small>元/月</small>
          </div>
        </div>
        <div v-if="report.regions.length === 0" class="rg-empty">暂无在租房源数据</div>
      </div>

      <div class="story-strip" v-if="report.weekRented > 0">
        <i class="ph ph-heart"></i>
        <span>🎉 本周 <b>{{ report.weekRented }}</b> 位校友通过「校友安居」找到新家，愿你们在新家安居。</span>
      </div>
    </div>

    <van-skeleton v-else title :row="6" class="rp-sk" />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { showToast } from 'vant'
import TopBar from '@/modules/houserent/components/TopBar.vue'
import { houseApi } from '@/modules/houserent/api'
import { formatMoney } from '@/utils/format'

const report = ref(null)

onMounted(async () => {
  try {
    report.value = await houseApi.reportWeekly()
  } catch (e) {
    showToast(e.message || '加载失败')
  }
})
</script>

<style scoped>
.report-page {
  padding-bottom: 40px;
  min-height: 100vh;
}
.report-hero {
  background: linear-gradient(135deg, var(--primary-soft), #ffebd6);
  padding: 20px 16px;
  border-bottom: 1px solid var(--border);
}
.report-hero .big {
  font-size: 1rem;
  font-weight: 700;
}
.report-hero .small {
  font-size: 0.72rem;
  color: var(--fg2);
  margin-top: 4px;
}
.overview {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  padding: 14px 16px;
}
.ov-card {
  background: var(--card);
  border-radius: 14px;
  padding: 14px 8px;
  border: 1px solid var(--border);
  text-align: center;
}
.ov-v {
  font-size: 1.4rem;
  font-weight: 700;
  font-family: var(--num);
  color: var(--primary-deep);
}
.ov-k {
  font-size: 0.66rem;
  color: var(--fg3);
  margin-top: 4px;
}
.sec-title {
  font-size: 0.82rem;
  font-weight: 600;
  padding: 6px 16px;
}
.region-list {
  padding: 10px 16px;
}
.region-card {
  background: var(--card);
  border-radius: 14px;
  padding: 14px;
  border: 1px solid var(--border);
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.rg-name {
  font-size: 0.86rem;
  font-weight: 600;
}
.rg-count {
  font-size: 0.66rem;
  color: var(--fg3);
  margin-top: 3px;
}
.rg-price span {
  font-size: 1.3rem;
  font-weight: 700;
  font-family: var(--num);
  color: var(--primary-deep);
}
.rg-price small {
  font-size: 0.7rem;
  color: var(--fg2);
  margin-left: 2px;
}
.rg-empty {
  font-size: 0.8rem;
  color: var(--fg3);
  padding: 24px 0;
  text-align: center;
}
.story-strip {
  margin: 6px 16px 20px;
  padding: 12px;
  background: var(--accent-soft);
  border: 1px solid var(--accent-border);
  border-radius: 12px;
  font-size: 0.74rem;
  color: var(--accent);
  line-height: 1.6;
  display: flex;
  align-items: flex-start;
  gap: 8px;
}
.story-strip i {
  font-size: 1.2rem;
  flex-shrink: 0;
}
.rp-sk {
  margin: 16px;
}
</style>
