import { Activity } from 'lucide-react'

export default function Logo({ badge, size = 'md' }) {
  const text = size === 'lg' ? 'text-2xl' : 'text-xl'
  return (
    <div className="flex items-center gap-2">
      <Activity className="text-primary-600" strokeWidth={2.5} size={size === 'lg' ? 30 : 26} />
      <span className={`font-extrabold text-slate-900 ${text}`}>MediBridge</span>
      {badge && (
        <span
          className={`ml-1 rounded-full px-2.5 py-0.5 text-xs font-semibold ${
            badge === 'Admin' ? 'bg-red-100 text-red-600' : 'bg-blue-100 text-primary-600'
          }`}
        >
          {badge}
        </span>
      )}
    </div>
  )
}
