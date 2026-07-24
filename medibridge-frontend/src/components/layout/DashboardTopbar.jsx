import { Bell, LogOut } from 'lucide-react'
import { useDispatch, useSelector } from 'react-redux'
import { useNavigate } from 'react-router-dom'
import Logo from '../common/Logo'
import Avatar from '../common/Avatar'
import { logout } from '../../features/auth/authSlice'

/**
 * Top bar.
 *
 * Frosted translucent rather than solid white, so content scrolling beneath
 * stays faintly visible — a small cue that costs nothing and immediately
 * reads as a modern product surface.
 */
export default function DashboardTopbar({ badge }) {
  const dispatch = useDispatch()
  const navigate = useNavigate()
  const user = useSelector((s) => s.auth.user) || {}

  const sub =
    user.role === 'doctor' ? user.specialization || 'Doctor'
    : user.role === 'admin' ? 'System Administrator'
    : 'Patient'

  const handleLogout = () => { dispatch(logout()); navigate('/login') }

  return (
    <header className="sticky top-0 z-30 border-b border-sand-200/70 bg-white/80 backdrop-blur-xl backdrop-saturate-150">
      <div className="mx-auto flex max-w-[1400px] items-center justify-between px-6 py-3">
        <Logo badge={badge} />

        <div className="flex items-center gap-3">
          <button
            className="relative rounded-full p-2 text-sand-500 transition-colors hover:bg-sand-100 hover:text-sand-800"
            title="Notifications"
          >
            <Bell size={19} strokeWidth={2} />
            <span className="absolute right-1.5 top-1.5 h-2 w-2 rounded-full bg-accent-500 ring-2 ring-white" />
          </button>

          <div className="h-7 w-px bg-sand-200" />

          <div className="flex items-center gap-2.5 rounded-full py-1 pl-1 pr-2 transition-colors hover:bg-sand-50">
            <Avatar
              name={user.name}
              color={user.role === 'admin' ? 'red' : 'blue'}
              size={34}
              ring={false}
            />
            <div className="hidden sm:block">
              <div className="text-[13px] font-bold leading-tight tracking-tight text-sand-900">
                {user.name || 'User'}
              </div>
              <div className="text-[11px] font-medium text-sand-500">{sub}</div>
            </div>
          </div>

          <button
            onClick={handleLogout}
            className="rounded-full p-2 text-sand-500 transition-colors hover:bg-danger-50 hover:text-danger-600"
            title="Log out"
          >
            <LogOut size={19} strokeWidth={2} />
          </button>
        </div>
      </div>
    </header>
  )
}
