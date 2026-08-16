<template>
  <div class="publish-page">
    <TopBar back title="发布房源" />

    <div class="form-sec">
      <div class="form-label"><span>房源照片</span><span class="hint">最多 9 张，第一张为主图</span></div>
      <div class="upload-grid">
        <div
          v-for="(img, i) in images"
          :key="i"
          class="upload-box filled"
          @click="removeImage(i)"
        >
          <img :src="img" alt="" />
          <i class="ph ph-x-circle"></i>
        </div>
        <div v-if="images.length < 9" class="upload-box" @click="addImage">
          <i class="ph ph-camera"></i>
          <span>上传</span>
        </div>
      </div>
      <div class="ai-tip">
        <i class="ph ph-sparkle"></i>
        <span>填写小区后，AI 会自动带出区域与参考租金区间（功能接入中）</span>
      </div>
    </div>

    <div class="form-sec">
      <div class="form-label"><span>小区名称</span><span class="req">*</span></div>
      <div class="form-field">
        <input v-model="form.community" placeholder="如：西溪八方城" />
      </div>
      <div class="form-label"><span>房号（仅审核可见）</span><span class="hint">公开展示只到小区</span></div>
      <div class="form-field">
        <input v-model="form.roomNo" placeholder="如：8-1201" />
      </div>
      <div class="form-label"><span>区域</span><span class="req">*</span></div>
      <div class="form-field">
        <input v-model="form.region" placeholder="如：杭州西溪" />
      </div>
    </div>

    <div class="form-sec">
      <div class="form-label"><span>户型</span><span class="req">*</span></div>
      <div class="seg">
        <div
          v-for="t in houseTypes"
          :key="t"
          class="seg-item"
          :class="{ on: form.houseType === t }"
          @click="form.houseType = t"
        >
          {{ t }}
        </div>
      </div>
      <div class="form-label"><span>面积（㎡）</span><span class="req">*</span></div>
      <div class="form-field">
        <input v-model.number="form.area" type="number" placeholder="如：89" />
      </div>
      <div class="form-label"><span>月租（元）</span><span class="req">*</span></div>
      <div class="form-field">
        <input v-model.number="form.rent" type="number" placeholder="如：5800" />
      </div>
      <div class="form-label"><span>押付方式</span><span class="req">*</span></div>
      <div class="seg">
        <div
          v-for="p in payTypes"
          :key="p"
          class="seg-item"
          :class="{ on: form.depositPay === p }"
          @click="form.depositPay = p"
        >
          {{ p }}
        </div>
      </div>
    </div>

    <div class="form-sec">
      <div class="form-label"><span>房源标签</span><span class="req">*</span></div>
      <div class="seg">
        <div
          v-for="l in labels"
          :key="l.value"
          class="seg-item"
          :class="{ on: form.label === l.value }"
          @click="form.label = l.value"
        >
          {{ l.text }}
        </div>
      </div>
      <div class="form-label"><span>通勤说明</span><span class="hint">到最近园区，参考值</span></div>
      <div class="form-field">
        <input v-model="form.commute" placeholder="如：西溪园区 15 分钟" />
      </div>
      <div class="form-label"><span>可养宠</span></div>
      <div class="seg">
        <div class="seg-item" :class="{ on: form.petOk === 1 }" @click="form.petOk = 1">可以</div>
        <div class="seg-item" :class="{ on: form.petOk === 0 }" @click="form.petOk = 0">不可以</div>
      </div>
      <div class="form-label"><span>一句话描述</span><span class="hint">可空</span></div>
      <div class="form-field">
        <input v-model="form.description" placeholder="如：房东自住刚搬走，家具全" />
      </div>
    </div>

    <div class="submit-wrap fixed-shell">
      <button class="btn-primary" :disabled="submitting" @click="submit">
        {{ submitting ? '提交中…' : '提交审核' }}
      </button>
      <p class="submit-tip">提交后预计 2 小时内完成审核</p>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showSuccessToast } from 'vant'
import TopBar from '@/modules/houserent/components/TopBar.vue'
import { houseApi } from '@/modules/houserent/api'

const router = useRouter()
const submitting = ref(false)
const images = ref([])

