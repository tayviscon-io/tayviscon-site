import { afterEach, describe, expect, it, vi } from 'vitest'
import { get, list } from './courses.js'

function jsonResponse(status, body) {
  return Promise.resolve({
    ok: status >= 200 && status < 300,
    status,
    json: () => Promise.resolve(body),
  })
}

describe('list()', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('shares one in-flight GET and does not cache after settle', async () => {
    const fetchMock = vi.fn(() => jsonResponse(200, { courses: [] }))
    vi.stubGlobal('fetch', fetchMock)
    await Promise.all([list(), list()])
    expect(fetchMock).toHaveBeenCalledTimes(1)
    await list()
    expect(fetchMock).toHaveBeenCalledTimes(2)
  })
})

describe('get()', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('encodes the course id in the request path', async () => {
    const fetchMock = vi.fn(() => jsonResponse(200, { id: 'sql' }))
    vi.stubGlobal('fetch', fetchMock)
    await get('sql?x=1')
    expect(fetchMock).toHaveBeenCalledWith('/api/courses/sql%3Fx%3D1')
  })
})
