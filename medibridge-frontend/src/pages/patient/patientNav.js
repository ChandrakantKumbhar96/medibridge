import { Home, Calendar, Search, FileText, Settings } from 'lucide-react'

export const patientNav = [
  { to: '/patient', label: 'Overview', icon: Home, end: true },
  { to: '/patient/appointments', label: 'Appointments', icon: Calendar },
  { to: '/patient/find-doctors', label: 'Find Doctors', icon: Search },
  { to: '/patient/records', label: 'Medical Records', icon: FileText },
  { to: '/patient/settings', label: 'Settings', icon: Settings },
]
