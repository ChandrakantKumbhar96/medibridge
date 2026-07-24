import { Link } from 'react-router-dom'
import Logo from '../common/Logo'

/**
 * Public site header — frosted and sticky, so it stays available as the
 * landing page scrolls without blocking the hero imagery behind it.
 */
export default function PublicNavbar() {
  return (
    <header className="sticky top-0 z-40 border-b border-sand-200/60 bg-white/75 backdrop-blur-xl backdrop-saturate-150">
      <div className="mx-auto flex max-w-[1200px] items-center justify-between px-6 py-3.5">
        <Link to="/"><Logo /></Link>

        <nav className="flex items-center gap-1 sm:gap-2">
          {[['About', '#about'], ['Departments', '#services'], ['Contact', '#contact']].map(
            ([label, href]) => (
              <a
                key={label}
                href={href}
                className="hidden rounded-full px-4 py-2 text-[13.5px] font-semibold text-sand-600
                           transition-colors hover:bg-sand-100 hover:text-sand-900 sm:block"
              >
                {label}
              </a>
            )
          )}

          <Link
            to="/login"
            className="ml-2 rounded-full bg-gradient-to-b from-primary-500 to-primary-600 px-6 py-2.5
                       text-[13.5px] font-bold text-white shadow-[0_6px_18px_-6px_rgba(15,133,123,.6)]
                       transition-all hover:from-primary-600 hover:to-primary-700"
          >
            Sign in
          </Link>
        </nav>
      </div>
    </header>
  )
}
