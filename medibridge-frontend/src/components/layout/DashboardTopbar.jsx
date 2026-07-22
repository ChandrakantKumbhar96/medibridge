import { Bell, LogOut, User, Settings } from 'lucide-react'
import { useDispatch, useSelector } from 'react-redux'
import { useNavigate } from 'react-router-dom'
import Logo from '../common/Logo'
import Avatar from '../common/Avatar'
import { logout } from '../../features/auth/authSlice'

export default function DashboardTopbar({ badge }) {
  const dispatch = useDispatch()
  const navigate = useNavigate()
  const user = useSelector((s) => s.auth.user) || {}
  const sub =
    user.role === 'doctor' ? user.specialization || 'Cardiologist'
    : user.role === 'admin' ? 'System Administrator'
    : 'Patient'
  const isAdmin = user.role === 'admin'

  const handleLogout = () => { dispatch(logout()); navigate('/login') }

  return (
    <header className="sticky top-0 z-20 border-b border-slate-200 bg-white">
      <div className="flex items-center justify-between px-6 py-3.5">
        <Logo badge={badge} />
        <div className="flex items-center gap-4">
          <button className="relative text-slate-500 hover:text-slate-700">
            <Bell size={20} />
            <span className="absolute -right-0.5 -top-0.5 h-2 w-2 rounded-full bg-red-500" />
          </button>
          <div className="h-8 w-px bg-slate-200" />
          <div className="flex items-center gap-2.5">
            <Avatar color={isAdmin ? 'red' : 'blue'} size={36} icon={isAdmin ? Settings : User} />
            <div className="hidden sm:block">
              <div className="text-sm font-semibold leading-tight text-slate-800">{user.name || 'User'}</div>
              <div className="text-xs text-slate-500">{sub}</div>
            </div>
          </div>
          <button onClick={handleLogout} className="text-slate-500 hover:text-red-500" title="Logout">
            <LogOut size={20} />
          </button>
        </div>
      </div>
    </header>
  )
}
