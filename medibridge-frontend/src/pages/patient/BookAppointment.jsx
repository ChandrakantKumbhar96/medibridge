import { useEffect, useState } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import {
  Check, Star, Calendar, Clock, ArrowLeft, BadgeCheck, Award, Video,
} from 'lucide-react'
import DashboardTopbar from '../../components/layout/DashboardTopbar'
import Avatar from '../../components/common/Avatar'
import Button from '../../components/common/Button'
import { doctorService } from '../../services/doctorService'

const money = (n) => `₹${Number(n ?? 0).toLocaleString('en-IN')}`
const todayStr = () => new Date().toISOString().split('T')[0]

// Two entry modes. Cold: full funnel from the "Book appointment" button.
// Warm: a doctor was already chosen on Find Doctors / the profile page, so the
// specialty + doctor steps are already done and we open straight on Time.
const FULL_STEPS = [
  { key: 'specialty', label: 'Specialty' },
  { key: 'doctor', label: 'Doctor' },
  { key: 'time', label: 'Time' },
  { key: 'confirm', label: 'Confirm' },
]
const WARM_STEPS = [
  { key: 'time', label: 'Time' },
  { key: 'confirm', label: 'Confirm' },
]

function Stepper({ steps, currentKey }) {
  const currentIdx = steps.findIndex((s) => s.key === currentKey)
  return (
    <div className="flex items-center justify-center gap-1.5 py-6 sm:gap-2">
      {steps.map((s, i) => (
        <div key={s.key} className="flex items-center">
          <div className="flex items-center gap-2">
            <div className={`flex h-8 w-8 items-center justify-center rounded-full text-sm font-bold transition ${
              i < currentIdx ? 'bg-primary-600 text-white'
                : i === currentIdx ? 'bg-primary-600 text-white shadow-glow'
                  : 'bg-sand-200 text-sand-500'}`}>
              {i < currentIdx ? <Check size={16} /> : i + 1}
            </div>
            <span className={`hidden text-sm font-semibold sm:block ${
              i <= currentIdx ? 'text-sand-800' : 'text-sand-400'}`}>{s.label}</span>
          </div>
          {i < steps.length - 1 && (
            <div className={`mx-2 h-0.5 w-8 rounded-full sm:mx-3 sm:w-12 ${
              i < currentIdx ? 'bg-primary-500' : 'bg-sand-200'}`} />
          )}
        </div>
      ))}
    </div>
  )
}

/** Compact "who you're booking" strip shown on the Time and Confirm steps. */
function DoctorStrip({ doctor }) {
  return (
    <div className="flex items-center gap-4 rounded-2xl border border-sand-100 bg-sand-25 p-4">
      <Avatar name={doctor.full_name} size={52} color="solid" />
      <div className="min-w-0 flex-1">
        <div className="flex items-center gap-1.5">
          <span className="truncate font-bold text-sand-900">{doctor.full_name}</span>
          <BadgeCheck size={15} className="shrink-0 text-primary-600" />
        </div>
        <div className="text-sm text-primary-700">{doctor.specialization}</div>
      </div>
      <div className="text-right">
        <div className="text-lg font-extrabold text-sand-900">{money(doctor.consultation_fee)}</div>
        <div className="text-[11px] uppercase tracking-wide text-sand-400">per consult</div>
      </div>
    </div>
  )
}

