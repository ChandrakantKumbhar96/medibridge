const map = {
  confirmed: 'bg-green-100 text-green-700',
  active: 'bg-green-100 text-green-700',
  Available: 'bg-green-100 text-green-700',
  Paid: 'bg-green-100 text-green-700',
  pending: 'bg-yellow-100 text-yellow-700',
  Requested: 'bg-yellow-100 text-yellow-700',
  inactive: 'bg-slate-100 text-slate-500',
  Unavailable: 'bg-slate-100 text-slate-500',
  cancelled: 'bg-red-100 text-red-600',
  Cancelled: 'bg-red-100 text-red-600',
  suspended: 'bg-red-100 text-red-600',
}

export default function Badge({ status, children, className = '' }) {
  const style = map[status] || 'bg-blue-100 text-primary-600'
  return (
    <span className={`inline-flex items-center rounded-full px-3 py-1 text-xs font-medium capitalize ${style} ${className}`}>
      {children || status}
    </span>
  )
}
