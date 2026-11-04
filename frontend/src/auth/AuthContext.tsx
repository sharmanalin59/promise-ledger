import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import { apiGet, apiMutate, clearCsrfToken } from '../api/client'

type AuthState = {
  username: string | null
  loading: boolean
  login: (username: string, password: string) => Promise<void>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [username, setUsername] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    apiGet<{ username: string }>('/api/auth/me')
      .then((me) => {
        if (!cancelled) {
          setUsername(me.username)
        }
      })
      .catch(() => {
        if (!cancelled) {
          setUsername(null)
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false)
        }
      })
    return () => {
      cancelled = true
    }
  }, [])

  const login = useCallback(async (user: string, password: string) => {
    clearCsrfToken()
    const me = await apiMutate<{ username: string }>('/api/auth/login', 'POST', {
      username: user,
      password,
    })
    setUsername(me?.username ?? user)
  }, [])

  const logout = useCallback(async () => {
    await apiMutate('/api/auth/logout', 'POST')
    clearCsrfToken()
    setUsername(null)
  }, [])

  const value = useMemo(
    () => ({ username, loading, login, logout }),
    [username, loading, login, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) {
    throw new Error('useAuth must be used within AuthProvider')
  }
  return ctx
}
