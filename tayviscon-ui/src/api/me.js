export async function me() {
  const res = await fetch('/api/me', { credentials: 'include' })
  const data = await res.json().catch(() => ({}))
  if (!res.ok) {
    throw Object.assign(
      new Error(data.error || 'не удалось загрузить профиль'),
      {
        status: res.status,
      },
    )
  }
  return data
}
