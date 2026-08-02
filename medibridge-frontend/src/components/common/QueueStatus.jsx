import { Users, Clock3 } from 'lucide-react'

/**
 * "25 min", "1 hr 40 min", "3 hr". A morning slot can leave the last patient of
 * the day nearly five hours out, and "about 295 min wait" is a number nobody
 * converts in their head.
 */
const humanMinutes = (total) => {
  if (total < 60) return `${total} min`
  const hours = Math.floor(total / 60)
  const minutes = total % 60
  return minutes === 0 ? `${hours} hr` : `${hours} hr ${minutes} min`
}

/**
 * Live queue standing for a confirmed appointment happening today.
 *
 * The server sends `queue_position` only for those, so the absence of the field
 * is what hides this - there is no date check to keep in sync here.
 *
 * `delay_minutes` is likewise absent rather than 0 when the doctor is on time,
 * which is why the on-time branch tests the falsy value directly.
 */
export default function QueueStatus({ appointment, as = 'patient' }) {
  const position = appointment.queue_position
  if (position == null) return null

  const delay = appointment.delay_minutes
  const eta = appointment.eta_minutes

  const place = as === 'doctor'
    ? `#${position} in today's queue`
    : `You are #${position}`

  // "Running late" is the headline when it is true: it is the thing that
  // changes what the patient does next. The ETA is the detail behind it, so it
  // only leads when there is no delay to report.
  const detail = delay
    ? `running ~${humanMinutes(delay)} late`
    : eta > 0
      ? `on time · about ${humanMinutes(eta)} wait`
      : 'on time'

  const late = Boolean(delay)

  return (
    <div className={`mt-1.5 inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-semibold ${
      late ? 'bg-warning-50 text-warning-700' : 'bg-primary-50 text-primary-700'}`}>
      {late ? <Clock3 size={12} /> : <Users size={12} />}
      {place} · {detail}
    </div>
  )
}
