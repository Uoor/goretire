import { describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import FilterChips from '@/modules/houserent/components/FilterChips.vue'

const chips = [
  { key: 'label1', label: '房东直租' },
  { key: 'pet', label: '可养宠' }
]

describe('FilterChips', () => {
  it('渲染 chips 且未选中态', () => {
    const wrapper = mount(FilterChips, { props: { chips, modelValue: '' } })
    expect(wrapper.findAll('.chip')).toHaveLength(2)
    expect(wrapper.find('.chip.on').exists()).toBe(false)
  })

  it('选中态高亮', () => {
    const wrapper = mount(FilterChips, { props: { chips, modelValue: 'pet' } })
    expect(wrapper.find('.chip.on').text()).toBe('可养宠')
  })

  it('点击未选中项 emit 该 key', async () => {
    const wrapper = mount(FilterChips, { props: { chips, modelValue: '' } })
    await wrapper.findAll('.chip')[1].trigger('click')
    expect(wrapper.emitted('update:modelValue')[0]).toEqual(['pet'])
  })

  it('点击已选中项取消选择', async () => {
    const wrapper = mount(FilterChips, { props: { chips, modelValue: 'pet' } })
    await wrapper.findAll('.chip')[1].trigger('click')
    expect(wrapper.emitted('update:modelValue')[0]).toEqual([''])
  })
})
