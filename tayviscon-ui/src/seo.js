const DEFAULT_TITLE = 'Tayviscon IO — открытые курсы по программированию'
const DEFAULT_DESCRIPTION =
  'Tayviscon IO (Tayviscon, Тайвискон) — открытые курсы Yet Another Course, блог и практика по Java, SQL, Docker и не только. Бесплатное обучение без барьеров.'

function setMeta(name, content) {
  if (!content) {
    return
  }
  let el = document.querySelector(`meta[name="${name}"]`)
  if (!el) {
    el = document.createElement('meta')
    el.setAttribute('name', name)
    document.head.appendChild(el)
  }
  el.setAttribute('content', content)
}

function setCanonical(href) {
  let el = document.querySelector('link[rel="canonical"]')
  if (!el) {
    el = document.createElement('link')
    el.setAttribute('rel', 'canonical')
    document.head.appendChild(el)
  }
  el.setAttribute('href', href)
}

/** Updates document title, description, and canonical for the active Vue route. */
export function applyRouteSeo(to, extras = {}) {
  const title = extras.title || titleForRoute(to)
  const description = extras.description || descriptionForRoute(to)
  document.title = title
  setMeta('description', description)
  setCanonical(`https://tayviscon.com${to.path === '/' ? '/' : to.path}`)
  if (to.path === '/login') {
    setMeta('robots', 'noindex, nofollow')
  } else {
    setMeta('robots', 'index, follow')
  }
}

function titleForRoute(to) {
  if (to.path === '/') {
    return DEFAULT_TITLE
  }
  if (to.path === '/courses') {
    return 'Курсы — Tayviscon IO'
  }
  if (to.path.startsWith('/courses/')) {
    return 'Курс — Tayviscon IO'
  }
  if (to.path === '/blog') {
    return 'Блог — Tayviscon IO'
  }
  if (to.path.startsWith('/blog/')) {
    return 'Статья — Tayviscon IO'
  }
  if (to.path === '/login') {
    return 'Вход — Tayviscon IO'
  }
  return DEFAULT_TITLE
}

function descriptionForRoute(to) {
  if (to.path === '/courses' || to.path.startsWith('/courses/')) {
    return 'Каталог открытых курсов Yet Another Course от Tayviscon IO (Тайвискон): Java, SQL, Docker и другие направления.'
  }
  if (to.path === '/blog' || to.path.startsWith('/blog/')) {
    return 'Блог и база знаний Tayviscon IO (Тайвискон): статьи о разработке, системах и обучении.'
  }
  if (to.path === '/login') {
    return 'Вход в Tayviscon IO через GitHub.'
  }
  return DEFAULT_DESCRIPTION
}
