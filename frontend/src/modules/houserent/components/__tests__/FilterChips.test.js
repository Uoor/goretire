import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import FilterChips from '@/modules/houserent/components/FilterChips.vue'

const chips = [
  { group: 'label', key: 'label1', label: '房东直租' },
  { group: 'price', key: 'p-3000', label: '3000以下' },
  { group: 'pet', key: 'pet', label: '可养宠' }
]

describe('FilterChips', () => {
  it('渲染 chips 且未选中态', () => {
    const wrapper = mount(FilterChips, { props: { chips, modelValue: {} } })
    expect(wrapper.findAll('.chip')).toHaveLength(3)
    expect(wrapper.find('.chip.on').exists()).toBe(false)
  })

  it('同组选中高亮', () => {
    const wrapper = mount(FilterChips, { props: { chips, modelValue: { price: 'p-3000' } } })
    expect(wrapper.find('.chip.on').text()).toBe('3000以下')
  })

  it('点击未选中项 emit 该组选择（不同组互不影响）', async () => {
    const wrapper = mount(FilterChips, {
      props: { chips, modelValue: { label: 'label1' } }
    })
    await wrapper.findAll('.chip')[1].trigger('click')
    expect(wrapper.emitted('update:modelValue')[0]).toEqual([
      { label: 'label1', price: 'p-3000' }
    ])
  })

  it('点击已选中项取消该组选择', async () => {
    const wrapper = mount(FilterChips, {
      props: { chips, modelValue: { label: 'label1' } }
    })
    await wrapper.findAll('.chip')[0].trigger('click')
    expect(wrapper.emitted('update:modelValue')[0]).toEqual([{}])
  })
})
