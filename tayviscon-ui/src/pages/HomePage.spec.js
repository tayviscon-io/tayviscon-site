import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import HomePage from './HomePage.vue'

const latestBody = {
  posts: [
    {
      title: 'Надежные системы',
      summary: 'Модульная',
      date: '2026-09-20',
      path: 'systems/online-code-execution',
    },
    {
      title: 'Агенты',
      summary: 'Вторая',
      date: '2026-09-18',
      path: 'ai/agents',
    },
  ],
}

const videos = [
  {
    id: 'lhGamS89atg',
    title: 'Tayviscon: Рождение из хаоса',
    url: 'https://www.youtube.com/watch?v=lhGamS89atg',
    published: '2024-08-05',
    thumbnailUrl: 'https://i.ytimg.com/vi/lhGamS89atg/mqdefault.jpg',
  },
  {
    id: 'pKrDonOhoGU',
    title: 'Tayviscon: Трейлер',
    url: 'https://www.youtube.com/watch?v=pKrDonOhoGU',
    published: '2024-05-19',
    thumbnailUrl: 'https://i.ytimg.com/vi/pKrDonOhoGU/mqdefault.jpg',
  },
]

function jsonResponse(status, body) {
  return Promise.resolve({
    ok: status >= 200 && status < 300,
    status,
    json: () => Promise.resolve(body),
  })
}

function stubFeeds({
  blogStatus = 200,
  blogBody = latestBody,
  youtubeStatus = 200,
  youtubeBody = { videos },
} = {}) {
  return vi.fn((url) => {
    if (url === '/api/blog/latest') {
      return jsonResponse(blogStatus, blogBody)
    }
    if (String(url).includes('/api/youtube/latest')) {
      return jsonResponse(youtubeStatus, youtubeBody)
    }
    return jsonResponse(404, {})
  })
}

async function mountHome() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: HomePage },
      { path: '/courses', component: { template: '<div />' } },
      { path: '/blog', component: { template: '<div />' } },
      { path: '/blog/:pathMatch(.*)*', component: { template: '<div />' } },
    ],
  })
  await router.push('/')
  await router.isReady()
  const wrapper = mount(HomePage, { global: { plugins: [router] } })
  await flushPromises()
  return wrapper
}

describe('HomePage', () => {
  let wrapper

  beforeEach(() => {
    vi.stubGlobal('fetch', stubFeeds())
  })

  afterEach(() => {
    wrapper?.unmount()
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('H1 is tagline without repeating brand (logo already has it)', async () => {
    wrapper = await mountHome()
    expect(wrapper.get('h1').text()).toBe('делает знания')
    expect(wrapper.get('.mascot').attributes('alt')).toBe('Tayviscon')
  })

  it('has «Открыть курсы»', async () => {
    wrapper = await mountHome()
    expect(wrapper.text()).toContain('Открыть курсы')
  })

  it('shows Tyomych photo next to the quote, not a letter T', async () => {
    wrapper = await mountHome()
    const photo = wrapper.get('.who-photo')
    expect(photo.element.tagName).toBe('IMG')
    expect(photo.attributes('src')).toBe('/brand/tyomych.png')
    expect(photo.text()).toBe('')
  })

  it('makes blog rows real links', async () => {
    wrapper = await mountHome()
    const posts = wrapper.findAll('a.post')
    expect(posts).toHaveLength(2)
    expect(posts[0].attributes('href')).toBe(
      '/blog/systems/online-code-execution',
    )
    expect(posts[1].attributes('href')).toBe('/blog/ai/agents')
    expect(posts[0].text()).toContain('Надежные системы')
    expect(posts[1].text()).toContain('Агенты')
    expect(posts[0].get('small').text()).toBe('Модульная')
    expect(posts[1].get('small').text()).toBe('Вторая')
    expect(wrapper.html()).not.toContain('knowledge-base.tayviscon.com')
    expect(wrapper.get('.from-blog').classes()).toContain('ink')
    expect(wrapper.get('.latest h2').text()).toBe('Последние обновления')
    expect(wrapper.get('.latest-lead').text()).toContain('блогом и YouTube')
    expect(wrapper.get('.blog-more').attributes('href')).toBe('/blog')
    expect(posts[0].get('.pill').text()).toBe('20.09.2026')
    expect(posts[1].get('.pill').text()).toBe('18.09.2026')
  })

  it('links the Статьи door to /blog', async () => {
    wrapper = await mountHome()
    const door = wrapper
      .findAll('.door')
      .find((item) => item.get('h3').text() === 'Статьи')
    expect(door).toBeTruthy()
    expect(door.get('a').attributes('href')).toBe('/blog')
  })

  it('hides the blog column when latest is 503', async () => {
    vi.stubGlobal(
      'fetch',
      stubFeeds({
        blogStatus: 503,
        blogBody: { error: 'блог временно недоступен' },
      }),
    )
    wrapper = await mountHome()
    expect(wrapper.findAll('a.post')).toHaveLength(0)
    expect(
      wrapper.findAll('h3').map((heading) => heading.text()),
    ).not.toContain('Из блога')
    expect(wrapper.text()).toContain('С YouTube')
    expect(wrapper.get('.latest h2').text()).toBe('Последние обновления')
  })

  it('shows the blog heading with zero rows when latest is empty', async () => {
    const fetchMock = stubFeeds({ blogBody: { posts: [] } })
    vi.stubGlobal('fetch', fetchMock)
    wrapper = await mountHome()
    expect(fetchMock).toHaveBeenCalledWith('/api/blog/latest')
    expect(wrapper.findAll('a.post')).toHaveLength(0)
    expect(wrapper.findAll('h3').map((heading) => heading.text())).toContain(
      'Из блога',
    )
  })

  it('renders latest YouTube videos from the API, not channel stubs', async () => {
    wrapper = await mountHome()
    const rows = wrapper.findAll('a.yt')
    expect(rows).toHaveLength(2)
    expect(rows[0].attributes('href')).toBe(
      'https://www.youtube.com/watch?v=lhGamS89atg',
    )
    expect(rows[0].text()).toContain('Рождение из хаоса')
    expect(rows[0].text()).toContain('05.08.2024')
    expect(wrapper.get('.yt-thumb img').attributes('src')).toBe(
      'https://i.ytimg.com/vi/lhGamS89atg/mqdefault.jpg',
    )
    expect(wrapper.get('a.yt-more').attributes('href')).toBe(
      'https://www.youtube.com/@tayviscon',
    )
    expect(wrapper.text()).not.toContain('5:18')
  })

  it('shows a quiet empty line when the channel has no videos', async () => {
    vi.stubGlobal('fetch', stubFeeds({ youtubeBody: { videos: [] } }))
    wrapper = await mountHome()
    expect(wrapper.findAll('a.yt')).toHaveLength(0)
    expect(wrapper.text()).toContain('пока нет роликов')
  })

  it('shows a quiet down line when the feed is unavailable', async () => {
    vi.stubGlobal(
      'fetch',
      stubFeeds({
        youtubeStatus: 503,
        youtubeBody: { error: 'канал временно недоступен' },
      }),
    )
    wrapper = await mountHome()
    expect(wrapper.findAll('a.yt')).toHaveLength(0)
    expect(wrapper.text()).toContain('канал временно недоступен')
  })
})
