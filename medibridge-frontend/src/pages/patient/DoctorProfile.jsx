import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  Star, BadgeCheck, Award, Clock, ArrowLeft, Stethoscope, ShieldCheck,
  CalendarDays, MessageSquareQuote, Video,
} from 'lucide-react'
import DashboardLayout from '../../components/layout/DashboardLayout'
import Avatar from '../../components/common/Avatar'
import Button from '../../components/common/Button'
import { patientNav } from './patientNav'
import { doctorService } from '../../services/doctorService'
import { reviewService } from '../../services/reviewService'

const money = (n) => `₹${Number(n ?? 0).toLocaleString('en-IN')}`
const today = () => new Date().toISOString().split('T')[0]

/** Row of five stars filled up to `value` (rounds to nearest whole star). */
function Stars({ value = 0, size = 14 }) {
  const v = Math.round(Number(value) || 0)
  return (
    <span className="inline-flex items-center gap-0.5">
      {[1, 2, 3, 4, 5].map((i) => (
        <Star key={i} size={size}
          className={i <= v ? 'text-warning-500' : 'text-sand-300'}
          fill={i <= v ? 'currentColor' : 'none'} />
      ))}
    </span>
  )
}

/**
 * Practo-style single-doctor page: credentials + about + patient reviews on the
 * left, a sticky booking card (date -> live slots -> book) on the right.
 *
 * All three data sources already exist on the backend:
 *   GET /doctors/{id}          -> profile
 *   GET /doctors/{id}/reviews  -> patient reviews
 *   GET /doctors/{id}/slots    -> bookable slots for a date
 * Booking itself hands off to the existing /patient/book flow, so this page
 * adds the discovery/trust layer without touching the booking logic.
 */
