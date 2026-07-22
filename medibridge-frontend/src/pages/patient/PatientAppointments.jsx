import { useEffect } from 'react'
import { Link } from 'react-router-dom'
import { useDispatch, useSelector } from 'react-redux'
import { Calendar, Clock } from 'lucide-react'
import DashboardLayout from '../../components/layout/DashboardLayout'
import Card from '../../components/common/Card'
import Badge from '../../components/common/Badge'
import Avatar from '../../components/common/Avatar'
import Button from '../../components/common/Button'
import { patientNav } from './patientNav'
import { fetchPatientAppointments, cancelAppointment } from '../../features/appointments/appointmentsSlice'

export default function PatientAppointments() {
  const dispatch = useDispatch()
  const { upcoming, past } = useSelector((s) => s.appointments.patient)

  useEffect(() => { dispatch(fetchPatientAppointments()) }, [dispatch])

  return (
    <DashboardLayout navItems={patientNav}>
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-extrabold text-slate-900">My Appointments</h1>
        <Link to="/patient/book" className="rounded-lg bg-primary-600 px-5 py-2.5 text-sm font-semibold text-white hover:bg-primary-700">
          Book New Appointment
        </Link>
      </div>

      <Card className="mt-6">
        <h2 className="text-lg font-bold text-slate-900">Upcoming</h2>
        <div className="mt-4 space-y-3">
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
                  </div>
                </div>
              </div>
              <div className="flex items-center gap-2">
                <Badge status={a.status} />
                <Button variant="danger" className="px-3 py-1.5" onClick={() => dispatch(cancelAppointment(a.appointment_id))}>Cancel</Button>
                <Button variant="primary" className="px-3 py-1.5">Reschedule</Button>
              </div>
            </div>
          ))}
        </div>
      </Card>

      <Card className="mt-6">
        <h2 className="text-lg font-bold text-slate-900">Past Appointments</h2>
        <div className="mt-4 space-y-3">
          {past.map((a) => (
            <div key={a.appointment_id} className="flex flex-wrap items-center justify-between gap-3 rounded-xl bg-slate-50 p-4">
              <div className="flex items-center gap-3">
                <Avatar color="gray" />
                <div>
                  <div className="font-semibold text-slate-800">{a.doctor}</div>
                  <div className="text-sm text-slate-500">{a.specialization}</div>
                  <div className="mt-1 text-xs text-slate-500">{a.appointment_date} • {a.time} &nbsp;•&nbsp; {a.reason}</div>
                </div>
              </div>
              <Button variant="outline" className="px-4 py-1.5">View Details</Button>
            </div>
          ))}
        </div>
      </Card>
    </DashboardLayout>
  )
}
