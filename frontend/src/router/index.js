import { createRouter, createWebHashHistory } from 'vue-router'
import { getAuthCode, configDingtalk, isDingTalk, redirectToDingTalkOAuth } from '@/utils/dd'
import { showDialog } from 'vant'
import { authApi } from '@/api'
import { useUserStore } from '@/store/user'

// 路由壳：懒加载业务模块页面（src/modules/<module>/views/）
// meta.tab = true 的页面显示底部 TabBar（见 App.vue）
const routes = [
  { path: '/', name: 'home', component: () => import('@/modules/houserent/views/HomeView.vue'), meta: { title: '首页', tab: true } },
  { path: '/house/:id', name: 'house-detail', component: () => import('@/modules/houserent/views/HouseDetailView.vue'), meta: { title: '房源详情' } },
  { path: '/demand', name: 'demand', component: () => import('@/modules/houserent/views/DemandView.vue'), meta: { title: '求租', tab: true } },
  { path: '/publish', name: 'publish', component: () => import('@/modules/houserent/views/PublishView.vue'), meta: { title: '发布' } },
  { path: '/subscribe', name: 'subscribe', component: () => import('@/modules/houserent/views/SubscribeView.vue'), meta: { title: '订阅', tab: true } },
  { path: '/me', name: 'me', component: () => import('@/modules/houserent/views/MeView.vue'), meta: { title: '我的', tab: true } },
  { path: '/guide', name: 'guide', component: () => import('@/modules/houserent/views/GuideView.vue'), meta: { title: '避坑指南' } },
  { path: '/report', name: 'report', component: () => import('@/modules/houserent/views/RentReportView.vue'), meta: { title: '租金周报' } },
  { path: '/admin', name: 'admin', component: () => import('@/modules/houserent/views/admin/AdminView.vue'), meta: { title: '管理后台', admin: true } },
  { path: '/join', name: 'join', component: () => import('@/modules/houserent/views/JoinView.vue'), meta: { title: '加入组织' } },
  // 登录引导页：说明应用 + 钉钉扫码登录按钮
  { path: '/login', name: 'login', component: () => import('@/modules/houserent/views/LoginView.vue'), meta: { title: '登录' } },
  // PC 浏览器引导页：测试期仅开放移动端，PC 浏览器统一引导手机扫码（plain = 不进 App 壳）
  { path: '/pc-guide', name: 'pc-guide', component: () => import('@/modules/houserent/views/PcGuideView.vue'), meta: { title: '请使用钉钉打开', plain: true } },
  // 群卡片/推送落地页：无 token 自动扫码，有 token 直接跳目标页（redirect 参数）
  { path: '/landing', name: 'landing', component: () => import('@/modules/houserent/views/LandingView.vue'), meta: { title: '跳转中' } },
  // OAuth2 扫码登录回调页：从 URL 参数读取 token 和用户信息，存入 store 后跳目标页
  { path: '/oauth-callback', name: 'oauth-callback', component: () => import('@/modules/houserent/views/OAuthCallbackView.vue'), meta: { title: '登录中' } },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

/**
 * 免登：
 * - 浏览器联调：取 dev-code 桩（后端放行），token 持久化后复用；
 * - 钉钉容器内：先 dd.config 授权 JSAPI，再 requestAuthCode 真实免登。
 *   关键：钉钉内若残留 dev-code 桩身份（浏览器联调遗留的 localStorage），
 *   必须清除后重新真实免登，否则 staffId 是 dev-code、单聊无法唤起。
 */
async function ensureLogin() {
  const store = useUserStore()
  const inDingTalk = isDingTalk()
  // 钉钉内：dev-code 桩身份（或旧 token 缺 dingtalkUserId 字段）一律重登
  if (inDingTalk && store.isLoggedIn) {
    const uid = store.userInfo?.dingtalkUserId
    if (!uid || uid === 'dev-code') store.clear()
  }
  if (store.isLoggedIn) return
  try {
    if (inDingTalk) {
      // 钉钉容器内：JSAPI 免登
      await configDingtalk()
      const code = await getAuthCode()
      const data = await authApi.login(code)
      store.setSession(data.token, data.user)
    } else {
      // 浏览器环境：跳登录引导页（说明应用 + 扫码按钮）
      return { name: 'login' }
    }
  } catch (e) {
    // 钉钉容器内免登失败（含换 userid 失败、requestAuthCode/JSAPI 被拦）：
    // 用户无法完成身份验证，统一标记引导「加入组织」页
    if (inDingTalk) {
      const err = e instanceof Error ? e : new Error(String(e))
      if (!err.__loginFailed) err.__loginFailed = true
      throw err
    }
    throw e
  }
}

router.beforeEach(async (to) => {
  document.title = to.meta.title ? `校友直租 · ${to.meta.title}` : '校友直租'
  // PC 浏览器（非钉钉容器）统一引导到扫码页：测试期仅开放移动端。
  // 钉钉容器内（含 PC 端钉钉）放行；VITE_PC_GUIDE_ENABLED=false 可关闭。
  if (import.meta.env.VITE_PC_GUIDE_ENABLED !== 'false' && to.name !== 'pc-guide') {
    const inDingTalk = isDingTalk()
    const pcViewport = typeof window !== 'undefined' && window.innerWidth >= 768
    if (!inDingTalk && pcViewport) {
      return { name: 'pc-guide' }
    }
  }
  // 加入组织页：已登录回首页；未登录直接放行（不再触发免登，避免死循环）
  if (to.name === 'join') {
    return useUserStore().isLoggedIn ? { name: 'home' } : true
  }
  // OAuth 回调页 / 落地页不检查登录（LandingView 自己处理扫码与跳转）
  if (to.name === 'oauth-callback' || to.name === 'landing') {
    return true
  }
  if (to.name === 'login') {
    return useUserStore().isLoggedIn ? { name: 'home' } : true
  }
  try {
    const result = await ensureLogin()
    if (result) return result
  } catch (e) {
    // 免登失败（非组织成员/JSAPI 被拦等）：先弹窗说明，用户确认后再引导加入组织
    if (e?.__loginFailed) {
      await showDialog({
        title: '校友专属服务',
        message: '「校友直租」是「阿里人·一起提前退休」社群专属的租房服务，需要先加入社群组织才能使用。',
        confirmButtonText: '查看如何加入',
        closeOnClickOverlay: false
      }).catch(() => {})
      return { name: 'join' }
    }
    console.warn('[router] login failed:', e)
  }
  // 管理后台角色校验（dev-code 种子用户为管理员，可直接访问）
  if (to.meta.admin && !useUserStore().isAdmin) {
    return { name: 'home' }
  }
  return true
})

export default router
