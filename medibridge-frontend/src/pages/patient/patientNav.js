import { Home, Calendar, Search, FileText, Settings, Sparkles, ClipboardPlus } from 'lucide-react'

export const patientNav = [
  { to: '/patient', label: 'Overview', icon: Home, end: true },
  { to: '/patient/find-doctors', label: 'Find Doctors', icon: Search },
  { to: '/patient/symptom-checker', label: 'Symptom Checker', icon: Sparkles },
  { to: '/patient/second-opinion', label: 'Second Opinion', icon: ClipboardPlus },
  { to: '/patient/appointments', label: 'Appointments', icon: Calendar },
  { to: '/patient/records', label: 'Medical Records', icon: FileText },
  { to: '/patient/settings', label: 'Settings', icon: Settings },
]
