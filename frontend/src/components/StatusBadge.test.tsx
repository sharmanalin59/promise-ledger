import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { StatusBadge } from './StatusBadge'

describe('StatusBadge', () => {
  it('renders Unassessed without calling the promise missed', () => {
    render(<StatusBadge assessment="Unassessed" />)
    expect(screen.getByText('Unassessed')).toBeInTheDocument()
    expect(screen.queryByText(/missed/i)).not.toBeInTheDocument()
  })
})
