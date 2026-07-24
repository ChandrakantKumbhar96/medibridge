export function Field({ label, hint, error, children }) {
  return (
    <div className="space-y-1.5">
      {label && (
        <label className="block text-[13px] font-bold tracking-tight text-sand-700">
          {label}
        </label>
      )}
      {children}
      {hint && !error && <p className="text-xs text-sand-500">{hint}</p>}
      {error && <p className="text-xs font-medium text-danger-600">{error}</p>}
    </div>
  )
}

/**
 * Text input.
 *
 * Sits on a faint sand fill rather than pure white, so fields read as
 * recessed against the card — closer to how real product forms look than a
 * plain bordered box. Focus uses a soft brand ring, not the default outline.
 */
export default function Input({ icon: Icon, className = '', invalid, ...props }) {
  return (
    <div className="relative">
      {Icon && (
        <Icon
          className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-sand-400"
          size={17}
        />
      )}
      <input
        className={`w-full rounded-xl border bg-sand-50/60 px-4 py-3 text-sm font-medium
                    text-sand-900 outline-none transition-all duration-200
                    placeholder:font-normal placeholder:text-sand-400
                    hover:border-sand-300 hover:bg-white
                    focus:border-primary-400 focus:bg-white focus:ring-4 focus:ring-primary-500/10
                    disabled:cursor-not-allowed disabled:bg-sand-100 disabled:text-sand-400
                    ${invalid ? 'border-danger-500 focus:ring-danger-500/10' : 'border-sand-200'}
                    ${Icon ? 'pl-11' : ''} ${className}`}
        {...props}
      />
    </div>
  )
}
