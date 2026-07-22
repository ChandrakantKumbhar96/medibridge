import { useEffect, useState } from 'react'
import { useDispatch } from 'react-redux'
import { useNavigate } from 'react-router-dom'
import Input, { Field } from '../../components/common/Input'
import { registerDoctor } from '../../features/auth/authSlice'
import { doctorService } from '../../services/doctorService'

export default function DoctorRegisterForm() {
  const dispatch = useDispatch()
  const navigate = useNavigate()
  // Fetched, not imported from mock data: the backend rejects a specialization
  // name it does not have a row for, so this list must come from the server.
  const [specializations, setSpecializations] = useState([])
  const [notice, setNotice] = useState(null)
  const [f, setF] = useState({
    full_name: '', email: '', phone: '', password: '', specialization: 'Cardiology',
    license_number: '', experience_years: '', consultation_fee: '', consultation_duration_min: 30, bio: '',
  })
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value })

  useEffect(() => {
    doctorService.getSpecializations()
      .then((list) => {
        setSpecializations(list)
        if (list.length) setF((prev) => ({ ...prev, specialization: list[0] }))
      })
      .catch(() => setSpecializations([]))
  }, [])

  const submit = async (e) => {
    e.preventDefault()
    const res = await dispatch(registerDoctor(f))
    if (registerDoctor.fulfilled.match(res)) {
      // Doctors land in 'pending' and get no token until an admin approves them,
      // so there is no dashboard to navigate to yet.
      setNotice(
        'Account created. An administrator will review and approve your account — ' +
        'you will be able to sign in once approved.'
      )
    }
  }

  return (
    <form onSubmit={submit} className="mt-6">
      <h2 className="text-2xl font-extrabold text-slate-900">Create Doctor Account</h2>
      <p className="mt-1 text-sm text-slate-500">Join MediBridge as a healthcare provider</p>

      {notice && (
        <div className="mt-4 rounded-lg bg-green-50 px-4 py-3 text-sm text-green-700">{notice}</div>
      )}

      <div className="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-2">
        <Field label="Full Name"><Input required value={f.full_name} onChange={set('full_name')} placeholder="Dr. Sarah Johnson" /></Field>
        <Field label="Email"><Input required type="email" value={f.email} onChange={set('email')} placeholder="you@medibridge.com" /></Field>
        <Field label="Phone"><Input required value={f.phone} onChange={set('phone')} placeholder="+1 234 567 8900" /></Field>
        <Field label="Specialization">
          <select className="w-full rounded-lg border border-slate-300 px-3.5 py-2.5 text-sm" value={f.specialization} onChange={set('specialization')}>
            {specializations.map((s) => <option key={s}>{s}</option>)}
          </select>
        </Field>
        <Field label="License Number"><Input required value={f.license_number} onChange={set('license_number')} placeholder="MD-12345-2020" /></Field>
        <Field label="Experience (Years)"><Input required type="number" value={f.experience_years} onChange={set('experience_years')} placeholder="15" /></Field>
        <Field label="Consultation Fee ($)"><Input required type="number" value={f.consultation_fee} onChange={set('consultation_fee')} placeholder="150" /></Field>
        <Field label="Avg. Duration (min)">
          <select className="w-full rounded-lg border border-slate-300 px-3.5 py-2.5 text-sm" value={f.consultation_duration_min} onChange={set('consultation_duration_min')}>
            <option value={15}>15 minutes</option><option value={30}>30 minutes</option><option value={45}>45 minutes</option><option value={60}>60 minutes</option>
          </select>
        </Field>
        <Field label="Password"><Input required type="password" value={f.password} onChange={set('password')} placeholder="Create a password" /></Field>
      </div>
      <div className="mt-4">
        <Field label="Bio">
          <textarea rows={3} className="w-full rounded-lg border border-slate-300 px-3.5 py-2.5 text-sm" value={f.bio} onChange={set('bio')} placeholder="Tell patients about your experience and specialties" />
        </Field>
      </div>

      <button type="submit" className="mt-6 w-full rounded-lg bg-primary-600 py-3 text-sm font-semibold text-white hover:bg-primary-700">
        Create Account
      </button>
    </form>
  )
}
