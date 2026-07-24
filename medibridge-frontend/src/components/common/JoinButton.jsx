import { useState } from 'react'
import { Video } from 'lucide-react'
import { appointmentService } from '../../services/appointmentService'

/**
 * Video-consultation join control.
 *
 * The room URL is never in the appointment list — the server treats it as a
 * bearer secret. This button fetches it on click, and only the backend decides
 * whether the caller may have it (right person, paid, inside the window).
 *
 * `appointment.can_join` is present (true/false) only when a room exists for a
 * participant, so its absence hides the control entirely (e.g. admin views).
 */
export default function JoinButton({ appointment, label = 'Join', onError }) {
  const [loading, setLoading] = useState(false)

  const canJoin = appointment.can_join
  if (canJoin === undefined || canJoin === null) return null

  // Room exists but the window has not opened yet.
  if (!canJoin) {
    return (
      <span
        title={`The video room opens at ${appointment.join_from || 'your appointment time'}`}
        className="inline-flex items-center gap-1.5 rounded-full border border-sand-200 bg-sand-50 px-3.5 py-2 text-xs font-semibold text-sand-500">
        <Video size={14} /> Opens {appointment.join_from}
      </span>
    )
  }

  const open = async () => {
    setLoading(true)
    try {
      const link = await appointmentService.getJoinLink(appointment.appointment_id)
      window.open(link, '_blank', 'noopener,noreferrer')
    } catch (err) {
      onError?.(err?.response?.data?.message || 'Could not open the consultation room.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <button
      onClick={open}
      disabled={loading}
      className="inline-flex items-center gap-1.5 rounded-full bg-gradient-to-b from-success-500 to-success-600
                 px-4 py-2 text-sm font-bold text-white shadow-[0_8px_20px_-8px_rgba(3,152,85,.5)]
                 transition-all hover:from-success-600 hover:to-success-700 disabled:opacity-60">
      <Video size={14} /> {loading ? 'Opening…' : label}
    </button>
  )
}
