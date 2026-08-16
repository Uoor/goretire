<template>
  <div class="join-page">
    <!-- Hero（参考社群宣传页结构：badge + 标题 + 副标题 + 数据） -->
    <div class="join-hero">
      <div class="badge">🤝 互助型 · 校友社群</div>
      <h1>校友<span class="hl">安居</span></h1>
      <p class="subtitle">阿里&蚂蚁在职/离职校友专属租房网络</p>
      <p class="tagline">直租免中介 · 身份互见 · 真实房源 · 互相托底</p>
      <div class="stats">
        <div><div class="num">6000+</div><div class="k">群成员</div></div>
        <div><div class="num">直租</div><div class="k">免中介</div></div>
        <div><div class="num">已核实</div><div class="k">房源审核</div></div>
      </div>
    </div>

    <div class="join-body">
      <div class="join-intro">
        <p>「校友安居」只面向<span class="hl">阿里 / 蚂蚁</span>在职与离职校友开放。</p>
        <p>你当前使用的账号<span class="strong">不在校友社群组织中</span>，暂时无法使用租房服务。</p>
      </div>

      <div class="join-steps">
        <div class="step">
          <span class="n">1</span>
          <div class="tx"><b>查看入群指南</b><p>了解如何加入「阿里人·一起提前退休」社群</p></div>
        </div>
        <div class="step">
          <span class="n">2</span>
          <div class="tx"><b>加入校友组织</b><p>入群后按指引加入组织，校友身份即登录</p></div>
        </div>
        <div class="step">
          <span class="n">3</span>
          <div class="tx"><b>回到安居</b><p>重新打开本应用，即可使用租房服务</p></div>
        </div>
      </div>
    </div>

    <!-- CTA 栏（底部固定，橙色主按钮） -->
    <div class="join-cta">
      <span>🏠 查看【提前退休】入群指南 → 自动进入全员群</span>
      <a
        class="cta-btn"
        :href="guideUrl"
        target="_blank"
        rel="noopener"
      >查看入群指南 📖</a>
    </div>
  </div>
</template>

<script setup>
// 入群指南文档链接（钉钉云文档，与社群宣传页一致）
const guideUrl = 'https://alidocs.dingtalk.com/i/p/O1pMzN6O07ezBnePqWXw3p83wOVloX31?dontjump=true'
</script>

<style scoped>
.join-page {
  min-height: 100vh;
  padding-bottom: 90px;
  background: #f5f5f4;
}
.join-hero {
  background: linear-gradient(135deg, #fff7f0 0%, #ffe6d6 50%, #ffd6b3 100%);
  padding: 44px 20px 32px;
  text-align: center;
}
.join-hero .badge {
  display: inline-block;
  background: #fff;
  color: var(--primary);
  padding: 4px 16px;
  border-radius: 20px;
  font-size: 0.72rem;
  border: 1px solid rgba(255, 106, 0, 0.15);
  margin-bottom: 14px;
}
.join-hero h1 {
  font-size: 1.7rem;
  font-weight: 700;
  color: var(--fg);
}
.join-hero h1 .hl {
  color: var(--primary);
}
.join-hero .subtitle {
  font-size: 0.9rem;
  color: rgba(0, 0, 0, 0.55);
  margin-top: 6px;
}
.join-hero .tagline {
  font-size: 0.78rem;
  color: rgba(0, 0, 0, 0.45);
  margin-top: 4px;
}
.join-hero .stats {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 20px;
}
.join-hero .stats > div {
  padding: 6px 16px;
}
.join-hero .stats .num {
  font-size: 1.15rem;
  font-weight: 700;
  color: var(--primary);
}
.join-hero .stats .k {
  font-size: 0.68rem;
  color: rgba(0, 0, 0, 0.4);
  margin-top: 2px;
}
.join-body {
  padding: 18px 16px;
}
.join-intro {
  background: #fff;
  border-radius: 12px;
  padding: 14px 16px;
  font-size: 0.8rem;
  color: var(--fg2);
  line-height: 1.7;
}
.join-intro .hl {
  color: var(--primary);
  font-weight: 600;
}
.join-intro .strong {
  color: var(--fg);
  font-weight: 600;
}
.join-steps {
  margin-top: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.step {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #fff;
  border-radius: 12px;
  padding: 12px 14px;
  border: 1px solid var(--border);
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
.join-cta {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  border-top: 1px solid var(--border);
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.06);
}
.join-cta span {
  font-size: 0.78rem;
  color: var(--fg2);
}
.cta-btn {
  background: var(--primary);
  color: #fff;
  border: none;
  padding: 10px 28px;
  border-radius: 8px;
  font-weight: 600;
  font-size: 0.85rem;
  cursor: pointer;
  text-decoration: none;
  box-shadow: 0 2px 0 var(--primary-deep);
}
.cta-btn:active {
  transform: translateY(1px);
  box-shadow: none;
}
</style>
