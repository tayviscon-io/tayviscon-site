let listInFlight = null

async function fetchList() {
  const res = await fetch('/api/courses')
  const data = await res.json().catch(() => ({}))
  if (!res.ok) {
    throw Object.assign(
      new Error(data.error || 'каталог временно недоступен'),
      {
        status: res.status,
      },
    )
  }
  return data
}

export function list() {
  if (!listInFlight) {
    listInFlight = fetchList().finally(() => {
      listInFlight = null
    })
  }
  return listInFlight
}

export async function get(id) {
  const res = await fetch('/api/courses/' + encodeURIComponent(id))
  if (res.status === 404) {
    throw Object.assign(new Error('404'), { status: 404 })
  }
  const data = await res.json().catch(() => ({}))
  if (!res.ok) {
    throw Object.assign(
      new Error(data.error || 'каталог временно недоступен'),
      {
        status: res.status,
      },
    )
  }
  return data
}
