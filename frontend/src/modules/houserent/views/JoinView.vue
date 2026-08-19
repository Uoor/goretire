<template>
  <div class="join-page">
    <!-- Hero（品牌化：阿里人·一起提前退休） -->
    <div class="join-hero">
      <div class="badge">🤝 互助型 · 校友社群</div>
      <h1>【阿里人·一起<span class="hl">提前退休</span>】</h1>
      <p class="subtitle">蚂蚁校友创办 · 阿里&蚂蚁在职/离职员工专属</p>
      <p class="tagline">互助避坑 · 拉平信息差 · 提升认知 · 善用金融工具<br />和校友一起早日提前退休</p>
      <div class="stats">
        <div><div class="num">6000+</div><div class="k">累计群成员</div></div>
        <div><div class="num">7+</div><div class="k">生态子群</div></div>
        <div><div class="num">6+</div><div class="k">智能机器人</div></div>
      </div>
    </div>

    <div class="join-body">
      <!-- 安居定位 -->
      <div class="join-intro">
        <p>「<span class="hl">校友安居</span>」是社群生态子群之一，只面向<span class="hl">阿里 / 蚂蚁</span>在职与离职校友的专属租房网络。</p>
        <p>你当前使用的账号<span class="strong">不在校友社群组织中</span>，加入后即可使用租房服务。</p>
      </div>

      <!-- 社群价值 -->
      <div class="join-values">
        <div class="value-c">
          <span class="icon">🎯</span>
          <h4>初心</h4>
          <p>打破阿里人与优质资源之间的信息壁垒，互助共享、不收费不割韭菜</p>
        </div>
        <div class="value-c">
          <span class="icon">🏠</span>
          <h4>校友安居</h4>
          <p>直租免中介 · 身份互见 · 真实房源 · 互相托底</p>
        </div>
      </div>

      <!-- 加入步骤 -->
      <div class="join-steps">
        <div class="step">
          <span class="n">1</span>
          <div class="tx"><b>加入校友组织</b><p>点击下方按钮，申请加入社群组织</p></div>
        </div>
        <div class="step">
          <span class="n">2</span>
          <div class="tx"><b>回到安居</b><p>重新打开本应用，校友身份即自动登录</p></div>
        </div>
      </div>

      <!-- 结语 -->
      <div class="join-vision">
        不贩卖焦虑，只分享认知 · 不承诺暴富，只帮你避开每一个坑<br />
        <span class="tags">✅ 信息共享 · ✅ 互助避坑 · ✅ 认知提升 · ✅ 工具赋能</span>
      </div>
    </div>

    <!-- CTA 栏（底部固定，一键加入） -->
    <div class="join-cta">
      <a class="cta-btn" :href="inviteUrl">一键加入社群</a>
    </div>
  </div>
</template>

<script setup>
// 钉钉组织邀请链接（管理后台生成，钉钉内直接打开邀请页）
const inviteUrl =
  'dingtalk://dingtalkclient/page/link?pc_slide=true&url=https%3A%2F%2Fh5.dingtalk.com%2Finvite-page%2Findex.html%3FcorpId%3Ddinga6f137a9a2238a9635c2f4657eb6378f%26inviteCode%3DXW4gV4n4HWMrRF9%26dtaction%3Dos%26org_name%3D%25E8%25AF%25A0%25E8%25A7%2586%25E6%2596%2587%25E5%258C%2596'
</script>

