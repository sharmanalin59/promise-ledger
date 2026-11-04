import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { apiGet } from '../api/client'
import type { PromiseDetail } from '../api/types'
import { AppShell } from '../components/AppShell'
import { StatePanel } from '../components/StatePanel'
import { StatusBadge } from '../components/StatusBadge'

export function PromisePage() {
  const { promiseId } = useParams()
  const [detail, setDetail] = useState<PromiseDetail | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!promiseId) {
      return
    }
    let cancelled = false
    setDetail(null)
    setError(null)
    apiGet<PromiseDetail>(`/api/promises/${promiseId}`)
      .then((data) => {
        if (!cancelled) {
          setDetail(data)
        }
      })
      .catch((err: unknown) => {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : 'Could not load promise.')
        }
      })
    return () => {
      cancelled = true
    }
  }, [promiseId])

  return (
    <AppShell title={detail ? 'Promise detail' : 'Promise'}>
      {detail ? (
        <p className="crumb">
          <Link to="/companies">Companies</Link>
          {' / '}
          <Link to={`/companies/${detail.company.id}`}>{detail.company.ticker}</Link>
          {' / '}
          FY{detail.transcript.fiscalYear} Q{detail.transcript.fiscalQuarter}
        </p>
      ) : null}
      {error ? <StatePanel title="Could not load promise" message={error} /> : null}
      {!error && detail === null ? (
        <StatePanel title="Loading" message="Loading promise source and evidence…" />
      ) : null}
      {detail ? (
        <article className="promise-detail">
          <div className="promise-detail-head">
            <StatusBadge assessment={detail.assessment} />
            <p className="muted">
              {detail.importance}
              {detail.category ? ` · ${detail.category}` : ''}
            </p>
          </div>
          <h2 className="promise-statement-lg">{detail.statement}</h2>
          <dl className="meta-grid">
            <div>
              <dt>Timeframe</dt>
              <dd>{detail.timeframe ?? 'Not stated in the transcript'}</dd>
            </div>
            <div>
              <dt>Speaker</dt>
              <dd>{detail.speaker ?? 'Not available in the current data model'}</dd>
            </div>
            <div>
              <dt>Status code</dt>
              <dd>{detail.status}</dd>
            </div>
          </dl>
          <section>
            <h3>Transcript source quote</h3>
            {detail.sourceQuote ? (
              <blockquote className="source-quote">{detail.sourceQuote}</blockquote>
            ) : (
              <p className="muted">No source quote was stored for this promise.</p>
            )}
          </section>
          <section>
            <h3>Verification evidence</h3>
            {detail.evidence.length === 0 ? (
              <p className="muted">
                No verification evidence has been recorded yet. That means the promise is still
                unassessed — not missed.
              </p>
            ) : (
              <ul className="evidence-list">
                {detail.evidence.map((item) => (
                  <li key={item.id}>
                    <p className="evidence-type">{item.sourceType.replace(/_/g, ' ')}</p>
                    <p>{item.text}</p>
                  </li>
                ))}
              </ul>
            )}
          </section>
        </article>
      ) : null}
    </AppShell>
  )
}
