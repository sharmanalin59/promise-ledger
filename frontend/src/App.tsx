import type { ReactNode } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider, useAuth } from './auth/AuthContext'
import { CompaniesPage } from './pages/CompaniesPage'
import { CompanyPage } from './pages/CompanyPage'
import { LoginPage } from './pages/LoginPage'
import { PromisePage } from './pages/PromisePage'
import { StatePanel } from './components/StatePanel'

function Protected({ children }: { children: ReactNode }) {
  const { username, loading } = useAuth()
  if (loading) {
    return <StatePanel title="Loading" message="Checking your session…" />
  }
  if (!username) {
    return <Navigate to="/login" replace state={{ from: window.location.pathname }} />
  }
  return children
}

export default function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route
          path="/companies"
          element={
            <Protected>
              <CompaniesPage />
            </Protected>
          }
        />
        <Route
          path="/companies/:companyId"
          element={
            <Protected>
              <CompanyPage />
            </Protected>
          }
        />
        <Route
          path="/promises/:promiseId"
          element={
            <Protected>
              <PromisePage />
            </Protected>
          }
        />
        <Route path="*" element={<Navigate to="/companies" replace />} />
      </Routes>
    </AuthProvider>
  )
}
