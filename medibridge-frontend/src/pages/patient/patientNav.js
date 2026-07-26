import { Home, Calendar, Search, FileText, Settings, Sparkles, ClipboardPlus } from 'lucide-react'

// Core destinations first so the mobile bottom-nav (first four + Settings) is
// sensible; the two extra services sit after and are also surfaced as tiles on
// the home page.
export const patientNav = [
  { to: '/patient', label: 'Home', icon: Home, end: true },
  { to: '/patient/find-doctors', label: 'Doctors', icon: Search },
  { to: '/patient/appointments', label: 'Appointments', icon: Calendar },
  { to: '/patient/records', label: 'Records', icon: FileText },
  { to: '/patient/symptom-checker', label: 'Symptom Checker', icon: Sparkles },
  { to: '/patient/second-opinion', label: 'Second Opinion', icon: ClipboardPlus },
  { to: '/patient/settings', label: 'Settings', icon: Settings },
]
