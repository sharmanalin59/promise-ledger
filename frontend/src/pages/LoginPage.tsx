import { useState, type FormEvent } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export function LoginPage() {
  const { username, loading, login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [user, setUser] = useState('demo')
  const [password, setPassword] = useState('demo')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  if (!loading && username) {
    const redirect = (location.state as { from?: string } | null)?.from ?? '/companies'
    return <Navigate to={redirect} replace />
  }

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    setSubmitting(true)
    setError(null)
    try {
      await login(user, password)
      navigate('/companies', { replace: true })
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Login failed.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="login-shell">
      <section className="login-panel">
        <p className="brand-mark">PromiseLedger</p>
        <h1>Sign in to the shared ledger</h1>
        <p className="lede">
          Browse management promises across the five covered companies. Every authenticated user sees
          the same catalogue.
        </p>
        <form onSubmit={onSubmit} className="login-form">
          <label>
            Username
            <input
              name="username"
              autoComplete="username"
              value={user}
              onChange={(e) => setUser(e.target.value)}
              required
            />
          </label>
          <label>
            Password
            <input
              name="password"
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </label>
          {error ? (
            <p className="form-error" role="alert">
              {error}
            </p>
          ) : null}
          <button type="submit" disabled={submitting}>
            {submitting ? 'Signing in…' : 'Sign in'}
          </button>
        </form>
      </section>
    </main>
  )
}
