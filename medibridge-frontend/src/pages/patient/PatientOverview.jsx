import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useDispatch, useSelector } from 'react-redux'
import {
  Calendar, FileText, Activity, Clock, Download, Search, Video,
  Plus, ChevronRight, HeartPulse,
} from 'lucide-react'
import DashboardLayout from '../../components/layout/DashboardLayout'
import Badge from '../../components/common/Badge'
import Avatar from '../../components/common/Avatar'
import { patientNav } from './patientNav'
import { fetchPatientAppointments } from '../../features/appointments/appointmentsSlice'
import { fetchRecords } from '../../features/records/recordsSlice'
import { patientProfileService } from '../../services/profileService'
import { recordService } from '../../services/recordService'

const STAT_CARDS = [
  { key: 'upcomingAppointments', icon: Calendar, label: 'Upcoming', sub: 'appointments',
    grad: 'from-primary-500 to-primary-700' },
  { key: 'medicalRecords', icon: FileText, label: 'Medical', sub: 'records',
    grad: 'from-success-500 to-success-700' },
  { key: 'completedConsultations', icon: Activity, label: 'Completed', sub: 'consultations',
    grad: 'from-accent-400 to-accent-600' },
]

export default function PatientOverview() {
  const dispatch = useDispatch()
  const user = useSelector((s) => s.auth.user)
  const { upcoming } = useSelector((s) => s.appointments.patient)
  const records = useSelector((s) => s.records.list)

  const [stats, setStats] = useState({
    upcomingAppointments: 0, medicalRecords: 0, completedConsultations: 0,
  })

  useEffect(() => {
    dispatch(fetchPatientAppointments())
    dispatch(fetchRecords())
    patientProfileService.stats().then(setStats).catch(() => {})
  }, [dispatch])

  const firstName = (user?.name || 'there').replace(/^(Dr\.?)\s+/i, '').split(' ')[0]

  return (
    <DashboardLayout navItems={patientNav}>
      {/* ---- Hero ---- */}
      <div className="animate-fade-up overflow-hidden rounded-4xl bg-gradient-to-br from-primary-600 to-primary-800 bg-mesh-teal p-7 text-white shadow-lift sm:p-9">
        <div className="flex flex-wrap items-center justify-between gap-6">
          <div>
            <span className="text-[11px] font-bold uppercase tracking-[0.16em] text-white/70">
              Your health, on your time
            </span>
            <h1 className="mt-2 text-display-sm">Hello, {firstName} 👋</h1>
            <p className="mt-1 max-w-md text-white/80">
              Consult top specialists over secure video, keep your records in one
              place, and never miss a follow-up.
            </p>
            <div className="mt-5 flex flex-wrap gap-3">
              <Link to="/patient/book"
                className="inline-flex items-center gap-2 rounded-full bg-white px-5 py-2.5 text-sm font-bold text-primary-700 shadow-soft transition hover:bg-primary-50">
                <Plus size={16} /> Book appointment
              </Link>
              <Link to="/patient/find-doctors"
                className="inline-flex items-center gap-2 rounded-full border border-white/40 px-5 py-2.5 text-sm font-bold text-white transition hover:bg-white/10">
                <Search size={16} /> Find doctors
              </Link>
            </div>
          </div>
          <div className="hidden h-28 w-28 items-center justify-center rounded-full bg-white/10 backdrop-blur md:flex">
            <HeartPulse size={54} className="text-white/90" />
          </div>
        </div>
      </div>

      {/* ---- Stat tiles ---- */}
      <div className="mt-6 grid gap-4 sm:grid-cols-3">
        {STAT_CARDS.map((s) => (
          <div key={s.key}
            className={`relative overflow-hidden rounded-2xl bg-gradient-to-br ${s.grad} p-5 text-white shadow-card`}>
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-white/15 backdrop-blur">
              <s.icon size={20} />
            </div>
            <div className="mt-6 text-4xl font-extrabold leading-none">{stats[s.key] ?? 0}</div>
            <div className="mt-1.5 text-sm font-medium text-white/85">{s.label} {s.sub}</div>
          </div>
        ))}
      </div>

      <div className="mt-6 grid gap-6 lg:grid-cols-5">
        {/* ---- Upcoming ---- */}
        <div className="lg:col-span-3">
          <div className="surface p-6">
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-extrabold text-sand-900">Upcoming appointments</h2>
              <Link to="/patient/appointments"
                className="inline-flex items-center gap-1 text-sm font-bold text-primary-600 hover:text-primary-700">
                View all <ChevronRight size={15} />
              </Link>
            </div>

            <div className="mt-4 space-y-3">
              {upcoming.length === 0 && (
                <div className="flex flex-col items-center gap-2 rounded-2xl border border-dashed border-sand-200 py-10 text-center">
                  <Calendar size={26} className="text-sand-300" />
                  <p className="text-sm text-sand-500">No upcoming appointments.</p>
                  <Link to="/patient/book" className="text-sm font-bold text-primary-600">Book one now →</Link>
                </div>
              )}
              {upcoming.slice(0, 3).map((a) => (
                <div key={a.appointment_id}
                  className="flex items-center justify-between gap-3 rounded-2xl border border-sand-100 bg-sand-25 p-4 transition hover:border-primary-200 hover:bg-white">
                  <div className="flex min-w-0 items-center gap-3">
                    <Avatar name={a.doctor} size={46} color="solid" />
                    <div className="min-w-0">
                      <div className="truncate font-bold text-sand-900">{a.doctor}</div>
                      <div className="text-sm text-sand-500">{a.specialization}</div>
                    </div>
                  </div>
                  <div className="hidden text-right text-xs text-sand-500 sm:block">
                    <div className="flex items-center justify-end gap-1.5"><Calendar size={13} /> {a.appointment_date}</div>
                    <div className="mt-1 flex items-center justify-end gap-1.5"><Clock size={13} /> {a.time}</div>
                  </div>
                  <Badge status={a.status} />
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* ---- Quick actions ---- */}
        <div className="lg:col-span-2">
          <div className="surface p-6">
            <h2 className="text-lg font-extrabold text-sand-900">Quick actions</h2>
            <div className="mt-4 space-y-2.5">
              {[
                { to: '/patient/find-doctors', icon: Search, label: 'Find a doctor', tone: 'text-primary-600 bg-primary-50' },
                { to: '/patient/book', icon: Video, label: 'Book a consultation', tone: 'text-accent-600 bg-accent-50' },
                { to: '/patient/records', icon: FileText, label: 'View medical records', tone: 'text-success-600 bg-success-50' },
                { to: '/patient/appointments', icon: Calendar, label: 'My appointments', tone: 'text-info-600 bg-info-50' },
              ].map((x) => (
                <Link key={x.to} to={x.to}
                  className="flex items-center gap-3 rounded-2xl border border-sand-100 p-3 transition hover:border-primary-200 hover:bg-sand-25">
                  <span className={`flex h-10 w-10 items-center justify-center rounded-xl ${x.tone}`}>
                    <x.icon size={18} />
                  </span>
                  <span className="flex-1 text-sm font-bold text-sand-800">{x.label}</span>
                  <ChevronRight size={16} className="text-sand-300" />
                </Link>
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* ---- Recent records ---- */}
      <div className="surface mt-6 p-6">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-extrabold text-sand-900">Recent medical records</h2>
          <Link to="/patient/records"
            className="inline-flex items-center gap-1 text-sm font-bold text-primary-600 hover:text-primary-700">
            View all <ChevronRight size={15} />
          </Link>
        </div>
        <div className="mt-4 grid gap-3 sm:grid-cols-2">
          {records.length === 0 && (
            <p className="text-sm text-sand-500">No records uploaded yet.</p>
          )}
          {records.slice(0, 4).map((r) => (
            <div key={r.report_id}
              className="flex items-center justify-between gap-3 rounded-2xl border border-sand-100 p-4 transition hover:border-primary-200">
              <div className="flex min-w-0 items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-success-50 text-success-600">
                  <FileText size={18} />
                </div>
                <div className="min-w-0">
                  <div className="truncate font-bold text-sand-800">{r.report_name}</div>
                  <div className="text-xs text-sand-500">{r.upload_date} • {r.size}</div>
                </div>
              </div>
              <button title="Download"
                onClick={() => recordService.download(r.report_id, r.report_name)}
                className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl text-primary-600 transition hover:bg-primary-50">
                <Download size={17} />
              </button>
            </div>
          ))}
        </div>
      </div>
    </DashboardLayout>
  )
}
