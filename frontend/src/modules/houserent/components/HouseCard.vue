<template>
  <div class="h-card" @click="$emit('click')">
    <div class="h-thumb" :class="thumbClass">
      <img v-if="cover" :src="cover" alt="" loading="lazy" />
      <span class="pricetag num">{{ formatMoney(house.rent) }}</span>
    </div>
    <div class="h-body">
      <div class="h-title">{{ house.community }} · {{ house.houseType }} {{ house.area }}㎡</div>
      <div class="h-meta">{{ meta }}</div>
      <div class="h-tags">
        <span class="tag owner">{{ labelText }}</span>
        <span v-if="house.petOk === 1" class="tag verify">可养宠</span>
        <!-- 已核实标：列表均为已上架（审核通过），产品 4.1 卡片 6 要素之一 -->
        <span class="tag verify"><i class="ph ph-seal-check"></i> 已核实</span>
      </div>
      <div class="h-foot">
        <span class="commute"><i class="ph ph-bicycle"></i> {{ house.commute || '通勤待填' }}</span>
        <span class="num price-num">¥{{ formatMoney(house.rent) }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { formatMoney, parseImages, timeAgo } from '@/utils/format'
import { HOUSE_LABEL_TEXT } from '@/constants/status'

const props = defineProps({
  house: {
    type: Object,
    required: true,
    validator: (value) => {
      // 必须包含房源基本字段
      return (
        value.id != null &&
        value.community &&
        value.houseType &&
        value.rent != null &&
        value.area != null
      )
    }
  },
  index: { type: Number, default: 0 }
})
defineEmits(['click'])

const cover = computed(() => parseImages(props.house.images)[0] || '')
const thumbClass = computed(() => ['thumb-a', 'thumb-b', 'thumb-c'][props.index % 3])
const meta = computed(() =>
  [props.house.depositPay, props.house.petOk === 1 ? '可养猫' : '', timeAgo(props.house.createdAt)]
    .filter(Boolean)
    .join(' · ')
)
const labelText = computed(() => HOUSE_LABEL_TEXT[props.house.label] || '')
</script>

<style scoped>
.h-card {
  background: var(--card);
  border-radius: 14px;
  padding: 12px;
  display: flex;
  gap: 12px;
  border: 1px solid var(--border);
  cursor: pointer;
  transition: box-shadow 0.2s;
}
.h-card:active {
  box-shadow: 0 4px 16px rgba(255, 106, 0, 0.15);
}
.h-thumb {
  width: 96px;
  height: 96px;
  border-radius: 10px;
  flex-shrink: 0;
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #ffd9c2, #ffb98a);
}
.h-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.h-thumb.thumb-b {
  background: linear-gradient(135deg, #c9e4ff, #8fc1ff);
}
.h-thumb.thumb-c {
  background: linear-gradient(135deg, #d8f5d8, #a8e6b8);
}
.pricetag {
  position: absolute;
  left: 6px;
  bottom: 6px;
  background: rgba(0, 0, 0, 0.72);
  color: #fff;
  font-size: 0.72rem;
  font-weight: 700;
  font-family: var(--num);
  padding: 2px 8px;
  border-radius: 6px;
  z-index: 1;
}
.h-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.h-title {
  font-size: 0.86rem;
  font-weight: 600;
  color: var(--fg);
  line-height: 1.35;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.h-meta {
  font-size: 0.72rem;
  color: var(--fg2);
  margin-top: 3px;
}
.h-tags {
  margin-top: 5px;
}
.h-foot {
  margin-top: auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 0.7rem;
  color: var(--fg3);
  padding-top: 6px;
}
.commute {
  color: var(--fg2);
  display: flex;
  align-items: center;
  gap: 2px;
}
.commute i {
  color: var(--primary);
}
.price-num {
  color: var(--primary-deep);
  font-weight: 700;
}
.tag i {
  font-size: 0.7rem;
  vertical-align: -0.05em;
  margin-right: 1px;
}
</style>
