// 桌面宽视口（PC 钉钉 / 普通浏览器 ≥500px）适配：
// 页面按 375 设计稿用 vw 单位构建（postcss-px-to-viewport），在宽视口下
// 1vw 随屏幕变宽而放大，导致 PC 端交互错乱（元素巨大、定位错位）。
// 解法：给 #app 设置 CSS zoom = 375 / 视口宽，使整个页面（含 vw 元素与
// fixed 定位的 TabBar / van-popup）视觉等比缩回 375 设计稿宽度。
// 真机（<500px）不做处理，保持原生全屏。
import { onMounted, onBeforeUnmount } from 'vue'

const DESIGN_WIDTH = 375 // 与 postcss.config.cjs viewportWidth 一致
const DESKTOP_BREAKPOINT = 500

function applyDesktopZoom() {
  const app = document.getElementById('app')
  if (!app) return
  if (window.innerWidth >= DESKTOP_BREAKPOINT) {
    // zoom 后 #app 视觉宽度 = 视口宽 × zoom = 375（设计稿宽度）
    const zoom = DESIGN_WIDTH / window.innerWidth
    app.style.setProperty('--desktop-zoom', zoom.toFixed(6))
  } else {
    app.style.removeProperty('--desktop-zoom')
  }
}

/** 在任意组件 setup 中调用，自动随视口变化更新缩放 */
export function useDesktopZoom() {
  onMounted(() => {
    applyDesktopZoom()
    window.addEventListener('resize', applyDesktopZoom)
    window.addEventListener('orientationchange', applyDesktopZoom)
  })
  onBeforeUnmount(() => {
    window.removeEventListener('resize', applyDesktopZoom)
    window.removeEventListener('orientationchange', applyDesktopZoom)
  })
}
