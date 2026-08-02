import { useEffect, useRef, useState } from 'react'
import { X, Video, Loader2, CheckCircle2, UserX } from 'lucide-react'
import Button from './Button'
import { appointmentService } from '../../services/appointmentService'

/** How often to ask whether the doctor has arrived. */
const POLL_MS = 5000

/**
 * Waiting room shown between the patient clicking Join and entering the video
 * room.
 *
 * <p>It exists because the video provider is a plain URL: both parties open the
 * same room and nothing tells either of them the other is there. Without this
 * the patient clicks Join, lands in an empty room, and cannot tell whether the
 * doctor is late, in another consultation, or never coming.
 *
 * <p><b>It does not gate entry.</b> The patient may walk in at any time — a
 * doctor who joined a moment ago, or who is already on camera without having
 * re-fetched the link, would otherwise leave the patient locked out of a
 * consultation that is actually happening. The screen reports; it does not
 * decide.
 *
 * <p>The link is fetched once, on open, rather than on entry. That single call
 * is what records the patient as having turned up — so a patient who waits and
 * gives up is still, correctly, counted as present when the no-show sweep
 * decides who failed to attend.
 */
export default function WaitingRoom({ appointment, onClose, onError }) {
  const [link, setLink] = useState(null)
  const [status, setStatus] = useState(null)
  const [fatal, setFatal] = useState(null)
  const [entered, setEntered] = useState(false)
  const timer = useRef(null)

  const doctorHere = status?.doctor_joined === true

  // Fetch the room link once. This is also the moment the patient's attendance
  // is stamped server-side, which is why it happens on open and not on entry.
  useEffect(() => {
    let live = true
    appointmentService.getJoinLink(appointment.appointment_id)
      .then((url) => { if (live) setLink(url) })
      .catch((err) => {
        const message = err?.response?.data?.message
          || 'Could not open the consultation room.'
        if (live) setFatal(message)
        onError?.(message)
      })
    return () => { live = false }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [appointment.appointment_id])

  // Poll until the doctor shows up, then stop — the flag never goes back to
  // false, so there is nothing further to learn.
  useEffect(() => {
    if (fatal) return
    let live = true

    const check = async () => {
      try {
        const next = await appointmentService.getRoomStatus(appointment.appointment_id)
        if (!live) return
        setStatus(next)
        if (!next.doctor_joined) {
          timer.current = setTimeout(check, POLL_MS)
        }
      } catch {
        // A failed poll is not worth interrupting the wait for — the patient can
        // still enter. Try again on the next tick.
        if (live) timer.current = setTimeout(check, POLL_MS)
      }
    }
    check()

    return () => { live = false; clearTimeout(timer.current) }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [appointment.appointment_id, fatal])

  const enter = () => {
    if (!link) return
    window.open(link, '_blank', 'noopener,noreferrer')
    setEntered(true)
  }

  const cancelled = status?.status === 'cancelled'

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-sand-900/50 p-4"
      onClick={onClose}>
      <div className="w-full max-w-md rounded-2xl bg-white p-6 shadow-xl"
        onClick={(e) => e.stopPropagation()}>

        <div className="flex items-start justify-between">
          <div>
            <h2 className="text-lg font-bold text-sand-900">Consultation room</h2>
            <p className="text-sm text-sand-500">
              {appointment.doctor} · {appointment.time}
            </p>
          </div>
          <button onClick={onClose} className="text-sand-400 hover:text-sand-600">
            <X size={20} />
          </button>
        </div>

        {fatal && (
          <div className="mt-4 rounded-lg bg-danger-50 px-4 py-2.5 text-sm text-danger-600">
            {fatal}
          </div>
        )}

        {/* An appointment cancelled while the patient sat here has to stop them
            waiting for someone who is not coming. */}
        {cancelled && (
          <div className="mt-4 flex items-center gap-2 rounded-lg bg-danger-50 px-4 py-2.5 text-sm text-danger-600">
            <UserX size={16} /> This appointment was cancelled. Any refund due has been issued.
          </div>
        )}

        {!fatal && !cancelled && (
          <>
            <div className={`mt-5 flex items-center gap-3 rounded-2xl px-4 py-4 ${
              doctorHere ? 'bg-success-50' : 'bg-sand-50'}`}>
              {doctorHere
                ? <CheckCircle2 size={22} className="shrink-0 text-success-600" />
                : <Loader2 size={22} className="shrink-0 animate-spin text-sand-400" />}
              <div>
                <div className={`font-bold ${doctorHere ? 'text-success-700' : 'text-sand-800'}`}>
                  {doctorHere
                    ? `${appointment.doctor} has joined`
                    : `Waiting for ${appointment.doctor} to join…`}
                </div>
                <div className="mt-0.5 text-xs text-sand-500">
                  {doctorHere
                    ? 'You can enter the consultation now.'
                    : "We'll tell you the moment they open the room. You can go in and wait if you prefer."}
                </div>
              </div>
            </div>

            {/* Said plainly, because it is the difference between "the doctor is
                on camera" and what the platform can actually observe. */}
            {!doctorHere && (
              <p className="mt-3 text-xs text-sand-400">
                This updates every few seconds. Keep this window open — you do not
                need to refresh.
              </p>
            )}

            <div className="mt-5 flex justify-end gap-3">
              <button onClick={onClose}
                className="rounded-lg border border-sand-300 px-4 py-2 text-sm font-semibold text-sand-700 hover:bg-sand-50">
                {entered ? 'Done' : 'Leave'}
              </button>
              <Button onClick={enter} disabled={!link}
                variant={!link ? 'disabled' : 'primary'} className="px-5 py-2">
                <Video size={15} />
                {!link ? 'Preparing…' : entered ? 'Re-open room' : 'Enter consultation'}
              </Button>
            </div>
          </>
        )}
      </div>
    </div>
  )
}
