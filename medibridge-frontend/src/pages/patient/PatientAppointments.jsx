import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useDispatch, useSelector } from 'react-redux'
import {
  Calendar, Clock, Star, CreditCard, CalendarClock, Plus, CalendarX2,
} from 'lucide-react'
import DashboardLayout from '../../components/layout/DashboardLayout'
import Badge from '../../components/common/Badge'
import Avatar from '../../components/common/Avatar'
import Button from '../../components/common/Button'
import { patientNav } from './patientNav'
import { fetchPatientAppointments } from '../../features/appointments/appointmentsSlice'
import { appointmentService } from '../../services/appointmentService'
import RescheduleModal from '../../components/common/RescheduleModal'
import JoinButton from '../../components/common/JoinButton'

const money = (n) => `₹${Number(n ?? 0).toLocaleString('en-IN')}`

export default function PatientAppointments() {
  const dispatch = useDispatch()
  const navigate = useNavigate()
  const { upcoming, past } = useSelector((s) => s.appointments.patient)

  const [busy, setBusy] = useState(null)
  const [msg, setMsg] = useState(null)
  const [rescheduling, setRescheduling] = useState(null)

  useEffect(() => { dispatch(fetchPatientAppointments()) }, [dispatch])

  const notify = (text, error = false) => {
    setMsg({ text, error })
    setTimeout(() => setMsg(null), 6000)
  }

  /**
   * Cancelling refunds automatically on the server: the full amount outside the
   * free-cancellation window, a partial amount inside it. The warning here
   * mirrors that so the patient is not surprised.
   */
  const cancel = async (a) => {
    const reason = window.prompt(
      'Cancelling may incur a charge if your appointment is within 24 hours.\n\n'
      + 'Reason for cancelling (optional):')

    if (reason === null) return   // dismissed the dialog

    setBusy(`c-${a.appointment_id}`)
    try {
      await appointmentService.cancelAppointment(a.appointment_id, reason || null)
      dispatch(fetchPatientAppointments())
      notify('Appointment cancelled. Any refund due has been issued.')
    } catch (err) {
      notify(err?.response?.data?.message || 'Could not cancel this appointment.', true)
    } finally {
      setBusy(null)
    }
  }

  return (
    <DashboardLayout navItems={patientNav}>
      {/* ---- Header ---- */}
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <span className="eyebrow">Your care</span>
          <h1 className="mt-1 text-display-sm text-sand-900">My appointments</h1>
          <p className="mt-1 text-sand-500">Join, reschedule or review your consultations.</p>
        </div>
        <Link to="/patient/book"
          className="inline-flex items-center gap-2 rounded-full bg-primary-600 px-5 py-2.5 text-sm font-bold text-white shadow-soft transition hover:bg-primary-700">
          <Plus size={16} /> Book new appointment
        </Link>
      </div>

      {msg && (
        <div className={`mt-5 rounded-xl px-4 py-2.5 text-sm ${
          msg.error ? 'bg-danger-50 text-danger-600' : 'bg-success-50 text-success-700'}`}>{msg.text}</div>
      )}

      {/* ---- Upcoming ---- */}
      <div className="surface mt-6 p-6">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-extrabold text-sand-900">Upcoming</h2>
          {upcoming.length > 0 && (
            <span className="chip-primary">{upcoming.length} scheduled</span>
          )}
        </div>
        <div className="mt-4 space-y-3">
          {upcoming.length === 0 && (
            <div className="flex flex-col items-center gap-2 rounded-2xl border border-dashed border-sand-200 py-10 text-center">
              <Calendar size={26} className="text-sand-300" />
              <p className="text-sm text-sand-500">No upcoming appointments.</p>
              <Link to="/patient/book" className="text-sm font-bold text-primary-600">Book one now →</Link>
            </div>
          )}
          {upcoming.map((a) => (
            <div key={a.appointment_id}
              className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-sand-100 bg-sand-25 p-4 transition hover:border-primary-200 hover:bg-white">
              <div className="flex items-center gap-3">
                <Avatar name={a.doctor} size={46} color="solid" />
                <div>
                  <div className="font-bold text-sand-900">{a.doctor}</div>
                  <div className="text-sm text-primary-700">{a.specialization}</div>
                  <div className="mt-1.5 flex flex-wrap items-center gap-2 text-xs text-sand-500">
                    <span className="flex items-center gap-1"><Calendar size={12} /> {a.appointment_date}</span>
                    <span className="flex items-center gap-1"><Clock size={12} /> {a.time}</span>
                    {a.consultation_fee != null && <span className="font-semibold text-sand-700">{money(a.consultation_fee)}</span>}
                  </div>
                </div>
              </div>
              <div className="flex flex-wrap items-center gap-2">
                <Badge status={a.status} />

                {/* Unpaid holds expire, so surface the way to finish paying. */}
                {a.status === 'pending' && !a.meeting_link && (
                  <Button variant="primary" className="px-3 py-1.5"
                    onClick={() => navigate('/patient/book')}>
                    <CreditCard size={14} /> Complete payment
                  </Button>
                )}

                {/* Fetches the room link on click, only inside the join window. */}
                <JoinButton appointment={a} label="Join"
                  onError={(m) => notify(m, true)} />

                {/* Only a confirmed appointment can move - an unpaid hold has
                    nothing to carry over, so cancel/rebook is the right path. */}
                {a.status === 'confirmed' && (
                  <Button variant="outline" className="px-3 py-1.5"
                    onClick={() => setRescheduling(a)}>
                    <CalendarClock size={14} /> Reschedule
                  </Button>
                )}

                <Button variant="danger" className="px-3 py-1.5"
                  disabled={busy === `c-${a.appointment_id}`}
                  onClick={() => cancel(a)}>
                  {busy === `c-${a.appointment_id}` ? 'Cancelling…' : 'Cancel'}
                </Button>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* ---- Past ---- */}
      <div className="surface mt-6 p-6">
        <h2 className="text-lg font-extrabold text-sand-900">Past appointments</h2>
        <div className="mt-4 space-y-3">
          {past.length === 0 && (
            <div className="flex flex-col items-center gap-2 rounded-2xl border border-dashed border-sand-200 py-10 text-center">
              <CalendarX2 size={26} className="text-sand-300" />
              <p className="text-sm text-sand-500">No past appointments yet.</p>
            </div>
          )}
          {past.map((a) => (
            <div key={a.appointment_id}
              className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-sand-100 p-4">
              <div className="flex items-center gap-3">
                <Avatar name={a.doctor} size={44} color="gray" />
                <div>
                  <div className="font-bold text-sand-800">{a.doctor}</div>
                  <div className="text-sm text-sand-500">{a.specialization}</div>
                  <div className="mt-1 text-xs text-sand-500">
                    {a.appointment_date} • {a.time}
                    {a.reason ? ` • ${a.reason}` : ''}
                  </div>
                </div>
              </div>
              <div className="flex items-center gap-2">
                <Badge status={a.status} />
                {/* Only a completed consultation can be reviewed - the server
                    enforces this too, but offering the button otherwise would
                    just produce an error. */}
                {a.status === 'confirmed' && (
                  <Button variant="outline" className="px-4 py-1.5"
                    onClick={() => navigate('/patient/rate', { state: { appointment: a } })}>
                    <Star size={14} /> Rate
                  </Button>
                )}
              </div>
            </div>
          ))}
        </div>
      </div>

      {rescheduling && (
        <RescheduleModal
          appointment={rescheduling}
          onClose={() => setRescheduling(null)}
          onDone={(text) => {
            setRescheduling(null)
            dispatch(fetchPatientAppointments())
            notify(text)
          }}
        />
      )}
    </DashboardLayout>
  )
}
