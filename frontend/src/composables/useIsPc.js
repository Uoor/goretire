import { onBeforeUnmount, ref } from 'vue'

/**
 * 断点与样式里的 `@media (min-width: 768px)` 保持一致：
 * 见 src/styles/base.css 与 src/App.vue 的 PC 壳。改这里就要同步改 CSS。
 */
export const PC_BREAKPOINT = 768

/**
 * 当前是否 PC 端（视口宽 ≥ 768px）。
 *
 * 初始值直接取真实视口，避免 PC 上先按移动端布局渲染一帧再切换（闪烁）；
 * 视口变化时同步更新，组件卸载时自动解绑监听。
 */
export function useIsPc() {
  const isPc = ref(window.innerWidth >= PC_BREAKPOINT)

  const update = () => {
    isPc.value = window.innerWidth >= PC_BREAKPOINT
  }

  window.addEventListener('resize', update)
  onBeforeUnmount(() => window.removeEventListener('resize', update))

  return isPc
}
