import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useDispatch, useSelector } from 'react-redux'
import { Clock, FileText, CheckCircle2, XCircle, CalendarDays, CalendarClock, UserX } from 'lucide-react'
import DashboardLayout from '../../components/layout/DashboardLayout'
import Avatar from '../../components/common/Avatar'
import Button from '../../components/common/Button'
import JoinButton from '../../components/common/JoinButton'
import Badge from '../../components/common/Badge'
import RescheduleModal from '../../components/common/RescheduleModal'
import { doctorNav } from './doctorNav'
import { fetchDoctorDashboard } from '../../features/appointments/appointmentsSlice'
import { appointmentService } from '../../services/appointmentService'

export default function DoctorAppointments() {
  const dispatch = useDispatch()
  const navigate = useNavigate()
  const { today = [], upcoming = [], pending = [], completed = [] } =
    useSelector((s) => s.appointments.doctor)

  const [busy, setBusy] = useState(null)
  const [msg, setMsg] = useState(null)
  const [rescheduling, setRescheduling] = useState(null)

  useEffect(() => { dispatch(fetchDoctorDashboard()) }, [dispatch])

  const notify = (text, error = false) => {
    setMsg({ text, error })
    setTimeout(() => setMsg(null), 6000)
  }

  const complete = async (a) => {
    setBusy(`k-${a.appointment_id}`)
    try {
      await appointmentService.completeAppointment(a.appointment_id)
      dispatch(fetchDoctorDashboard())
      notify('Consultation marked complete.')
    } catch (err) {
      notify(err?.response?.data?.message || 'Could not complete this consultation.', true)
    } finally {
      setBusy(null)
    }
  }

  /**
   * A doctor cancelling always refunds the patient in full - they did nothing
   * wrong. The confirm text says so, because the doctor should know the cost.
   */
  const cancel = async (a) => {
    const reason = window.prompt(
      'Cancelling refunds the patient in full and frees the slot.\n\n'
      + 'Reason (the patient will see this):')

    if (reason === null) return

    setBusy(`c-${a.appointment_id}`)
    try {
      await appointmentService.cancelAppointment(a.appointment_id, reason || null)
      dispatch(fetchDoctorDashboard())
      notify('Appointment cancelled and the patient refunded in full.')
    } catch (err) {
      notify(err?.response?.data?.message || 'Could not cancel this appointment.', true)
    } finally {
      setBusy(null)
    }
  }

  /**
   * Offered on Today as well as Upcoming: the reschedule_min_hours cutoff is
   * enforced for patients only, so a doctor with an emergency can still move a
   * slot that starts in ten minutes.
   */
  const rescheduleBtn = (a) => (
    <Button variant="outline" className="px-4 py-1.5" onClick={() => setRescheduling(a)}>
      <CalendarClock size={14} /> Reschedule
    </Button>
  )

  const Row = ({ a, actions, muted }) => (
    <div className={`flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-sand-100 p-4 transition ${
      muted ? '' : 'bg-sand-25 hover:border-primary-200 hover:bg-white'}`}>
      <div className="flex items-center gap-4">
        <Avatar name={a.patient} size={46} color={muted ? 'gray' : 'solid'} />
        <div>
          <div className="font-bold text-sand-900">{a.patient}</div>
          <div className="text-sm text-sand-500">
            {a.age != null ? `${a.age} years • ` : ''}{a.type}
          </div>
          <div className="mt-1 flex items-center gap-3 text-xs text-sand-400">
            <span className="flex items-center gap-1">
              <Clock size={12} /> {a.appointment_date} at {a.time}
            </span>
            {a.reason && <span className="text-sand-500">{a.reason}</span>}
          </div>
        </div>
      </div>
      <div className="flex flex-wrap items-center gap-2">{actions}</div>
    </div>
  )

  const Section = ({ title, count, chipClass, children }) => (
    <div className="surface mt-6 p-6">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-extrabold text-sand-900">{title}</h2>
        {count > 0 && <span className={`chip ${chipClass || ''}`}>{count}</span>}
      </div>
      <div className="mt-4 space-y-3">{children}</div>
    </div>
  )

  const empty = (text) => (
    <div className="flex flex-col items-center gap-2 rounded-2xl border border-dashed border-sand-200 py-8 text-center">
      <CalendarDays size={22} className="text-sand-300" />
      <p className="text-sm text-sand-500">{text}</p>
    </div>
  )

  return (
    <DashboardLayout badge="Doctor" navItems={doctorNav}>
      <span className="eyebrow">Your consultations</span>
      <h1 className="mt-1 text-display-sm text-sand-900">Appointments</h1>
      <p className="mt-1 text-sand-500">
        Booked slots confirm automatically once the patient pays — no accept/reject step.
      </p>

      {msg && (
        <div className={`mt-5 rounded-xl px-4 py-2.5 text-sm ${
          msg.error ? 'bg-danger-50 text-danger-600' : 'bg-success-50 text-success-700'}`}>{msg.text}</div>
      )}

      <Section title="Today" count={today.length} chipClass="border-primary-100 bg-primary-50 text-primary-700">
        {today.length === 0 && empty('Nothing scheduled today.')}
        {today.map((a) => (
          <Row key={a.appointment_id} a={a} actions={<>
            <JoinButton appointment={a} label="Start consultation" onError={(m) => notify(m, true)} />
            <Button variant="outline" className="px-4 py-1.5"
              onClick={() => navigate(`/doctor/prescribe/${a.appointment_id}`)}>
              <FileText size={14} /> Prescribe
            </Button>
            {rescheduleBtn(a)}
          </>} />
        ))}
      </Section>

      {/* Past their scheduled time but not yet written up - the real to-do list. */}
      <Section title="Awaiting consultation notes" count={pending.length}
        chipClass="border-warning-100 bg-warning-50 text-warning-700">
        {pending.length === 0 && empty('Nothing awaiting notes.')}
        {pending.map((a) => (
          <Row key={a.appointment_id} a={a} actions={<>
            <Button className="px-4 py-1.5"
              onClick={() => navigate(`/doctor/prescribe/${a.appointment_id}`)}>
              <FileText size={14} /> Write prescription
            </Button>
            <Button variant="outline" className="px-4 py-1.5"
              disabled={busy === `k-${a.appointment_id}`}
              onClick={() => complete(a)}>
              <CheckCircle2 size={14} /> Mark complete
            </Button>
          </>} />
        ))}
      </Section>

      <Section title="Upcoming" count={upcoming.length}>
        {upcoming.length === 0 && empty('No upcoming appointments.')}
        {upcoming.map((a) => (
          <Row key={a.appointment_id} a={a} actions={<>
            <Badge status={a.status} />
            {rescheduleBtn(a)}
            <Button variant="danger" className="px-4 py-1.5"
              disabled={busy === `c-${a.appointment_id}`}
              onClick={() => cancel(a)}>
              <XCircle size={14} /> Cancel
            </Button>
          </>} />
        ))}
      </Section>

      {/* Holds no-shows as well as completed consultations - both are finished
          slots the doctor was scheduled for, and a patient no-show was paid. */}
      <Section title="Past consultations" count={completed.length}
        chipClass="border-success-100 bg-success-50 text-success-700">
        {completed.length === 0 && empty('No past consultations yet.')}
        {completed.map((a) => (
          <Row key={a.appointment_id} a={a} muted actions={
            a.no_show_by
              ? <span className="chip border-sand-300 bg-sand-100 text-sand-700">
                  <UserX size={13} />
                  {a.no_show_by === 'PATIENT' ? 'Patient no-show — paid' : 'Not attended'}
                </span>
              : <span className="chip border-success-100 bg-success-50 text-success-700">
                  <CheckCircle2 size={13} /> Completed
                </span>
          } />
        ))}
      </Section>

      {rescheduling && (
        <RescheduleModal
          appointment={rescheduling}
          as="doctor"
          onClose={() => setRescheduling(null)}
          onDone={(text) => {
            setRescheduling(null)
            dispatch(fetchDoctorDashboard())
            notify(text)
          }}
        />
      )}
    </DashboardLayout>
  )
}
