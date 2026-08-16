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
        <p>你当前使用的账号<span class="strong">不在校友社群组织中</span>，加入后即可使用租房服务。</p>
      </div>

      <!-- 一键加入：直接跳邀请/指南链接 -->
      <a class="join-now" :href="guideUrl" target="_blank" rel="noopener">
        <span class="jn-icon">🎟️</span>
        <span class="jn-tx">
          <b>一键加入校友社群</b>
          <small>点击后按指引申请加入组织</small>
        </span>
        <i class="ph ph-arrow-up-right"></i>
      </a>

      <div class="join-steps">
        <div class="step">
          <span class="n">1</span>
          <div class="tx"><b>点击上方一键加入</b><p>申请加入「阿里人·一起提前退休」社群组织</p></div>
        </div>
        <div class="step">
          <span class="n">2</span>
          <div class="tx"><b>回到安居</b><p>重新打开本应用，校友身份即自动登录</p></div>
        </div>
      </div>
    </div>

    <!-- CTA 栏（底部固定，一键加入主按钮） -->
    <div class="join-cta">
      <a class="cta-btn" :href="guideUrl" target="_blank" rel="noopener">
        🎟️ 一键加入社群
      </a>
    </div>
  </div>
</template>

<script setup>
// 加入入口链接：钉钉组织邀请链接或入群指南文档（管理员生成邀请链接后替换此值）
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
.join-now {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 14px;
  padding: 16px 14px;
  background: linear-gradient(135deg, var(--primary), var(--primary-deep));
  border-radius: 14px;
  text-decoration: none;
  box-shadow: 0 3px 10px rgba(255, 106, 0, 0.25);
}
.join-now .jn-icon {
  font-size: 1.5rem;
}
.join-now .jn-tx {
  flex: 1;
  color: #fff;
}
.join-now .jn-tx b {
  display: block;
  font-size: 0.95rem;
}
.join-now .jn-tx small {
  font-size: 0.72rem;
  opacity: 0.85;
}
.join-now > i {
  color: #fff;
  font-size: 1.1rem;
}
.join-now:active {
  transform: translateY(1px);
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
  align-items: center;
  justify-content: center;
  border-top: 1px solid var(--border);
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.06);
}
.cta-btn {
  background: var(--primary);
  color: #fff;
  border: none;
  padding: 13px 40px;
  border-radius: 10px;
  font-weight: 700;
  font-size: 0.92rem;
  cursor: pointer;
  text-decoration: none;
  box-shadow: 0 2px 0 var(--primary-deep);
}
.cta-btn:active {
  transform: translateY(1px);
  box-shadow: none;
}
</style>
