const variants = {
  primary: 'bg-primary-600 hover:bg-primary-700 text-white',
  outline: 'border border-primary-600 text-primary-600 hover:bg-primary-50 bg-white',
  danger: 'border border-red-500 text-red-500 hover:bg-red-50 bg-white',
  ghost: 'text-slate-600 hover:bg-slate-100 bg-transparent',
  green: 'bg-green-600 hover:bg-green-700 text-white',
  disabled: 'bg-slate-200 text-slate-400 cursor-not-allowed',
}

export default function Button({ variant = 'primary', className = '', children, disabled, ...props }) {
  const style = disabled ? variants.disabled : variants[variant]
  return (
    <button
      disabled={disabled}
      className={`inline-flex items-center justify-center gap-2 rounded-lg px-4 py-2.5 text-sm font-semibold transition ${style} ${className}`}
      {...props}
    >
      {children}
    </button>
  )
}
