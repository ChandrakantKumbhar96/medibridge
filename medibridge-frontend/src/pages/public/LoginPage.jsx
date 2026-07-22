import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useDispatch, useSelector } from 'react-redux'
import { Mail, Lock, User, Stethoscope, Check } from 'lucide-react'
import Logo from '../../components/common/Logo'
import Input, { Field } from '../../components/common/Input'
import { login } from '../../features/auth/authSlice'
import PatientRegisterForm from './PatientRegisterForm'
import DoctorRegisterForm from './DoctorRegisterForm'
import GoogleSignInButton from '../../components/common/GoogleSignInButton'

const benefits = [
  { color: 'bg-blue-100 text-primary-600', title: 'Secure & Encrypted', text: 'Your health data is protected with industry-standard security' },
  { color: 'bg-green-100 text-green-600', title: '24/7 Access', text: 'Connect with healthcare professionals anytime, anywhere' },
  { color: 'bg-purple-100 text-purple-600', title: 'Easy Management', text: 'Manage appointments and records with a user-friendly interface' },
]

export default function LoginPage() {
  const [role, setRole] = useState('patient')
  const [tab, setTab] = useState('login')
  const [form, setForm] = useState({ email: '', password: '' })
  const [googleError, setGoogleError] = useState(null)
  const dispatch = useDispatch()
  const navigate = useNavigate()
  const { status, error } = useSelector((s) => s.auth)

  const handleLogin = async (e) => {
    e.preventDefault()
    const res = await dispatch(login({ ...form, role }))
    if (login.fulfilled.match(res)) {
      navigate(role === 'doctor' ? '/doctor' : '/patient')
    }
  }

  return (
    <div className="grid min-h-screen lg:grid-cols-2">
      {/* Left branding panel */}
      <div className="hidden flex-col justify-center bg-gradient-to-br from-blue-50 to-white p-14 lg:flex">
        <Logo size="lg" />
        <h1 className="mt-10 text-4xl font-extrabold leading-tight text-slate-900">
          Welcome to Your Digital Healthcare Platform
        </h1>
        <p className="mt-4 max-w-md text-slate-500">
          Access your medical consultations, appointments, and health records securely from anywhere.
        </p>
        <div className="mt-10 space-y-6">
          {benefits.map((b) => (
            <div key={b.title} className="flex gap-4">
              <div className={`flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-full ${b.color}`}>
                <Check size={16} />
              </div>
              <div>
                <div className="font-bold text-slate-800">{b.title}</div>
                <div className="text-sm text-slate-500">{b.text}</div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Right form panel */}
      <div className="flex items-center justify-center bg-white p-6 sm:p-10">
        <div className="w-full max-w-md">
          {/* Role toggle */}
          <div className="grid grid-cols-2 gap-3">
            <button
              onClick={() => setRole('patient')}
              className={`flex items-center justify-center gap-2 rounded-lg py-3 text-sm font-semibold transition ${
                role === 'patient' ? 'bg-primary-600 text-white' : 'bg-slate-100 text-slate-500'
              }`}
            >
              <User size={16} /> Patient
            </button>
            <button
              onClick={() => setRole('doctor')}
              className={`flex items-center justify-center gap-2 rounded-lg py-3 text-sm font-semibold transition ${
                role === 'doctor' ? 'bg-primary-600 text-white' : 'bg-slate-100 text-slate-500'
              }`}
            >
              <Stethoscope size={16} /> Doctor
            </button>
          </div>

          {/* Login / Register tabs */}
          <div className="mt-6 flex gap-6 border-b border-slate-200">
            {['login', 'register'].map((t) => (
              <button
                key={t}
                onClick={() => setTab(t)}
                className={`-mb-px border-b-2 pb-2.5 text-sm font-semibold capitalize transition ${
                  tab === t ? 'border-primary-600 text-primary-600' : 'border-transparent text-slate-400'
                }`}
              >
                {t}
              </button>
            ))}
          </div>

          {tab === 'login' ? (
            <form onSubmit={handleLogin} className="mt-6">
              <h2 className="text-2xl font-extrabold text-slate-900">Welcome Back</h2>
              <p className="mt-1 text-sm text-slate-500">Login to your {role} account</p>

              {error && <div className="mt-4 rounded-lg bg-red-50 px-4 py-2.5 text-sm text-red-600">{error}</div>}

              <div className="mt-6 space-y-4">
                <Field label="Email Address">
                  <Input icon={Mail} type="email" required placeholder="Enter your email"
                    value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
                </Field>
                <Field label="Password">
                  <Input icon={Lock} type="password" required placeholder="Enter your password"
                    value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} />
                </Field>
              </div>

              <div className="mt-4 flex items-center justify-between text-sm">
                <label className="flex items-center gap-2 text-slate-600">
                  <input type="checkbox" className="h-4 w-4 rounded border-slate-300" /> Remember me
                </label>
                <a href="#" className="font-medium text-primary-600">Forgot Password?</a>
              </div>

              <button type="submit" disabled={status === 'loading'}
                className="mt-6 w-full rounded-lg bg-primary-600 py-3 text-sm font-semibold text-white hover:bg-primary-700 disabled:opacity-60">
                {status === 'loading' ? 'Signing in...' : 'Login'}
              </button>

              {/* Google Sign-In creates a patient account, so it is only
                  offered on the patient tab. A doctor needs a licence number
                  verified by an admin before the account means anything. */}
              {role === 'patient' && (
                <>
                  <div className="my-6 flex items-center gap-3 text-xs text-slate-400">
                    <div className="h-px flex-1 bg-slate-200" /> Or continue with <div className="h-px flex-1 bg-slate-200" />
                  </div>
                  <GoogleSignInButton onError={setGoogleError} />
                  {googleError && (
                    <div className="mt-3 rounded-lg bg-red-50 px-4 py-2.5 text-sm text-red-600">
                      {googleError}
                    </div>
                  )}
                </>
              )}
              <div className="mt-6 text-center">
                <Link to="/admin/login" className="text-sm font-semibold text-slate-700 hover:text-primary-600">Admin Login →</Link>
              </div>
            </form>
          ) : role === 'patient' ? (
            <PatientRegisterForm />
          ) : (
            <DoctorRegisterForm />
          )}
        </div>
      </div>
    </div>
  )
}
