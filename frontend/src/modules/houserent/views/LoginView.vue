<template>
  <div class="login-page">
    <div class="login-frame">
      <!-- 品牌区（PC：左栏） -->
      <div class="brand">
        <div class="logo">
          <i class="ph ph-house-line"></i>
        </div>
        <h1>校友安居</h1>
        <p class="subtitle">
          「<span class="community-name">阿里人·一起提前退休</span>」<br>
          社群专属 · 校友互信租房
        </p>
        <div class="member-badge"><span class="dot"></span>仅限社群成员使用</div>
      </div>

      <!-- 信任清单（PC：左栏） -->
      <div class="trust-card">
        <div class="trust-title">
          <i class="ph ph-shield-check"></i>
          <span>授权与隐私说明</span>
        </div>
        <div class="trust-item">
          <span class="icon yes">✓</span>
          <span>仅获取 <b>昵称、钉钉ID</b>，用于校验你是否属于本社群</span>
        </div>
        <div class="trust-item highlight">
          <span class="icon yes">✓</span>
          <span>仅关联「阿里人·一起提前退休」<b>社群企业</b>；跨企业，即阿里/蚂蚁企业内的信息，<em>不会收集，技术上也无法获取</em></span>
        </div>
        <div class="trust-item">
          <span class="icon yes">✓</span>
          <span>授权由 <b>钉钉官方</b> 完成，可随时在钉钉中撤销</span>
        </div>
        <div class="trust-item">
          <span class="icon no">✗</span>
          <span>无法读取 <b>聊天记录、通讯录、公司组织信息</b> 等个人数据，全程通过钉钉沟通</span>
        </div>
      </div>

      <!-- 登录卡（PC：右栏纯行动区） -->
      <div class="login-card">
        <div class="lc-title">登录以使用校友安居</div>
        <p class="lc-sub">通过钉钉官方授权校验社群身份，约 3 秒完成</p>
        <button class="dingtalk-login-btn" @click="login">
          <svg class="dingtalk-icon" viewBox="0 0 1024 1024" width="20" height="20">
            <path fill="currentColor" d="M512 0C229.2 0 0 229.2 0 512s229.2 512 512 512 512-229.2 512-512S794.8 0 512 0z m256.4 438.4l-95.6 44.8c-4 2-8.4-0.4-9.6-4.4l-13.2-44.4c-1.2-4 1.6-8.4 5.6-9.2l95.6-22.4c4.4-1.2 8.4 2 8.8 6.4l8.4 20.4c0.8 4-1.6 8-5.6 8.8z m-124.8 188.8c-16.4 24-60.4 56.4-60.4 56.4l-132.4-116.4 92.4-76.4-20.4-17.2-116.4 96.4-44.4-38.8c-8.4-7.2-8.8-20-0.8-27.6l180.4-168.4c8.4-7.6 21.2-7.2 28.8 0.8l168.4 180.4c7.6 8.4 7.2 21.2-0.8 28.8l-94.8 82z"/>
          </svg>
          <span>使用钉钉授权登录</span>
        </button>
        <p class="btn-sub">点击登录即代表你同意 <a href="javascript:;">《隐私说明》</a></p>

        <!-- 非社群成员：跳 Join 页了解并加入 -->
        <a class="join-entry" href="javascript:;" @click="goJoin">
          还不是社群成员？加入社群
        </a>
      </div>

      <!-- 底部背书（PC：左栏底部） -->
      <div class="footer">
        <p class="comm">
          由「阿里人·一起提前退休」社群发起<br>
          已服务 <span class="num">6500+</span> 位校友 · 信任来自社群
        </p>
        <div class="divider"></div>
        <p class="powered">POWERED BY ALUMNI COMMUNITY</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showDialog } from 'vant'
import { redirectToDingTalkOAuth } from '@/utils/dd'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const store = useUserStore()

function login() {
  redirectToDingTalkOAuth()
}

// 非社群成员：跳 Join 页了解社群（hero 数据 / 价值 / 加入步骤）后再加入。
// 先清掉旧会话（浏览器联调残留的 dev-code / 之前的登录态）：
// 路由守卫对 /join 有"已登录回首页"逻辑，不清会话会被拦截回首页，跳不到 Join 页。
function goJoin() {
  store.clear()
  router.push({ name: 'join' })
}

