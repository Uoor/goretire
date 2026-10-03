import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import HouseCard from '@/modules/houserent/components/HouseCard.vue'

const baseHouse = {
  id: 1,
  community: '西溪八方城',
  houseType: '2室1厅',
  area: 89,
  rent: 5800,
  depositPay: '押一付三',
  label: 1,
  petOk: 1,
  commute: '西溪园区 15 分钟',
  images: '["https://x/h1.jpg"]',
  createdAt: new Date().toISOString()
}

describe('HouseCard', () => {
  it('渲染标题、价格与标签', () => {
    const wrapper = mount(HouseCard, { props: { house: baseHouse, index: 0 } })
    expect(wrapper.text()).toContain('西溪八方城')
    expect(wrapper.text()).toContain('2室1厅')
    expect(wrapper.text()).toContain('房东直租')
    expect(wrapper.text()).toContain('可养宠')
  })

  it('点击触发 click 事件', async () => {
    const wrapper = mount(HouseCard, { props: { house: baseHouse, index: 0 } })
    await wrapper.trigger('click')
    expect(wrapper.emitted('click')).toBeTruthy()
  })

  it('无图时显示渐变占位（不渲染 img）', () => {
    const wrapper = mount(HouseCard, { props: { house: { ...baseHouse, images: null }, index: 0 } })
    expect(wrapper.find('img').exists()).toBe(false)
  })

  it('价格角标使用千分位', () => {
    const wrapper = mount(HouseCard, { props: { house: baseHouse, index: 0 } })
    expect(wrapper.find('.pricetag').text()).toBe('5,800')
  })
})
