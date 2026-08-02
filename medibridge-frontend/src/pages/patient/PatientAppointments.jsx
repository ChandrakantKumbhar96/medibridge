import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useDispatch, useSelector } from 'react-redux'
import {
  Calendar, Clock, Star, CreditCard, CalendarClock, Plus, CalendarX2, Gift,
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
import QueueStatus from '../../components/common/QueueStatus'

const money = (n) => `₹${Number(n ?? 0).toLocaleString('en-IN')}`

/**
 * Whether this consultation's free revisit is still claimable.
 *
 * The server sends `follow_up_eligible_until` only on a completed consultation
 * that has not spent its revisit, so the deadline is the whole rule - there is
 * no status or count to re-check here. The clock is still worth testing on the
 * client: a tab left open overnight would otherwise keep offering a window that
 * closed hours ago, and the server would refuse it.
 */
const followUpOpen = (a) =>
  !!a.follow_up_eligible_until && new Date(a.follow_up_eligible_until) > new Date()

const untilDate = (iso) =>
  new Date(iso).toLocaleDateString('en-IN', { day: 'numeric', month: 'short' })

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
   *
   * `free_cancellation` is set when the doctor moved the appointment. Showing
   * the fee warning then would be threatening a charge the server will not
   * make - and would push people into keeping a slot they never picked.
   */
  const cancel = async (a) => {
    const reason = window.prompt(
      (a.free_cancellation
        ? 'Your doctor moved this appointment, so cancelling is free - you will be refunded in full.\n\n'
        : 'Cancelling may incur a charge if your appointment is within 24 hours.\n\n')
      + 'Reason for cancelling (optional):')

    if (reason === null) return   // dismissed the dialog

    setBusy(`c-${a.appointment_id}`)
    try {
      await appointmentService.cancelAppointment(a.appointment_id, reason || null)
      dispatch(fetchPatientAppointments())
      notify(a.free_cancellation
        ? 'Appointment cancelled. You have been refunded in full.'
        : 'Appointment cancelled. Any refund due has been issued.')
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
                  {/* Present only when the visit is for a dependent — an account
                      with several people in it needs to say whose slot this is
                      before the date, not after. */}
                  {a.family_member_id && (
                    <div className="mt-1 inline-flex items-center gap-1 rounded-full bg-accent-50 px-2 py-0.5 text-[11px] font-bold text-accent-700">
                      For {a.patient}{a.booked_for ? ` • ${a.booked_for}` : ''}
                    </div>
                  )}
                  <div className="mt-1.5 flex flex-wrap items-center gap-2 text-xs text-sand-500">
                    <span className="flex items-center gap-1"><Calendar size={12} /> {a.appointment_date}</span>
                    <span className="flex items-center gap-1"><Clock size={12} /> {a.time}</span>
                    {a.consultation_fee != null && <span className="font-semibold text-sand-700">{money(a.consultation_fee)}</span>}
                  </div>

                  {/* Where they stand in today's queue, and whether the clinic
                      is behind. Only present for today's confirmed bookings -
                      the component hides itself otherwise. */}
                  <QueueStatus appointment={a} />

                  {/* The patient never chose this time, so they need to know the
                      usual late-cancellation charge does not apply - before
                      they decide whether to keep it. */}
                  {a.free_cancellation && (
                    <div className="mt-1.5 text-xs font-semibold text-warning-700">
                      Moved by your doctor — cancel any time for a full refund
                    </div>
                  )}
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

                  {/* A no-show is the one past outcome where the patient may
                      have been charged, so it has to say who missed it. Left
                      unexplained it reads as an unexplained deduction. */}
                  {a.no_show_by === 'PATIENT' && (
                    <div className="mt-1 text-xs font-semibold text-sand-700">
                      You did not join — this consultation was not refunded
                    </div>
                  )}
                  {(a.no_show_by === 'DOCTOR' || a.no_show_by === 'BOTH') && (
                    <div className="mt-1 text-xs font-semibold text-success-700">
                      Did not take place — you were refunded in full
                    </div>
                  )}

                  {/* Says when it runs out, not just that it exists. "Free
                      follow-up available" with no date is the kind of offer
                      people put off until it has quietly expired. */}
                  {followUpOpen(a) && (
                    <div className="mt-1 text-xs font-semibold text-success-700">
                      Free follow-up with {a.doctor} until {untilDate(a.follow_up_eligible_until)}
                    </div>
                  )}
                </div>
              </div>
              <div className="flex flex-wrap items-center gap-2">
                <Badge status={a.status} />

                {/* Straight into the booking funnel with the doctor already
                    fixed - a follow-up is a revisit to the same person, so
                    there is no specialty or doctor left to choose. */}
                {followUpOpen(a) && (
                  <Button variant="primary" className="px-3 py-1.5"
                    onClick={() => navigate('/patient/book', {
                      state: {
                        doctorId: a.doctor_id,
                        followUpOf: a.appointment_id,
                        consultType: 'Follow-up',
                        reason: a.reason || '',
                      },
                    })}>
                    <Gift size={14} /> Book free follow-up
                  </Button>
                )}

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
