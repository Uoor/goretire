<template>
  <!-- 手机浏览器（非钉钉容器）引导提示：放行可用，但提示用钉钉扫码体验更完整。
       钉钉容器内 / PC 视口不渲染（PC 由路由守卫统一拦截到 /pc-guide）。 -->
  <div v-if="visible" class="dd-tip-bar" @click="openQr">
    <i class="ph ph-qr-code"></i>
    <span class="tip-text">建议使用钉钉打开，体验更完整</span>
    <i class="ph ph-x tip-close" @click.stop="visible = false"></i>
  </div>

  <van-popup v-model:show="qrShow" round position="bottom" class="dd-tip-popup">
    <div class="popup-title">用手机钉钉扫一扫</div>
    <div class="popup-qr">
      <img v-if="qrDataUrl" :src="qrDataUrl" alt="钉钉扫码打开" />
      <span v-else class="qr-loading">二维码生成中…</span>
    </div>
    <p class="popup-tip">扫码后在钉钉内打开，登录、联系房东等完整功能可用</p>
    <div class="popup-note">本服务由「阿里人·一起提前退休」社群运营</div>
  </van-popup>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import QRCode from 'qrcode'

const visible = ref(true)
const qrShow = ref(false)
const qrDataUrl = ref('')

function openQr() {
  qrShow.value = true
  if (!qrDataUrl.value) {
    QRCode.toDataURL(location.href, { width: 220, margin: 1, errorCorrectionLevel: 'M' })
      .then((url) => { qrDataUrl.value = url })
      .catch((e) => console.error('[dd-tip] 二维码生成失败:', e))
  }
}
</script>

<style scoped>
.dd-tip-bar {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  background: var(--primary-soft);
  border-bottom: 1px solid rgba(255, 106, 0, 0.3);
  font-size: 0.78rem;
  color: var(--primary-deep);
  cursor: pointer;
}

.dd-tip-bar i {
  font-size: 0.9rem;
}

.dd-tip-bar .tip-text {
  flex: 1;
}

.dd-tip-bar .tip-close {
  color: var(--fg3);
  font-size: 0.85rem;
}

/* ===== 弹层 ===== */
.dd-tip-popup {
  padding: 24px 20px 28px;
  text-align: center;
}

.popup-title {
  font-size: 1rem;
  font-weight: 700;
  color: var(--fg);
}

.popup-qr {
  margin: 16px auto 8px;
  width: 220px;
  height: 220px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  border: 1px solid var(--border);
  border-radius: 12px;
  overflow: hidden;
}

.popup-qr img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.qr-loading {
  font-size: 0.78rem;
  color: var(--fg3);
}

.popup-tip {
  font-size: 0.8rem;
  color: var(--fg2);
  margin: 6px 0 4px;
  line-height: 1.6;
}

.popup-note {
  font-size: 0.7rem;
  color: var(--fg3);
  margin-top: 8px;
}
</style>
