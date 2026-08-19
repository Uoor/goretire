<template>
  <div class="login-page">
    <div class="login-card">
      <div class="logo">
        <i class="ph ph-house-line"></i>
      </div>
      <h1>校友安居</h1>
      <p class="subtitle">校友专属租房服务</p>
      
      <div class="features">
        <div class="feature">
          <i class="ph ph-shield-check"></i>
          <span>真实校友身份验证</span>
        </div>
        <div class="feature">
          <i class="ph ph-house"></i>
          <span>精选优质房源</span>
        </div>
        <div class="feature">
          <i class="ph ph-lock"></i>
          <span>数据安全有保障</span>
        </div>
      </div>

      <div class="privacy-notice">
        <i class="ph ph-info"></i>
        <span>登录通过钉钉官方授权，数据仅在钉钉体系内流转</span>
      </div>

      <button class="login-btn" @click="login">
        <i class="ph ph-qr-code"></i>
        钉钉扫码登录
      </button>

      <p class="hint">
        仅限阿里/蚂蚁校友使用<br>
        非校友请<a href="https://www.alibaba.com" target="_blank">了解更多</a>
      </p>
    </div>
  </div>
</template>

<script setup>
import { redirectToDingTalkOAuth } from '@/utils/dd'

function login() {
  redirectToDingTalkOAuth()
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.login-card {
  background: white;
  border-radius: 20px;
  padding: 40px 30px;
  max-width: 400px;
  width: 100%;
  text-align: center;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.logo {
  width: 80px;
  height: 80px;
  margin: 0 auto 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo i {
  font-size: 40px;
  color: white;
}

h1 {
  font-size: 28px;
  font-weight: 600;
  margin: 0 0 8px;
  color: #333;
}

.subtitle {
  font-size: 14px;
  color: #666;
  margin: 0 0 30px;
}

.features {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 30px;
}

.feature {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: #f8f9fa;
  border-radius: 12px;
  text-align: left;
}

.feature i {
  font-size: 24px;
  color: #667eea;
  flex-shrink: 0;
}

.feature span {
  font-size: 14px;
  color: #333;
}

.privacy-notice {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: #e8f5e9;
  border-radius: 12px;
  margin-bottom: 24px;
  text-align: left;
}

.privacy-notice i {
  font-size: 20px;
  color: #4caf50;
  flex-shrink: 0;
}

.privacy-notice span {
  font-size: 13px;
  color: #2e7d32;
  line-height: 1.4;
}

.login-btn {
  width: 100%;
  padding: 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: transform 0.2s, box-shadow 0.2s;
}

.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(102, 126, 234, 0.4);
}

.login-btn:active {
  transform: translateY(0);
}

.login-btn i {
  font-size: 20px;
}

.hint {
  margin-top: 20px;
  font-size: 12px;
  color: #999;
  line-height: 1.6;
}

.hint a {
  color: #667eea;
  text-decoration: none;
}

.hint a:hover {
  text-decoration: underline;
}

@media (max-width: 480px) {
  .login-card {
    padding: 30px 20px;
  }
  
  h1 {
    font-size: 24px;
  }
}
</style>
