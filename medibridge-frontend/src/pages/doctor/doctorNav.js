import { Home, Calendar, Users, Clock, Wallet, Settings } from 'lucide-react'

export const doctorNav = [
  { to: '/doctor', label: 'Overview', icon: Home, end: true },
  { to: '/doctor/appointments', label: 'Appointments', icon: Calendar },
  { to: '/doctor/patients', label: 'Patient Records', icon: Users },
  { to: '/doctor/schedule', label: 'Schedule', icon: Clock },
  { to: '/doctor/earnings', label: 'Earnings', icon: Wallet },
  { to: '/doctor/settings', label: 'Settings', icon: Settings },
]
