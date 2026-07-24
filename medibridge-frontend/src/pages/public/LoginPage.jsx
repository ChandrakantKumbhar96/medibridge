import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useDispatch, useSelector } from 'react-redux'
import { Mail, Lock, User, Stethoscope, ShieldCheck, Clock, FileLock2 } from 'lucide-react'
import Logo from '../../components/common/Logo'
import Input, { Field } from '../../components/common/Input'
import GoogleSignInButton from '../../components/common/GoogleSignInButton'
import { login } from '../../features/auth/authSlice'
import PatientRegisterForm from './PatientRegisterForm'
import DoctorRegisterForm from './DoctorRegisterForm'

/* Clinical environment photography, not a portrait — see LandingPage note. */
const PANEL_IMG =
  'https://images.unsplash.com/photo-1516549655169-df83a0774514?auto=format&fit=crop&w=1000&q=80'

const benefits = [
  { icon: FileLock2, title: 'Encrypted records', text: 'Reports and prescriptions only you and your doctor can open.' },
  { icon: Clock, title: 'Real availability', text: 'Slots come straight from each doctor’s live schedule.' },
  { icon: ShieldCheck, title: 'Verified doctors', text: 'Every practitioner’s registration is checked before approval.' },
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
    <div className="grid min-h-screen lg:grid-cols-[1.05fr_1fr]">
      {/* ---------------- Left: image-backed brand panel ---------------- */}
      <div className="relative hidden overflow-hidden lg:block">
        <img src={PANEL_IMG} alt="" className="absolute inset-0 h-full w-full object-cover" />
        {/* brand wash over the photo so text stays legible */}
        <div className="absolute inset-0 bg-gradient-to-br from-primary-900/92 via-primary-800/88 to-primary-700/85" />
        <div aria-hidden className="absolute inset-0 bg-mesh-teal opacity-40" />

        <div className="relative flex h-full flex-col justify-between p-14">
          <Link to="/" className="inline-flex">
            <span className="flex items-center gap-2.5">
              <span className="relative inline-flex h-10 w-10 items-center justify-center rounded-[30%] bg-white/15 backdrop-blur">
                <svg viewBox="0 0 24 24" width="24" height="24" fill="none">
                  <path d="M3 12.5h3.2l1.7-3.4 2.6 6.3 2.2-4.6 1.4 1.7H21"
                    stroke="white" strokeWidth="2.1" strokeLinecap="round" strokeLinejoin="round" />
                </svg>
              </span>
              <span className="text-[26px] font-extrabold tracking-[-0.03em] text-white">MediBridge</span>
            </span>
          </Link>

          <div>
            <h1 className="max-w-md text-[42px] font-extrabold leading-[1.08] tracking-[-0.035em] text-white">
              Care that starts with a conversation.
            </h1>
            <p className="mt-4 max-w-sm text-[15px] leading-relaxed text-primary-50/80">
              Book verified doctors, consult by video, and keep every record in one
              secure place.
            </p>

            <div className="mt-10 space-y-5">
              {benefits.map((b) => (
                <div key={b.title} className="flex gap-4">
                  <span className="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-xl bg-white/12 text-white backdrop-blur">
                    <b.icon size={18} strokeWidth={2.2} />
                  </span>
                  <div>
                    <div className="text-[14px] font-bold text-white">{b.title}</div>
                    <div className="text-[13px] leading-relaxed text-primary-50/70">{b.text}</div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div className="text-[12px] text-primary-50/50">
            © {new Date().getFullYear()} MediBridge · Digital Healthcare Platform
          </div>
        </div>
      </div>

      {/* ---------------- Right: form ---------------- */}
      <div className="flex items-center justify-center bg-white px-6 py-10 sm:px-12">
        <div className="w-full max-w-[400px] animate-fade-up">
          <div className="mb-8 lg:hidden"><Logo size="lg" /></div>

          {/* Role selector — segmented control on a sand track */}
          <div className="flex gap-1 rounded-full bg-sand-100 p-1">
            {[['patient', User, 'Patient'], ['doctor', Stethoscope, 'Doctor']].map(
              ([key, Icon, label]) => (
                <button
                  key={key}
                  onClick={() => setRole(key)}
                  className={`flex flex-1 items-center justify-center gap-2 rounded-full py-2.5
                              text-[13px] font-bold transition-all duration-200 ${
                                role === key
                                  ? 'bg-white text-primary-700 shadow-soft'
                                  : 'text-sand-500 hover:text-sand-700'
                              }`}
                >
                  <Icon size={15} strokeWidth={2.4} /> {label}
                </button>
              )
            )}
          </div>

          {/* Login / Register tabs */}
          <div className="mt-7 flex gap-7 border-b border-sand-200">
            {['login', 'register'].map((t) => (
              <button
                key={t}
                onClick={() => setTab(t)}
                className={`-mb-px border-b-2 pb-3 text-[13.5px] font-bold capitalize transition-colors ${
                  tab === t
                    ? 'border-primary-500 text-primary-700'
                    : 'border-transparent text-sand-400 hover:text-sand-600'
                }`}
              >
                {t}
              </button>
            ))}
          </div>

          {tab === 'login' ? (
            <form onSubmit={handleLogin} className="mt-7">
              <h2 className="text-[26px] font-extrabold tracking-[-0.03em] text-sand-900">
                Welcome back
              </h2>
              <p className="mt-1 text-[14px] text-sand-500">
                Sign in to your {role} account
              </p>

              {error && (
                <div className="mt-5 rounded-xl border border-danger-100 bg-danger-50 px-4 py-3 text-[13px] font-medium text-danger-700">
                  {error}
                </div>
              )}

              <div className="mt-6 space-y-4">
                <Field label="Email address">
                  <Input icon={Mail} type="email" required placeholder="you@example.com"
                    value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
                </Field>
                <Field label="Password">
                  <Input icon={Lock} type="password" required placeholder="••••••••"
                    value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} />
                </Field>
              </div>

              <div className="mt-4 flex items-center justify-between text-[13px]">
                <label className="flex cursor-pointer items-center gap-2 font-medium text-sand-600">
                  <input type="checkbox"
                    className="h-4 w-4 rounded border-sand-300 text-primary-600 focus:ring-primary-500/20" />
                  Remember me
                </label>
                <a href="#" className="font-bold text-primary-600 hover:text-primary-700">
                  Forgot password?
                </a>
              </div>

              <button
                type="submit"
                disabled={status === 'loading'}
                className="mt-6 w-full rounded-full bg-gradient-to-b from-primary-500 to-primary-600 py-3.5
                           text-sm font-bold text-white shadow-[0_8px_24px_-8px_rgba(15,133,123,.6)]
                           transition-all hover:from-primary-600 hover:to-primary-700
                           disabled:cursor-not-allowed disabled:opacity-60"
              >
                {status === 'loading' ? 'Signing in…' : 'Sign in'}
              </button>

              {/* Google Sign-In creates a patient account, so it is only
                  offered on the patient tab. A doctor needs a licence number
                  verified by an admin before the account means anything. */}
              {role === 'patient' && (
                <>
                  <div className="my-6 flex items-center gap-3 text-[11px] font-semibold uppercase tracking-wider text-sand-400">
                    <div className="h-px flex-1 bg-sand-200" /> or <div className="h-px flex-1 bg-sand-200" />
                  </div>
                  <GoogleSignInButton onError={setGoogleError} />
                  {googleError && (
                    <div className="mt-3 rounded-xl border border-danger-100 bg-danger-50 px-4 py-2.5 text-[13px] text-danger-700">
                      {googleError}
                    </div>
                  )}
                </>
              )}

              <div className="mt-7 border-t border-sand-100 pt-5 text-center">
                <Link to="/admin/login"
                  className="text-[13px] font-bold text-sand-500 transition-colors hover:text-primary-600">
                  Administrator sign-in →
                </Link>
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
