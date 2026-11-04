import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { apiGet } from '../api/client'
import type { Company } from '../api/types'
import { StatePanel } from '../components/StatePanel'
import { AppShell } from '../components/AppShell'

export function CompaniesPage() {
  const [companies, setCompanies] = useState<Company[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    apiGet<Company[]>('/api/companies')
      .then((data) => {
        if (!cancelled) {
          setCompanies(data)
        }
      })
      .catch((err: unknown) => {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : 'Could not load companies.')
        }
      })
    return () => {
      cancelled = true
    }
  }, [])

  return (
    <AppShell title="Companies">
      {error ? <StatePanel title="Could not load companies" message={error} /> : null}
      {!error && companies === null ? (
        <StatePanel title="Loading" message="Fetching the shared company catalogue…" />
      ) : null}
      {!error && companies && companies.length === 0 ? (
        <StatePanel
          title="No companies yet"
          message="The shared catalogue is empty. Seeded MVP companies should appear after migrations run."
        />
      ) : null}
      {!error && companies && companies.length > 0 ? (
        <ul className="company-list">
          {companies.map((company) => (
            <li key={company.id}>
              <Link to={`/companies/${company.id}`} className="company-row">
                <span className="company-name">{company.name}</span>
                <span className="company-ticker">{company.ticker}</span>
              </Link>
            </li>
          ))}
        </ul>
      ) : null}
    </AppShell>
  )
}
