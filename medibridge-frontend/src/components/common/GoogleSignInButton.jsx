import { useEffect, useRef, useState } from 'react'
import { useDispatch } from 'react-redux'
import { useNavigate } from 'react-router-dom'
import { loginWithGoogle } from '../../features/auth/authSlice'

const GSI_SRC = 'https://accounts.google.com/gsi/client'
const CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID || ''

/** Loads the Google Identity Services script once, even if two buttons mount. */
let gsiPromise = null

/**
 * GSI keeps one global config per page, so initialize() must run once. React
 * StrictMode mounts effects twice in development, which otherwise produces a
 * "initialize() is called multiple times" warning on every load.
 */
let gsiInitialized = false
function loadGsi() {
  if (gsiPromise) return gsiPromise

  gsiPromise = new Promise((resolve, reject) => {
    if (window.google?.accounts?.id) return resolve()

    const existing = document.querySelector(`script[src="${GSI_SRC}"]`)
    if (existing) {
      existing.addEventListener('load', () => resolve())
      existing.addEventListener('error', reject)
      return
    }

    const script = document.createElement('script')
    script.src = GSI_SRC
    script.async = true
    script.defer = true
    script.onload = () => resolve()
    script.onerror = () => reject(new Error('Could not load Google Sign-In'))
    document.head.appendChild(script)
  })

  return gsiPromise
}

/**
 * Renders Google's own sign-in button.
 *
 * <p>Google hands back a signed ID token ("credential"), which we forward to the
 * backend untouched. The backend verifies the signature and audience and reads
 * the email from the token itself - this component never tells the server who
 * the user is, because a browser is in no position to be believed about that.
 */
export default function GoogleSignInButton({ onError }) {
  const dispatch = useDispatch()
  const navigate = useNavigate()
  const container = useRef(null)
  const [failed, setFailed] = useState(null)

  useEffect(() => {
    if (!CLIENT_ID) {
      setFailed('not-configured')
      return
    }

    let cancelled = false

    loadGsi()
      .then(() => {
        if (cancelled || !container.current) return

        if (!gsiInitialized) {
          window.google.accounts.id.initialize({
            client_id: CLIENT_ID,
            callback: async (response) => {
              // Only the signed credential is forwarded. The backend verifies
              // it and reads the email from the token, so nothing here decides
              // who the user is.
              const result = await dispatch(loginWithGoogle(response.credential))
              if (loginWithGoogle.fulfilled.match(result)) {
                navigate('/patient')
              } else if (onError) {
                onError(result.payload || 'Google sign-in failed')
              }
            },
          })
          gsiInitialized = true
        }

        window.google.accounts.id.renderButton(container.current, {
          theme: 'outline',
          size: 'large',
          // Pixels, not '100%'. GSI rejects percentage widths and silently
          // falls back to its default size, logging a console warning.
          // 400 is Google's documented maximum and matches the form column.
          width: 400,
          text: 'continue_with',
          logo_alignment: 'center',
        })
      })
      .catch(() => !cancelled && setFailed('load-error'))

    return () => { cancelled = true }
  }, [dispatch, navigate, onError])

  // Without a client id the button cannot work, so say so rather than render a
  // control that silently does nothing.
  if (failed === 'not-configured') {
    return (
      <button type="button" disabled title="Set VITE_GOOGLE_CLIENT_ID in .env to enable"
        className="flex w-full cursor-not-allowed items-center justify-center gap-2 rounded-lg border border-slate-200 py-2.5 text-sm font-semibold text-slate-400">
        <span className="font-bold">G</span> Google sign-in not configured
      </button>
    )
  }

  if (failed === 'load-error') {
    return (
      <div className="rounded-lg border border-slate-200 py-2.5 text-center text-sm text-slate-400">
        Google Sign-In unavailable
      </div>
    )
  }

  return <div ref={container} className="flex justify-center" />
}
