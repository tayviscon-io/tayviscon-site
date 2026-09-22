import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import CourseOutline from './CourseOutline.vue'

describe('CourseOutline', () => {
  it('keeps nested levels closed until the parent is opened', async () => {
    const wrapper = mount(CourseOutline, {
      props: {
        node: {
          title: 'Основы SQL и баз данных',
          children: [
            {
              title: 'Транзакции и блокировки',
              children: [{ title: 'Транзакции', children: [] }],
            },
          ],
        },
      },
    })
    const root = wrapper.get('details')
    expect(wrapper.get('summary').text()).toBe('Основы SQL и баз данных')
    expect(root.element.open).toBe(false)
    await wrapper.get('summary').trigger('click')
    expect(root.element.open).toBe(true)
    const nested = wrapper.findAll('details')[1]
    expect(nested.get('summary').text()).toBe('Транзакции и блокировки')
    expect(nested.element.open).toBe(false)
    await nested.get('summary').trigger('click')
    expect(nested.element.open).toBe(true)
    expect(nested.text()).toContain('Транзакции')
  })
})
