import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useDispatch, useSelector } from 'react-redux'
import {
  Search, Star, BadgeCheck, Clock, Award, Stethoscope, ChevronRight,
} from 'lucide-react'
import DashboardLayout from '../../components/layout/DashboardLayout'
import Avatar from '../../components/common/Avatar'
import Button from '../../components/common/Button'
import { patientNav } from './patientNav'
import { fetchDoctors } from '../../features/doctors/doctorsSlice'
import { doctorService } from '../../services/doctorService'

const money = (n) => `₹${Number(n ?? 0).toLocaleString('en-IN')}`

export default function FindDoctors() {
  const dispatch = useDispatch()
  const navigate = useNavigate()
  const doctors = useSelector((s) => s.doctors.list)
  const [specializations, setSpecializations] = useState([])
  const [q, setQ] = useState('')
  const [spec, setSpec] = useState('All')
  const [sort, setSort] = useState('rating')

  useEffect(() => { dispatch(fetchDoctors()) }, [dispatch])
  useEffect(() => {
    doctorService.getSpecializations().then(setSpecializations).catch(() => setSpecializations([]))
  }, [])

  const filtered = doctors
    .filter((d) => {
      const matchQ = `${d.full_name} ${d.specialization}`.toLowerCase().includes(q.toLowerCase())
      const matchS = spec === 'All'
        || d.specialization.toLowerCase().startsWith(spec.toLowerCase().slice(0, 5))
      return matchQ && matchS
    })
    .sort((a, b) => {
      if (sort === 'fee') return (a.consultation_fee ?? 0) - (b.consultation_fee ?? 0)
      if (sort === 'experience') return (b.experience_years ?? 0) - (a.experience_years ?? 0)
      return (b.rating ?? 0) - (a.rating ?? 0)
    })

  return (
    <DashboardLayout navItems={patientNav}>
      {/* Header */}
      <div className="animate-fade-up">
        <span className="eyebrow">Find care</span>
        <h1 className="mt-1 text-display-sm text-sand-900">Find a doctor</h1>
        <p className="mt-1 text-sand-500">
          Consult verified specialists over secure video — book in under a minute.
        </p>
      </div>

      {/* Search + filters */}
      <div className="surface mt-6 p-5">
        <div className="relative">
          <Search className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-sand-400" size={18} />
          <input
            value={q}
            onChange={(e) => setQ(e.target.value)}
            placeholder="Search by doctor name or specialty…"
            className="w-full rounded-xl border border-sand-200 bg-sand-50/60 py-3 pl-11 pr-4 text-sm font-medium
                       text-sand-900 outline-none transition-all placeholder:font-normal placeholder:text-sand-400
                       hover:bg-white focus:border-primary-400 focus:bg-white focus:ring-4 focus:ring-primary-500/12"
          />
        </div>

        {/* Specialty chips — the Practo pattern, not a dropdown */}
        <div className="mt-4 flex flex-wrap gap-2">
          {['All', ...specializations].map((s) => (
            <button
              key={s}
              onClick={() => setSpec(s)}
              className={`rounded-full px-3.5 py-1.5 text-xs font-bold transition-all ${
                spec === s
                  ? 'bg-primary-600 text-white shadow-[0_6px_16px_-8px_rgba(15,133,123,.7)]'
                  : 'border border-sand-200 bg-white text-sand-600 hover:border-primary-300 hover:text-primary-700'
              }`}>
              {s}
            </button>
          ))}
        </div>
      </div>

      {/* Result bar */}
      <div className="mt-5 flex items-center justify-between">
        <p className="text-sm font-semibold text-sand-600">
          {filtered.length} doctor{filtered.length === 1 ? '' : 's'} available
        </p>
        <label className="flex items-center gap-2 text-sm text-sand-500">
          Sort by
          <select
            value={sort}
            onChange={(e) => setSort(e.target.value)}
            className="rounded-lg border border-sand-200 bg-white px-2.5 py-1.5 text-xs font-semibold text-sand-700 outline-none focus:border-primary-400">
            <option value="rating">Top rated</option>
            <option value="experience">Most experienced</option>
            <option value="fee">Lowest fee</option>
          </select>
        </label>
      </div>

      {/* Doctor cards */}
      <div className="mt-4 space-y-4">
        {filtered.length === 0 && (
          <div className="surface flex flex-col items-center gap-3 py-14 text-center">
            <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-primary-50 text-primary-500">
              <Stethoscope size={26} />
            </div>
            <p className="font-semibold text-sand-700">No doctors match your search</p>
            <p className="text-sm text-sand-500">Try another specialty or clear the search.</p>
          </div>
        )}

        {filtered.map((d, i) => (
          <div
            key={d.doctor_id}
            style={{ animationDelay: `${Math.min(i * 40, 240)}ms` }}
            className="surface-lift animate-fade-up flex flex-col gap-5 p-5 sm:flex-row">
            {/* Avatar */}
            <div className="relative shrink-0">
              <Avatar name={d.full_name} size={76} color="solid" />
              {d.status === 'active' && (
                <span className="absolute -bottom-1 -right-1 flex h-6 w-6 items-center justify-center rounded-full bg-white">
                  <BadgeCheck size={20} className="text-primary-600" fill="#D6F5EF" />
                </span>
              )}
            </div>

            {/* Details */}
            <div className="min-w-0 flex-1">
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div className="min-w-0">
                  <div className="flex items-center gap-2">
                    <h3 className="truncate text-lg font-extrabold text-sand-900">{d.full_name}</h3>
                    <span className="chip-primary hidden sm:inline-flex">
                      <BadgeCheck size={12} /> Verified
                    </span>
                  </div>
                  <p className="mt-0.5 text-sm font-semibold text-primary-700">{d.specialization}</p>
                </div>

                {/* Fee block */}
                <div className="text-right">
                  <div className="text-2xl font-extrabold text-sand-900">{money(d.consultation_fee)}</div>
                  <div className="text-[11px] font-medium uppercase tracking-wide text-sand-400">
                    per consult
                  </div>
                </div>
              </div>

              {/* Meta chips */}
              <div className="mt-3 flex flex-wrap items-center gap-2">
                <span className="chip">
                  <Star size={13} className="text-warning-500" fill="currentColor" />
                  {Number(d.rating) > 0 ? d.rating : 'New'}
                  {d.rating_count > 0 && <span className="text-sand-400">({d.rating_count})</span>}
                </span>
                <span className="chip">
                  <Award size={13} className="text-primary-500" /> {d.experience_years} yrs exp
                </span>
                <span className="chip">
                  <Clock size={13} className="text-primary-500" /> {d.consultation_duration_min} min
                </span>
                <span className={`chip ${d.available ? 'border-success-100 bg-success-50 text-success-700' : ''}`}>
                  <span className={`h-1.5 w-1.5 rounded-full ${d.available ? 'bg-success-500' : 'bg-sand-300'}`} />
                  {d.available ? 'Available today' : 'Not available'}
                </span>
              </div>

              {/* Bio */}
              {d.bio && (
                <p className="mt-3 line-clamp-2 text-sm leading-relaxed text-sand-500">{d.bio}</p>
              )}

              {/* Actions */}
              <div className="mt-4 flex items-center gap-3">
                <Button
                  variant={d.available ? 'primary' : 'disabled'}
                  disabled={!d.available}
                  onClick={() => navigate('/patient/book', { state: { doctorId: d.doctor_id } })}>
                  Book Appointment <ChevronRight size={16} />
                </Button>
                <Button variant="outline"
                  onClick={() => navigate(`/patient/doctors/${d.doctor_id}`)}>
                  View Profile
                </Button>
              </div>
            </div>
          </div>
        ))}
      </div>
    </DashboardLayout>
  )
}
