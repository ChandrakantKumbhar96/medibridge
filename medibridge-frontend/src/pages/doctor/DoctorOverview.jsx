import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useDispatch, useSelector } from 'react-redux'
import {
  Calendar, Clock, CheckCircle2, CalendarClock, FileText, Wallet,
  ChevronRight, Stethoscope,
} from 'lucide-react'
import DashboardLayout from '../../components/layout/DashboardLayout'
import Badge from '../../components/common/Badge'
import Button from '../../components/common/Button'
import Avatar from '../../components/common/Avatar'
import JoinButton from '../../components/common/JoinButton'
import { doctorNav } from './doctorNav'
import { fetchDoctorDashboard } from '../../features/appointments/appointmentsSlice'

export default function DoctorOverview() {
  const dispatch = useDispatch()
  const navigate = useNavigate()
  const user = useSelector((s) => s.auth.user)
  const { today = [], upcoming = [], pending = [], completed = [] } =
    useSelector((s) => s.appointments.doctor)

  useEffect(() => { dispatch(fetchDoctorDashboard()) }, [dispatch])

  const displayName = (user?.name || 'Doctor').replace(/^Dr\.?\s+/i, '')

  const stats = [
    { icon: Calendar, value: today.length, label: 'Today', sub: 'appointments',
      grad: 'from-primary-500 to-primary-700' },
    { icon: CalendarClock, value: upcoming.length, label: 'Upcoming', sub: 'confirmed',
      grad: 'from-info-500 to-info-600' },
    { icon: Clock, value: pending.length, label: 'Awaiting', sub: 'notes',
      grad: 'from-warning-400 to-warning-600' },
    { icon: CheckCircle2, value: completed.length, label: 'Completed', sub: 'consultations',
      grad: 'from-success-500 to-success-700' },
  ]

  return (
    <DashboardLayout badge="Doctor" navItems={doctorNav}>
      {/* ---- Hero ---- */}
      <div className="animate-fade-up overflow-hidden rounded-4xl bg-gradient-to-br from-primary-700 to-primary-900 bg-mesh-teal p-7 text-white shadow-lift sm:p-9">
        <div className="flex flex-wrap items-center justify-between gap-6">
          <div>
            <span className="text-[11px] font-bold uppercase tracking-[0.16em] text-white/70">
              {new Date().toLocaleDateString('en-IN', { weekday: 'long', day: 'numeric', month: 'long' })}
            </span>
            <h1 className="mt-2 text-display-sm">Welcome, Dr. {displayName}</h1>
            <p className="mt-1 max-w-md text-white/80">
              {today.length > 0
                ? `You have ${today.length} consultation${today.length === 1 ? '' : 's'} scheduled today.`
                : 'No consultations scheduled today — a good time to review records.'}
            </p>
            <div className="mt-5 flex flex-wrap gap-3">
              <button onClick={() => navigate('/doctor/appointments')}
                className="inline-flex items-center gap-2 rounded-full bg-white px-5 py-2.5 text-sm font-bold text-primary-700 shadow-soft transition hover:bg-primary-50">
                <Calendar size={16} /> View schedule
              </button>
              <button onClick={() => navigate('/doctor/earnings')}
                className="inline-flex items-center gap-2 rounded-full border border-white/40 px-5 py-2.5 text-sm font-bold text-white transition hover:bg-white/10">
                <Wallet size={16} /> My earnings
              </button>
            </div>
          </div>
          <div className="hidden h-28 w-28 items-center justify-center rounded-full bg-white/10 backdrop-blur md:flex">
            <Stethoscope size={52} className="text-white/90" />
          </div>
        </div>
      </div>

      {/* ---- Stat tiles ---- */}
      <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {stats.map((s) => (
          <div key={s.label}
            className={`relative overflow-hidden rounded-2xl bg-gradient-to-br ${s.grad} p-5 text-white shadow-card`}>
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-white/15 backdrop-blur">
              <s.icon size={20} />
            </div>
            <div className="mt-6 text-4xl font-extrabold leading-none">{s.value}</div>
            <div className="mt-1.5 text-sm font-medium text-white/85">{s.label} {s.sub}</div>
          </div>
        ))}
      </div>

      {/* ---- Today's schedule ---- */}
      <div className="surface mt-6 p-6">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-extrabold text-sand-900">Today's schedule</h2>
          <button onClick={() => navigate('/doctor/appointments')}
            className="inline-flex items-center gap-1 text-sm font-bold text-primary-600 hover:text-primary-700">
            All appointments <ChevronRight size={15} />
          </button>
        </div>
        <div className="mt-4 space-y-3">
          {today.length === 0 && (
            <div className="flex flex-col items-center gap-2 rounded-2xl border border-dashed border-sand-200 py-10 text-center">
              <Calendar size={26} className="text-sand-300" />
              <p className="text-sm text-sand-500">Nothing scheduled today.</p>
            </div>
          )}
          {today.map((a) => (
            <div key={a.appointment_id}
              className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-sand-100 bg-sand-25 p-4 transition hover:border-primary-200 hover:bg-white">
              <div className="flex items-center gap-4">
                <div className="flex flex-col items-center rounded-xl bg-primary-600 px-3 py-2 text-white">
                  <span className="text-[9px] font-semibold uppercase opacity-80">at</span>
                  <span className="text-sm font-extrabold leading-tight">{a.time?.split(' ')[0]}</span>
                  <span className="text-[9px] font-bold opacity-90">{a.time?.split(' ')[1]}</span>
                </div>
                <div className="flex items-center gap-3">
                  <Avatar name={a.patient} size={42} />
                  <div>
                    <div className="font-bold text-sand-900">{a.patient}</div>
                    <div className="text-sm text-sand-500">
                      {a.age != null ? `${a.age} yrs • ` : ''}{a.type}
                    </div>
                  </div>
                </div>
              </div>
              <div className="flex items-center gap-2">
                <Badge status={a.status} />
                <JoinButton appointment={a} label="Start" />
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* ---- Awaiting notes ---- */}
      <div className="surface mt-6 p-6">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-extrabold text-sand-900">Awaiting consultation notes</h2>
          {pending.length > 0 && (
            <span className="chip border-warning-100 bg-warning-50 text-warning-700">
              {pending.length} pending
            </span>
          )}
        </div>
        <div className="mt-4 grid gap-3 sm:grid-cols-2">
          {pending.length === 0 && (
            <div className="flex items-center gap-2 text-sm text-sand-500">
              <CheckCircle2 size={16} className="text-success-500" />
              You're all caught up — no consultations awaiting notes.
            </div>
          )}
          {pending.map((a) => (
            <div key={a.appointment_id} className="rounded-2xl border border-sand-100 p-4">
              <div className="flex items-center gap-3">
                <Avatar name={a.patient} size={40} />
                <div className="min-w-0 flex-1">
                  <div className="truncate font-bold text-sand-900">{a.patient}</div>
                  <div className="text-xs text-sand-500">
                    {a.appointment_date} • {a.time}
                  </div>
                </div>
              </div>
              {a.reason && (
                <div className="mt-2 line-clamp-1 text-sm text-sand-500">Reason: {a.reason}</div>
              )}
              <Button className="mt-3 w-full py-2"
                onClick={() => navigate(`/doctor/prescribe/${a.appointment_id}`)}>
                <FileText size={15} /> Write Prescription
              </Button>
            </div>
          ))}
        </div>
      </div>
    </DashboardLayout>
  )
}
