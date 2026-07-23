import { useEffect, useState } from 'react'
import { X, Calendar, Clock } from 'lucide-react'
import Button from './Button'
import { doctorService } from '../../services/doctorService'
import { appointmentService } from '../../services/appointmentService'

/**
 * Slot picker for moving an existing appointment.
 *
 * Only offers slots belonging to the same doctor — the backend rejects a
 * cross-doctor move, since that is a different consultation at a different
 * price, not a reschedule.
 */
export default function RescheduleModal({ appointment, onClose, onDone }) {
  const [date, setDate] = useState('')
  const [slots, setSlots] = useState([])
  const [slot, setSlot] = useState(null)
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState(null)

  useEffect(() => {
    if (!date) return
    setError(null)
    setSlot(null)
    setLoading(true)
    doctorService.getAvailableSlots(appointment.doctor_id, date)
      .then(setSlots)
      .catch((err) => {
        setError(err?.response?.data?.message || 'Could not load slots.')
        setSlots([])
      })
      .finally(() => setLoading(false))
  }, [date, appointment.doctor_id])

  const submit = async () => {
    if (!slot) return
    setError(null)
    setSaving(true)
    try {
      await appointmentService.reschedule(appointment.appointment_id, slot.schedule_id)
      onDone('Appointment rescheduled. Your payment carries over — nothing to pay again.')
    } catch (err) {
      setError(err?.response?.data?.message || 'Could not reschedule.')
      setSaving(false)
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4"
      onClick={onClose}>
      <div className="w-full max-w-lg rounded-2xl bg-white p-6 shadow-xl"
        onClick={(e) => e.stopPropagation()}>
        <div className="flex items-start justify-between">
          <div>
            <h2 className="text-lg font-bold text-slate-900">Reschedule appointment</h2>
            <p className="text-sm text-slate-500">
              {appointment.doctor} · currently {appointment.appointment_date} at {appointment.time}
            </p>
          </div>
          <button onClick={onClose} className="text-slate-400 hover:text-slate-600">
            <X size={20} />
          </button>
        </div>

        {error && (
          <div className="mt-4 rounded-lg bg-red-50 px-4 py-2.5 text-sm text-red-600">{error}</div>
        )}

        <div className="mt-5">
          <label className="block text-sm font-semibold text-slate-700">New date</label>
          <div className="relative mt-1.5">
            <Calendar className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={18} />
            <input type="date" value={date}
              min={new Date().toISOString().split('T')[0]}
              onChange={(e) => setDate(e.target.value)}
              className="w-full rounded-lg border border-slate-300 py-2.5 pl-10 pr-3 text-sm" />
          </div>
        </div>

        {loading && <div className="mt-4 text-sm text-slate-500">Loading slots…</div>}

        {!loading && date && slots.length === 0 && (
          <div className="mt-4 rounded-lg bg-amber-50 px-4 py-2.5 text-sm text-amber-700">
            No slots available on this date. The doctor may not consult that day.
          </div>
        )}

        {slots.length > 0 && (
          <div className="mt-4 grid max-h-48 grid-cols-3 gap-2 overflow-y-auto">
            {slots.map((s) => (
              <button key={s.schedule_id} type="button" onClick={() => setSlot(s)}
                className={`flex items-center justify-center gap-1.5 rounded-lg border py-2 text-sm font-medium transition ${
                  slot?.schedule_id === s.schedule_id
                    ? 'border-primary-600 bg-primary-50 text-primary-600'
                    : 'border-slate-200 text-slate-600 hover:border-primary-300'}`}>
                <Clock size={12} /> {s.label}
              </button>
            ))}
          </div>
        )}

        {/* Stated up front: the whole point of reschedule over cancel+rebook is
            that the money stays put. */}
        <div className="mt-5 rounded-lg bg-blue-50 px-4 py-2.5 text-xs text-primary-700">
          Your payment carries over to the new time — there is nothing to pay again.
        </div>

        <div className="mt-5 flex justify-end gap-3">
          <button onClick={onClose}
            className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50">
            Cancel
          </button>
          <Button onClick={submit} disabled={!slot || saving}
            variant={!slot || saving ? 'disabled' : 'primary'} className="px-5 py-2">
            {saving ? 'Rescheduling…' : 'Confirm new time'}
          </Button>
        </div>
      </div>
    </div>
  )
}
