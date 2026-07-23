import { Home, Users, UserCog, Calendar, TrendingUp, Wallet, Settings } from 'lucide-react'

export const adminNav = [
  { to: '/admin', label: 'Overview', icon: Home, end: true },
  { to: '/admin/patients', label: 'Manage Patients', icon: Users },
  { to: '/admin/doctors', label: 'Manage Doctors', icon: UserCog },
  { to: '/admin/appointments', label: 'Appointments', icon: Calendar },
  { to: '/admin/payouts', label: 'Doctor Payouts', icon: Wallet },
  { to: '/admin/analytics', label: 'Analytics', icon: TrendingUp },
  { to: '/admin/settings', label: 'System Settings', icon: Settings },
]
