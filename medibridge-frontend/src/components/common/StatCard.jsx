export default function StatCard({ icon: Icon, value, label, gradient }) {
  const grads = {
    blue: 'from-blue-500 to-blue-600',
    green: 'from-green-500 to-green-600',
    purple: 'from-purple-500 to-purple-600',
    orange: 'from-amber-400 to-amber-500',
  }
  return (
    <div className={`rounded-2xl bg-gradient-to-br ${grads[gradient]} p-5 text-white shadow-sm`}>
      <Icon size={28} className="opacity-90" />
      <div className="mt-6 text-3xl font-extrabold">{value}</div>
      <div className="mt-1 text-sm font-medium text-white/90">{label}</div>
    </div>
  )
}