// OAuth2 回调失败重定向到 /login?error=...（见后端 AuthController.oauthCallback）：
// not_in_org = 非社群组织成员 → 弹窗确认后跳 Join 页了解并加入；
// auth_failed = 其他登录失败 → 提示重试
onMounted(() => {
  const error = route.query.error
  if (!error) return
  if (error === 'not_in_org') {
    showDialog({
      title: '校友专属服务',
      message: '「校友安居」是「阿里人·一起提前退休」社群专属的租房服务，需要先加入社群组织才能使用。',
      confirmButtonText: '查看如何加入',
      closeOnClickOverlay: false
    }).then(() => goJoin()).catch(() => {})
  } else if (error === 'auth_failed') {
    showDialog({
      title: '登录失败',
      message: '钉钉授权未完成，请重试。若问题持续，请联系社群管理员。',
      confirmButtonText: '知道了'
    }).catch(() => {})
  }
  // 清除 error 参数，避免刷新后重复弹窗
  router.replace({ query: {} })
})
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg);
  padding: 16px;
}

/* 移动端：整体白卡（360px 单列） */
.login-frame {
  background: var(--card);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border);
  padding: 40px 24px 24px;
  max-width: 360px;
  width: 100%;
  display: flex;
  flex-direction: column;
  text-align: center;
}

/* ===== 品牌区 ===== */
.logo {
  width: 64px;
  height: 64px;
  margin: 0 auto 20px;
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
  font-size: 1.5rem;
  font-weight: 700;
  margin: 0 0 8px;
  color: var(--fg);
}

.subtitle {
  font-size: 0.85rem;
  color: var(--fg2);
  margin: 0;
  line-height: 1.6;
}

.subtitle .community-name {
  color: var(--primary-deep);
  font-weight: 600;
}

/* 社群成员徽章 */
.member-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  margin: 12px auto 0;
  padding: 4px 12px;
  background: var(--primary-soft);
  border: 1px solid rgba(255, 106, 0, 0.35);
  border-radius: 999px;
  font-size: 0.75rem;
  color: var(--primary-deep);
  font-weight: 500;
}

.member-badge .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--primary);
}

/* ===== 授权说明卡 ===== */
.trust-card {
  margin-top: 24px;
  background: var(--card);
  border-radius: var(--radius-md);
  border: 1px solid var(--border);
  padding: 14px 14px 10px;
  text-align: left;
}

.trust-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--fg);
  margin-bottom: 8px;
}

.trust-title i {
  font-size: 1rem;
  color: var(--accent);
}

.trust-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 4px 0;
  font-size: 0.78rem;
  line-height: 1.55;
  color: var(--fg2);
}

.trust-item .icon {
  flex-shrink: 0;
  margin-top: 1px;
  font-size: 0.78rem;
  line-height: 1.5;
}

.trust-item .yes { color: var(--accent); font-weight: 700; }
.trust-item .no { color: var(--fg3); font-weight: 700; }
.trust-item b { color: var(--fg); font-weight: 600; }

/* 重点承诺：仅关联社群企业，不触碰阿里企业（技术上也无法获取） */
.trust-item.highlight {
  background: var(--primary-soft);
  border-radius: 8px;
  padding: 8px 10px;
  margin: 4px 0;
  border: 1px solid rgba(255, 106, 0, 0.35);
}

.trust-item.highlight em {
  font-style: normal;
  color: var(--primary-deep);
  font-weight: 600;
}

/* ===== 登录卡（移动端：按钮块） ===== */
.login-card {
  margin-top: 20px;
}

.lc-title {
  display: none;
}

.lc-sub {
  display: none;
}

.btn-sub {
  margin-top: 10px;
  font-size: 0.7rem;
  color: var(--fg3);
  text-align: center;
}

.btn-sub a {
  color: var(--fg2);
  text-decoration: underline;
  text-underline-offset: 2px;
}

/* ===== 非社群成员：跳 Join 页（次按钮，突出可点击） ===== */
.join-entry {
  display: block;
  margin-top: 14px;
  padding: 12px;
  border: 1px solid var(--primary);
  border-radius: 10px;
  background: var(--card);
  color: var(--primary);
  font-size: 0.85rem;
  font-weight: 600;
  text-align: center;
  cursor: pointer;
  transition: all 0.15s ease;
}

