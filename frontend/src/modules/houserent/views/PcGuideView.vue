<template>
  <!-- PC 浏览器引导页：测试期仅开放移动端（手机钉钉不拦截域名）。
       只引导手机扫码，不放"在电脑钉钉打开"——PC 端蚂蚁钉会拦截域名，引向死路。 -->
  <div class="pc-guide">
    <div class="guide-frame">
      <!-- 品牌区 -->
      <div class="brand">
        <div class="logo">
          <i class="ph ph-house-line"></i>
        </div>
        <h1>校友直租</h1>
        <p class="subtitle">
          「<span class="community-name">阿里人·一起提前退休</span>」<br>
          社群专属 · 校友互信租房
        </p>
      </div>

      <!-- 扫码区 -->
      <div class="qr-card">
        <div class="qr-title">请使用手机钉钉扫码打开</div>
        <div class="qr-box">
          <img v-if="qrDataUrl" :src="qrDataUrl" alt="钉钉扫码打开" class="qr-img" />
          <div v-else class="qr-loading">二维码生成中…</div>
        </div>
        <div class="qr-steps">
          <div class="step"><span class="step-num">1</span>打开手机钉钉</div>
          <div class="step"><span class="step-num">2</span>点击「扫一扫」</div>
          <div class="step"><span class="step-num">3</span>自动进入应用</div>
        </div>
        <p class="qr-tip">移动端钉钉可正常访问，扫码后即可使用全部功能</p>
      </div>

      <!-- 底部背书 -->
      <div class="footer">
        <p class="comm">
          由「阿里人·一起提前退休」社群发起<br>
          仅限社群成员使用
        </p>
        <p class="powered">POWERED BY ALUMNI COMMUNITY</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import QRCode from 'qrcode'

const qrDataUrl = ref('')

// 二维码内容 = 应用首页（origin + pathname + #/）。
// 注意不能用 location.href：PC 浏览器已被守卫跳转到 /#/pc-guide，
// 用当前 URL 会把用户扫码引回引导页；首页进入后手机端正常免登。
onMounted(async () => {
  try {
    const homeUrl = location.origin + location.pathname + '#/'
    qrDataUrl.value = await QRCode.toDataURL(homeUrl, {
      width: 220,
      margin: 1,
      errorCorrectionLevel: 'M'
    })
  } catch (e) {
    console.error('[pc-guide] 二维码生成失败:', e)
  }
})
</script>

<style scoped>
.pc-guide {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg);
  padding: 24px;
}

.guide-frame {
  width: 100%;
  max-width: 420px;
  text-align: center;
}

/* ===== 品牌区 ===== */
.logo {
  width: 64px;
  height: 64px;
  margin: 0 auto 16px;
  background: var(--primary-soft);
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo i {
  font-size: 32px;
  color: var(--primary);
}

h1 {
  font-size: 1.6rem;
  font-weight: 700;
  margin: 0 0 8px;
  color: var(--fg);
}

.subtitle {
  font-size: 0.88rem;
  color: var(--fg2);
  margin: 0;
  line-height: 1.7;
}

.subtitle .community-name {
  color: var(--primary-deep);
  font-weight: 600;
}

/* ===== 扫码卡 ===== */
.qr-card {
  margin-top: 28px;
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 24px 20px 20px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.06);
}

.qr-title {
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--fg);
}

.qr-box {
  margin: 18px auto 6px;
  width: 240px;
  height: 240px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  border-radius: 12px;
  border: 1px solid var(--border);
  overflow: hidden;
}

.qr-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.qr-loading {
  font-size: 0.8rem;
  color: var(--fg3);
}

.qr-steps {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 14px;
}

.step {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.78rem;
  color: var(--fg2);
}

.step-num {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: var(--primary-soft);
  color: var(--primary-deep);
  font-weight: 700;
  font-size: 0.72rem;
  display: flex;
  align-items: center;
  justify-content: center;
}

.qr-tip {
  margin-top: 12px;
  font-size: 0.75rem;
  color: var(--fg3);
  line-height: 1.6;
}

/* ===== 底部背书 ===== */
.footer {
  margin-top: 28px;
  text-align: center;
}

.footer .comm {
  font-size: 0.75rem;
  color: var(--fg2);
  line-height: 1.8;
  margin: 0;
}

.footer .powered {
  margin-top: 10px;
  font-size: 0.62rem;
  color: var(--fg3);
  letter-spacing: 0.5px;
}
</style>
