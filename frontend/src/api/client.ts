import type { ApiError } from './types'

let csrfToken: string | null = null

async function ensureCsrf(): Promise<string> {
  if (csrfToken) {
    return csrfToken
  }
  const response = await fetch('/api/auth/csrf', { credentials: 'include' })
  if (!response.ok) {
    throw new Error('Could not load a CSRF token.')
  }
  const body = (await response.json()) as { token: string; headerName: string }
  csrfToken = body.token
  return csrfToken
}

export function clearCsrfToken() {
  csrfToken = null
}

async function parseError(response: Response): Promise<string> {
  try {
    const body = (await response.json()) as ApiError
    return body.detail ?? body.title ?? `Request failed (${response.status})`
  } catch {
    return `Request failed (${response.status})`
  }
}

export async function apiGet<T>(path: string): Promise<T> {
  const response = await fetch(path, { credentials: 'include' })
  if (!response.ok) {
    throw new Error(await parseError(response))
  }
  return (await response.json()) as T
}

export async function apiMutate<T>(
  path: string,
  method: 'POST' | 'PUT' | 'DELETE',
  body?: unknown,
): Promise<T | null> {
  const token = await ensureCsrf()
  const response = await fetch(path, {
    method,
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      'X-XSRF-TOKEN': token,
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  if (!response.ok) {
    if (response.status === 403) {
      clearCsrfToken()
    }
    throw new Error(await parseError(response))
  }
  if (response.status === 204) {
    return null
  }
  return (await response.json()) as T
}
