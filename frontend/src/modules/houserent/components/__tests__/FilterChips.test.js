import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import FilterChips from '@/modules/houserent/components/FilterChips.vue'

// van-popup 在测试环境未注册：stub 为始终渲染插槽的容器，便于断言弹层内容
const popupStub = {
  name: 'VanPopup',
  template: '<div v-show="modelValue"><slot /></div>',
  props: ['modelValue', 'show', 'position', 'round', 'safeAreaInsetBottom']
}

const mountFC = (props) =>
  mount(FilterChips, {
    props,
    global: { stubs: { 'van-popup': popupStub } }
  })

const chips = [
  { group: 'label', key: 'label1', label: '房东直租' },
  { group: 'price', key: 'p-3000', label: '3000以下' },
  { group: 'pet', key: 'pet', label: '可养宠' }
]

describe('FilterChips', () => {
  it('按显示组聚合渲染入口（不逐个平铺）', () => {
    const wrapper = mountFC({ chips, modelValue: {} })
    const items = wrapper.findAll('.f-item')
    expect(items).toHaveLength(3)
    expect(wrapper.findAll('.chip').length).toBe(0)
  })

  it('pet/new 合并为一个「其他」入口', () => {
    const chips2 = [
      { group: 'pet', key: 'pet', label: '可养宠' },
      { group: 'new', key: 'new', label: '新上架' }
    ]
    const wrapper = mountFC({ chips: chips2, modelValue: {} })
    const items = wrapper.findAll('.f-item')
    expect(items).toHaveLength(1)
    expect(items[0].text()).toContain('其他')
  })

  it('已选显示组高亮并显示选中值', () => {
    const wrapper = mountFC({ chips, modelValue: { price: 'p-3000' } })
    const priceItem = wrapper.findAll('.f-item')[1]
    expect(priceItem.classes()).toContain('on')
    expect(priceItem.find('.f-val').text()).toBe('3000以下')
  })

  it('点击入口打开弹层，选 option 触发该组选择', async () => {
    const wrapper = mount(FilterChips, { props: { chips, modelValue: { label: 'label1' } } })
    await wrapper.findAll('.f-item')[1].trigger('click')
    const options = wrapper.findAll('.s-option')
    expect(options).toHaveLength(1)
    expect(options[0].text()).toContain('3000以下')
    await options[0].trigger('click')
    expect(wrapper.emitted('update:modelValue')[0]).toEqual([
      { label: 'label1', price: 'p-3000' }
    ])
  })

  it('再次点击已选项取消该组选择', async () => {
    const wrapper = mountFC({ chips, modelValue: { label: 'label1' } })
    await wrapper.findAll('.f-item')[0].trigger('click')
    const options = wrapper.findAll('.s-option')
    await options[0].trigger('click')
    expect(wrapper.emitted('update:modelValue')[0]).toEqual([{}])
  })

  it('清空按钮清除所有筛选', async () => {
    const wrapper = mountFC({ chips, modelValue: { label: 'label1', price: 'p-3000' } })
    await wrapper.find('.f-clear').trigger('click')
    expect(wrapper.emitted('update:modelValue')[0]).toEqual([{}])
  })

  it('「其他」弹层内 pet/new 可同时选中', async () => {
    const chips2 = [
      { group: 'pet', key: 'pet', label: '可养宠' },
      { group: 'new', key: 'new', label: '新上架' }
    ]
    const wrapper = mountFC({ chips: chips2, modelValue: {} })
    await wrapper.find('.f-item').trigger('click')
    const options = wrapper.findAll('.s-option')
    expect(options).toHaveLength(2)
    await options[0].trigger('click')
    expect(wrapper.emitted('update:modelValue')[0]).toEqual([{ pet: 'pet' }])
    // 模拟父组件 v-model 回传后，再选第二项（真实 app 中 HomeView 会回传）
    await wrapper.setProps({ modelValue: { pet: 'pet' } })
    await wrapper.findAll('.s-option')[1].trigger('click')
    expect(wrapper.emitted('update:modelValue')[1]).toEqual([{ pet: 'pet', new: 'new' }])
  })
})
