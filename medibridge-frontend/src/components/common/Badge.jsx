/**
 * Status pill.
 *
 * Each status carries a small coloured dot alongside the label — a pattern
 * real dashboards use because colour alone fails for colour-blind users and
 * at a glance. Ring + tinted fill instead of a flat block.
 */
const map = {
  confirmed:   { s: 'bg-success-50 text-success-700 ring-success-100', d: 'bg-success-500' },
  active:      { s: 'bg-success-50 text-success-700 ring-success-100', d: 'bg-success-500' },
  Available:   { s: 'bg-success-50 text-success-700 ring-success-100', d: 'bg-success-500' },
  Paid:        { s: 'bg-success-50 text-success-700 ring-success-100', d: 'bg-success-500' },
  completed:   { s: 'bg-success-50 text-success-700 ring-success-100', d: 'bg-success-500' },

  pending:     { s: 'bg-warning-50 text-warning-700 ring-warning-100', d: 'bg-warning-500' },
  Requested:   { s: 'bg-warning-50 text-warning-700 ring-warning-100', d: 'bg-warning-500' },
  Processing:  { s: 'bg-warning-50 text-warning-700 ring-warning-100', d: 'bg-warning-500' },

  inactive:    { s: 'bg-sand-100 text-sand-600 ring-sand-200', d: 'bg-sand-400' },
  Unavailable: { s: 'bg-sand-100 text-sand-600 ring-sand-200', d: 'bg-sand-400' },

  cancelled:   { s: 'bg-danger-50 text-danger-700 ring-danger-100', d: 'bg-danger-500' },
  Cancelled:   { s: 'bg-danger-50 text-danger-700 ring-danger-100', d: 'bg-danger-500' },
  suspended:   { s: 'bg-danger-50 text-danger-700 ring-danger-100', d: 'bg-danger-500' },
  Refunded:    { s: 'bg-accent-50 text-accent-700 ring-accent-200', d: 'bg-accent-500' },

  // Deliberately not the cancelled red: nobody cancelled this, and a patient
  // who reads "cancelled" next to "not refunded" assumes they were charged a
  // cancellation fee. Slate reads as "did not happen", which is what it means.
  no_show:     { s: 'bg-sand-100 text-sand-700 ring-sand-300', d: 'bg-sand-500' },
}

const fallback = { s: 'bg-primary-50 text-primary-700 ring-primary-200', d: 'bg-primary-500' }

/** Statuses whose raw value is not readable once upper-cased by the pill. */
const labels = { no_show: 'No show' }

export default function Badge({ status, children, className = '', dot = true }) {
  const tone = map[status] || fallback
  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1
                  text-[11px] font-bold uppercase tracking-wide ring-1
                  ${tone.s} ${className}`}
    >
      {dot && <span className={`h-1.5 w-1.5 rounded-full ${tone.d}`} />}
      {children || labels[status] || status}
    </span>
  )
}
