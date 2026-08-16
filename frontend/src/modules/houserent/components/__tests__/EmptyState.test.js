import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import EmptyState from '@/modules/houserent/components/EmptyState.vue'

describe('EmptyState', () => {
  it('渲染默认文案与图标', () => {
    const wrapper = mount(EmptyState)
    expect(wrapper.text()).toContain('暂无内容')
    expect(wrapper.find('i').classes()).toContain('ph-house-simple')
  })

  it('支持自定义文案与插槽', () => {
    const wrapper = mount(EmptyState, {
      props: { text: '暂无房源' },
      slots: { default: '<button>去发布</button>' }
    })
    expect(wrapper.text()).toContain('暂无房源')
    expect(wrapper.find('button').text()).toBe('去发布')
  })
})
