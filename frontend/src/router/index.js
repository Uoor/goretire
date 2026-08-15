import { createRouter, createWebHashHistory } from 'vue-router'

// 路由壳：懒加载业务模块页面（src/modules/<module>/views/）
// 未来新增业务模块：在此追加路由，页面放在对应模块目录
const routes = [
  { path: '/', name: 'home', component: () => import('@/modules/houserent/views/HomeView.vue'), meta: { title: '首页' } },
  { path: '/house/:id', name: 'house-detail', component: () => import('@/modules/houserent/views/HouseDetailView.vue'), meta: { title: '房源详情' } },
  { path: '/demand', name: 'demand', component: () => import('@/modules/houserent/views/DemandView.vue'), meta: { title: '求租' } },
  { path: '/publish', name: 'publish', component: () => import('@/modules/houserent/views/PublishView.vue'), meta: { title: '发布' } },
  { path: '/subscribe', name: 'subscribe', component: () => import('@/modules/houserent/views/SubscribeView.vue'), meta: { title: '订阅' } },
  { path: '/me', name: 'me', component: () => import('@/modules/houserent/views/MeView.vue'), meta: { title: '我的' } },
  { path: '/admin', name: 'admin', component: () => import('@/modules/houserent/views/admin/AdminView.vue'), meta: { title: '管理后台', admin: true } },
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