export default function DoctorProfile() {
  const { doctorId } = useParams()
  const navigate = useNavigate()

  const [doctor, setDoctor] = useState(null)
  const [reviews, setReviews] = useState([])
  const [loading, setLoading] = useState(true)
  const [notFound, setNotFound] = useState(false)

  const [date, setDate] = useState(today())
  const [slots, setSlots] = useState([])
  const [slot, setSlot] = useState(null)
  const [slotsLoading, setSlotsLoading] = useState(false)

  useEffect(() => {
    setLoading(true)
    setNotFound(false)
    Promise.all([
      doctorService.getDoctor(doctorId),
      reviewService.getForDoctor(doctorId).catch(() => []),
    ])
      .then(([doc, revs]) => {
        if (!doc) { setNotFound(true); return }
        setDoctor(doc)
        setReviews(Array.isArray(revs) ? revs : [])
      })
      .catch(() => setNotFound(true))
      .finally(() => setLoading(false))
  }, [doctorId])

  useEffect(() => {
    if (!doctorId || !date) return
    setSlot(null)
    setSlotsLoading(true)
    doctorService.getAvailableSlots(doctorId, date)
      .then(setSlots)
      .catch(() => setSlots([]))
      .finally(() => setSlotsLoading(false))
  }, [doctorId, date])

  const book = () => {
    navigate('/patient/book', {
      state: { doctorId, scheduleId: slot?.schedule_id, date },
    })
  }

  if (loading) {
    return (
      <DashboardLayout navItems={patientNav}>
        <div className="animate-pulse space-y-4">
          <div className="h-40 rounded-4xl bg-sand-100" />
          <div className="h-64 rounded-2xl bg-sand-100" />
        </div>
      </DashboardLayout>
    )
  }

  if (notFound || !doctor) {
    return (
      <DashboardLayout navItems={patientNav}>
        <div className="surface flex flex-col items-center gap-3 py-16 text-center">
          <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-primary-50 text-primary-500">
            <Stethoscope size={26} />
          </div>
          <p className="font-semibold text-sand-700">Doctor not found</p>
          <Button variant="outline" onClick={() => navigate('/patient/find-doctors')}>
            Back to doctors
          </Button>
        </div>
      </DashboardLayout>
    )
  }

  const rating = Number(doctor.rating) || 0

  return (
    <DashboardLayout navItems={patientNav}>
      <button onClick={() => navigate('/patient/find-doctors')}
        className="mb-4 inline-flex items-center gap-1.5 text-sm font-semibold text-sand-500 transition hover:text-primary-600">
        <ArrowLeft size={16} /> Back to doctors
      </button>

      {/* ---- Header ---- */}
      <div className="surface animate-fade-up p-6 sm:p-7">
        <div className="flex flex-col gap-5 sm:flex-row">
          <div className="relative shrink-0">
            <Avatar name={doctor.full_name} size={92} color="solid" />
            {doctor.status === 'active' && (
              <span className="absolute -bottom-1 -right-1 flex h-7 w-7 items-center justify-center rounded-full bg-white">
                <BadgeCheck size={22} className="text-primary-600" fill="#D6F5EF" />
              </span>
            )}
          </div>

          <div className="min-w-0 flex-1">
            <div className="flex items-center gap-2">
              <h1 className="truncate text-2xl font-extrabold text-sand-900">{doctor.full_name}</h1>
              <span className="chip-primary hidden sm:inline-flex">
                <BadgeCheck size={12} /> Verified
              </span>
            </div>
            <p className="mt-0.5 text-sm font-semibold text-primary-700">{doctor.specialization}</p>

            <div className="mt-3 flex flex-wrap items-center gap-2">
              <span className="chip">
                <Stars value={rating} />
                <span className="ml-1 font-bold text-sand-700">{rating > 0 ? rating.toFixed(1) : 'New'}</span>
                {doctor.rating_count > 0 && (
                  <span className="text-sand-400">({doctor.rating_count})</span>
                )}
              </span>
              <span className="chip"><Award size={13} className="text-primary-500" /> {doctor.experience_years} yrs exp</span>
              <span className="chip"><Clock size={13} className="text-primary-500" /> {doctor.consultation_duration_min} min consult</span>
              <span className={`chip ${doctor.available ? 'border-success-100 bg-success-50 text-success-700' : ''}`}>
                <span className={`h-1.5 w-1.5 rounded-full ${doctor.available ? 'bg-success-500' : 'bg-sand-300'}`} />
                {doctor.available ? 'Available today' : 'Not available'}
              </span>
            </div>
          </div>
        </div>
      </div>

      <div className="mt-6 grid gap-6 lg:grid-cols-3">
        {/* ---- Left: about + reviews ---- */}
        <div className="space-y-6 lg:col-span-2">
          {/* About */}
          <div className="surface p-6">
            <h2 className="text-lg font-extrabold text-sand-900">About</h2>
            <p className="mt-2 text-sm leading-relaxed text-sand-600">
              {doctor.bio || 'This doctor has not added a bio yet.'}
            </p>
            <div className="mt-5 grid gap-3 sm:grid-cols-2">
              <div className="flex items-center gap-3 rounded-2xl border border-sand-100 bg-sand-25 p-3.5">
                <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary-50 text-primary-600">
                  <Award size={18} />
                </span>
                <div>
                  <div className="text-xs text-sand-500">Experience</div>
                  <div className="font-bold text-sand-800">{doctor.experience_years} years</div>
                </div>
              </div>
              <div className="flex items-center gap-3 rounded-2xl border border-sand-100 bg-sand-25 p-3.5">
                <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-success-50 text-success-600">
                  <Video size={18} />
                </span>
                <div>
                  <div className="text-xs text-sand-500">Consultation</div>
                  <div className="font-bold text-sand-800">Video · {doctor.consultation_duration_min} min</div>
                </div>
              </div>
              <div className="flex items-center gap-3 rounded-2xl border border-sand-100 bg-sand-25 p-3.5">
                <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-info-50 text-info-600">
                  <ShieldCheck size={18} />
                </span>
                <div>
                  <div className="text-xs text-sand-500">Registration</div>
                  <div className="font-bold text-sand-800">Verified by admin</div>
                </div>
              </div>
              <div className="flex items-center gap-3 rounded-2xl border border-sand-100 bg-sand-25 p-3.5">
                <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-accent-50 text-accent-600">
                  <Stethoscope size={18} />
                </span>
                <div>
                  <div className="text-xs text-sand-500">Specialty</div>
                  <div className="font-bold text-sand-800">{doctor.specialization}</div>
                </div>
              </div>
            </div>
          </div>

          {/* Reviews */}
          <div className="surface p-6">
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-extrabold text-sand-900">Patient reviews</h2>
              {reviews.length > 0 && (
                <span className="chip">
                  <Stars value={rating} size={12} />
                  <span className="ml-1 font-bold text-sand-700">{rating.toFixed(1)}</span>
                  <span className="text-sand-400">· {reviews.length}</span>
                </span>
              )}
            </div>

            {reviews.length === 0 ? (
              <div className="mt-4 flex flex-col items-center gap-2 rounded-2xl border border-dashed border-sand-200 py-10 text-center">
                <MessageSquareQuote size={24} className="text-sand-300" />
                <p className="text-sm text-sand-500">No reviews yet — be the first after your consultation.</p>
              </div>
            ) : (
              <div className="mt-4 space-y-3">
                {reviews.map((r) => (
                  <div key={r.rating_id} className="rounded-2xl border border-sand-100 p-4">
                    <div className="flex items-center justify-between gap-3">
                      <div className="flex items-center gap-3">
                        <Avatar name={r.patient_name} size={38} />
                        <div>
                          <div className="font-bold text-sand-900">{r.patient_name}</div>
                          <Stars value={r.stars} size={12} />
                        </div>
                      </div>
                      <span className="text-xs text-sand-400">{r.created_at}</span>
                    </div>
                    {Array.isArray(r.what_stood_out) && r.what_stood_out.length > 0 && (
                      <div className="mt-2.5 flex flex-wrap gap-1.5">
                        {r.what_stood_out.map((tag) => (
                          <span key={tag} className="chip-primary text-[11px]">{tag}</span>
                        ))}
                      </div>
                    )}
                    {r.review_text && (
                      <p className="mt-2.5 text-sm leading-relaxed text-sand-600">“{r.review_text}”</p>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* ---- Right: sticky booking card ---- */}
        <div className="lg:col-span-1">
          <div className="surface-lift sticky top-6 p-6">
            <div className="flex items-end justify-between">
              <div>
                <div className="text-xs font-medium uppercase tracking-wide text-sand-400">Consultation fee</div>
                <div className="text-3xl font-extrabold text-sand-900">{money(doctor.consultation_fee)}</div>
              </div>
              <span className="chip border-success-100 bg-success-50 text-success-700">
                <Video size={13} /> Video
              </span>
            </div>

            <div className="mt-5">
              <label className="mb-1.5 flex items-center gap-1.5 text-sm font-semibold text-sand-700">
                <CalendarDays size={15} className="text-primary-500" /> Pick a date
              </label>
              <input type="date" value={date} min={today()}
                onChange={(e) => setDate(e.target.value)}
                className="w-full rounded-xl border border-sand-200 bg-sand-50/60 px-3 py-2.5 text-sm font-medium
                           text-sand-900 outline-none transition hover:bg-white
                           focus:border-primary-400 focus:bg-white focus:ring-4 focus:ring-primary-500/12" />
            </div>

            <div className="mt-4">
              <div className="mb-2 text-sm font-semibold text-sand-700">Available slots</div>
              {slotsLoading ? (
                <div className="text-sm text-sand-500">Loading slots…</div>
              ) : slots.length === 0 ? (
                <div className="rounded-xl bg-warning-50 px-3 py-2.5 text-xs text-warning-700">
                  No slots on this date. Try another day.
                </div>
              ) : (
                <div className="grid max-h-44 grid-cols-3 gap-2 overflow-y-auto pr-1">
                  {slots.map((s) => (
                    <button key={s.schedule_id} type="button" onClick={() => setSlot(s)}
                      className={`rounded-lg border py-2 text-xs font-semibold transition ${
                        slot?.schedule_id === s.schedule_id
                          ? 'border-primary-600 bg-primary-50 text-primary-700'
                          : 'border-sand-200 text-sand-600 hover:border-primary-300'}`}>
                      {s.label}
                    </button>
                  ))}
                </div>
              )}
            </div>

            <Button className="mt-5 w-full py-2.5"
              variant={doctor.available ? 'primary' : 'disabled'}
              disabled={!doctor.available}
              onClick={book}>
              {slot ? `Book ${slot.label}` : 'Book Appointment'}
            </Button>
            <p className="mt-2.5 text-center text-[11px] text-sand-400">
              You’ll confirm payment on the next step. Free cancellation window applies.
            </p>
          </div>
        </div>
      </div>
    </DashboardLayout>
  )
}
