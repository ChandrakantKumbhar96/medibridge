/**
 * Button.
 *
 * Pill-shaped (`rounded-full`) rather than the `rounded-lg` default — one of
 * the quickest ways to move away from a stock Tailwind look. Primary uses a
 * subtle gradient plus a brand-tinted shadow so it reads as raised rather
 * than as a flat colour block.
 */
const variants = {
  primary:
    'bg-gradient-to-b from-primary-500 to-primary-600 text-white shadow-[0_1px_2px_rgba(26,24,21,.08),0_8px_20px_-8px_rgba(15,133,123,.55)] ' +
    'hover:from-primary-600 hover:to-primary-700 hover:shadow-[0_2px_4px_rgba(26,24,21,.1),0_12px_28px_-10px_rgba(15,133,123,.65)] ' +
    'active:translate-y-px',
  accent:
    'bg-gradient-to-b from-accent-400 to-accent-500 text-white shadow-[0_1px_2px_rgba(26,24,21,.08),0_8px_20px_-8px_rgba(246,95,62,.5)] ' +
    'hover:from-accent-500 hover:to-accent-600 active:translate-y-px',
  outline:
    'border border-sand-300 bg-white text-sand-700 hover:border-primary-400 hover:bg-primary-50 hover:text-primary-700',
  danger:
    'border border-danger-100 bg-danger-50 text-danger-600 hover:border-danger-500 hover:bg-danger-500 hover:text-white',
  ghost:
    'bg-transparent text-sand-600 hover:bg-sand-100 hover:text-sand-800',
  green:
    'bg-gradient-to-b from-success-500 to-success-600 text-white shadow-[0_8px_20px_-8px_rgba(3,152,85,.5)] hover:from-success-600 hover:to-success-700',
  disabled:
    'bg-sand-100 text-sand-400 cursor-not-allowed shadow-none',
}

export default function Button({
  variant = 'primary',
  className = '',
  children,
  disabled,
  ...props
}) {
  const style = disabled ? variants.disabled : variants[variant] || variants.primary
  return (
    <button
      disabled={disabled}
      className={`inline-flex items-center justify-center gap-2 rounded-full px-5 py-2.5
                  text-sm font-bold tracking-tight transition-all duration-200
                  focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-primary-500/20
                  ${style} ${className}`}
      {...props}
    >
      {children}
    </button>
  )
}