.join-entry:hover {
  background: var(--primary-soft);
  color: var(--primary-deep);
}

.join-entry:active {
  opacity: 0.85;
}

/* ===== 底部背书 ===== */
.footer {
  margin-top: auto;
  text-align: center;
  padding-top: 28px;
}

.footer .comm {
  font-size: 0.75rem;
  color: var(--fg2);
  line-height: 1.8;
}

.footer .num {
  font-family: var(--num);
  font-weight: 700;
  color: var(--primary);
  font-size: 0.8rem;
}

.footer .divider {
  width: 24px;
  height: 1px;
  background: var(--border);
  margin: 12px auto;
}

.footer .powered {
  font-size: 0.62rem;
  color: var(--fg3);
  letter-spacing: 0.5px;
}

@media (max-width: 480px) {
  .login-frame {
    padding: 32px 20px 24px;
  }

  h1 {
    font-size: 1.3rem;
  }
}

/* ===== PC 端（≥768px）：左右分栏，信息不重复，一屏内不滚动 =====
   左侧 = 说服（品牌 + 信任 + 背书）；右侧 = 行动（登录卡）
   App.vue 壳层已有顶部导航 56px + 内容区 padding 24*2，垂直居中需减去 */
@media (min-width: 768px) {
  .login-page {
    padding: 0;
    min-height: calc(100vh - 104px);
  }

  .login-frame {
    max-width: 1200px;
    background: transparent;
    border: none;
    box-shadow: none;
    padding: 24px 56px;
    text-align: left;
    display: grid;
    grid-template-columns: 1.2fr 1fr;
    grid-template-areas:
      'brand  action'
      'trust  action'
      'footer action';
    column-gap: 56px;
    align-items: center;
  }

  /* 左栏：品牌 + 信任 + 背书 */
  .brand {
    grid-area: brand;
    text-align: left;
  }

  .logo {
    width: 56px;
    height: 56px;
    margin: 0 0 14px;
    border-radius: 16px;
  }

  .logo i {
    font-size: 28px;
  }

  h1 {
    font-size: 1.7rem;
  }

  .subtitle {
    font-size: 0.92rem;
    line-height: 1.55;
  }

  .member-badge {
    margin: 10px 0 0;
    padding: 4px 14px;
    font-size: 0.78rem;
  }

  .trust-card {
    grid-area: trust;
    margin-top: 18px;
    background: var(--card);
    border-radius: 14px;
    padding: 12px 16px 8px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  }

  .trust-title {
    font-size: 0.85rem;
    margin-bottom: 4px;
  }

  .trust-item {
    font-size: 0.8rem;
    padding: 3px 0;
    line-height: 1.5;
  }

  .trust-item .icon {
    font-size: 0.8rem;
  }

  .trust-item.highlight {
    padding: 7px 10px;
    margin: 2px 0;
  }

  /* 右栏：登录卡（纯行动） */
  .login-card {
    grid-area: action;
    margin-top: 0;
    background: var(--card);
    border: 1px solid var(--border);
    border-radius: 20px;
    padding: 28px 32px 24px;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.06);
    text-align: center;
  }

  .lc-title {
    display: block;
    font-size: 1.1rem;
    font-weight: 700;
    color: var(--fg);
  }

  .lc-sub {
    display: block;
    margin-top: 6px;
    font-size: 0.78rem;
    color: var(--fg2);
    line-height: 1.5;
  }

  .login-card .dingtalk-login-btn {
    margin-top: 20px;
    height: 46px;
    max-width: 320px;
    margin-left: auto;
    margin-right: auto;
  }

  .btn-sub {
    margin-top: 10px;
  }

  /* 非社群成员入口：PC 下与主按钮同宽居中 */
  .join-entry {
    margin-top: 12px;
    max-width: 320px;
    margin-left: auto;
    margin-right: auto;
    font-size: 0.8rem;
    padding: 10px;
  }

  /* 左栏底部背书 */
  .footer {
    grid-area: footer;
    margin-top: 14px;
    padding-top: 0;
    text-align: left;
  }

  .footer .comm {
    font-size: 0.76rem;
    line-height: 1.7;
  }

  .footer .divider {
    margin: 8px 0;
  }
}
</style>
