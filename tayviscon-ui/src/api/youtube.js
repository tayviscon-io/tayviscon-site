export async function latest() {
  const res = await fetch('/api/youtube/latest')
  const data = await res.json().catch(() => ({}))
  if (!res.ok) {
    throw Object.assign(new Error(data.error || 'канал временно недоступен'), {
      status: res.status,
    })
  }
  return data
}
