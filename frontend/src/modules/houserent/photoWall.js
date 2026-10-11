/**
 * PC 照片墙：图片数量 → 布局分支。
 *
 * 移动端是单图轮播（一次看一张），PC 端改为宫格一次看全。产品约束：最多 9 张，
 * 第一张为主图（见 PublishView 的「最多 9 张，第一张为主图」）。
 *
 * 抽成纯函数是为了让"几张图用什么布局"这条规则可单测，模板只负责映射成 CSS 类。
 *
 * @param {number} count 图片数量
 * @returns {{ kind: 'empty'|'single'|'two'|'three'|'four'|'mosaic', visible: number, overflow: number }}
 *   kind    布局分支，对应样式类 `wall-<kind>`
 *   visible 实际渲染成瓦片的图片数
 *   overflow 超出可见瓦片、改用 "+N" 覆盖层表达的数量
 */
export function photoWallLayout(count) {
  const n = Number.isFinite(count) && count > 0 ? Math.floor(count) : 0

  if (n === 0) return { kind: 'empty', visible: 0, overflow: 0 }
  if (n === 1) return { kind: 'single', visible: 1, overflow: 0 }
  if (n === 2) return { kind: 'two', visible: 2, overflow: 0 }
  if (n === 3) return { kind: 'three', visible: 3, overflow: 0 }
  if (n === 4) return { kind: 'four', visible: 4, overflow: 0 }

  // ≥5：左 1 大（跨 2 行）+ 右侧 2×2 四格，第 5 格起盖 +N
  return { kind: 'mosaic', visible: 5, overflow: n - 5 }
}
