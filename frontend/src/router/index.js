import { createRouter, createWebHashHistory } from 'vue-router'

// hash 模式：钉钉容器内刷新不 404
const routes = [
  { path: '/', name: 'home', component: () => import('@/views/HomeView.vue'), meta: { title: '首页' } },
  { path: '/house/:id', name: 'house-detail', component: () => import('@/views/HouseDetailView.vue'), meta: { title: '房源详情' } },
  { path: '/demand', name: 'demand', component: () => import('@/views/DemandView.vue'), meta: { title: '求租' } },
  { path: '/publish', name: 'publish', component: () => import('@/views/PublishView.vue'), meta: { title: '发布' } },
  { path: '/subscribe', name: 'subscribe', component: () => import('@/views/SubscribeView.vue'), meta: { title: '订阅' } },
  { path: '/me', name: 'me', component: () => import('@/views/MeView.vue'), meta: { title: '我的' } },
  { path: '/admin', name: 'admin', component: () => import('@/admin/AdminView.vue'), meta: { title: '管理后台', admin: true } },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

// 守卫骨架：当前仅设置标题；
// TODO(免登接入): 无 token → 触发钉钉免登；meta.admin 路由校验 role==1
router.beforeEach((to) => {
  document.title = to.meta.title ? `校友安居 · ${to.meta.title}` : '校友安居'
  return true
})

export default router
