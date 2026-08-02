import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useDispatch, useSelector } from 'react-redux'
import {
  Calendar, FileText, Search, Video, ChevronRight, ClipboardPlus, Sparkles, Clock,
} from 'lucide-react'
import DashboardLayout from '../../components/layout/DashboardLayout'
import Badge from '../../components/common/Badge'
import Avatar from '../../components/common/Avatar'
import QueueStatus from '../../components/common/QueueStatus'
import { patientNav } from './patientNav'
import { fetchPatientAppointments } from '../../features/appointments/appointmentsSlice'
import { doctorService } from '../../services/doctorService'

// The service row real telemedicine homes lead with — each links to a flow that
// already exists in the app.
const SERVICES = [
  { to: '/patient/find-doctors', icon: Video, label: 'Video consult', desc: 'Talk to a specialist', tone: 'bg-primary-50 text-primary-600' },
  { to: '/patient/second-opinion', icon: ClipboardPlus, label: 'Second opinion', desc: 'Review your reports', tone: 'bg-accent-50 text-accent-600' },
  { to: '/patient/symptom-checker', icon: Sparkles, label: 'Symptom checker', desc: 'Find the right doctor', tone: 'bg-info-50 text-info-600' },
  { to: '/patient/records', icon: FileText, label: 'Health records', desc: 'Reports & prescriptions', tone: 'bg-success-50 text-success-600' },
]

export default function PatientOverview() {
  const dispatch = useDispatch()
  const navigate = useNavigate()
  const user = useSelector((s) => s.auth.user)
  const { upcoming } = useSelector((s) => s.appointments.patient)

  const [specialties, setSpecialties] = useState([])
  const [q, setQ] = useState('')

  useEffect(() => {
    dispatch(fetchPatientAppointments())
    doctorService.getSpecialties().then(setSpecialties).catch(() => setSpecialties([]))
  }, [dispatch])

  const firstName = (user?.name || 'there').replace(/^(Dr\.?)\s+/i, '').split(' ')[0]

  const submitSearch = (e) => {
    e.preventDefault()
    navigate(`/patient/find-doctors${q.trim() ? `?q=${encodeURIComponent(q.trim())}` : ''}`)
  }

  const bySpecialty = (name) => navigate(`/patient/find-doctors?spec=${encodeURIComponent(name)}`)

  return (
    <DashboardLayout navItems={patientNav}>
      {/* ---- Search hero ---- */}
      <div className="animate-fade-up overflow-hidden rounded-4xl bg-gradient-to-br from-primary-700 to-primary-900 bg-mesh-teal p-7 text-white shadow-lift sm:p-10">
        <span className="text-[11px] font-bold uppercase tracking-[0.16em] text-white/70">
          Hello, {firstName} 👋
        </span>
        <h1 className="mt-2 max-w-2xl text-display-sm">Find and book care in a minute</h1>
        <p className="mt-1.5 max-w-lg text-white/75">
          Search verified specialists, consult by secure video, and keep every record in one place.
        </p>

        {/* Big search bar — the Practo/Apollo centrepiece */}
        <form onSubmit={submitSearch}
          className="mt-6 flex max-w-2xl items-center gap-2 rounded-2xl bg-white p-2 shadow-lift">
          <Search className="ml-2 text-sand-400" size={20} />
          <input value={q} onChange={(e) => setQ(e.target.value)}
            placeholder="Search doctors, specialities, symptoms…"
            className="min-w-0 flex-1 bg-transparent px-1 py-2 text-sm font-medium text-sand-900 outline-none placeholder:text-sand-400" />
          <button type="submit"
            className="shrink-0 rounded-xl bg-primary-600 px-5 py-2.5 text-sm font-bold text-white transition hover:bg-primary-700">
            Search
          </button>
        </form>

        {/* quick specialty pills */}
        <div className="mt-4 flex flex-wrap gap-2">
          {specialties.slice(0, 6).map((s) => (
            <button key={s.name} onClick={() => bySpecialty(s.name)}
              className="rounded-full bg-white/12 px-3.5 py-1.5 text-xs font-bold text-white backdrop-blur transition hover:bg-white/25">
              {s.name}
            </button>
          ))}
        </div>
      </div>

      {/* ---- Services ---- */}
      <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {SERVICES.map((s) => (
          <Link key={s.to} to={s.to} className="surface-lift flex items-center gap-3 p-4">
            <span className={`flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl ${s.tone}`}>
              <s.icon size={22} />
            </span>
            <div className="min-w-0">
              <div className="font-extrabold text-sand-900">{s.label}</div>
              <div className="text-xs text-sand-500">{s.desc}</div>
            </div>
          </Link>
        ))}
      </div>

      {/* ---- Specialties ---- */}
      <div className="surface mt-6 p-6">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-extrabold text-sand-900">Consult by specialty</h2>
          <Link to="/patient/find-doctors"
            className="inline-flex items-center gap-1 text-sm font-bold text-primary-600 hover:text-primary-700">
            View all <ChevronRight size={15} />
          </Link>
        </div>
        <div className="mt-4 grid grid-cols-3 gap-3 sm:grid-cols-4 lg:grid-cols-6">
          {specialties.length === 0 && (
            <div className="col-span-full text-sm text-sand-500">Loading specialties…</div>
          )}
          {specialties.map((s) => (
            <button key={s.name} onClick={() => bySpecialty(s.name)}
              className="group rounded-2xl border border-sand-200/70 bg-white p-4 text-center shadow-soft transition-all hover:-translate-y-0.5 hover:border-primary-200 hover:shadow-card">
              <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-sand-50 text-2xl transition-colors group-hover:bg-primary-50">
                {s.emoji}
              </div>
              <div className="mt-2 text-[13px] font-bold text-sand-800">{s.name}</div>
              {s.doctors != null && (
                <div className="text-[11px] text-sand-400">{s.doctors} doctor{s.doctors === 1 ? '' : 's'}</div>
              )}
            </button>
          ))}
        </div>
      </div>

      {/* ---- Upcoming ---- */}
      <div className="surface mt-6 p-6">
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
                  <QueueStatus appointment={a} />
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
    </DashboardLayout>
  )
}
