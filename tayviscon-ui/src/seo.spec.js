import { describe, expect, it } from 'vitest'
import { applyRouteSeo } from './seo.js'

describe('applyRouteSeo', () => {
  it('sets home title with brand and Tayviscon aliases in description', () => {
    applyRouteSeo({ path: '/' })
    expect(document.title).toMatch(/Tayviscon IO/)
    const description = document.querySelector('meta[name="description"]')
    expect(description.getAttribute('content')).toMatch(/Тайвискон/)
    expect(
      document.querySelector('link[rel="canonical"]').getAttribute('href'),
    ).toBe('https://tayviscon.com/')
  })

  it('noindexes login', () => {
    applyRouteSeo({ path: '/login' })
    expect(document.title).toMatch(/Вход/)
    expect(
      document.querySelector('meta[name="robots"]').getAttribute('content'),
    ).toBe('noindex, nofollow')
  })
})