<style scoped>
.join-page {
  min-height: 100vh;
  padding-bottom: 90px;
  background: var(--bg);
}
/* Hero：品牌渐变橙 + 光晕（对齐宣传页设计规范） */
.join-hero {
  background: linear-gradient(135deg, #fff7f0 0%, #ffe6d6 50%, #ffd6b3 100%);
  padding: 42px 20px 30px;
  text-align: center;
  position: relative;
  overflow: hidden;
}
.join-hero::before {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(ellipse at 70% 50%, rgba(255, 106, 0, 0.08) 0%, transparent 60%);
  pointer-events: none;
}
.join-hero > * {
  position: relative;
  z-index: 1;
}
.join-hero .badge {
  display: inline-block;
  background: var(--card);
  color: var(--primary);
  padding: 4px 16px;
  border-radius: 20px;
  font-size: 0.72rem;
  border: 1px solid rgba(255, 106, 0, 0.15);
  margin-bottom: 14px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
}
.join-hero h1 {
  font-size: 1.55rem;
  font-weight: 700;
  color: var(--fg);
  line-height: 1.35;
}
.join-hero h1 .hl {
  color: var(--primary);
}
.join-hero .subtitle {
  font-size: 0.82rem;
  color: var(--fg2);
  margin-top: 8px;
}
.join-hero .tagline {
  font-size: 0.74rem;
  color: var(--fg3);
  margin-top: 6px;
  line-height: 1.6;
}
.join-hero .stats {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 18px;
}
.join-hero .stats > div {
  padding: 6px 14px;
}
.join-hero .stats .num {
  font-size: 1.2rem;
  font-weight: 700;
  color: var(--primary);
}
.join-hero .stats .k {
  font-size: 0.66rem;
  color: var(--fg3);
  margin-top: 2px;
}
.join-body {
  padding: 16px;
}
.join-intro {
  background: var(--card);
  border-radius: 12px;
  padding: 14px 16px;
  font-size: 0.8rem;
  color: var(--fg2);
  line-height: 1.7;
  border: 1px solid rgba(0, 0, 0, 0.04);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
}
.join-intro .hl {
  color: var(--primary);
  font-weight: 600;
}
.join-intro .strong {
  color: var(--fg);
  font-weight: 600;
}
.join-values {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-top: 12px;
}
.value-c {
  background: var(--card);
  border-radius: 12px;
  padding: 14px 12px;
  border: 1px solid rgba(0, 0, 0, 0.04);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
}
.value-c .icon {
  font-size: 1.3rem;
}
.value-c h4 {
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--fg);
  margin-top: 6px;
}
.value-c p {
  font-size: 0.68rem;
  color: var(--fg3);
  line-height: 1.5;
  margin-top: 4px;
}
.join-steps {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.step {
  display: flex;
  align-items: center;
  gap: 12px;
  background: var(--card);
  border-radius: 12px;
  padding: 12px 14px;
  border: 1px solid rgba(0, 0, 0, 0.04);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
}
.step .n {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: var(--primary-soft);
  color: var(--primary-deep);
  font-weight: 700;
  font-size: 0.82rem;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.step .tx b {
  font-size: 0.82rem;
  color: var(--fg);
}
.step .tx p {
  font-size: 0.7rem;
  color: var(--fg3);
  margin-top: 2px;
}
.join-vision {
  text-align: center;
  margin-top: 16px;
  font-size: 0.74rem;
  color: var(--fg3);
  line-height: 1.8;
}
.join-vision .tags {
  color: var(--fg3);
  font-size: 0.68rem;
  opacity: 0.7;
}

/* ===== PC 端（≥768px）：hero 更大气，内容限宽居中，避免全宽稀疏 ===== */
@media (min-width: 768px) {
  .join-hero {
    padding: 64px 20px 48px;
  }
  .join-hero h1 {
    font-size: 2.1rem;
  }
  .join-hero .subtitle {
    font-size: 0.95rem;
  }
  .join-hero .tagline {
    font-size: 0.85rem;
  }
  .join-hero .stats {
    gap: 48px;
    margin-top: 24px;
  }
  .join-hero .stats .num {
    font-size: 1.6rem;
  }
  .join-hero .stats .k {
    font-size: 0.74rem;
  }

  /* 内容区限宽居中（App.vue 壳层已是 1200px 容器，这里再收窄让行宽舒适、信息密集） */
  .join-body {
    max-width: 760px;
    margin: 0 auto;
    padding: 24px 16px 40px;
  }
  .join-intro {
    font-size: 0.9rem;
    padding: 18px 20px;
  }
  .value-c {
    padding: 18px 16px;
  }
  .value-c h4 {
    font-size: 0.9rem;
  }
  .value-c p {
    font-size: 0.76rem;
  }
  .step {
    padding: 14px 18px;
  }
  .step .tx b {
    font-size: 0.9rem;
  }
  .step .tx p {
    font-size: 0.76rem;
  }
  .join-vision {
    font-size: 0.82rem;
    margin-top: 24px;
  }
  .join-vision .tags {
    font-size: 0.74rem;
  }
}
.join-cta {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: var(--card);
  padding: 10px 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-top: 1px solid var(--border);
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.06);
}
@media (min-width: 768px) {
  .join-cta {
    max-width: 1200px;
    margin: 0 auto;
  }
}
.cta-btn {
  display: inline-block;
  background: var(--primary);
  color: var(--card);
  border: none;
  padding: 8px 24px;
  border-radius: 8px;
  font-weight: 600;
  font-size: 0.8rem;
  cursor: pointer;
  text-decoration: none;
  /* 按钮文字居中（PC 下 width:100% 限宽后文字靠左的问题） */
  text-align: center;
}
@media (min-width: 768px) {
  .cta-btn {
    max-width: 320px;
    width: 100%;
    padding: 10px 24px;
  }
}
.cta-btn:hover {
  background: var(--primary-deep);
}
.cta-btn:active {
  opacity: 0.9;
}
</style>
