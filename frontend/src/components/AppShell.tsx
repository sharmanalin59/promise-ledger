import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

type Props = {
  title: string
  children: ReactNode
}

export function AppShell({ title, children }: Props) {
  const { username, logout } = useAuth()

  return (
    <div className="app-shell">
      <header className="topbar">
        <Link to="/companies" className="brand-link">
          PromiseLedger
        </Link>
        <div className="topbar-meta">
          <span>{username}</span>
          <button type="button" className="ghost" onClick={() => void logout()}>
            Sign out
          </button>
        </div>
      </header>
      <main className="page">
        <h1>{title}</h1>
        {children}
      </main>
    </div>
  )
}
