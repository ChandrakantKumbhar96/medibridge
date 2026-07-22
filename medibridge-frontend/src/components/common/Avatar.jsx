import { User } from 'lucide-react'

export default function Avatar({ color = 'blue', size = 40, icon: Icon = User }) {
  const colors = {
    blue: 'bg-blue-100 text-primary-600',
    gray: 'bg-slate-100 text-slate-400',
    yellow: 'bg-yellow-100 text-yellow-600',
    green: 'bg-green-100 text-green-600',
    solid: 'bg-primary-600 text-white',
    red: 'bg-red-500 text-white',
  }
  return (
    <div
      className={`flex flex-shrink-0 items-center justify-center rounded-full ${colors[color]}`}
      style={{ width: size, height: size }}
    >
      <Icon size={size * 0.5} />
    </div>
  )
}
