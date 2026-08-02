import { useEffect } from 'react'
import { useSelector } from 'react-redux'

const HOME = { patient: '/patient', doctor: '/doctor', admin: '/admin' }

const readUser = (raw) => {
  try { return JSON.parse(raw) } catch { return null }
}

/**
 * Keeps this tab's session in step with the other tabs.
 *
 * <p>The two halves of auth read from different places at different times.
 * `authSlice` seeds its state from localStorage once, when the module loads;
 * `axiosClient` re-reads `mb_token` on every single request. localStorage is
 * shared across every tab on the origin, and all three roles use the same keys.
 *
 * <p>So signing in as a doctor in a second tab silently repoints the first tab's
 * requests at the doctor's token, while `ProtectedRoute` — reading the store —
 * still believes a patient is logged in and keeps rendering patient pages. Every
 * call from that tab then fails authorization. This listener closes that gap.
 *
 * <p><b>It reloads rather than navigating.</b> Patching the auth slice and
 * routing across would leave the rest of the store untouched — the previous
 * user's appointments, records and admin tables all still cached, and shown to
 * whoever just took over the tab until each page happens to refetch. A reload
 * rebuilds every slice from scratch against the new identity. It also sidesteps
 * a race: `ProtectedRoute` re-renders on the role change and redirects to
 * `/login` before any imperative navigation lands.
 *
 * <p>Keyed on `mb_user`, not `mb_token`: the 401 interceptor rotates the token
 * routinely, and treating a refresh as an account switch would sign people out
 * mid-session for no reason.
 */
export default function SessionSync() {
  // The store, not localStorage, is what this tab currently believes — and it is
  // what ProtectedRoute and every page are rendering from.
  const user = useSelector((s) => s.auth.user)

  useEffect(() => {
    const onStorage = (e) => {
      // Fires only in the *other* tabs, which is what we want: the tab that
      // signed in has already updated its own store.
      if (e.key !== 'mb_user') return

      const next = readUser(e.newValue)

      // Signed out elsewhere — or the 401 interceptor gave up and cleared.
      if (!next) {
        if (user) window.location.replace('/login')
        return
      }

      // Same person: a token refresh, or a profile edit. Nothing to do.
      if (user
          && String(next.id) === String(user.id)
          && next.role === user.role) {
        return
      }

      // Their own area, not wherever this tab happened to be.
      window.location.replace(HOME[next.role] || '/login')
    }

    window.addEventListener('storage', onStorage)
    return () => window.removeEventListener('storage', onStorage)
  }, [user])

  return null
}
