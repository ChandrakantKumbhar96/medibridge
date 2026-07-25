import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Thermometer, Sparkles, HeartPulse, Bone, Baby, Brain, ArrowRight, Star,
  Award, BadgeCheck, Info,
} from 'lucide-react'
import DashboardLayout from '../../components/layout/DashboardLayout'
import Avatar from '../../components/common/Avatar'
import Button from '../../components/common/Button'
import { patientNav } from './patientNav'
import { doctorService } from '../../services/doctorService'

const money = (n) => `₹${Number(n ?? 0).toLocaleString('en-IN')}`

// A guidance layer, not a diagnosis: each concern maps to the specialty a real
// triage desk would route it to. `prefix` matches doctor.specialization
// (e.g. 'Cardiologist') the same way Find Doctors does.
const CONCERNS = [
  { icon: Thermometer, label: 'Fever, cough or infection', specialty: 'General Physician', prefix: 'gener',
    examples: 'Fever, cold, sore throat, fatigue, body ache' },
  { icon: Sparkles, label: 'Skin, hair or nails', specialty: 'Dermatology', prefix: 'derma',
    examples: 'Rash, acne, hair fall, allergies, pigmentation' },
  { icon: HeartPulse, label: 'Heart, chest or blood pressure', specialty: 'Cardiology', prefix: 'cardi',
    examples: 'Chest pain, palpitations, high BP, breathlessness' },
  { icon: Bone, label: 'Bones, joints or injury', specialty: 'Orthopedics', prefix: 'ortho',
    examples: 'Back pain, joint pain, fractures, sports injury' },
  { icon: Baby, label: 'Child health', specialty: 'Pediatrics', prefix: 'pedia',
    examples: 'Vaccination, growth, fever in children, nutrition' },
  { icon: Brain, label: 'Headache or nervous system', specialty: 'Neurology', prefix: 'neuro',
    examples: 'Migraine, dizziness, numbness, seizures' },
]

export default function SymptomChecker() {
  const navigate = useNavigate()
  const [doctors, setDoctors] = useState([])
  const [picked, setPicked] = useState(null)

  useEffect(() => {
    doctorService.getDoctors().then(setDoctors).catch(() => setDoctors([]))
  }, [])

  const matches = picked
    ? doctors.filter((d) => d.specialization.toLowerCase().startsWith(picked.prefix))
    : []

  return (
    <DashboardLayout navItems={patientNav}>
      <div className="animate-fade-up">
        <span className="eyebrow">Not sure who to see?</span>
        <h1 className="mt-1 text-display-sm text-sand-900">Symptom checker</h1>
        <p className="mt-1 max-w-xl text-sand-500">
          Tell us what's bothering you and we'll point you to the right specialty.
          This is guidance to help you book — not a medical diagnosis.
        </p>
      </div>

      {/* ---- Concern grid ---- */}
      <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {CONCERNS.map((c) => (
          <button key={c.label} onClick={() => setPicked(c)}
            className={`surface-lift p-5 text-left transition ${
              picked?.label === c.label ? 'ring-2 ring-primary-500/40' : ''}`}>
            <span className={`flex h-12 w-12 items-center justify-center rounded-2xl ${
              picked?.label === c.label ? 'bg-primary-600 text-white' : 'bg-primary-50 text-primary-600'}`}>
              <c.icon size={22} />
            </span>
            <div className="mt-4 font-extrabold text-sand-900">{c.label}</div>
            <p className="mt-1 text-xs text-sand-500">{c.examples}</p>
          </button>
        ))}
      </div>

      {/* ---- Recommendation ---- */}
      {picked && (
        <div className="animate-fade-up mt-8">
          <div className="surface flex flex-wrap items-center justify-between gap-3 border-l-4 border-l-primary-600 p-5">
            <div className="flex items-center gap-3">
              <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary-50 text-primary-600">
                <picked.icon size={20} />
              </span>
              <div>
                <div className="text-sm text-sand-500">For “{picked.label.toLowerCase()}”, see a</div>
                <div className="text-lg font-extrabold text-sand-900">{picked.specialty} specialist</div>
              </div>
            </div>
            <Button variant="outline" onClick={() => navigate('/patient/find-doctors')}>
              Browse all doctors
            </Button>
          </div>

          <h2 className="mt-6 text-lg font-extrabold text-sand-900">
            {picked.specialty} doctors available
          </h2>
          <div className="mt-3 space-y-3">
            {matches.length === 0 && (
              <div className="surface flex items-center gap-2 p-5 text-sm text-sand-500">
                <Info size={16} className="text-sand-400" />
                No {picked.specialty} specialists are available right now — check back soon or browse all doctors.
              </div>
            )}
            {matches.map((d) => (
              <div key={d.doctor_id} className="surface flex flex-wrap items-center justify-between gap-4 p-5">
                <div className="flex items-center gap-4">
                  <Avatar name={d.full_name} size={52} color="solid" />
                  <div>
                    <div className="flex items-center gap-1.5 font-bold text-sand-900">
                      {d.full_name} <BadgeCheck size={15} className="text-primary-600" />
                    </div>
                    <div className="text-sm text-primary-700">{d.specialization}</div>
                    <div className="mt-1.5 flex flex-wrap items-center gap-2 text-xs">
                      <span className="chip"><Star size={12} className="text-warning-500" fill="currentColor" /> {Number(d.rating) > 0 ? d.rating : 'New'}</span>
                      <span className="chip"><Award size={12} className="text-primary-500" /> {d.experience_years} yrs</span>
                      <span className="font-bold text-sand-900">{money(d.consultation_fee)}</span>
                    </div>
                  </div>
                </div>
                <div className="flex gap-2">
                  <Button variant="outline" className="px-4 py-2"
                    onClick={() => navigate(`/patient/doctors/${d.doctor_id}`)}>Profile</Button>
                  <Button variant={d.available ? 'primary' : 'disabled'} disabled={!d.available}
                    onClick={() => navigate('/patient/book', { state: { doctorId: d.doctor_id } })}>
                    Book <ArrowRight size={15} />
                  </Button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </DashboardLayout>
  )
}
