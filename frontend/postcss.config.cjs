module.exports = {
  plugins: {
    // px → rem 转换：rootValue=16 表示 16px=1rem。
    // PC 端 html { font-size: 16px }，rem 值 = 原始 px 值，样式可预测。
    // 移动端可配合动态 root font-size 实现等比缩放（当前不做，保持固定）。
    'postcss-pxtorem': {
      rootValue: 16,            // 16px = 1rem
      propList: ['*'],          // 转换所有属性
      selectorBlackList: [/^body$/, /^#app$/, '.ignore-pxtorem'],
      replace: true,
      mediaQuery: false,        // 不转换 @media 内的 px
      minPixelValue: 2,         // 跳过 1px 边框
    },
  },
}
