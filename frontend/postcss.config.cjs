module.exports = {
  plugins: {
    // 移动端适配：375 设计稿，移动端竖屏用 vw。
    // max-vw-mode：content 在视口 ≤maxDisplayWidth 范围内按 vw 缩放，超出后固定不再放大。
    // PC 端内容宽度锁定在 750px 以内（不会像裸 vw 那样在 1920px 上放大 5 倍）。
    // 再配合自定义 @media (min-width:768px) CSS 布局（如多列网格），实现桌面端正常显示。
    'postcss-mobile-forever': {
      viewportWidth: 375,
      mobileUnit: 'vw',
      unitPrecision: 5,
      maxDisplayWidth: 750,          // 最大显示宽度（px），内容不随视口无限放大
      appSelector: '#app',           // 自动给 #app 加 max-width:750px + margin auto
      border: false,
      selectorBlackList: ['.ignore-mobile-forever'],
    },
  },
}
