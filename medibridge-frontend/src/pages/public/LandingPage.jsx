import { Link } from 'react-router-dom'
import { Calendar, FileText, Shield, Users, Activity, Clock } from 'lucide-react'
import PublicNavbar from '../../components/layout/PublicNavbar'

const features = [
  { icon: Calendar, color: 'bg-blue-100 text-primary-600', title: 'Easy Scheduling', text: 'Book appointments with doctors based on their specialization and availability in just a few clicks.' },
  { icon: FileText, color: 'bg-green-100 text-green-600', title: 'Medical Records', text: 'Upload and access your medical history, prescriptions, and reports securely from anywhere.' },
  { icon: Shield, color: 'bg-purple-100 text-purple-600', title: 'Secure & Private', text: 'Your health data is encrypted and protected with industry-standard security protocols.' },
]

const steps = [
  { n: 1, title: 'Register & Login', text: 'Create your account as a patient or doctor and access your personalized dashboard.' },
  { n: 2, title: 'Search & Book', text: 'Find doctors by specialization, check availability, and book your appointment instantly.' },
  { n: 3, title: 'Consult & Get Care', text: 'Attend your consultation, receive prescriptions, and access all records digitally.' },
]

const stats = [
  { icon: Users, value: '10,000+', label: 'Active Patients' },
  { icon: Activity, value: '500+', label: 'Certified Doctors' },
  { icon: Clock, value: '24/7', label: 'Support Available' },
]

export default function LandingPage() {
  return (
    <div className="min-h-screen bg-white">
      <PublicNavbar />

      {/* Hero */}
      <section className="bg-gradient-to-b from-blue-50 to-white">
        <div className="mx-auto max-w-6xl px-6 pt-16 text-center">
          <h1 className="text-5xl font-extrabold tracking-tight text-slate-900">Your Health, Our Priority</h1>
          <p className="mx-auto mt-5 max-w-2xl text-lg text-slate-500">
            Connect with certified doctors online, book appointments instantly, and manage your health records securely - all in one place.
          </p>
          <div className="mt-8 flex items-center justify-center gap-4">
            <Link to="/login" className="rounded-lg bg-primary-600 px-7 py-3.5 text-sm font-semibold text-white hover:bg-primary-700">
              Book Appointment
            </Link>
            <Link to="/login" className="rounded-lg border border-primary-600 bg-white px-7 py-3.5 text-sm font-semibold text-primary-600 hover:bg-primary-50">
              Find a Doctor
            </Link>
          </div>

          {/* Feature cards */}
          <div className="mt-16 grid gap-6 pb-4 md:grid-cols-3">
            {features.map((f) => (
              <div key={f.title} className="rounded-2xl border border-slate-100 bg-white p-8 text-center shadow-sm">
                <div className={`mx-auto flex h-14 w-14 items-center justify-center rounded-full ${f.color}`}>
                  <f.icon size={26} />
                </div>
                <h3 className="mt-5 text-lg font-bold text-slate-900">{f.title}</h3>
                <p className="mt-2 text-sm leading-relaxed text-slate-500">{f.text}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* How it works */}
      <section id="services" className="border-t-4 border-slate-900">
        <div className="mx-auto max-w-6xl px-6 py-16">
          <h2 className="text-center text-3xl font-extrabold text-slate-900">How It Works</h2>
          <div className="mt-12 grid gap-8 md:grid-cols-3">
            {steps.map((s) => (
              <div key={s.n} className="text-center">
                <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-primary-600 text-lg font-bold text-white">
                  {s.n}
                </div>
                <h3 className="mt-5 text-lg font-bold text-slate-900">{s.title}</h3>
                <p className="mx-auto mt-2 max-w-xs text-sm leading-relaxed text-slate-500">{s.text}</p>
              </div>
            ))}
          </div>
        </div>

        {/* Stats band */}
        <div className="bg-primary-600">
          <div className="mx-auto grid max-w-6xl gap-8 px-6 py-14 text-center md:grid-cols-3">
            {stats.map((s) => (
              <div key={s.label} className="text-white">
                <s.icon size={34} className="mx-auto opacity-90" />
                <div className="mt-4 text-4xl font-extrabold">{s.value}</div>
                <div className="mt-1 text-sm text-white/90">{s.label}</div>
              </div>
            ))}
          </div>
        </div>
      </section>

      <footer id="contact" className="bg-slate-900 py-8 text-center text-sm text-slate-400">
        © 2026 MediBridge. Your Health, Our Priority.
      </footer>
    </div>
  )
}
