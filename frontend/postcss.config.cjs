module.exports = {
  plugins: {
    // 移动端适配：375 设计稿，移动端转 vw，桌面端自动保留固定 px 并限制最大宽度
    'postcss-mobile-forever': {
      viewportWidth: 375,
      mobileUnit: 'vw',
      unitPrecision: 5,
      // 桌面端最大显示宽度（超过后按 1480px 基准计算缩放，内容不随视口无限放大）
      maxDisplayWidth: 1480,
      // 媒体查询断点（≥768px 启用桌面布局，固定 px 值）
      mobileMediaQuery: '(min-width: 768px)',
      // appSelector 配合 maxDisplayWidth 使用，让 #app 容器宽度与内容同步
      appSelector: '#app',
      // 特定选择器排除（不转换）
      selectorBlackList: ['.ignore-mobile-forever'],
      // 页面级排除（管理后台等单独适配）
      pageMaxWidthMap: {},
    },
  },
}
