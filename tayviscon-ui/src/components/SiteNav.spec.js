import { afterEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import SiteNav from './SiteNav.vue'

function jsonResponse(status, body) {
  return Promise.resolve({
    ok: status >= 200 && status < 300,
    status,
    json: () => Promise.resolve(body),
  })
}

function stubFetch(meBody) {
  return vi.fn((url, options) => {
    if (url === '/api/me') {
      expect(options?.credentials).toBe('include')
      return jsonResponse(200, meBody)
    }
    if (url === '/api/courses') {
      return jsonResponse(200, { courses: [] })
    }
    return jsonResponse(404, {})
  })
}

async function mountNav() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: { template: '<div />' } },
      { path: '/login', component: { template: '<div />' } },
      { path: '/courses', component: { template: '<div />' } },
      { path: '/courses/:id', component: { template: '<div />' } },
      { path: '/blog', component: { template: '<div />' } },
    ],
  })
  await router.push('/')
  await router.isReady()
  const wrapper = mount(SiteNav, { global: { plugins: [router] } })
  await flushPromises()
  return wrapper
}

describe('SiteNav auth', () => {
  let wrapper

  afterEach(() => {
    wrapper?.unmount()
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('guest sees Войти as a link to the sketch login page', async () => {
    vi.stubGlobal('fetch', stubFetch({ authenticated: false }))
    wrapper = await mountNav()
    const login = wrapper.get('a.login')
    expect(login.text()).toContain('Войти')
    expect(login.attributes('href')).toBe('/login')
    expect(wrapper.find('button.login').exists()).toBe(false)
    expect(wrapper.text()).not.toContain('Выйти')
    expect(wrapper.get('a[href="/blog"]').text()).toContain('Блог')
  })

  it('authenticated user sees name, avatar, and Выйти', async () => {
    vi.stubGlobal(
      'fetch',
      stubFetch({
        authenticated: true,
        id: '550e8400-e29b-41d4-a716-446655440000',
        name: 'Alice',
        avatarUrl: 'https://avatars.githubusercontent.com/u/1',
      }),
    )
    wrapper = await mountNav()
    expect(wrapper.text()).toContain('Alice')
    expect(wrapper.text()).toContain('Выйти')
    expect(wrapper.find('img.avatar').attributes('src')).toBe(
      'https://avatars.githubusercontent.com/u/1',
    )
    expect(wrapper.get('a.login').text()).toContain('Выйти')
    expect(wrapper.text()).not.toContain('Войти')
  })

  it('does not show Войти before me() settles', async () => {
    let resolveMe
    vi.stubGlobal(
      'fetch',
      vi.fn((url, options) => {
        if (url === '/api/me') {
          expect(options?.credentials).toBe('include')
          return new Promise((resolve) => {
            resolveMe = () =>
              resolve({
                ok: true,
                status: 200,
                json: () => Promise.resolve({ authenticated: false }),
              })
          })
        }
        if (url === '/api/courses') {
          return jsonResponse(200, { courses: [] })
        }
        return jsonResponse(404, {})
      }),
    )
    wrapper = await mountNav()
    expect(wrapper.text()).not.toContain('Войти')
    expect(wrapper.find('a.login').exists()).toBe(false)
    expect(wrapper.text()).not.toContain('Выйти')
    resolveMe()
    await flushPromises()
    expect(wrapper.get('a.login').text()).toContain('Войти')
  })
})

describe('SiteNav mega-menu', () => {
  let wrapper

  afterEach(() => {
    wrapper?.unmount()
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('groups courses under Программирование, not by status', async () => {
    const courses = [
      { id: 'sql', title: 'SQL Course', status: 'published' },
      ...Array.from({ length: 6 }, (_, index) => ({
        id: 'wip-' + index,
        title: 'WIP ' + index,
        status: 'in_progress',
      })),
    ]
    vi.stubGlobal(
      'fetch',
      vi.fn((url) => {
        if (url === '/api/me') {
          return jsonResponse(200, { authenticated: false })
        }
        if (url === '/api/courses') {
          return jsonResponse(200, { courses })
        }
        return jsonResponse(404, {})
      }),
    )
    wrapper = await mountNav()
    await wrapper.get('.mega-toggle').trigger('click')
    expect(wrapper.text()).toContain('Программирование')
    expect(wrapper.text()).not.toContain('Доступны')
    expect(wrapper.text()).not.toContain('В работе')
    expect(wrapper.text()).toContain('SQL Course')
    expect(wrapper.text()).toContain('WIP 5')
    expect(wrapper.get('a[href="/courses/sql"]').text()).toBe('SQL Course')
    expect(wrapper.get('a.all').text()).toContain('Все курсы')
    expect(wrapper.get('a.all').attributes('href')).toBe('/courses')
    expect(wrapper.get('.mega-label').text()).toBe('Программирование')
  })
})
