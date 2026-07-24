/**
 * Content card.
 *
 * Hairline warm border + layered shadow instead of the stock
 * `border-sand-200 shadow-sm`. Pass `hover` for an interactive card that
 * lifts, or `flush` to drop the padding (tables manage their own).
 */
export default function Card({
  className = '',
  children,
  hover = false,
  flush = false,
  ...props
}) {
  return (
    <div
      className={`rounded-2xl border border-sand-200/70 bg-white
                  ${flush ? '' : 'p-6'}
                  ${hover
                    ? 'shadow-card transition-all duration-300 hover:-translate-y-0.5 hover:border-primary-200 hover:shadow-lift'
                    : 'shadow-soft'}
                  ${className}`}
      {...props}
    >
      {children}
    </div>
  )
}
