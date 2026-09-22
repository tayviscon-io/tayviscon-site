import { afterEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import { createMemoryHistory, createRouter } from 'vue-router'
import BlogPage from './BlogPage.vue'

const treeBody = {
  groups: [
    {
      name: 'systems',
      articles: [
        { title: 'Надежные системы', path: 'systems/online-code-execution' },
      ],
    },
    {
      name: 'ai',
      articles: [{ title: 'Агенты', path: 'ai/agents' }],
    },
  ],
}

const articleBody = {
  title: 'Надежные системы',
  summary: 'Модульная',
  date: '2026-09-20',
  path: 'systems/online-code-execution',
  html: '<blockquote><p><strong>Товкач Артем Юрьевич</strong>\ntyomych.tovkach@tayviscon.com\nПеревод и адаптация статьи: <a rel="nofollow" href="https://ieeexplore.ieee.org/document/9245310"><em>Robust and Scalable Online Code Execution System</em></a></p></blockquote><p>hi</p><p><img src="/api/blog/assets/systems/online-code-execution/engine.excalidraw.svg" alt="движок"><em>Рисунок 3: Архитектура движка исполнения кода</em></p>',
}

function jsonResponse(status, body) {
  return Promise.resolve({
    ok: status >= 200 && status < 300,
    status,
    json: () => Promise.resolve(body),
  })
}

function stubBlogFetch() {
  return vi.fn((url) => {
    if (url === '/api/blog/tree') {
      return jsonResponse(200, treeBody)
    }
    if (url === '/api/blog/articles/systems/online-code-execution') {
      return jsonResponse(200, articleBody)
    }
    return jsonResponse(404, {})
  })
}

async function mountPage(path = '/blog') {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/blog', component: BlogPage },
      { path: '/blog/:pathMatch(.*)*', component: BlogPage },
    ],
  })
  await router.push(path)
  await router.isReady()
  const wrapper = mount(BlogPage, {
    global: { plugins: [router] },
  })
  await flushPromises()
  await nextTick()
  return { wrapper, router }
}

describe('BlogPage', () => {
  let wrapper

  afterEach(() => {
    wrapper?.unmount()
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('shows the empty column and tree, without a mascot image', async () => {
    vi.stubGlobal('fetch', stubBlogFetch())
    ;({ wrapper } = await mountPage('/blog'))
    expect(wrapper.text()).toContain(
      'Выбери статью слева — что сейчас интересно.',
    )
    expect(wrapper.text()).toContain('Надежные системы')
    expect(wrapper.find('.blog-empty img').exists()).toBe(false)
    expect(wrapper.findAll('details').every((d) => d.element.open)).toBe(true)
  })

  it('opens an article from the tree and keeps the author figure caption', async () => {
    vi.stubGlobal('fetch', stubBlogFetch())
    const mounted = await mountPage('/blog')
    wrapper = mounted.wrapper
    await mounted.router.push('/blog/systems/online-code-execution')
    await flushPromises()
    await nextTick()
    expect(wrapper.get('h1').text()).toBe('Надежные системы')
    expect(wrapper.get('.blog-article div img').attributes('src')).toBe(
      '/api/blog/assets/systems/online-code-execution/engine.excalidraw.svg',
    )
    expect(wrapper.get('.blog-meta').text()).toContain('20.09.2026')
    expect(wrapper.text()).toContain(
      'Рисунок 3: Архитектура движка исполнения кода',
    )
    expect(wrapper.find('.blog-cap').exists()).toBe(false)
    expect(wrapper.get('.blog-byline-name').attributes('href')).toBe(
      'https://github.com/tyomych-tovkach',
    )
    expect(wrapper.get('.blog-byline-name').text()).toBe('Tyomych Tovkach')
    expect(wrapper.find('.blog-byline-mail').exists()).toBe(false)
    expect(wrapper.get('.blog-byline-source a').attributes('href')).toBe(
      'https://ieeexplore.ieee.org/document/9245310',
    )
    expect(wrapper.get('.blog-byline-source').text()).toContain(
      'Перевод и адаптация',
    )
    expect(wrapper.get('.blog-byline-source').text()).toContain(
      'Robust and Scalable Online Code Execution System',
    )
    expect(wrapper.get('.blog-byline-photo').attributes('src')).toBe(
      '/brand/tyomych.png',
    )
    expect(wrapper.text()).not.toContain('Товкач Артем Юрьевич')
  })

  it('collapses a folder so its articles are hidden', async () => {
    vi.stubGlobal('fetch', stubBlogFetch())
    ;({ wrapper } = await mountPage('/blog'))
    const folders = wrapper.findAll('details')
    expect(folders).toHaveLength(2)
    await folders[0].get('summary').trigger('click')
    expect(folders[0].element.open).toBe(false)
    expect(folders[1].element.open).toBe(true)
    expect(folders[1].text()).toContain('Агенты')
  })

  it('shows «блог временно недоступен» when the tree is 503', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn((url) => {
        if (url === '/api/blog/tree') {
          return jsonResponse(503, { error: 'блог временно недоступен' })
        }
        return jsonResponse(404, {})
      }),
    )
    ;({ wrapper } = await mountPage('/blog'))
    expect(wrapper.text()).toContain('блог временно недоступен')
  })

  it('shows «статьи нет» for a missing article and keeps the tree', async () => {
    vi.stubGlobal('fetch', stubBlogFetch())
    ;({ wrapper } = await mountPage('/blog/systems/missing'))
    expect(wrapper.text()).toContain('статьи нет')
    expect(wrapper.text()).toContain('Надежные системы')
  })

  it('treats a group-only URL as the empty column, not 404', async () => {
    vi.stubGlobal('fetch', stubBlogFetch())
    ;({ wrapper } = await mountPage('/blog/systems'))
    expect(wrapper.text()).toContain(
      'Выбери статью слева — что сейчас интересно.',
    )
    expect(wrapper.text()).not.toContain('статьи нет')
  })
})
