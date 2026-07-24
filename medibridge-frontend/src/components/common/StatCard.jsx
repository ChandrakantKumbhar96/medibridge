/**
 * Dashboard stat tile.
 *
 * Deliberately NOT the saturated `from-primary-500 to-primary-600` gradient block
 * that every scaffolded dashboard uses. This is a white surface with a tinted
 * icon chip and an oversized figure — the pattern real analytics products
 * (Stripe, Linear, Vercel) use, and considerably easier to read.
 */
const tones = {
  blue:   { chip: 'bg-primary-50 text-primary-600', accent: 'from-primary-400/70' },
  green:  { chip: 'bg-success-50 text-success-600', accent: 'from-success-500/60' },
  purple: { chip: 'bg-accent-50 text-accent-600',   accent: 'from-accent-400/60' },
  orange: { chip: 'bg-warning-50 text-warning-600', accent: 'from-warning-500/60' },
}

export default function StatCard({ icon: Icon, value, label, gradient = 'blue', hint }) {
  const tone = tones[gradient] || tones.blue
  return (
    <div className="group relative overflow-hidden rounded-2xl border border-sand-200/70 bg-white p-5 shadow-soft transition-all duration-300 hover:-translate-y-0.5 hover:shadow-card">
      {/* hairline accent along the top edge */}
      <span className={`absolute inset-x-0 top-0 h-[3px] bg-gradient-to-r ${tone.accent} to-transparent`} />
      <div className={`inline-flex rounded-xl p-2.5 ${tone.chip}`}>
        <Icon size={20} strokeWidth={2.2} />
      </div>
      <div className="mt-5 text-[32px] font-extrabold leading-none tracking-[-0.03em] text-sand-900">
        {value}
      </div>
      <div className="mt-1.5 text-sm font-semibold text-sand-500">{label}</div>
      {hint && <div className="mt-0.5 text-xs text-sand-400">{hint}</div>}
    </div>
  )
}
