import { describe, expect, it } from 'vitest'
import { formatMoney, parseImages, timeAgo } from '@/utils/format'

describe('parseImages', () => {
  it('解析 JSON 数组字符串', () => {
    expect(parseImages('["/a.jpg","/b.jpg"]')).toEqual(['/a.jpg', '/b.jpg'])
  })
  it('解析逗号分隔', () => {
    expect(parseImages('/a.jpg, /b.jpg')).toEqual(['/a.jpg', '/b.jpg'])
  })
  it('空值返回空数组', () => {
    expect(parseImages(null)).toEqual([])
    expect(parseImages('')).toEqual([])
  })
  it('已是数组原样返回', () => {
    expect(parseImages(['/a.jpg'])).toEqual(['/a.jpg'])
  })
})

describe('formatMoney', () => {
  it('千分位格式化', () => {
    expect(formatMoney(5800)).toBe('5,800')
    expect(formatMoney(5800.5)).toBe('5,800.5')
  })
  it('空值返回空串', () => {
    expect(formatMoney(null)).toBe('')
    expect(formatMoney(undefined)).toBe('')
  })
})

describe('timeAgo', () => {
  it('今天', () => {
    expect(timeAgo(new Date().toISOString())).toBe('今天')
  })
  it('昨天', () => {
    const y = new Date(Date.now() - 86400000)
    expect(timeAgo(y.toISOString())).toBe('昨天')
  })
  it('N 天前', () => {
    const d = new Date(Date.now() - 5 * 86400000)
    expect(timeAgo(d.toISOString())).toBe('5 天前')
  })
  it('空值', () => {
    expect(timeAgo('')).toBe('')
  })
})
