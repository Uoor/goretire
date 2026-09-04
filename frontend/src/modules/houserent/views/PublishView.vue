<template>
  <div class="publish-page">
    <TopBar back :title="editId ? '修改房源' : '发布房源'" />

    <div class="form-sec">
      <div class="form-label"><span>房源照片</span><span class="hint">最多 9 张，第一张为主图</span></div>
      <van-uploader
        v-model="fileList"
        :max-count="9"
        :after-read="afterRead"
        :preview-image="true"
        class="photo-uploader"
      />
      <div class="ai-tip">
        <i class="ph ph-sparkle"></i>
        <span>填写小区后，AI 会自动带出区域与参考租金区间（功能接入中）</span>
      </div>
    </div>

    <div class="form-sec">
      <div class="form-label"><span>标题</span><span class="hint">可空，用于卡片展示（如：西溪八方城精装三居，近地铁）</span></div>
      <div class="form-field">
        <input v-model="form.title" placeholder="一句话吸引租客，建议 20 字以内" maxlength="100" />
      </div>
      <div class="form-label"><span>小区名称</span><span class="req">*</span></div>
      <div class="form-field">
        <input v-model="form.community" placeholder="如：西溪八方城" @blur="autoRegion" />
      </div>
      <!-- 常用小区快捷选择：选小区自动带出区域（产品第 2 步"AI 自动带出区域"） -->
      <div class="quick-communities">
        <span
          v-for="c in quickCommunities"
          :key="c.name"
          class="qc-chip"
          :class="{ on: form.community === c.name }"
          @click="pickCommunity(c)"
        >
          {{ c.name }}
        </span>
      </div>
      <div class="form-label"><span>区域</span><span class="req">*</span></div>
      <div class="form-field">
        <input v-model="form.region" placeholder="如：杭州西溪" @blur="loadRegionPrice" @input="priceTip = null" />
      </div>
      <div v-if="priceTip" class="ai-tip price-tip">
        <i class="ph ph-sparkle"></i>
        <span>AI 定价参考：{{ priceTip }}</span>
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
      <div class="form-label"><span>租期</span><span class="hint">可空，默认面议</span></div>
      <div class="seg">
        <div
          v-for="t in leaseTerms"
          :key="t"
          class="seg-item"
          :class="{ on: form.leaseTerm === t }"
          @click="form.leaseTerm = t"
        >
          {{ t }}
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
      <div class="form-label"><span>水电网物业</span><span class="hint">如：含水电网 / 物业自理，可空</span></div>
      <div class="form-field">
        <input v-model="form.utilities" placeholder="如：含水电网，物业费 2 元/㎡" />
      </div>
      <div class="form-label"><span>可养宠</span></div>
      <div class="seg">
        <div class="seg-item" :class="{ on: form.petOk === 1 }" @click="form.petOk = 1">可以</div>
        <div class="seg-item" :class="{ on: form.petOk === 0 }" @click="form.petOk = 0">不可以</div>
      </div>
      <div class="form-label"><span>房源描述</span><span class="hint">可空，介绍亮点、配套、舍友等</span></div>
      <div class="form-field">
        <van-field v-model="form.description" type="textarea" rows="3" autosize maxlength="500" show-word-limit placeholder="如：房东自住刚搬走，家具全；近地铁，步行 5 分钟到园区" />
      </div>
    </div>

    <div class="submit-wrap fixed-shell">
      <button class="btn-primary" :disabled="submitting" @click="submit">
        {{ submitting ? '提交中…' : (editId ? '保存修改' : '发布') }}
      </button>
      <p class="submit-tip">发布后新上架房源自动推送到租房群</p>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast, showSuccessToast } from 'vant'
import TopBar from '@/modules/houserent/components/TopBar.vue'
import { houseApi } from '@/modules/houserent/api'
import { uploadApi } from '@/api'
import { formatMoney } from '@/utils/format'
import { compressImage } from '@/utils/image'
import { AUDIT_STATUS } from '@/constants/status'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const store = useUserStore()
const submitting = ref(false)
const fileList = ref([])
const priceTip = ref(null)
const editId = ref(null) // 编辑模式：null=发布，有值=修改重新提交
const editLoading = ref(false)

const houseTypes = ['1室0厅', '1室1厅', '2室1厅', '2室2厅', '3室1厅', '3室2厅', '主卧', '次卧', '整租']
const payTypes = ['押一付一', '押一付三', '押二付一', '半年付', '年付', '面议']
const leaseTerms = ['面议', '半年', '一年', '两年', '三年以上']
const labels = [
  { value: 1, text: '房东直租' },
  { value: 2, text: '校友转租' },
  { value: 3, text: '合租拼室友' }
]

