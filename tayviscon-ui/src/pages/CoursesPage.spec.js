import { readFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import CoursesPage from './CoursesPage.vue'

const publishedCourse = {
  id: 'sql',
  title: 'Yet Another SQL Course',
  summary: 'Запросы, транзакции и схемы на PostgreSQL.',
  status: 'published',
  badge: 'доступен',
  logoUrl: '/api/courses/sql/logo',
  fallbackLetter: 'S',
}

const inProgressCourse = {
  id: 'java',
  title: 'Yet Another Java Course',
  summary: 'Java-курс с длинным описанием для компактной строки.',
  status: 'in_progress',
  badge: 'в работе',
  logoUrl: '/api/courses/java/logo',
  fallbackLetter: 'J',
}

const finishedCourse = {
  id: 'legacy',
  title: 'Finished Course',
  summary: 'done',
  status: 'finished',
  badge: 'завершён',
  fallbackLetter: 'F',
}

function jsonResponse(status, body) {
  return Promise.resolve({
    ok: status >= 200 && status < 300,
    status,
    json: () => Promise.resolve(body),
  })
}

async function mountPage() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/courses', component: CoursesPage },
      { path: '/courses/:id', component: { template: '<div />' } },
    ],
  })
  await router.push('/courses')
  await router.isReady()
  const wrapper = mount(CoursesPage, {
    global: { plugins: [router] },
  })
  await flushPromises()
  return wrapper
}

describe('CoursesPage', () => {
  let wrapper

  afterEach(() => {
    wrapper?.unmount()
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('shows equal cards under Программирование, not status sections', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(() =>
        jsonResponse(200, { courses: [publishedCourse, inProgressCourse] }),
      ),
    )
    wrapper = await mountPage()
    expect(wrapper.findAll('.ccard')).toHaveLength(2)
    expect(wrapper.find('.crow').exists()).toBe(false)
    expect(wrapper.get('.catalog-heading').text()).toBe('Программирование')
    expect(wrapper.findAll('.catalog-heading')).toHaveLength(1)
    expect(wrapper.get('.ccard').text()).toContain('Yet Another SQL Course')
    expect(wrapper.text()).toContain('Yet Another Java Course')
    expect(wrapper.text()).toContain('в работе')
  })

  it('pins each status badge in the card footer', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(() =>
        jsonResponse(200, { courses: [publishedCourse, inProgressCourse] }),
      ),
    )
    wrapper = await mountPage()
    const cards = wrapper.findAll('.ccard')
    expect(cards).toHaveLength(2)
    expect(cards[0].get('.cfoot .badge').text()).toBe('доступен')
    expect(cards[1].get('.cfoot .badge').text()).toBe('в работе')
    expect(cards[0].get('.cbody').element.lastElementChild.className).toContain(
      'cfoot',
    )
  })

  it('uses outline status classes, not ready fill', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(() =>
        jsonResponse(200, {
          courses: [publishedCourse, inProgressCourse, finishedCourse],
        }),
      ),
    )
    wrapper = await mountPage()
    const badges = wrapper.findAll('.cfoot .badge')
    expect(badges[0].classes()).toEqual(
      expect.arrayContaining(['badge', 'is-published']),
    )
    expect(badges[1].classes()).toEqual(
      expect.arrayContaining(['badge', 'is-progress']),
    )
    expect(badges[2].classes()).toEqual(
      expect.arrayContaining(['badge', 'is-finished']),
    )
    expect(badges[0].classes()).not.toContain('ready')
    expect(badges[1].classes()).not.toContain('ready')
    expect(badges[2].text()).toBe('завершён')
  })

  it('filters by status without changing card layout', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(() =>
        jsonResponse(200, { courses: [publishedCourse, inProgressCourse] }),
      ),
    )
    wrapper = await mountPage()
    await wrapper.get('button.filter:nth-child(3)').trigger('click')
    expect(wrapper.findAll('.ccard')).toHaveLength(1)
    expect(wrapper.get('.ccard').text()).toContain('Yet Another Java Course')
    await wrapper.get('button.filter:nth-child(2)').trigger('click')
    expect(wrapper.get('.ccard').text()).toContain('Yet Another SQL Course')
  })

  it('uses object-fit contain for the course logo', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(() => jsonResponse(200, { courses: [publishedCourse] })),
    )
    wrapper = await mountPage()
    const img = wrapper.get('.banner img')
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
    expect(img.attributes('src')).toBe('/api/courses/sql/logo')
  })

  it('shows .fallback when logoUrl is absent', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(() =>
        jsonResponse(200, {
          courses: [{ ...publishedCourse, logoUrl: null, fallbackLetter: 'S' }],
        }),
      ),
    )
    wrapper = await mountPage()
    expect(wrapper.get('.fallback').text()).toBe('S')
    expect(wrapper.find('.banner img').exists()).toBe(false)
  })

  it('paints the card banner with the same white paper as the page', () => {
    const css = readFileSync(
      join(dirname(fileURLToPath(import.meta.url)), '../styles/sketch.css'),
      'utf8',
    )
    const banner = css.match(/\.banner\s*\{[^}]+\}/)[0]
    expect(banner).toMatch(/background:\s*#ffffff/)
    expect(css).toMatch(/body,\s*\n?#app\s*\{[^}]*background:\s*#ffffff/)
  })

  it('shows «каталог временно недоступен» on 503', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(() => jsonResponse(503, { error: 'каталог временно недоступен' })),
    )
    wrapper = await mountPage()
    expect(wrapper.text()).toContain('каталог временно недоступен')
    expect(wrapper.find('.ccard').exists()).toBe(false)
  })
})
