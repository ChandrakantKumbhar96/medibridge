import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useDispatch, useSelector } from 'react-redux'
import { Calendar, Clock, CheckCircle2, Users, Video, FileText } from 'lucide-react'
import DashboardLayout from '../../components/layout/DashboardLayout'
import Card from '../../components/common/Card'
import Badge from '../../components/common/Badge'
import Button from '../../components/common/Button'
import { doctorNav } from './doctorNav'
import { fetchDoctorDashboard } from '../../features/appointments/appointmentsSlice'

export default function DoctorOverview() {
  const dispatch = useDispatch()
  const navigate = useNavigate()
  const user = useSelector((s) => s.auth.user)
  const { today = [], upcoming = [], pending = [], completed = [] } =
    useSelector((s) => s.appointments.doctor)

  useEffect(() => { dispatch(fetchDoctorDashboard()) }, [dispatch])

  const lastName = (user?.name || 'Dr. Johnson').split(' ').slice(-1)[0]

  // Counts derived from the loaded dashboard, not hardcoded, so the tiles
  // always agree with the lists underneath them.
  const stats = [
    { icon: Calendar, value: today.length, label: "Today's Appointments",
      grad: 'from-blue-500 to-blue-600' },
    { icon: Clock, value: pending.length, label: 'Awaiting Notes',
      grad: 'from-amber-400 to-amber-500' },
    { icon: CheckCircle2, value: completed.length, label: 'Completed',
      grad: 'from-green-500 to-green-600' },
    { icon: Users, value: upcoming.length, label: 'Upcoming',
      grad: 'from-purple-500 to-purple-600' },
  ]

  return (
    <DashboardLayout badge="Doctor" navItems={doctorNav}>
      <h1 className="text-3xl font-extrabold text-slate-900">Welcome, Dr. {lastName}!</h1>
      <p className="mt-1 text-slate-500">Here's your overview for today</p>

      <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {stats.map((s) => (
          <div key={s.label} className={`rounded-2xl bg-gradient-to-br ${s.grad} p-5 text-white shadow-sm`}>
            <s.icon size={24} className="opacity-90" />
            <div className="mt-6 text-3xl font-extrabold">{s.value}</div>
            <div className="mt-1 text-sm text-white/90">{s.label}</div>
          </div>
        ))}
      </div>

      <Card className="mt-6">
        <h2 className="text-lg font-bold text-slate-900">Today's Schedule</h2>
        <div className="mt-4 space-y-3">
          {today.length === 0 && (
            <div className="py-3 text-sm text-slate-500">Nothing scheduled today.</div>
          )}
          {today.map((a) => (
            <div key={a.appointment_id}
              className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-slate-100 p-4">
              <div className="flex items-center gap-4">
                <div className="rounded-lg bg-blue-50 px-3 py-1.5 text-center">
                  <div className="text-[10px] uppercase text-slate-400">Time</div>
                  <div className="text-sm font-bold text-primary-600">{a.time}</div>
                </div>
                <div>
                  <div className="font-semibold text-slate-800">{a.patient}</div>
                  <div className="text-sm text-slate-500">
                    {a.age != null ? `${a.age} years • ` : ''}{a.type}
                  </div>
                </div>
              </div>
              <div className="flex items-center gap-3">
                <Badge status={a.status} />
                {/* Only rendered when the join window is open - the backend
                    withholds the link outside it. */}
                {a.meeting_link && (
                  <a href={a.meeting_link} target="_blank" rel="noopener noreferrer"
                    className="flex items-center gap-1.5 rounded-lg bg-green-600 px-4 py-1.5 text-sm font-semibold text-white hover:bg-green-700">
                    <Video size={14} /> Start Consultation
                  </a>
                )}
              </div>
            </div>
          ))}
        </div>
      </Card>

      {/*
        Replaces the old "Pending Appointment Requests" panel. There are no
        requests to accept any more: paying for a published slot confirms the
        appointment outright. What a doctor actually owes is consultation notes
        for visits that have already happened.
      */}
      <Card className="mt-6">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-bold text-slate-900">Awaiting Consultation Notes</h2>
          {pending.length > 0 && <Badge status="pending">{pending.length} pending</Badge>}
        </div>
        <div className="mt-4 space-y-4">
          {pending.length === 0 && (
            <div className="py-3 text-sm text-slate-500">
              You are up to date - no consultations awaiting notes.
            </div>
          )}
          {pending.map((a) => (
            <div key={a.appointment_id} className="rounded-xl border border-slate-100 p-4">
              <div className="flex items-start justify-between">
                <div>
                  <div className="font-semibold text-slate-800">{a.patient}</div>
                  <div className="text-sm text-slate-500">
                    {a.age != null ? `${a.age} years` : ''}
                  </div>
                </div>
                <div className="text-right text-sm text-slate-500">
                  <div>{a.appointment_date}</div><div>{a.time}</div>
                </div>
              </div>
              {a.reason && (
                <div className="mt-2 text-sm text-slate-500">Reason: {a.reason}</div>
              )}
              <div className="mt-3">
                <Button className="w-full py-2"
                  onClick={() => navigate(`/doctor/prescribe/${a.appointment_id}`)}>
                  <FileText size={15} /> Write Prescription
                </Button>
              </div>
            </div>
          ))}
        </div>
      </Card>
    </DashboardLayout>
  )
}
