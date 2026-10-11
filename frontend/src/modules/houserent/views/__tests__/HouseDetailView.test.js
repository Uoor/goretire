import { afterEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { nextTick } from 'vue'

// setup 里会读 useRoute().params.id，onMounted 会请求详情接口，所以两者都要 mock。
// vi.hoisted 让 fixture 在 vi.mock 工厂之前就绪（vi.mock 会被提升到 import 之前）。
const { house } = vi.hoisted(() => ({
  house: {
    id: 3,
    community: '西溪八方城',
    houseType: '2室1厅',
    area: 89,
    rent: 5800,
    depositPay: '押一付三',
    label: 1,
    rackStatus: 1,
    title: '',
    // 9 张（产品上限），用于验证宫格只铺 5 块 + 末格 +N
    images: JSON.stringify(Array.from({ length: 9 }, (_, i) => `https://x/p${i}.jpg`))
  }
}))

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { id: '3' } }),
  useRouter: () => ({ push: vi.fn() })
}))

vi.mock('@/modules/houserent/api', () => ({
  houseApi: {
    detail: () => Promise.resolve(house),
    contact: () => Promise.resolve({ staffId: 's1', nickname: '房东' }),
    report: () => Promise.resolve()
  }
}))

import HouseDetailView from '@/modules/houserent/views/HouseDetailView.vue'

// 详情页用到但测试环境未注册的 Vant 组件
const stubs = {
  'van-skeleton': true,
  'van-popup': true,
  'van-field': true
}

function setViewport(width) {
  Object.defineProperty(window, 'innerWidth', { writable: true, configurable: true, value: width })
}

let wrapper

async function mountAt(width) {
  setViewport(width)
  wrapper = mount(HouseDetailView, { global: { stubs } })
  await flushPromises()
  return wrapper
}

const lightbox = () => document.querySelector('.pc-lightbox')

afterEach(() => {
  wrapper?.unmount()
  wrapper = undefined
  document.body.style.overflow = ''
})

describe('HouseDetailView 照片墙', () => {
  it('PC 端（≥768px）渲染宫格：9 张图只铺 5 块瓦片，末格盖 +4', async () => {
    const w = await mountAt(1200)
    const tiles = w.findAll('.wall-tile')
    expect(tiles).toHaveLength(5)
    expect(w.find('.wall-more').text()).toBe('+4')
    expect(w.find('.photo-wall').classes()).toContain('wall-mosaic')
    // 宫格版不再渲染移动端那套轮播箭头与圆点
    expect(w.find('.hero-nav').exists()).toBe(false)
    expect(w.find('.dots').exists()).toBe(false)
  })

  it('PC 端每块瓦片是带 aria-label 的可聚焦按钮', async () => {
    const w = await mountAt(1200)
    const first = w.findAll('.wall-tile')[0]
    expect(first.element.tagName).toBe('BUTTON')
    expect(first.attributes('aria-label')).toBe('查看第 1 张照片，共 9 张')
  })

  it('PC 端点击瓦片打开灯箱：定位到该张、显示计数、锁住页面滚动、焦点进入灯箱', async () => {
    const w = await mountAt(1200)
    await w.findAll('.wall-tile')[1].trigger('click')
    await nextTick()
    const box = lightbox()
    expect(box).toBeTruthy()
    expect(box.querySelector('.lb-img').getAttribute('src')).toBe('https://x/p1.jpg')
    expect(box.querySelector('.lb-count').textContent).toBe('2 / 9')
    expect(document.body.style.overflow).toBe('hidden')
    // 焦点必须进到弹层里，否则 Tab 会跑到背后的详情页
    await flushPromises()
    expect(document.activeElement).toBe(box)
  })

  it('PC 端灯箱支持键盘 ←/→ 循环翻图、Esc 关闭', async () => {
    const w = await mountAt(1200)
    await w.findAll('.wall-tile')[0].trigger('click')
    await nextTick()
    expect(lightbox().querySelector('.lb-count').textContent).toBe('1 / 9')

    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowRight' }))
    await nextTick()
    expect(lightbox().querySelector('.lb-count').textContent).toBe('2 / 9')

    // 从第 1 张往左应回到最后一张（与移动端轮播同样取模）
    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowLeft' }))
    await nextTick()
    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowLeft' }))
    await nextTick()
    expect(lightbox().querySelector('.lb-count').textContent).toBe('9 / 9')

    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))
    await nextTick()
    expect(lightbox()).toBeFalsy()
    expect(document.body.style.overflow).toBe('')
  })

  it('PC 端点遮罩关闭灯箱', async () => {
    const w = await mountAt(1200)
    await w.findAll('.wall-tile')[0].trigger('click')
    await nextTick()
    lightbox().dispatchEvent(new MouseEvent('click', { bubbles: true }))
    await nextTick()
    expect(lightbox()).toBeFalsy()
  })

  it('移动端（<768px）仍是单图轮播，不出现宫格', async () => {
    const w = await mountAt(375)
    expect(w.find('.photo-wall').exists()).toBe(false)
    expect(w.findAll('.wall-tile')).toHaveLength(0)
    expect(w.findAll('.hero-nav')).toHaveLength(2)
    expect(w.find('.dots').exists()).toBe(true)
  })

  it('只有 1 张图时 PC 端单张满铺，无箭头', async () => {
    house.images = JSON.stringify(['https://x/only.jpg'])
    const w = await mountAt(1200)
    expect(w.find('.photo-wall').classes()).toContain('wall-single')
    expect(w.findAll('.wall-tile')).toHaveLength(1)
    expect(w.find('.wall-more').exists()).toBe(false)
    // 复原，避免影响其它用例
    house.images = JSON.stringify(Array.from({ length: 9 }, (_, i) => `https://x/p${i}.jpg`))
  })
})
