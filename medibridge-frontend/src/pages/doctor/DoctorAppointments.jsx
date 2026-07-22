import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useDispatch, useSelector } from 'react-redux'
import { Clock, Video, FileText, CheckCircle2, XCircle } from 'lucide-react'
import DashboardLayout from '../../components/layout/DashboardLayout'
import Card from '../../components/common/Card'
import Avatar from '../../components/common/Avatar'
import Button from '../../components/common/Button'
import Badge from '../../components/common/Badge'
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

  const Row = ({ a, actions }) => (
    <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-slate-100 p-4">
      <div className="flex items-center gap-4">
        <Avatar />
        <div>
          <div className="font-semibold text-slate-800">{a.patient}</div>
          <div className="text-sm text-slate-500">
            {a.age != null ? `${a.age} years • ` : ''}{a.type}
          </div>
          <div className="mt-1 flex items-center gap-3 text-xs text-slate-400">
            <span className="flex items-center gap-1">
              <Clock size={12} /> {a.appointment_date} at {a.time}
            </span>
            {a.reason && <span className="text-slate-500">{a.reason}</span>}
          </div>
        </div>
      </div>
      <div className="flex flex-wrap items-center gap-2">{actions}</div>
    </div>
  )

  return (
    <DashboardLayout badge="Doctor" navItems={doctorNav}>
      <h1 className="text-3xl font-extrabold text-slate-900">Appointment Management</h1>
      <p className="mt-1 text-slate-500">
        Booked slots are confirmed automatically once the patient pays.
      </p>

      {msg && (
        <div className={`mt-5 rounded-lg px-4 py-2.5 text-sm ${
          msg.error ? 'bg-red-50 text-red-600' : 'bg-green-50 text-green-700'}`}>{msg.text}</div>
      )}

      <Card className="mt-6">
        <h2 className="text-lg font-bold text-slate-900">Today</h2>
        <div className="mt-4 space-y-3">
          {today.length === 0 && (
            <div className="py-3 text-sm text-slate-500">Nothing scheduled today.</div>
          )}
          {today.map((a) => (
            <Row key={a.appointment_id} a={a} actions={<>
              {a.meeting_link && (
                <a href={a.meeting_link} target="_blank" rel="noopener noreferrer"
                  className="flex items-center gap-1.5 rounded-lg bg-green-600 px-4 py-1.5 text-sm font-semibold text-white hover:bg-green-700">
                  <Video size={14} /> Start Consultation
                </a>
              )}
              <Button variant="outline" className="px-4 py-1.5"
                onClick={() => navigate(`/doctor/prescribe/${a.appointment_id}`)}>
                <FileText size={14} /> Prescribe
              </Button>
            </>} />
          ))}
        </div>
      </Card>

      {/* Past their scheduled time but not yet written up - the real to-do list. */}
      <Card className="mt-6">
        <h2 className="text-lg font-bold text-slate-900">Awaiting consultation notes</h2>
        <div className="mt-4 space-y-3">
          {pending.length === 0 && (
            <div className="py-3 text-sm text-slate-500">Nothing awaiting notes.</div>
          )}
          {pending.map((a) => (
            <Row key={a.appointment_id} a={a} actions={<>
              <Button className="px-4 py-1.5"
                onClick={() => navigate(`/doctor/prescribe/${a.appointment_id}`)}>
                <FileText size={14} /> Write Prescription
              </Button>
              <Button variant="outline" className="px-4 py-1.5"
                disabled={busy === `k-${a.appointment_id}`}
                onClick={() => complete(a)}>
                <CheckCircle2 size={14} /> Mark Complete
              </Button>
            </>} />
          ))}
        </div>
      </Card>

      <Card className="mt-6">
        <h2 className="text-lg font-bold text-slate-900">Upcoming</h2>
        <div className="mt-4 space-y-3">
          {upcoming.length === 0 && (
            <div className="py-3 text-sm text-slate-500">No upcoming appointments.</div>
          )}
          {upcoming.map((a) => (
            <Row key={a.appointment_id} a={a} actions={<>
              <Badge status={a.status} />
              <Button variant="danger" className="px-4 py-1.5"
                disabled={busy === `c-${a.appointment_id}`}
                onClick={() => cancel(a)}>
                <XCircle size={14} /> Cancel
              </Button>
            </>} />
          ))}
        </div>
      </Card>

      <Card className="mt-6">
        <h2 className="text-lg font-bold text-slate-900">Completed</h2>
        <div className="mt-4 space-y-3">
          {completed.length === 0 && (
            <div className="py-3 text-sm text-slate-500">No completed consultations yet.</div>
          )}
          {completed.map((a) => (
            <Row key={a.appointment_id} a={a} actions={
              <span className="rounded-full bg-green-100 px-3 py-1 text-xs font-medium text-green-700">
                Completed
              </span>
            } />
          ))}
        </div>
      </Card>
    </DashboardLayout>
  )
}