// 常用小区 → 区域映射（模拟"选小区 AI 带出区域"，无真实小区库时先用常用列表）
const quickCommunities = [
  { name: '西溪八方城', region: '杭州西溪' },
  { name: '西溪蝶园', region: '杭州西溪' },
  { name: '滨江长河', region: '杭州滨江' },
  { name: '江陵路', region: '杭州滨江' },
  { name: '翠苑', region: '杭州西湖' },
  { name: '文三路', region: '杭州西湖' },
  { name: '北京望京', region: '北京望京' },
  { name: '西二旗', region: '北京西二旗' },
  { name: '张江高科', region: '上海张江' },
  { name: '漕河泾', region: '上海漕河泾' }
]

const form = reactive({
  title: '',
  community: '',
  roomNo: '',
  region: '',
  houseType: '',
  area: null,
  rent: null,
  depositPay: '',
  leaseTerm: '',
  label: null,
  petOk: 0,
  commute: '',
  utilities: '',
  description: ''
})

/** 快捷选择小区：自动带出区域并触发定价参考 */
function pickCommunity(c) {
  form.community = c.name
  form.region = c.region
  priceTip.value = null
  loadRegionPrice()
}

/** 手动填小区失焦：若区域为空且命中常用映射则自动带出 */
function autoRegion() {
  if (form.region.trim()) return
  const hit = quickCommunities.find((c) => c.name === form.community.trim())
  if (hit) {
    form.region = hit.region
    loadRegionPrice()
  }
}

/** 区域失焦：拉取该区域参考租金（AI 定价参考） */
async function loadRegionPrice() {
  const region = form.region.trim()
  if (!region) {
    priceTip.value = null
    return
  }
  try {
    const stat = await houseApi.regionReport(region)
    if (stat && stat.count > 0) {
      priceTip.value = `${region} 在租 ${stat.count} 套，均价 ${formatMoney(stat.avgRent)} 元/月`
        + (stat.minRent && stat.maxRent ? `（区间 ${formatMoney(stat.minRent)} - ${formatMoney(stat.maxRent)} 元）` : '')
    } else {
      priceTip.value = `${region} 暂无在租房源数据，可参考附近区域定价`
    }
  } catch {
    priceTip.value = null
  }
}

/** 选图后压缩并上传到后端，成功替换为服务端 URL */
async function afterRead(item) {
  // 幂等保护：已有 URL / 已在上传 / 已完成，避免 uploader 回调与 fileList 兜底监听重复上传
  if (item.url || item.status === 'uploading' || item.status === 'done') {
    return
  }
  item.status = 'uploading'
  item.message = '压缩中…'
  try {
    // 压缩图片（最大 1200px，质量 0.8）
    const compressed = await compressImage(item.file, {
      maxWidth: 1200,
      maxHeight: 1200,
      quality: 0.8
    })
    item.message = '上传中…'
    const url = await uploadApi.image(compressed)
    item.url = url
    item.status = 'done'
    item.message = ''
  } catch (e) {
    item.status = 'failed'
    // 失败原因可读化：axios 网络/超时/HTTP 错误映射成用户能看懂的话
    const msg = e?.message || ''
    item.message = /timeout|timeout of/.test(msg)
      ? '上传超时，请重试'
      : /network|Network|Failed to fetch/.test(msg)
        ? '网络异常，请重试'
        : /status code 4\d\d/.test(msg)
          ? '图片格式或大小不支持'
          : e?.response?.data?.msg || msg || '上传失败'
  }
}

/**
 * 兜底：部分 WebView（如华为 UWS 内核）van-uploader 的 after-read 可能不触发，
 * 导致选图后只有本地缩略图、从未发起上传（保存时图片被静默丢弃）。
 * 监听 fileList：存在"有文件但从未处理过"的项时主动补触发上传。
 * （afterRead 内会同步置 status='uploading'，不会重复触发。）
 */
watch(
  fileList,
  (list) => {
    list.forEach((item) => {
      if (item.file && !item.url && !item.status) {
        afterRead(item)
      }
    })
  },
  { deep: true }
)

