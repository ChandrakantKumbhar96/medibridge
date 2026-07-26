import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  ClipboardPlus, Upload, Stethoscope, FileCheck2, Star, Award, BadgeCheck,
  ArrowRight, CheckCircle2,
} from 'lucide-react'
import DashboardLayout from '../../components/layout/DashboardLayout'
import Avatar from '../../components/common/Avatar'
import Button from '../../components/common/Button'
import { patientNav } from './patientNav'
import { doctorService } from '../../services/doctorService'
import { recordService } from '../../services/recordService'

const money = (n) => `₹${Number(n ?? 0).toLocaleString('en-IN')}`

const STEPS = [
  { icon: Upload, title: 'Share your reports', text: 'Upload existing prescriptions, lab tests and scans to your records.' },
  { icon: Stethoscope, title: 'Pick a specialist', text: 'Choose an independent expert in the right field.' },
  { icon: FileCheck2, title: 'Get a written opinion', text: 'They review your case over video and issue a documented evaluation.' },
]

/**
 * Second Opinion is a consult *type*, not a separate system: the patient attaches
 * their existing records, picks a specialist, and books with
 * consult_type='Second Opinion'. The doctor sees that type, reviews the uploaded
 * documents, and writes an evaluation through the normal prescription flow — so
 * this page reuses booking, records and prescriptions end to end.
 */
export default function SecondOpinion() {
  const navigate = useNavigate()
  const [specializations, setSpecializations] = useState([])
  const [doctors, setDoctors] = useState([])
  const [spec, setSpec] = useState('All')
  const [reason, setReason] = useState('')
  const [recordCount, setRecordCount] = useState(null)

  useEffect(() => {
    doctorService.getDoctors().then(setDoctors).catch(() => setDoctors([]))
    doctorService.getSpecializations().then(setSpecializations).catch(() => setSpecializations([]))
    recordService.getRecords()
      .then((r) => setRecordCount(Array.isArray(r) ? r.length : 0))
      .catch(() => setRecordCount(null))
  }, [])

  const filtered = doctors.filter((d) =>
    spec === 'All' || d.specialization.toLowerCase().startsWith(spec.toLowerCase().slice(0, 5)))

  const request = (d) => {
    navigate('/patient/book', {
      state: {
        doctorId: d.doctor_id,
        consultType: 'Second Opinion',
        reason: reason || 'Second opinion on my existing reports and diagnosis.',
      },
    })
  }

  return (
    <DashboardLayout navItems={patientNav}>
      {/* ---- Hero ---- */}
      <div className="animate-fade-up overflow-hidden rounded-4xl bg-gradient-to-br from-primary-700 to-primary-900 bg-mesh-teal p-7 text-white shadow-lift sm:p-9">
        <div className="flex flex-wrap items-center justify-between gap-6">
          <div className="max-w-xl">
            <span className="inline-flex items-center gap-2 rounded-full bg-white/15 px-3 py-1 text-[11px] font-bold uppercase tracking-[0.16em]">
              <ClipboardPlus size={13} /> Second opinion
            </span>
            <h1 className="mt-3 text-display-sm">Not sure about a diagnosis?</h1>
            <p className="mt-2 text-white/80">
              Get an independent specialist to review your existing reports and
              treatment plan — before you commit to surgery, long-term medication
              or a major decision.
            </p>
          </div>
          <div className="hidden h-28 w-28 items-center justify-center rounded-full bg-white/10 backdrop-blur md:flex">
            <ClipboardPlus size={52} className="text-white/90" />
          </div>
        </div>
      </div>

      {/* ---- How it works ---- */}
      <div className="mt-6 grid gap-4 sm:grid-cols-3">
        {STEPS.map((s, i) => (
          <div key={s.title} className="surface p-5">
            <div className="flex items-center gap-2">
              <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-primary-50 text-primary-600">
                <s.icon size={18} />
              </span>
              <span className="text-xs font-bold uppercase tracking-wide text-sand-400">Step {i + 1}</span>
            </div>
            <div className="mt-3 font-extrabold text-sand-900">{s.title}</div>
            <p className="mt-1 text-sm text-sand-500">{s.text}</p>
          </div>
        ))}
      </div>

      {/* ---- Reports reminder ---- */}
      <div className="surface mt-6 flex flex-wrap items-center justify-between gap-3 p-5">
        <div className="flex items-center gap-3">
          <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-success-50 text-success-600">
            <CheckCircle2 size={20} />
          </span>
          <div>
            <div className="font-bold text-sand-900">Have your reports ready</div>
            <div className="text-sm text-sand-500">
              {recordCount == null
                ? 'Upload your prescriptions, lab tests and scans so the specialist can review them.'
                : recordCount === 0
                  ? 'You have no documents uploaded yet — add them so the specialist can review.'
                  : `${recordCount} document${recordCount === 1 ? '' : 's'} in your records, ready to share.`}
            </div>
          </div>
        </div>
        <Button variant="outline" onClick={() => navigate('/patient/records')}>
          <Upload size={15} /> Manage records
        </Button>
      </div>

      {/* ---- What to review ---- */}
      <div className="surface mt-6 p-6">
        <label className="block font-extrabold text-sand-900">What would you like reviewed?</label>
        <p className="mt-1 text-sm text-sand-500">
          Describe your current diagnosis or the decision you need help with. The
          specialist sees this with your booking.
        </p>
        <textarea rows={3} value={reason} onChange={(e) => setReason(e.target.value)}
          placeholder="e.g. I was advised knee surgery for my ACL tear — I'd like a second view on whether physiotherapy is an option first."
          className="mt-3 w-full rounded-xl border border-sand-200 bg-sand-50/60 px-4 py-3 text-sm font-medium
                     text-sand-900 outline-none transition placeholder:font-normal placeholder:text-sand-400
                     hover:bg-white focus:border-primary-400 focus:bg-white focus:ring-4 focus:ring-primary-500/10" />
      </div>

      {/* ---- Choose specialist ---- */}
      <div className="mt-6">
        <h2 className="text-lg font-extrabold text-sand-900">Choose a specialist</h2>
        <div className="mt-3 flex flex-wrap gap-2">
          {['All', ...specializations].map((s) => (
            <button key={s} onClick={() => setSpec(s)}
              className={`rounded-full px-3.5 py-1.5 text-xs font-bold transition ${
                spec === s
                  ? 'bg-primary-600 text-white shadow-[0_6px_16px_-8px_rgba(37,99,235,.7)]'
                  : 'border border-sand-200 bg-white text-sand-600 hover:border-primary-300 hover:text-primary-700'}`}>
              {s}
            </button>
          ))}
        </div>

        <div className="mt-4 space-y-3">
          {filtered.length === 0 && (
            <div className="surface p-5 text-sm text-sand-500">No specialists available in this field.</div>
          )}
          {filtered.map((d) => (
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
              <Button variant={d.available ? 'primary' : 'disabled'} disabled={!d.available}
                onClick={() => request(d)}>
                Request second opinion <ArrowRight size={15} />
              </Button>
            </div>
          ))}
        </div>
      </div>
    </DashboardLayout>
  )
}
