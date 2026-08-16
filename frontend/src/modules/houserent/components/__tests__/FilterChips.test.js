import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import FilterChips from '@/modules/houserent/components/FilterChips.vue'

const chips = [
  { group: 'label', key: 'label1', label: '房东直租' },
  { group: 'price', key: 'p-3000', label: '3000以下' },
  { group: 'pet', key: 'pet', label: '可养宠' }
]

describe('FilterChips', () => {
  it('按分组聚合渲染入口（不逐个平铺）', () => {
    const wrapper = mount(FilterChips, { props: { chips, modelValue: {} } })
    const items = wrapper.findAll('.f-item')
    expect(items).toHaveLength(3)
    expect(wrapper.findAll('.chip').length).toBe(0)
  })

  it('已选组高亮并显示选中值', () => {
    const wrapper = mount(FilterChips, { props: { chips, modelValue: { price: 'p-3000' } } })
    const priceItem = wrapper.findAll('.f-item')[1]
    expect(priceItem.classes()).toContain('on')
    expect(priceItem.find('.f-val').text()).toBe('3000以下')
  })

  it('点击入口打开弹层，选 option 触发该组选择', async () => {
    const wrapper = mount(FilterChips, { props: { chips, modelValue: { label: 'label1' } } })
    await wrapper.findAll('.f-item')[1].trigger('click')
    // 弹层里展示 price 组的全部选项
    const options = wrapper.findAll('.s-option')
    expect(options).toHaveLength(1)
    expect(options[0].text()).toContain('3000以下')
    await options[0].trigger('click')
    expect(wrapper.emitted('update:modelValue')[0]).toEqual([
      { label: 'label1', price: 'p-3000' }
    ])
  })

  it('再次点击已选项取消该组选择', async () => {
    const wrapper = mount(FilterChips, {
      props: { chips, modelValue: { label: 'label1' } }
    })
    await wrapper.findAll('.f-item')[0].trigger('click')
    const options = wrapper.findAll('.s-option')
    await options[0].trigger('click')
    expect(wrapper.emitted('update:modelValue')[0]).toEqual([{}])
  })

  it('清空按钮清除所有筛选', async () => {
    const wrapper = mount(FilterChips, {
      props: { chips, modelValue: { label: 'label1', price: 'p-3000' } }
    })
    await wrapper.find('.f-clear').trigger('click')
    expect(wrapper.emitted('update:modelValue')[0]).toEqual([{}])
  })
})
