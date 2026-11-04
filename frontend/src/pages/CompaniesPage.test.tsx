import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AuthProvider } from '../auth/AuthContext'
import { CompaniesPage } from './CompaniesPage'

describe('CompaniesPage', () => {
  beforeEach(() => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo) => {
        const url = String(input)
        if (url.includes('/api/auth/me')) {
          return new Response(JSON.stringify({ username: 'demo' }), {
            status: 200,
            headers: { 'Content-Type': 'application/json' },
          })
        }
        if (url.includes('/api/companies')) {
          return new Response(
            JSON.stringify([
              { id: '1', name: 'KPIT Technologies', ticker: 'KPITTECH' },
              { id: '2', name: 'Coforge', ticker: 'COFORGE' },
            ]),
            { status: 200, headers: { 'Content-Type': 'application/json' } },
          )
        }
        return new Response('not found', { status: 404 })
      }),
    )
  })

  it('lists companies from the shared catalogue', async () => {
    render(
      <MemoryRouter>
        <AuthProvider>
          <CompaniesPage />
        </AuthProvider>
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(screen.getByText('KPIT Technologies')).toBeInTheDocument()
    })
    expect(screen.getByText('Coforge')).toBeInTheDocument()
    expect(screen.getByText('KPITTECH')).toBeInTheDocument()
  })
})
