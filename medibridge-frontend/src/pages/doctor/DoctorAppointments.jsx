import { useEffect } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { Clock, CheckCircle2, XCircle, Check } from 'lucide-react'
import DashboardLayout from '../../components/layout/DashboardLayout'
import Card from '../../components/common/Card'
import Avatar from '../../components/common/Avatar'
import Button from '../../components/common/Button'
import { doctorNav } from './doctorNav'
import { fetchDoctorDashboard } from '../../features/appointments/appointmentsSlice'

export default function DoctorAppointments() {
  const dispatch = useDispatch()
  const { today, pending, completed } = useSelector((s) => s.appointments.doctor)
  useEffect(() => { dispatch(fetchDoctorDashboard()) }, [dispatch])

  return (
    <DashboardLayout badge="Doctor" navItems={doctorNav}>
      <h1 className="text-3xl font-extrabold text-slate-900">Appointment Management</h1>

      <Card className="mt-6">
        <h2 className="text-lg font-bold text-slate-900">Today's Appointments</h2>
        <div className="mt-4 space-y-3">
          {today.map((t) => (
            <div key={t.id} className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-slate-100 p-4">
              <div className="flex items-center gap-4">
                <Avatar />
                <div>
                  <div className="font-semibold text-slate-800">{t.name}</div>
                  <div className="text-sm text-slate-500">{t.age} years • {t.type}</div>
                  <div className="mt-1 flex items-center gap-1 text-xs text-slate-400"><Clock size={12} /> {t.time}</div>
                </div>
              </div>
              <div className="flex items-center gap-2">
                <Button variant="outline" className="px-4 py-1.5">View History</Button>
                <Button className="px-4 py-1.5">Start Consultation</Button>
              </div>
            </div>
          ))}
        </div>
      </Card>

      <Card className="mt-6">
        <h2 className="text-lg font-bold text-slate-900">Pending Requests</h2>
        <div className="mt-4 space-y-4">
          {pending.map((p) => (
            <div key={p.id} className="rounded-xl border border-amber-200 bg-amber-50/50 p-4">
              <div className="flex items-start justify-between">
                <div className="flex items-center gap-3">
                  <Avatar color="yellow" />
                  <div>
                    <div className="font-semibold text-slate-800">{p.name}</div>
                    <div className="text-sm text-slate-500">{p.age} years</div>
                  </div>
                </div>
                <div className="text-right text-sm text-slate-500"><div>{p.date}</div><div>{p.time}</div></div>
              </div>
              <div className="mt-3 rounded-lg bg-white p-3 text-sm">
                <div className="text-slate-500">Reason for visit:</div>
                <div className="text-slate-800">{p.reason}</div>
              </div>
              <div className="mt-3 grid gap-2 sm:grid-cols-3">
                <button className="flex items-center justify-center gap-1.5 rounded-lg bg-green-600 py-2.5 text-sm font-semibold text-white hover:bg-green-700"><Check size={15} /> Accept Request</button>
                <button className="rounded-lg bg-amber-500 py-2.5 text-sm font-semibold text-white hover:bg-amber-600">Suggest Different Time</button>
                <button className="flex items-center justify-center gap-1.5 rounded-lg bg-red-500 py-2.5 text-sm font-semibold text-white hover:bg-red-600"><XCircle size={15} /> Decline</button>
              </div>
            </div>
          ))}
        </div>
      </Card>

      <Card className="mt-6">
        <h2 className="text-lg font-bold text-slate-900">Completed Consultations</h2>
        <div className="mt-4 space-y-3">
          {completed.map((c) => (
            <div key={c.id} className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-green-100 bg-green-50/50 p-4">
              <div className="flex items-center gap-3">
                <Avatar color="green" icon={CheckCircle2} />
                <div>
                  <div className="font-semibold text-slate-800">{c.name}</div>
                  <div className="text-sm text-slate-500">{c.age} years • {c.time}</div>
                  <div className="text-sm text-slate-500">Diagnosis: {c.diagnosis}</div>
                </div>
              </div>
              <div className="flex items-center gap-3">
                <span className={`rounded-full px-3 py-1 text-xs font-medium ${c.prescription ? 'bg-blue-100 text-primary-600' : 'bg-slate-100 text-slate-500'}`}>
                  Prescription: {c.prescription ? 'Yes' : 'No'}
                </span>
                <Button variant="outline" className="px-4 py-1.5">View Details</Button>
              </div>
            </div>
          ))}
        </div>
      </Card>
    </DashboardLayout>
  )
}
