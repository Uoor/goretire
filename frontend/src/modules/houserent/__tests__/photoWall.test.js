import { describe, expect, it } from 'vitest'
import { photoWallLayout } from '@/modules/houserent/photoWall'

describe('photoWallLayout', () => {
  it('没有图片时走空态分支，不渲染任何瓦片', () => {
    expect(photoWallLayout(0)).toEqual({ kind: 'empty', visible: 0, overflow: 0 })
  })

  it('1~4 张各自一档布局，都不出现 +N', () => {
    expect(photoWallLayout(1)).toEqual({ kind: 'single', visible: 1, overflow: 0 })
    expect(photoWallLayout(2)).toEqual({ kind: 'two', visible: 2, overflow: 0 })
    expect(photoWallLayout(3)).toEqual({ kind: 'three', visible: 3, overflow: 0 })
    expect(photoWallLayout(4)).toEqual({ kind: 'four', visible: 4, overflow: 0 })
  })

  it('≥5 张只渲染 5 块瓦片，其余用 +N 表达（产品上限 9 张）', () => {
    expect(photoWallLayout(5)).toEqual({ kind: 'mosaic', visible: 5, overflow: 0 })
    expect(photoWallLayout(6)).toEqual({ kind: 'mosaic', visible: 5, overflow: 1 })
    expect(photoWallLayout(9)).toEqual({ kind: 'mosaic', visible: 5, overflow: 4 })
  })

  it('脏数据按空态或向下取整处理，不会产生负数/小数瓦片', () => {
    expect(photoWallLayout(undefined).kind).toBe('empty')
    expect(photoWallLayout(null).kind).toBe('empty')
    expect(photoWallLayout(-3).kind).toBe('empty')
    expect(photoWallLayout(NaN).kind).toBe('empty')
    expect(photoWallLayout(2.7)).toEqual({ kind: 'two', visible: 2, overflow: 0 })
  })
})
