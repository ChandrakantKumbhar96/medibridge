import { useEffect } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { Calendar, Clock, CheckCircle2, Users, Check } from 'lucide-react'
import DashboardLayout from '../../components/layout/DashboardLayout'
import Card from '../../components/common/Card'
import Badge from '../../components/common/Badge'
import Avatar from '../../components/common/Avatar'
import Button from '../../components/common/Button'
import { doctorNav } from './doctorNav'
import { fetchDoctorDashboard } from '../../features/appointments/appointmentsSlice'

const stats = [
  { icon: Calendar, value: 4, label: "Today's Appointments", grad: 'from-blue-500 to-blue-600' },
  { icon: Clock, value: 3, label: 'Pending Requests', grad: 'from-amber-400 to-amber-500' },
  { icon: CheckCircle2, value: 2, label: 'Completed Today', grad: 'from-green-500 to-green-600' },
  { icon: Users, value: 156, label: 'Total Patients', grad: 'from-purple-500 to-purple-600' },
]

export default function DoctorOverview() {
  const dispatch = useDispatch()
  const user = useSelector((s) => s.auth.user)
  const { today, pending } = useSelector((s) => s.appointments.doctor)
  useEffect(() => { dispatch(fetchDoctorDashboard()) }, [dispatch])

  const lastName = (user?.name || 'Dr. Johnson').split(' ').slice(-1)[0]

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
          {today.map((t) => (
            <div key={t.id} className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-slate-100 p-4">
              <div className="flex items-center gap-4">
                <div className="rounded-lg bg-blue-50 px-3 py-1.5 text-center">
                  <div className="text-[10px] uppercase text-slate-400">Time</div>
                  <div className="text-sm font-bold text-primary-600">{t.time}</div>
                </div>
                <div>
                  <div className="font-semibold text-slate-800">{t.name}</div>
                  <div className="text-sm text-slate-500">{t.age} years • {t.type}</div>
                </div>
              </div>
              <div className="flex items-center gap-3">
                <Badge status={t.status} />
                <Button className="px-4 py-1.5">Start Consultation</Button>
              </div>
            </div>
          ))}
        </div>
      </Card>

      <Card className="mt-6">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-bold text-slate-900">Pending Appointment Requests</h2>
          <Badge status="pending">{pending.length} Pending</Badge>
        </div>
        <div className="mt-4 space-y-4">
          {pending.map((p) => (
            <div key={p.id} className="rounded-xl border border-slate-100 p-4">
              <div className="flex items-start justify-between">
                <div>
                  <div className="font-semibold text-slate-800">{p.name}</div>
                  <div className="text-sm text-slate-500">{p.age} years</div>
                </div>
                <div className="text-right text-sm text-slate-500">
                  <div>{p.date}</div><div>{p.time}</div>
                </div>
              </div>
              <div className="mt-2 text-sm text-slate-500">Reason: {p.reason}</div>
              <div className="mt-3 grid grid-cols-3 gap-2">
                <button className="flex items-center justify-center gap-1.5 rounded-lg bg-green-600 py-2 text-sm font-semibold text-white hover:bg-green-700"><Check size={15} /> Accept</button>
                <button className="rounded-lg bg-amber-500 py-2 text-sm font-semibold text-white hover:bg-amber-600">Reschedule</button>
                <button className="rounded-lg bg-red-500 py-2 text-sm font-semibold text-white hover:bg-red-600">Reject</button>
              </div>
            </div>
          ))}
        </div>
      </Card>
    </DashboardLayout>
  )
}
