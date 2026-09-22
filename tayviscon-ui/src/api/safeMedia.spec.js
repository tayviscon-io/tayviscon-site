import { describe, expect, it } from 'vitest'
import {
  courseLogoUrl,
  githubAvatarUrl,
  youtubeThumbUrl,
  youtubeWatchUrl,
} from './safeMedia.js'

describe('safeMedia', () => {
  it('keeps https YouTube watch URLs and rejects javascript', () => {
    expect(youtubeWatchUrl('https://www.youtube.com/watch?v=abc')).toBe(
      'https://www.youtube.com/watch?v=abc',
    )
    expect(youtubeWatchUrl('javascript:alert(1)')).toBe('')
  })

  it('keeps ytimg thumbnails only', () => {
    expect(
      youtubeThumbUrl('https://i.ytimg.com/vi/abc/mqdefault.jpg'),
    ).toContain('i.ytimg.com')
    expect(youtubeThumbUrl('https://evil.example/x.png')).toBe('')
  })

  it('keeps GitHub avatars only', () => {
    expect(githubAvatarUrl('https://avatars.githubusercontent.com/u/1')).toBe(
      'https://avatars.githubusercontent.com/u/1',
    )
    expect(githubAvatarUrl('https://evil.example/u/1')).toBe('')
  })

  it('keeps same-origin course logos only', () => {
    expect(courseLogoUrl('/api/courses/sql/logo')).toBe('/api/courses/sql/logo')
    expect(courseLogoUrl('/api/courses/../logo')).toBe('')
    expect(courseLogoUrl('https://evil.example/logo.svg')).toBe('')
  })
})