const houseTypes = ['1室0厅', '1室1厅', '2室1厅', '2室2厅', '3室1厅', '3室2厅', '主卧', '次卧', '整租']
const payTypes = ['押一付一', '押一付三', '押二付一', '半年付', '年付']
const labels = [
  { value: 1, text: '房东直租' },
  { value: 2, text: '校友转租' },
  { value: 3, text: '合租拼室友' }
]

const form = reactive({
  community: '',
  roomNo: '',
  region: '',
  houseType: '',
  area: null,
  rent: null,
  depositPay: '',
  label: null,
  petOk: 0,
  commute: '',
  description: ''
})

function addImage() {
  // MVP：接入 OSS/钉钉上传前，先用占位图模拟
  const placeholder = `https://picsum.photos/seed/p${Date.now()}/400/300`
  images.value.push(placeholder)
}

function removeImage(i) {
  images.value.splice(i, 1)
}

async function submit() {
  if (!form.community || !form.region || !form.houseType || !form.area || !form.rent || !form.depositPay || !form.label) {
    showToast('请填写必填项（带 * 的字段）')
    return
  }
  if (form.rent <= 0 || form.area <= 0) {
    showToast('面积与租金需大于 0')
    return
  }
  submitting.value = true
  try {
    const id = await houseApi.publish({
      ...form,
      images: JSON.stringify(images.value)
    })
    showSuccessToast('已提交审核，预计 2 小时内上架')
    router.push({ name: 'house-detail', params: { id } })
  } catch (e) {
    showToast(e.message || '发布失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.publish-page {
  padding-bottom: 120px;
}
.form-sec {
  background: var(--card);
  padding: 14px 16px;
  border-bottom: 1px solid var(--border);
}
.form-label {
  font-size: 0.8rem;
  font-weight: 600;
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  gap: 4px;
}
.form-label .req {
  color: var(--destructive);
}
.form-label .hint {
  font-weight: 400;
  font-size: 0.66rem;
  color: var(--fg3);
  margin-left: auto;
}
.upload-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-bottom: 10px;
}
.upload-box {
  aspect-ratio: 1;
  border-radius: 10px;
  border: 1.5px dashed #d9d9d9;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--fg3);
  font-size: 0.64rem;
  gap: 4px;
  background: #fafafa;
  cursor: pointer;
  position: relative;
  overflow: hidden;
}
.upload-box i {
  font-size: 1.2rem;
  color: var(--fg3);
}
.upload-box.filled {
  border-style: solid;
  border-color: transparent;
}
.upload-box.filled img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.upload-box.filled i {
  position: absolute;
  top: 4px;
  right: 4px;
  color: #fff;
  background: rgba(0, 0, 0, 0.5);
  border-radius: 50%;
  font-size: 1rem;
}
.ai-tip {
  background: linear-gradient(90deg, var(--primary-soft), #fff7e6);
  border: 1px solid rgba(255, 106, 0, 0.2);
  border-radius: 10px;
  padding: 10px 12px;
  font-size: 0.72rem;
  color: #874d00;
  display: flex;
  gap: 8px;
  align-items: flex-start;
}
.ai-tip i {
  color: var(--primary);
  margin-top: 2px;
}
.form-field {
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 10px 12px;
  margin-bottom: 10px;
  font-size: 0.8rem;
  color: var(--fg2);
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.form-field input {
  border: none;
  outline: none;
  flex: 1;
  font-size: 0.8rem;
  font-family: inherit;
  text-align: right;
  color: var(--fg);
}
.seg {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}
.seg-item {
  font-size: 0.74rem;
  padding: 6px 14px;
  border-radius: 8px;
  border: 1px solid var(--border);
  color: var(--fg2);
  background: #fff;
  cursor: pointer;
  transition: all 0.15s ease;
}
.seg-item.on {
  background: var(--primary-soft);
  border-color: var(--primary);
  color: var(--primary-deep);
  font-weight: 600;
}
.submit-wrap {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  max-width: 480px;
  margin: 0 auto;
  background: var(--card);
  border-top: 1px solid var(--border);
  padding: 12px 16px 24px;
  z-index: 50;
}
.submit-tip {
  text-align: center;
  font-size: 0.66rem;
  color: var(--fg3);
  margin-top: 8px;
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
