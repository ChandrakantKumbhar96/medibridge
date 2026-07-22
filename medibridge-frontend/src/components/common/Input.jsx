export function Field({ label, children }) {
  return (
    <div className="space-y-1.5">
      {label && <label className="block text-sm font-semibold text-slate-700">{label}</label>}
      {children}
    </div>
  )
}

export default function Input({ icon: Icon, className = '', ...props }) {
  return (
    <div className="relative">
      {Icon && <Icon className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={18} />}
      <input
        className={`w-full rounded-lg border border-slate-300 bg-white px-3.5 py-2.5 text-sm text-slate-800 outline-none transition placeholder:text-slate-400 focus:border-primary-600 focus:ring-2 focus:ring-primary-100 ${
          Icon ? 'pl-10' : ''
        } ${className}`}
        {...props}
      />
    </div>
  )
}
