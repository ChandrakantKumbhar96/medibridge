import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Check, Star, Calendar, Clock } from 'lucide-react'
import DashboardTopbar from '../../components/layout/DashboardTopbar'
import Card from '../../components/common/Card'
import Avatar from '../../components/common/Avatar'
import Button from '../../components/common/Button'
import { doctorService } from '../../services/doctorService'

const steps = ['Select Specialty', 'Choose Doctor', 'Select Time', 'Confirm']

function Stepper({ current }) {
  return (
    <div className="flex items-center justify-center gap-2 py-6">
      {steps.map((label, i) => (
        <div key={label} className="flex items-center">
          <div className="flex items-center gap-2">
            <div className={`flex h-8 w-8 items-center justify-center rounded-full text-sm font-semibold ${
              i <= current ? 'bg-primary-600 text-white' : 'bg-slate-200 text-slate-500'}`}>
              {i < current ? <Check size={16} /> : i + 1}
            </div>
            <span className={`hidden text-sm font-medium sm:block ${i <= current ? 'text-slate-800' : 'text-slate-400'}`}>{label}</span>
          </div>
          {i < steps.length - 1 && <div className="mx-3 h-px w-10 bg-slate-300" />}
        </div>
      ))}
    </div>
  )
}

export default function BookAppointment() {
  const navigate = useNavigate()

  const [step, setStep] = useState(0)
  const [specialties, setSpecialties] = useState([])
  const [doctors, setDoctors] = useState([])
  const [slots, setSlots] = useState([])

  const [specialty, setSpecialty] = useState(null)
  const [doctor, setDoctor] = useState(null)
  const [date, setDate] = useState('')
  const [slot, setSlot] = useState(null)          // { schedule_id, label }
  const [reason, setReason] = useState('')

  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  // Step 1 - specialty cards come from the API, not a hardcoded mock import.
  useEffect(() => {
    doctorService.getSpecialties()
      .then(setSpecialties)
      .catch(() => setError('Could not load specialties. Is the backend running?'))
  }, [])

  const chooseSpecialty = async (name) => {
    setSpecialty(name)
    setError(null)
    setLoading(true)
    try {
      setDoctors(await doctorService.getDoctors({ specialization: name }))
      setStep(1)
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
    setStep(2)
  }

  // Slots are per doctor per date and come back with a schedule_id we must keep.
  const chooseDate = async (value) => {
    setDate(value)
    setSlot(null)
    if (!value || !doctor) return

    setError(null)
    setLoading(true)
    try {
      setSlots(await doctorService.getAvailableSlots(doctor.doctor_id, value))
    } catch (e) {
      setError(e?.response?.data?.message || 'Could not load available slots.')
      setSlots([])
    } finally {
      setLoading(false)
    }
  }

  // The appointment is created on the payment screen, so the whole booking is
  // one transaction: no payment means no half-created appointment left behind.
  const proceedToPayment = () => {
    navigate('/patient/payment', {
      state: { doctor, specialty, date, slot, reason },
    })
  }

  return (
    <div className="min-h-screen bg-slate-50">
      <DashboardTopbar />
      <div className="mx-auto max-w-5xl px-6">
        <Stepper current={step} />

        {error && (
          <div className="mb-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">{error}</div>
        )}

        {step === 0 && (
          <div>
            <h1 className="text-3xl font-extrabold text-slate-900">Select Specialization</h1>
            <p className="mt-1 text-slate-500">Choose the medical specialty you need</p>
            <div className="mt-6 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
              {specialties.map((s) => (
                <button key={s.name} onClick={() => chooseSpecialty(s.name)}
                  className="rounded-2xl border border-slate-200 bg-white p-6 text-left shadow-sm transition hover:border-primary-600 hover:shadow-md">
                  <div className="text-3xl">{s.emoji}</div>
                  <div className="mt-4 text-lg font-bold text-slate-900">{s.name}</div>
                  <div className="text-sm text-slate-500">{s.doctors} doctors available</div>
                </button>
              ))}
              {specialties.length === 0 && !error && (
                <div className="text-sm text-slate-500">Loading specialties…</div>
              )}
            </div>
          </div>
        )}

        {step === 1 && (
          <div>
            <h1 className="text-3xl font-extrabold text-slate-900">Choose a Doctor</h1>
            <p className="mt-1 text-slate-500">{specialty} specialists available</p>
            <div className="mt-6 space-y-4">
              {doctors.map((d) => (
                <Card key={d.doctor_id} className="flex flex-wrap items-center justify-between gap-4">
                  <div className="flex items-center gap-4">
                    <Avatar color="solid" size={52} />
                    <div>
                      <div className="font-bold text-slate-900">{d.full_name}</div>
                      <div className="text-sm text-slate-500">{d.specialization}</div>
                      <div className="mt-1 flex items-center gap-2 text-sm">
                        <span className="flex items-center gap-1 text-amber-500">
                          <Star size={14} fill="currentColor" /> {d.rating}
                        </span>
                        <span className="text-slate-400">
                          • {d.experience_years} yrs • ₹{d.consultation_fee}
                        </span>
                      </div>
                    </div>
                  </div>
                  <Button onClick={() => chooseDoctor(d)} className="px-5 py-2">Select</Button>
                </Card>
              ))}
              {doctors.length === 0 && (
                <Card className="text-sm text-slate-500">
                  No doctors are currently available in {specialty}.
                </Card>
              )}
            </div>
            <button onClick={() => setStep(0)} className="mt-5 text-sm font-medium text-slate-500 hover:text-primary-600">← Back</button>
          </div>
        )}

        {step === 2 && (
          <div>
            <h1 className="text-3xl font-extrabold text-slate-900">Select Date &amp; Time</h1>
            <p className="mt-1 text-slate-500">Pick a slot with {doctor?.full_name}</p>
            <Card className="mt-6">
              <label className="block text-sm font-semibold text-slate-700">Appointment Date</label>
              <div className="relative mt-1.5 max-w-xs">
                <Calendar className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={18} />
                <input type="date" value={date}
                  min={new Date().toISOString().split('T')[0]}
                  onChange={(e) => chooseDate(e.target.value)}
                  className="w-full rounded-lg border border-slate-300 py-2.5 pl-10 pr-3 text-sm" />
              </div>

              {loading && <div className="mt-5 text-sm text-slate-500">Loading slots…</div>}

              {!loading && date && slots.length === 0 && (
                <div className="mt-5 rounded-lg bg-amber-50 px-4 py-3 text-sm text-amber-700">
                  No slots available on this date. The doctor may not consult on this day —
                  please try another.
                </div>
              )}

              <div className="mt-6 grid grid-cols-3 gap-3 sm:grid-cols-4">
                {slots.map((s) => (
                  <button key={s.schedule_id} onClick={() => setSlot(s)}
                    className={`flex items-center justify-center gap-1.5 rounded-lg border py-2.5 text-sm font-medium transition ${
                      slot?.schedule_id === s.schedule_id
                        ? 'border-primary-600 bg-primary-50 text-primary-600'
                        : 'border-slate-200 text-slate-600 hover:border-primary-300'}`}>
                    <Clock size={13} /> {s.label}
                  </button>
                ))}
              </div>

              <div className="mt-6">
                <label className="block text-sm font-semibold text-slate-700">
                  Reason for visit <span className="font-normal text-slate-400">(optional)</span>
                </label>
                <textarea rows={2} value={reason} onChange={(e) => setReason(e.target.value)}
                  placeholder="Briefly describe your concern"
                  className="mt-1.5 w-full rounded-lg border border-slate-300 px-3.5 py-2.5 text-sm" />
              </div>
            </Card>
            <div className="mt-5 flex justify-between">
              <button onClick={() => setStep(1)} className="text-sm font-medium text-slate-500 hover:text-primary-600">← Back</button>
              <Button disabled={!date || !slot} variant={!date || !slot ? 'disabled' : 'primary'}
                onClick={() => setStep(3)} className="px-6">Continue</Button>
            </div>
          </div>
        )}

        {step === 3 && (
          <div>
            <h1 className="text-3xl font-extrabold text-slate-900">Confirm Appointment</h1>
            <p className="mt-1 text-slate-500">Review your booking details</p>
            <Card className="mt-6 max-w-lg">
              <div className="flex items-center gap-4 border-b border-slate-100 pb-4">
                <Avatar color="solid" size={52} />
                <div>
                  <div className="font-bold text-slate-900">{doctor?.full_name}</div>
                  <div className="text-sm text-slate-500">{doctor?.specialization}</div>
                </div>
              </div>
              <dl className="mt-4 space-y-3 text-sm">
                <div className="flex justify-between"><dt className="text-slate-500">Specialty</dt><dd className="font-medium text-slate-800">{specialty}</dd></div>
                <div className="flex justify-between"><dt className="text-slate-500">Date</dt><dd className="font-medium text-slate-800">{date}</dd></div>
                <div className="flex justify-between"><dt className="text-slate-500">Time</dt><dd className="font-medium text-slate-800">{slot?.label}</dd></div>
                {reason && (
                  <div className="flex justify-between gap-6">
                    <dt className="text-slate-500">Reason</dt>
                    <dd className="text-right font-medium text-slate-800">{reason}</dd>
                  </div>
                )}
                <div className="flex justify-between border-t border-slate-100 pt-3">
                  <dt className="text-slate-500">Consultation Fee</dt>
                  <dd className="font-bold text-slate-900">₹{doctor?.consultation_fee}</dd>
                </div>
              </dl>
            </Card>
            <div className="mt-5 flex max-w-lg justify-between">
              <button onClick={() => setStep(2)} className="text-sm font-medium text-slate-500 hover:text-primary-600">← Back</button>
              <Button onClick={proceedToPayment} className="px-6">Proceed to Payment</Button>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
