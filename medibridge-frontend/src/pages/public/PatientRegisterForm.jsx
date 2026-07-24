import { useState } from 'react'
import { useDispatch } from 'react-redux'
import { useNavigate } from 'react-router-dom'
import Input, { Field } from '../../components/common/Input'
import { registerPatient } from '../../features/auth/authSlice'

export default function PatientRegisterForm() {
  const dispatch = useDispatch()
  const navigate = useNavigate()
  const [f, setF] = useState({
    full_name: '', email: '', phone: '', another_number: '', password: '',
    date_of_birth: '', gender: 'Male', blood_group: 'O+', address: '', reason_of_consult: '',
  })
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value })

  const submit = async (e) => {
    e.preventDefault()
    const res = await dispatch(registerPatient(f))
    if (registerPatient.fulfilled.match(res)) navigate('/patient')
  }

  return (
    <form onSubmit={submit} className="mt-6">
      <h2 className="text-2xl font-extrabold text-sand-900">Create Patient Account</h2>
      <p className="mt-1 text-sm text-sand-500">Register to manage your health</p>

      <div className="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-2">
        <Field label="Full Name"><Input required value={f.full_name} onChange={set('full_name')} placeholder="John Doe" /></Field>
        <Field label="Email"><Input required type="email" value={f.email} onChange={set('email')} placeholder="you@email.com" /></Field>
        <Field label="Phone Number"><Input required value={f.phone} onChange={set('phone')} placeholder="+1 234 567 8900" /></Field>
        <Field label="Alternate Number"><Input value={f.another_number} onChange={set('another_number')} placeholder="Optional" /></Field>
        <Field label="Date of Birth"><Input required type="date" value={f.date_of_birth} onChange={set('date_of_birth')} /></Field>
        <Field label="Gender">
          <select className="w-full rounded-xl border border-sand-200 bg-sand-50/60 px-4 py-3 text-sm font-medium text-sand-900 outline-none transition-all hover:border-sand-300 hover:bg-white focus:border-primary-400 focus:bg-white focus:ring-4 focus:ring-primary-500/10" value={f.gender} onChange={set('gender')}>
            <option>Male</option><option>Female</option><option>Other</option>
          </select>
        </Field>
        <Field label="Blood Group">
          <select className="w-full rounded-xl border border-sand-200 bg-sand-50/60 px-4 py-3 text-sm font-medium text-sand-900 outline-none transition-all hover:border-sand-300 hover:bg-white focus:border-primary-400 focus:bg-white focus:ring-4 focus:ring-primary-500/10" value={f.blood_group} onChange={set('blood_group')}>
            {['O+','O-','A+','A-','B+','B-','AB+','AB-'].map((b) => <option key={b}>{b}</option>)}
          </select>
        </Field>
        <Field label="Password"><Input required type="password" value={f.password} onChange={set('password')} placeholder="Create a password" /></Field>
      </div>
      <div className="mt-4 space-y-4">
        <Field label="Address"><Input value={f.address} onChange={set('address')} placeholder="Street, City, State" /></Field>
        <Field label="Reason for Consultation">
          <textarea rows={2} className="w-full rounded-xl border border-sand-200 bg-sand-50/60 px-4 py-3 text-sm font-medium text-sand-900 outline-none transition-all hover:border-sand-300 hover:bg-white focus:border-primary-400 focus:bg-white focus:ring-4 focus:ring-primary-500/10" value={f.reason_of_consult} onChange={set('reason_of_consult')} placeholder="Briefly describe your concern" />
        </Field>
      </div>

      <button type="submit" className="mt-6 w-full rounded-xl bg-primary-600 py-3 text-sm font-semibold text-white hover:bg-primary-700">
        Create Account
      </button>
    </form>
  )
}
