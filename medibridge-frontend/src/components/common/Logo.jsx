/**
 * Wordmark.
 *
 * A custom SVG mark — a pulse line inside a rounded squircle — rather than a
 * stock lucide icon. A recognisable logo is one of the clearest signals that
 * a UI was designed rather than assembled from defaults.
 */
function Mark({ size = 34 }) {
  return (
    <span
      className="relative inline-flex items-center justify-center rounded-[30%]
                 bg-gradient-to-br from-primary-400 via-primary-500 to-primary-700
                 shadow-[0_4px_12px_-3px_rgba(15,133,123,.6)]"
      style={{ width: size, height: size }}
    >
      <svg viewBox="0 0 24 24" width={size * 0.62} height={size * 0.62} fill="none">
        <path
          d="M3 12.5h3.2l1.7-3.4 2.6 6.3 2.2-4.6 1.4 1.7H21"
          stroke="white"
          strokeWidth="2.1"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      </svg>
      <span className="absolute -right-0.5 -top-0.5 h-2 w-2 rounded-full bg-accent-400 ring-2 ring-white" />
    </span>
  )
}

const badgeTone = {
  Admin: 'bg-accent-50 text-accent-700 ring-accent-200',
  Doctor: 'bg-primary-50 text-primary-700 ring-primary-200',
}

export default function Logo({ badge, size = 'md' }) {
  const lg = size === 'lg'
  return (
    <div className="flex items-center gap-2.5">
      <Mark size={lg ? 40 : 34} />
      <span
        className={`font-extrabold tracking-[-0.03em] text-sand-900 ${
          lg ? 'text-[26px]' : 'text-[21px]'
        }`}
      >
        Medi<span className="text-primary-600">Bridge</span>
      </span>
      {badge && (
        <span
          className={`ml-0.5 rounded-full px-2.5 py-0.5 text-[11px] font-bold uppercase
                      tracking-wider ring-1 ${badgeTone[badge] || badgeTone.Doctor}`}
        >
          {badge}
        </span>
      )}
    </div>
  )
}
