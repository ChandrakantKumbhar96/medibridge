import { NavLink } from 'react-router-dom'

/**
 * Dashboard navigation.
 *
 * The active item gets a coloured left rail and a tinted fill rather than the
 * usual flat background swap — it reads as a physical indicator of position,
 * which is how mature product navs behave. Sticky so it stays with the user
 * on long pages.
 */
export default function Sidebar({ items }) {
  return (
    <aside className="hidden w-[236px] flex-shrink-0 lg:block">
      <nav className="sticky top-[86px] rounded-2xl border border-sand-200/70 bg-white p-2.5 shadow-soft">
        {items.map(({ to, label, icon: Icon, end }) => (
          <NavLink
            key={to}
            to={to}
            end={end}
            className={({ isActive }) =>
              `group relative mb-0.5 flex items-center gap-3 rounded-xl px-3.5 py-2.5
               text-[13.5px] font-semibold transition-all duration-200 ${
                 isActive
                   ? 'bg-primary-50 text-primary-700'
                   : 'text-sand-600 hover:bg-sand-50 hover:text-sand-900'
               }`
            }
          >
            {({ isActive }) => (
              <>
                <span
                  className={`absolute left-0 top-1/2 h-6 w-1 -translate-y-1/2 rounded-r-full
                              bg-primary-500 transition-all duration-200 ${
                                isActive ? 'opacity-100' : 'opacity-0'
                              }`}
                />
                <Icon
                  size={18}
                  strokeWidth={isActive ? 2.4 : 2}
                  className={isActive ? 'text-primary-600' : 'text-sand-400 group-hover:text-sand-600'}
                />
                {label}
              </>
            )}
          </NavLink>
        ))}
      </nav>
    </aside>
  )
}
