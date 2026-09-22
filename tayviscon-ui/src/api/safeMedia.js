function httpsUrl(url) {
  if (!url || typeof url !== 'string') {
    return null
  }
  try {
    const parsed = new URL(url)
    if (parsed.protocol !== 'https:') {
      return null
    }
    return parsed
  } catch {
    return null
  }
}

export function youtubeWatchUrl(url) {
  const parsed = httpsUrl(url)
  if (
    !parsed ||
    (parsed.hostname !== 'www.youtube.com' &&
      parsed.hostname !== 'youtube.com' &&
      parsed.hostname !== 'youtu.be')
  ) {
    return ''
  }
  return parsed.href
}

export function youtubeThumbUrl(url) {
  const parsed = httpsUrl(url)
  if (!parsed || parsed.hostname !== 'i.ytimg.com') {
    return ''
  }
  return parsed.href
}

export function githubAvatarUrl(url) {
  const parsed = httpsUrl(url)
  if (!parsed || parsed.hostname !== 'avatars.githubusercontent.com') {
    return ''
  }
  return parsed.href
}

export function courseLogoUrl(url) {
  if (typeof url !== 'string' || !url.startsWith('/api/courses/')) {
    return ''
  }
  if (url.includes('..') || url.includes('//')) {
    return ''
  }
  if (!url.endsWith('/logo')) {
    return ''
  }
  return url
}
