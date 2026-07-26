import { NavLink } from 'react-router-dom'

/**
 * Mobile bottom navigation — the standard consumer-app pattern (Practo, Apollo,
 * 1mg). Shown only below `lg`, where the sidebar is hidden. To keep the bar
 * uncramped, a nav longer than five items collapses to its first four
 * destinations plus the last (Settings).
 */
export default function MobileBottomNav({ items = [] }) {
  const shown = items.length <= 5
    ? items
    : [...items.slice(0, 4), items[items.length - 1]]

  return (
    <nav className="fixed inset-x-0 bottom-0 z-40 border-t border-sand-200/80 bg-white/95 backdrop-blur-lg lg:hidden">
      <div className="mx-auto flex max-w-md items-stretch justify-around px-1 pb-[env(safe-area-inset-bottom)]">
        {shown.map(({ to, label, icon: Icon, end }) => (
          <NavLink
            key={to}
            to={to}
            end={end}
            className={({ isActive }) =>
              `flex flex-1 flex-col items-center gap-0.5 py-2 text-[10px] font-bold transition-colors ${
                isActive ? 'text-primary-600' : 'text-sand-400'
              }`
            }
          >
            {({ isActive }) => (
              <>
                <Icon size={20} strokeWidth={isActive ? 2.4 : 2} />
                <span className="max-w-[68px] truncate">{label}</span>
              </>
            )}
          </NavLink>
        ))}
      </div>
    </nav>
  )
}
