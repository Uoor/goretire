import { createRouter, createWebHashHistory } from 'vue-router'
import { getAuthCode, configDingtalk, isDingTalk } from '@/utils/dd'
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
  { path: '/admin', name: 'admin', component: () => import('@/modules/houserent/views/admin/AdminView.vue'), meta: { title: '管理后台' } },
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
  // 钉钉容器内先 dd.config 授权 JSAPI，再取免登 code；浏览器联调直接跳过
  await configDingtalk()
  const code = await getAuthCode()
  const data = await authApi.login(code)
  store.setSession(data.token, data.user)
}

router.beforeEach(async (to) => {
  document.title = to.meta.title ? `校友安居 · ${to.meta.title}` : '校友安居'
  try {
    await ensureLogin()
  } catch (e) {
    // 免登失败：放行到页面，页面级请求会 401 提示（如钉钉容器内 http 环境被拦）
    console.warn('[router] login failed:', e)
  }
  // 管理后台角色校验（dev-code 种子用户为管理员，可直接访问）
  if (to.meta.admin && !useUserStore().isAdmin) {
    return { name: 'home' }
  }
  return true
})

export default router
