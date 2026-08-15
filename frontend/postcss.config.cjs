module.exports = {
  plugins: {
    // 移动端适配：375 设计稿 → vw（钉钉容器内全机型适配）
    'postcss-px-to-viewport': {
      viewportWidth: 375,
      unitPrecision: 5,
      viewportUnit: 'vw',
      selectorBlackList: ['.ignore-vw'],
      minPixelValue: 1,
      mediaQuery: false
    }
  }
}
