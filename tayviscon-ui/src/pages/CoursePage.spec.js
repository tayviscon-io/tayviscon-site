import { afterEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import CoursePage from './CoursePage.vue'

function jsonResponse(status, body) {
  return Promise.resolve({
    ok: status >= 200 && status < 300,
    status,
    json: () => Promise.resolve(body),
  })
}

async function mountPage(id) {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/courses/:id', component: CoursePage },
      { path: '/courses', component: { template: '<div />' } },
      { path: '/login', component: { template: '<div />' } },
    ],
  })
  await router.push('/courses/' + id)
  await router.isReady()
  const wrapper = mount(CoursePage, {
    global: { plugins: [router] },
  })
  await flushPromises()
  return wrapper
}

describe('CoursePage', () => {
  let wrapper

  afterEach(() => {
    wrapper?.unmount()
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('shows title, TOC, IDE hint, and contain banner', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(() =>
        jsonResponse(200, {
          id: 'sql',
          title: 'Yet Another SQL Course',
          summary: 'Запросы, транзакции и схемы на PostgreSQL.',
          status: 'published',
          badge: 'доступен',
          logoUrl: '/api/courses/sql/logo',
          fallbackLetter: 'S',
          sections: ['Введение', 'SELECT', 'JOIN'],
          outline: [
            { title: 'Введение', children: [] },
            {
              title: 'SELECT',
              children: [{ title: 'WHERE', children: [] }],
            },
            { title: 'JOIN', children: [] },
          ],
          idePath: 'courses/sql/',
        }),
      ),
    )
    wrapper = await mountPage('sql')
    expect(wrapper.get('h1').text()).toBe('Yet Another SQL Course')
    expect(wrapper.get('summary').text()).toBe('SELECT')
    expect(wrapper.find('details').element.open).toBe(false)
    expect(wrapper.text()).toContain('courses/sql/')
    expect(wrapper.get('.course-cover img').attributes('src')).toBe(
      '/api/courses/sql/logo',
    )
    const img = wrapper.get('.course-cover img')
    const style = img.attributes('style') || ''
    const computed =
      typeof getComputedStyle === 'function'
        ? getComputedStyle(img.element)
        : null
    expect(
      style.includes('object-fit: contain') ||
        computed?.objectFit === 'contain',
    ).toBe(true)
    expect(
      style.includes('object-fit: cover') || computed?.objectFit === 'cover',
    ).toBe(false)
    const heading = wrapper.get('.course-heading')
    expect(heading.get('h1').text()).toBe('Yet Another SQL Course')
    const badge = heading.get('.badge')
    expect(badge.text()).toBe('доступен')
    expect(badge.classes()).toEqual(
      expect.arrayContaining(['badge', 'is-published']),
    )
    expect(badge.classes()).not.toContain('ready')
    const enroll = wrapper.get('.course-enroll .cta')
    expect(enroll.text()).toContain('Поступить')
    expect(enroll.attributes('href')).toBe('/login')
    expect(enroll.classes()).not.toContain('enroll')
    expect(wrapper.find('.course-enroll .badge').exists()).toBe(false)
    expect(wrapper.find('.course-actions').exists()).toBe(false)
  })

  it('does not send authenticated users to /login for Поступить', async () => {
    const course = {
      id: 'sql',
      title: 'Yet Another SQL Course',
      summary: 'SQL',
      status: 'published',
      badge: 'доступен',
      logoUrl: '/api/courses/sql/logo',
      fallbackLetter: 'S',
      sections: [],
      outline: [],
      idePath: 'courses/sql/',
    }
    vi.stubGlobal(
      'fetch',
      vi.fn((url, options) => {
        if (url === '/api/me') {
          expect(options?.credentials).toBe('include')
          return jsonResponse(200, {
            authenticated: true,
            id: '550e8400-e29b-41d4-a716-446655440000',
            name: 'Alice',
            avatarUrl: 'https://avatars.githubusercontent.com/u/1',
          })
        }
        return jsonResponse(200, course)
      }),
    )
    wrapper = await mountPage('sql')
    const enroll = wrapper.get('.course-enroll .cta')
    expect(enroll.text()).toContain('Поступить')
    expect(enroll.attributes('href')).not.toBe('/login')
  })

  it('uses is-progress on the heading for in_progress', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(() =>
        jsonResponse(200, {
          id: 'java',
          title: 'Yet Another Java Course',
          summary: 'Java',
          status: 'in_progress',
          badge: 'в работе',
          logoUrl: '/api/courses/java/logo',
          fallbackLetter: 'J',
          sections: [],
          outline: [],
          idePath: 'courses/java/',
        }),
      ),
    )
    wrapper = await mountPage('java')
    const badge = wrapper.get('.course-heading .badge')
    expect(badge.text()).toBe('в работе')
    expect(badge.classes()).toEqual(
      expect.arrayContaining(['badge', 'is-progress']),
    )
    expect(badge.classes()).not.toContain('ready')
  })

  it('shows 404 text when the course is missing, not an empty card', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(() => jsonResponse(404, {})),
    )
    wrapper = await mountPage('nope')
    expect(wrapper.text()).toMatch(/404/)
    expect(wrapper.get('a.cta').attributes('href')).toBe('/courses')
    expect(wrapper.find('.ccard').exists()).toBe(false)
    expect(wrapper.find('.course-detail').exists()).toBe(false)
  })
})
