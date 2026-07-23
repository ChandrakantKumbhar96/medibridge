import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useDispatch, useSelector } from 'react-redux'
import { Calendar, Clock, Video, Star, CreditCard, CalendarClock } from 'lucide-react'
import DashboardLayout from '../../components/layout/DashboardLayout'
import Card from '../../components/common/Card'
import Badge from '../../components/common/Badge'
import Avatar from '../../components/common/Avatar'
import Button from '../../components/common/Button'
import { patientNav } from './patientNav'
import { fetchPatientAppointments } from '../../features/appointments/appointmentsSlice'
import { appointmentService } from '../../services/appointmentService'
import RescheduleModal from '../../components/common/RescheduleModal'

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
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-extrabold text-slate-900">My Appointments</h1>
        <Link to="/patient/book" className="rounded-lg bg-primary-600 px-5 py-2.5 text-sm font-semibold text-white hover:bg-primary-700">
          Book New Appointment
        </Link>
      </div>

      {msg && (
        <div className={`mt-5 rounded-lg px-4 py-2.5 text-sm ${
          msg.error ? 'bg-red-50 text-red-600' : 'bg-green-50 text-green-700'}`}>{msg.text}</div>
      )}

      <Card className="mt-6">
        <h2 className="text-lg font-bold text-slate-900">Upcoming</h2>
        <div className="mt-4 space-y-3">
          {upcoming.length === 0 && (
            <div className="py-4 text-sm text-slate-500">
              No upcoming appointments. Book one to get started.
            </div>
          )}
          {upcoming.map((a) => (
            <div key={a.appointment_id} className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-slate-100 p-4">
              <div className="flex items-center gap-3">
                <Avatar />
                <div>
                  <div className="font-semibold text-slate-800">{a.doctor}</div>
                  <div className="text-sm text-slate-500">{a.specialization}</div>
                  <div className="mt-1 flex items-center gap-3 text-xs text-slate-500">
                    <span className="flex items-center gap-1"><Calendar size={12} /> {a.appointment_date}</span>
                    <span className="flex items-center gap-1"><Clock size={12} /> {a.time}</span>
                    {a.consultation_fee != null && <span>₹{a.consultation_fee}</span>}
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

                {/* Only rendered once the backend says the join window is open. */}
                {a.meeting_link && (
                  <a href={a.meeting_link} target="_blank" rel="noopener noreferrer"
                    className="flex items-center gap-1.5 rounded-lg bg-green-600 px-3 py-1.5 text-sm font-semibold text-white hover:bg-green-700">
                    <Video size={14} /> Join
                  </a>
                )}

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
      </Card>

      <Card className="mt-6">
        <h2 className="text-lg font-bold text-slate-900">Past Appointments</h2>
        <div className="mt-4 space-y-3">
          {past.length === 0 && (
            <div className="py-4 text-sm text-slate-500">No past appointments yet.</div>
          )}
          {past.map((a) => (
            <div key={a.appointment_id} className="flex flex-wrap items-center justify-between gap-3 rounded-xl bg-slate-50 p-4">
              <div className="flex items-center gap-3">
                <Avatar color="gray" />
                <div>
                  <div className="font-semibold text-slate-800">{a.doctor}</div>
                  <div className="text-sm text-slate-500">{a.specialization}</div>
                  <div className="mt-1 text-xs text-slate-500">
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
      </Card>

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