export default function BookAppointment() {
  const navigate = useNavigate()
  const { state } = useLocation()

  const warm = !!state?.doctorId
  const steps = warm ? WARM_STEPS : FULL_STEPS

  const [step, setStep] = useState(warm ? 'time' : 'specialty')
  const [specialties, setSpecialties] = useState([])
  const [doctors, setDoctors] = useState([])
  const [slots, setSlots] = useState([])

  const [specialty, setSpecialty] = useState(null)
  const [doctor, setDoctor] = useState(null)
  const [date, setDate] = useState(warm && state?.date ? state.date : '')
  const [slot, setSlot] = useState(null)          // { schedule_id, label }
  const [reason, setReason] = useState(state?.reason || '')

  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  async function loadSlots(doctorId, value, preselectScheduleId) {
    setError(null)
    setLoading(true)
    try {
      const list = await doctorService.getAvailableSlots(doctorId, value)
      setSlots(list)
      if (preselectScheduleId) {
        const found = list.find((s) => String(s.schedule_id) === String(preselectScheduleId))
        if (found) setSlot(found)
      }
    } catch (e) {
      setError(e?.response?.data?.message || 'Could not load available slots.')
      setSlots([])
    } finally {
      setLoading(false)
    }
  }

  // Cold entry: specialty cards from the API.
  useEffect(() => {
    if (warm) return
    doctorService.getSpecialties()
      .then(setSpecialties)
      .catch(() => setError('Could not load specialties. Is the backend running?'))
  }, [warm])

  // Warm entry: load the pre-chosen doctor and, if a slot was pre-picked on the
  // profile page, carry the date + selected slot straight over.
  useEffect(() => {
    if (!warm) return
    setLoading(true)
    doctorService.getDoctor(state.doctorId)
      .then((d) => {
        if (!d) { setError('That doctor is no longer available.'); return }
        setDoctor(d)
        setSpecialty(d.specialization)
        if (state.date) loadSlots(d.doctor_id, state.date, state.scheduleId)
      })
      .catch(() => setError('Could not load that doctor.'))
      .finally(() => setLoading(false))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [warm])

  const chooseSpecialty = async (name) => {
    setSpecialty(name)
    setError(null)
    setLoading(true)
    try {
      setDoctors(await doctorService.getDoctors({ specialization: name }))
      setStep('doctor')
    } catch {
      setError('Could not load doctors for this specialty.')
    } finally {
      setLoading(false)
    }
  }

  const chooseDoctor = (d) => {
    setDoctor(d)
    setSlot(null)
    setSlots([])
    setDate('')
    setStep('time')
  }

  const chooseDate = (value) => {
    setDate(value)
    setSlot(null)
    if (value && doctor) loadSlots(doctor.doctor_id, value)
  }

  // The appointment is created on the payment screen (one transaction), so an
  // abandoned checkout never leaves an orphaned booking holding a slot.
  const proceedToPayment = () => {
    navigate('/patient/payment', {
      state: { doctor, specialty, date, slot, reason, consultType: state?.consultType || 'Consultation' },
    })
  }

  const backFromTime = () => (warm ? navigate(-1) : setStep('doctor'))

  return (
    <div className="min-h-screen bg-sand-50">
      <DashboardTopbar />
      <div className="mx-auto max-w-5xl px-6 pb-16">
        <Stepper steps={steps} currentKey={step} />

        {error && (
          <div className="mb-4 rounded-xl bg-danger-50 px-4 py-3 text-sm text-danger-600">{error}</div>
        )}

        {/* ---- Step: specialty ---- */}
        {step === 'specialty' && (
          <div className="animate-fade-up">
            <span className="eyebrow">Step 1</span>
            <h1 className="mt-1 text-display-sm text-sand-900">What do you need help with?</h1>
            <p className="mt-1 text-sand-500">Choose a specialty to see available doctors.</p>
            <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {specialties.map((s) => (
                <button key={s.name} onClick={() => chooseSpecialty(s.name)}
                  className="surface-lift group flex items-center gap-4 p-5 text-left">
                  <span className="flex h-14 w-14 items-center justify-center rounded-2xl bg-primary-50 text-3xl">
                    {s.emoji}
                  </span>
                  <span className="min-w-0">
                    <span className="block font-extrabold text-sand-900">{s.name}</span>
                    <span className="text-sm text-sand-500">{s.doctors} doctors available</span>
                  </span>
                </button>
              ))}
              {specialties.length === 0 && !error && (
                <div className="text-sm text-sand-500">Loading specialties…</div>
              )}
            </div>
          </div>
        )}

        {/* ---- Step: doctor ---- */}
        {step === 'doctor' && (
          <div className="animate-fade-up">
            <span className="eyebrow">Step 2</span>
            <h1 className="mt-1 text-display-sm text-sand-900">Choose a doctor</h1>
            <p className="mt-1 text-sand-500">{specialty} specialists available.</p>
            <div className="mt-6 space-y-4">
              {doctors.map((d) => (
                <div key={d.doctor_id} className="surface flex flex-wrap items-center justify-between gap-4 p-5">
                  <div className="flex items-center gap-4">
                    <Avatar name={d.full_name} size={56} color="solid" />
                    <div>
                      <div className="flex items-center gap-1.5 font-bold text-sand-900">
                        {d.full_name} <BadgeCheck size={15} className="text-primary-600" />
                      </div>
                      <div className="text-sm text-primary-700">{d.specialization}</div>
                      <div className="mt-1.5 flex flex-wrap items-center gap-2 text-xs">
                        <span className="chip">
                          <Star size={12} className="text-warning-500" fill="currentColor" />
                          {Number(d.rating) > 0 ? d.rating : 'New'}
                        </span>
                        <span className="chip"><Award size={12} className="text-primary-500" /> {d.experience_years} yrs</span>
                        <span className="font-bold text-sand-900">{money(d.consultation_fee)}</span>
                      </div>
                    </div>
                  </div>
                  <div className="flex gap-2">
                    <Button variant="outline" className="px-4 py-2"
                      onClick={() => navigate(`/patient/doctors/${d.doctor_id}`)}>Profile</Button>
                    <Button onClick={() => chooseDoctor(d)} className="px-5 py-2">Select</Button>
                  </div>
                </div>
              ))}
              {doctors.length === 0 && (
                <div className="surface p-5 text-sm text-sand-500">
                  No doctors are currently available in {specialty}.
                </div>
              )}
            </div>
            <button onClick={() => setStep('specialty')}
              className="mt-5 inline-flex items-center gap-1.5 text-sm font-semibold text-sand-500 hover:text-primary-600">
              <ArrowLeft size={15} /> Back
            </button>
          </div>
        )}

        {/* ---- Step: time ---- */}
        {step === 'time' && doctor && (
          <div className="animate-fade-up">
            <span className="eyebrow">Pick a time</span>
            <h1 className="mt-1 text-display-sm text-sand-900">Select date &amp; time</h1>

            <div className="mt-5 space-y-5">
              <DoctorStrip doctor={doctor} />

              <div className="surface p-6">
                <label className="mb-1.5 flex items-center gap-1.5 text-sm font-semibold text-sand-700">
                  <Calendar size={15} className="text-primary-500" /> Appointment date
                </label>
                <input type="date" value={date} min={todayStr()}
                  onChange={(e) => chooseDate(e.target.value)}
                  className="w-full max-w-xs rounded-xl border border-sand-200 bg-sand-50/60 px-3 py-2.5 text-sm font-medium
                             text-sand-900 outline-none transition hover:bg-white
                             focus:border-primary-400 focus:bg-white focus:ring-4 focus:ring-primary-500/12" />

                {loading && <div className="mt-5 text-sm text-sand-500">Loading slots…</div>}

                {!loading && date && slots.length === 0 && (
                  <div className="mt-5 rounded-xl bg-warning-50 px-4 py-3 text-sm text-warning-700">
                    No slots available on this date — the doctor may not consult that day. Try another.
                  </div>
                )}

                {slots.length > 0 && (
                  <>
                    <div className="mb-2 mt-6 text-sm font-semibold text-sand-700">Available slots</div>
                    <div className="grid grid-cols-3 gap-2.5 sm:grid-cols-4">
                      {slots.map((s) => (
                        <button key={s.schedule_id} onClick={() => setSlot(s)}
                          className={`flex items-center justify-center gap-1.5 rounded-xl border py-2.5 text-sm font-semibold transition ${
                            slot?.schedule_id === s.schedule_id
                              ? 'border-primary-600 bg-primary-50 text-primary-700'
                              : 'border-sand-200 text-sand-600 hover:border-primary-300'}`}>
                          <Clock size={13} /> {s.label}
                        </button>
                      ))}
                    </div>
                  </>
                )}

                <div className="mt-6">
                  <label className="block text-sm font-semibold text-sand-700">
                    Reason for visit <span className="font-normal text-sand-400">(optional)</span>
                  </label>
                  <textarea rows={2} value={reason} onChange={(e) => setReason(e.target.value)}
                    placeholder="Briefly describe your concern"
                    className="mt-1.5 w-full rounded-xl border border-sand-200 bg-sand-50/60 px-4 py-3 text-sm font-medium
                               text-sand-900 outline-none transition placeholder:font-normal placeholder:text-sand-400
                               hover:bg-white focus:border-primary-400 focus:bg-white focus:ring-4 focus:ring-primary-500/10" />
                </div>
              </div>
            </div>

            <div className="mt-5 flex items-center justify-between">
              <button onClick={backFromTime}
                className="inline-flex items-center gap-1.5 text-sm font-semibold text-sand-500 hover:text-primary-600">
                <ArrowLeft size={15} /> Back
              </button>
              <Button disabled={!date || !slot} variant={!date || !slot ? 'disabled' : 'primary'}
                onClick={() => setStep('confirm')} className="px-6">Continue</Button>
            </div>
          </div>
        )}

        {/* ---- Step: confirm ---- */}
        {step === 'confirm' && doctor && (
          <div className="animate-fade-up">
            <span className="eyebrow">Almost there</span>
            <h1 className="mt-1 text-display-sm text-sand-900">Confirm your appointment</h1>

            <div className="mt-5 max-w-lg space-y-5">
              <DoctorStrip doctor={doctor} />

              <div className="surface p-6">
                <dl className="space-y-3 text-sm">
                  <div className="flex justify-between"><dt className="text-sand-500">Specialty</dt><dd className="font-semibold text-sand-800">{specialty}</dd></div>
                  <div className="flex justify-between"><dt className="text-sand-500">Date</dt><dd className="font-semibold text-sand-800">{date}</dd></div>
                  <div className="flex justify-between"><dt className="text-sand-500">Time</dt><dd className="font-semibold text-sand-800">{slot?.label}</dd></div>
                  <div className="flex justify-between"><dt className="text-sand-500">Mode</dt><dd className="inline-flex items-center gap-1 font-semibold text-sand-800"><Video size={14} className="text-success-600" /> Video consultation</dd></div>
                  {reason && (
                    <div className="flex justify-between gap-6"><dt className="text-sand-500">Reason</dt><dd className="text-right font-semibold text-sand-800">{reason}</dd></div>
                  )}
                  <div className="flex justify-between border-t border-sand-100 pt-3">
                    <dt className="text-sand-500">Consultation fee</dt>
                    <dd className="text-lg font-extrabold text-sand-900">{money(doctor.consultation_fee)}</dd>
                  </div>
                </dl>
              </div>
            </div>

            <div className="mt-5 flex max-w-lg items-center justify-between">
              <button onClick={() => setStep('time')}
                className="inline-flex items-center gap-1.5 text-sm font-semibold text-sand-500 hover:text-primary-600">
                <ArrowLeft size={15} /> Back
              </button>
              <Button onClick={proceedToPayment} className="px-6">Proceed to payment</Button>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