async function submit() {
  if (!form.community || !form.region || !form.houseType || !form.area || !form.rent || !form.depositPay || !form.label) {
    showToast('请填写必填项（带 * 的字段）')
    return
  }
  if (form.rent <= 0 || form.area <= 0) {
    showToast('面积与租金需大于 0')
    return
  }
  const uploading = fileList.value.find((f) => f.status === 'uploading' || f.status === 'failed')
  if (uploading) {
    showToast('有图片上传中或失败，请稍候')
    return
  }
  // 兜底：存在没有服务端 URL 的缩略图（选图后上传从未成功，如部分 WebView 静默失败），
  // 直接保存会把图片静默丢弃成空数组，必须明确阻止并提示
  const noUrlCount = fileList.value.filter((f) => !f.url).length
  if (noUrlCount > 0) {
    showToast(`有 ${noUrlCount} 张图片未上传成功，请删除后重新选择`)
    return
  }
  const images = fileList.value.map((f) => f.url).filter(Boolean)
  submitting.value = true
  try {
    const payload = { ...form, images: JSON.stringify(images) }
    let result
    if (editId.value) {
      result = await houseApi.update(editId.value, payload)
      showSuccessToast(result?.auditStatus === AUDIT_STATUS.ONLINE ? '已保存，修改已生效' : '已修改并重新提交审核')
    } else {
      result = await houseApi.publish(payload)
      showSuccessToast(result?.auditStatus === AUDIT_STATUS.ONLINE ? '发布成功，已上架并推送到租房群' : '已提交审核，通过后自动推送')
    }
    // 待审核房源详情页对其 404；跳「我的发布」查看状态
    router.push({ name: 'me' })
  } catch (e) {
    showToast(e.message || '发布失败')
  } finally {
    submitting.value = false
  }
}

/** 编辑模式：加载原数据回填（mine 接口返回房号等完整字段） */
async function loadForEdit(id) {
  editLoading.value = true
  try {
    let h
    if (store.isAdmin) {
      // 管理员可编辑任意房源（包括其他人的），调用管理员专用接口
      h = await houseApi.adminDetail(id)
    } else {
      const list = await houseApi.mine()
      h = list.find((x) => x.id === Number(id))
    }
    if (!h) {
      showToast('房源不存在')
      return
    }
    Object.assign(form, {
      title: h.title || '',
      community: h.community || '',
      roomNo: h.roomNo || '',
      region: h.region || '',
      houseType: h.houseType || '',
      area: h.area ?? null,
      rent: h.rent ?? null,
      depositPay: h.depositPay || '',
      leaseTerm: h.leaseTerm || '',
      label: h.label ?? null,
      petOk: h.petOk ?? 0,
      commute: h.commute || '',
      utilities: h.utilities || '',
      description: h.description || ''
    })
    if (Array.isArray(h.images)) {
      fileList.value = h.images.map((url) => ({ url, status: 'done', name: url.split('/').pop() }))
    }
    if (h.region) loadRegionPrice()
  } catch (e) {
    showToast(e.message || '加载房源失败')
  } finally {
    editLoading.value = false
  }
}

// 编辑模式监听：Vue Router 同一路由不同 query 参数不会重新触发 onMounted，
// 改用 watch 监听 route.query.edit，保证从「我的发布」反复点击不同房源时都能正确加载数据。
watch(
  () => route.query.edit,
  (id) => {
    if (id) {
      editId.value = id
      loadForEdit(id)
    }
  },
  { immediate: true }
)
</script>

<style scoped>
.publish-page {
  padding-bottom: 120px;
}
@media (min-width: 768px) {
  /* PC 端仍需给 fixed 提交栏让位，否则滚动到底时最后字段（一句话描述）被盖住 */
  .publish-page {
    padding-bottom: 100px;
  }
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
.photo-uploader :deep(.van-uploader__preview-image),
.photo-uploader :deep(.van-uploader__upload) {
  border-radius: 10px;
  overflow: hidden;
}
.ai-tip {
  background: linear-gradient(90deg, var(--primary-soft), #fff7e6);
  border: 1px solid rgba(255, 106, 0, 0.2);
  border-radius: 10px;
  padding: 10px 12px;
  font-size: 0.72rem;
  color: var(--warning-text);
  display: flex;
  gap: 8px;
  align-items: flex-start;
}
.ai-tip i {
  color: var(--primary);
  margin-top: 2px;
}
.quick-communities {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 10px;
}
.qc-chip {
  font-size: 0.7rem;
  padding: 4px 10px;
  border-radius: 999px;
  border: 1px solid var(--border);
  background: var(--card);
  color: var(--fg2);
  cursor: pointer;
  transition: all 0.15s ease;
}
.qc-chip.on {
  background: var(--primary-soft);
  border-color: var(--primary);
  color: var(--primary-deep);
  font-weight: 600;
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
  background: var(--card);
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
  background: var(--card);
  border-top: 1px solid var(--border);
  padding: 12px 16px 24px;
  z-index: 50;
}
@media (min-width: 768px) {
  .submit-wrap {
    max-width: 1200px;
    margin: 0 auto;
    display: flex;
    flex-direction: column;
    align-items: center;
  }
}
.submit-tip {
  text-align: center;
  font-size: 0.66rem;
  color: var(--fg3);
  margin-top: 8px;
}
/* PC 端主按钮限宽居中（公共样式见 styles/components.css） */
@media (min-width: 768px) {
  .btn-primary {
    max-width: 320px;
    margin: 0 auto;
  }
}
</style>
