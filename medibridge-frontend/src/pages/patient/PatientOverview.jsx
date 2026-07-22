import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useDispatch, useSelector } from 'react-redux'
import { Calendar, FileText, Activity, Clock, Download } from 'lucide-react'
import DashboardLayout from '../../components/layout/DashboardLayout'
import Card from '../../components/common/Card'
import Badge from '../../components/common/Badge'
import Avatar from '../../components/common/Avatar'
import { patientNav } from './patientNav'
import { fetchPatientAppointments } from '../../features/appointments/appointmentsSlice'
import { fetchRecords } from '../../features/records/recordsSlice'
import { patientProfileService } from '../../services/profileService'
import { recordService } from '../../services/recordService'

const STAT_CARDS = [
  { key: 'upcomingAppointments', icon: Calendar, label: 'Upcoming Appointments', grad: 'from-blue-500 to-blue-600' },
  { key: 'medicalRecords', icon: FileText, label: 'Medical Records', grad: 'from-green-500 to-green-600' },
  { key: 'completedConsultations', icon: Activity, label: 'Completed Consultations', grad: 'from-purple-500 to-purple-600' },
]

export default function PatientOverview() {
  const dispatch = useDispatch()
  const user = useSelector((s) => s.auth.user)
  const { upcoming } = useSelector((s) => s.appointments.patient)
  const records = useSelector((s) => s.records.list)

  // Counts come from the server rather than being hardcoded, so the tiles match
  // the lists rendered below them.
  const [stats, setStats] = useState({
    upcomingAppointments: 0, medicalRecords: 0, completedConsultations: 0,
  })

  useEffect(() => {
    dispatch(fetchPatientAppointments())
    dispatch(fetchRecords())
    patientProfileService.stats().then(setStats).catch(() => {})
  }, [dispatch])

  const firstName = (user?.name || 'John Doe').replace('Dr. ', '').split(' ')[0]

  return (
    <DashboardLayout navItems={patientNav}>
      <h1 className="text-3xl font-extrabold text-slate-900">Welcome back, {firstName}!</h1>
      <p className="mt-1 text-slate-500">Here's your health overview</p>

      <div className="mt-6 grid gap-5 md:grid-cols-3">
        {STAT_CARDS.map((s) => (
          <div key={s.label} className={`rounded-2xl bg-gradient-to-br ${s.grad} p-6 text-white shadow-sm`}>
            <s.icon size={26} className="opacity-90" />
            <div className="mt-8 text-3xl font-extrabold">{stats[s.key] ?? 0}</div>
            <div className="mt-1 text-sm text-white/90">{s.label}</div>
          </div>
        ))}
      </div>

      <Card className="mt-6">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-bold text-slate-900">Upcoming Appointments</h2>
          <Link to="/patient/appointments" className="text-sm font-semibold text-primary-600">View All</Link>
        </div>
        <div className="mt-4 space-y-3">
          {upcoming.slice(0, 2).map((a) => (
            <div key={a.appointment_id} className="flex items-center justify-between rounded-xl border border-slate-100 p-4">
              <div className="flex items-center gap-3">
                <Avatar />
                <div>
                  <div className="font-semibold text-slate-800">{a.doctor}</div>
                  <div className="text-sm text-slate-500">{a.specialization}</div>
                </div>
              </div>
              <div className="hidden text-sm text-slate-600 sm:block">
                <div className="flex items-center gap-1.5"><Calendar size={14} /> {a.appointment_date}</div>
                <div className="mt-1 flex items-center gap-1.5"><Clock size={14} /> {a.time}</div>
              </div>
              <Badge status={a.status} />
            </div>
          ))}
        </div>
      </Card>

      <Card className="mt-6">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-bold text-slate-900">Recent Medical Records</h2>
          <Link to="/patient/records" className="text-sm font-semibold text-primary-600">View All</Link>
        </div>
        <div className="mt-4 space-y-3">
          {records.slice(0, 3).map((r) => (
            <div key={r.report_id} className="flex items-center justify-between rounded-xl border border-slate-100 p-4">
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-green-100 text-green-600"><FileText size={18} /></div>
                <div>
                  <div className="font-semibold text-slate-800">{r.report_name}</div>
                  <div className="text-sm text-slate-500">{r.upload_date} • {r.size}</div>
                </div>
              </div>
              <button title="Download"
                onClick={() => recordService.download(r.report_id, r.report_name)}
                className="text-primary-600 hover:text-primary-700">
                <Download size={18} />
              </button>
            </div>
          ))}
        </div>
      </Card>
    </DashboardLayout>
  )
}
