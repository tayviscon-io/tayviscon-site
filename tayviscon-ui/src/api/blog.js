export async function tree() {
  const res = await fetch('/api/blog/tree')
  const data = await res.json().catch(() => ({}))
  if (!res.ok) {
    throw Object.assign(new Error(data.error || 'блог временно недоступен'), {
      status: res.status,
    })
  }
  return data
}

export async function latest() {
  const res = await fetch('/api/blog/latest')
  const data = await res.json().catch(() => ({}))
  if (!res.ok) {
    throw Object.assign(new Error(data.error || 'блог временно недоступен'), {
      status: res.status,
    })
  }
  return data
}

export async function article(path) {
  const res = await fetch('/api/blog/articles/' + path)
  const data = await res.json().catch(() => ({}))
  if (res.status === 404) {
    return null
  }
  if (!res.ok) {
    throw Object.assign(new Error(data.error || 'блог временно недоступен'), {
      status: res.status,
    })
  }
  return data
}
