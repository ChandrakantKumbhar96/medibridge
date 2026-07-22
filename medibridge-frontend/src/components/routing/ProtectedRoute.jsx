import { Navigate, useLocation } from 'react-router-dom'
import { useSelector } from 'react-redux'

// Guards routes by authentication + role. Mirrors the RBAC described in the schema notes.
export default function ProtectedRoute({ role, children }) {
  const { isAuthenticated, user } = useSelector((s) => s.auth)
  const location = useLocation()

  if (!isAuthenticated) return <Navigate to="/login" state={{ from: location }} replace />
  if (role && user?.role !== role) return <Navigate to="/login" replace />
  return children
}
