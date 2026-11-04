import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { apiGet } from '../api/client'
import type { CompanyLedger } from '../api/types'
import { AppShell } from '../components/AppShell'
import { StatePanel } from '../components/StatePanel'
import { StatusBadge } from '../components/StatusBadge'

export function CompanyPage() {
  const { companyId } = useParams()
  const [ledger, setLedger] = useState<CompanyLedger | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!companyId) {
      return
    }
    let cancelled = false
    setLedger(null)
    setError(null)
    apiGet<CompanyLedger>(`/api/companies/${companyId}`)
      .then((data) => {
        if (!cancelled) {
          setLedger(data)
        }
      })
      .catch((err: unknown) => {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : 'Could not load company.')
        }
      })
    return () => {
      cancelled = true
    }
  }, [companyId])

  const title = ledger ? `${ledger.name} · ${ledger.ticker}` : 'Company'

  return (
    <AppShell title={title}>
      <p className="crumb">
        <Link to="/companies">Companies</Link>
      </p>
      {error ? <StatePanel title="Could not load company" message={error} /> : null}
      {!error && ledger === null ? (
        <StatePanel title="Loading" message="Loading promises grouped by quarter…" />
      ) : null}
      {!error && ledger && ledger.quarters.length === 0 ? (
        <StatePanel
          title="No transcripts yet"
          message="No earnings-call transcripts are available for this company. Missing quarters are unavailable, not missed promises."
        />
      ) : null}
      {!error && ledger
        ? ledger.quarters.map((quarter) => (
            <section
              key={`${quarter.fiscalYear}-Q${quarter.fiscalQuarter}`}
              className="quarter-block"
              aria-label={`FY${quarter.fiscalYear} Q${quarter.fiscalQuarter}`}
            >
              <header className="quarter-header">
                <h2>
                  FY{quarter.fiscalYear} · Q{quarter.fiscalQuarter}
                </h2>
                <p>
                  {quarter.promises.length === 0
                    ? 'Transcript available · no promises extracted'
                    : `${quarter.promises.length} promise${quarter.promises.length === 1 ? '' : 's'}`}
                </p>
              </header>
              {quarter.promises.length === 0 ? (
                <p className="muted">
                  This quarter has a transcript but no extracted promises. That is not treated as a
                  miss.
                </p>
              ) : (
                <ul className="promise-list">
                  {quarter.promises.map((promise) => (
                    <li key={promise.id}>
                      <Link to={`/promises/${promise.id}`} className="promise-row">
                        <div>
                          <p className="promise-statement">{promise.statement}</p>
                          <p className="muted">
                            {[promise.category, promise.timeframe].filter(Boolean).join(' · ') ||
                              'No timeframe recorded'}
                          </p>
                        </div>
                        <StatusBadge assessment={promise.assessment} />
                      </Link>
                    </li>
                  ))}
                </ul>
              )}
            </section>
          ))
        : null}
    </AppShell>
  )
}
