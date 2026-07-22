import { NavLink } from 'react-router-dom'

export default function Sidebar({ items }) {
  return (
    <aside className="w-60 flex-shrink-0">
      <nav className="rounded-2xl border border-slate-200 bg-white p-3 shadow-sm">
        {items.map(({ to, label, icon: Icon, end }) => (
          <NavLink
            key={to}
            to={to}
            end={end}
            className={({ isActive }) =>
              `mb-1 flex items-center gap-3 rounded-lg px-3.5 py-2.5 text-sm font-medium transition ${
                isActive ? 'bg-primary-50 text-primary-600' : 'text-slate-600 hover:bg-slate-50'
              }`
            }
          >
            <Icon size={18} />
            {label}
          </NavLink>
        ))}
      </nav>
    </aside>
  )
}
