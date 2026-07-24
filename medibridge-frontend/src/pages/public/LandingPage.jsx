import { Link } from 'react-router-dom'
import {
  Calendar, FileText, ShieldCheck, Video, Star, ArrowRight,
  Stethoscope, CheckCircle2, Clock,
} from 'lucide-react'
import PublicNavbar from '../../components/layout/PublicNavbar'
import Avatar from '../../components/common/Avatar'

/* Free-licence clinical photography (Unsplash). Deliberately environments and
   equipment rather than portraits — presenting a stock face as a named doctor
   with a registration number would be misrepresentation, even in a demo. */
const HERO_IMG =
  'https://images.unsplash.com/photo-1519494026892-80bbd2d6fd0d?auto=format&fit=crop&w=1200&q=80'
const CARE_IMG =
  'https://images.unsplash.com/photo-1631217868264-e5b90bb7e133?auto=format&fit=crop&w=1000&q=80'

const features = [
  { icon: Calendar, tone: 'bg-primary-50 text-primary-600', title: 'Real-time booking',
    text: 'See genuine open slots from each doctor’s live schedule and confirm instantly — no waiting for a callback.' },
  { icon: Video, tone: 'bg-accent-50 text-accent-600', title: 'Video consultation',
    text: 'A secure consultation link is issued on confirmation and activates shortly before your appointment.' },
  { icon: FileText, tone: 'bg-success-50 text-success-600', title: 'Digital prescriptions',
    text: 'Structured e-prescriptions and your full medical history, downloadable as PDF whenever you need them.' },
]

const steps = [
  { n: '01', title: 'Find your doctor', text: 'Browse verified specialists by department, experience and consultation fee.' },
  { n: '02', title: 'Pick a slot & pay', text: 'Choose a time that suits you and pay securely online. Confirmation is immediate.' },
  { n: '03', title: 'Consult & collect records', text: 'Join by video, then download your prescription and reports from your dashboard.' },
]

const specialties = [
  { name: 'Cardiology', emoji: '🫀' }, { name: 'Dermatology', emoji: '🧴' },
  { name: 'General Physician', emoji: '🩺' }, { name: 'Orthopedics', emoji: '🦴' },
  { name: 'Pediatrics', emoji: '👶' }, { name: 'Neurology', emoji: '🧠' },
]

export default function LandingPage() {
  return (
    <div className="min-h-screen bg-white">
      <PublicNavbar />

      {/* ================= HERO ================= */}
      <section className="relative overflow-hidden bg-sand-50">
        <div aria-hidden className="pointer-events-none absolute inset-0 bg-mesh-teal" />

        <div className="relative mx-auto grid max-w-[1200px] items-center gap-14 px-6 py-16 lg:grid-cols-2 lg:py-24">
          {/* copy */}
          <div className="animate-fade-up">
            <span className="chip-primary">
              <span className="h-1.5 w-1.5 rounded-full bg-primary-500" />
              Trusted by 10,000+ patients
            </span>

            <h1 className="mt-5 text-[clamp(2.4rem,5vw,3.6rem)] font-extrabold leading-[1.05] tracking-[-0.035em] text-sand-900">
              Healthcare that fits
              <br />
              <span className="text-gradient">around your life.</span>
            </h1>

            <p className="mt-5 max-w-lg text-[17px] leading-relaxed text-sand-600">
              Consult verified doctors by video, book real appointment slots in seconds,
              and keep every prescription and report in one secure place.
            </p>

            <div className="mt-8 flex flex-wrap items-center gap-3">
              <Link
                to="/login"
                className="inline-flex items-center gap-2 rounded-full bg-gradient-to-b from-primary-500 to-primary-600
                           px-7 py-3.5 text-sm font-bold text-white shadow-[0_8px_24px_-8px_rgba(15,133,123,.6)]
                           transition-all hover:from-primary-600 hover:to-primary-700 hover:shadow-lift"
              >
                Book an appointment <ArrowRight size={17} />
              </Link>
              <Link
                to="/login"
                className="inline-flex items-center gap-2 rounded-full border border-sand-300 bg-white px-7 py-3.5
                           text-sm font-bold text-sand-700 transition-all hover:border-primary-400 hover:text-primary-700"
              >
                <Stethoscope size={17} /> I’m a doctor
              </Link>
            </div>

            {/* trust row */}
            <div className="mt-9 flex flex-wrap items-center gap-x-7 gap-y-3 text-[13px] font-semibold text-sand-500">
              {['Verified doctors only', 'Encrypted records', 'Secure payments'].map((t) => (
                <span key={t} className="inline-flex items-center gap-1.5">
                  <CheckCircle2 size={15} className="text-success-500" /> {t}
                </span>
              ))}
            </div>
          </div>

          {/* imagery */}
          <div className="relative animate-fade-up lg:pl-6">
            <div className="relative overflow-hidden rounded-[28px] border border-white/70 shadow-lift">
              <img
                src={HERO_IMG}
                alt="Doctor consulting a patient"
                className="h-[420px] w-full object-cover"
                loading="eager"
              />
              <div className="absolute inset-0 bg-gradient-to-t from-primary-900/45 via-transparent to-transparent" />
            </div>

            {/* floating appointment card */}
            <div className="glass absolute -bottom-6 -left-4 w-[268px] p-4 md:-left-8">
              <div className="flex items-center gap-3">
                <Avatar name="Dr. Sarah Johnson" size={44} ring={false} />
                <div className="min-w-0">
                  <div className="truncate text-[13px] font-bold text-sand-900">Dr. Sarah Johnson</div>
                  <div className="text-[11px] font-medium text-sand-500">Cardiology</div>
                </div>
                <span className="ml-auto inline-flex items-center gap-1 rounded-full bg-warning-50 px-2 py-1 text-[11px] font-bold text-warning-700">
                  <Star size={11} fill="currentColor" /> 4.9
                </span>
              </div>
              <div className="mt-3 flex items-center gap-2 rounded-xl bg-success-50 px-3 py-2 text-[12px] font-bold text-success-700">
                <CheckCircle2 size={14} /> Appointment confirmed
              </div>
              <div className="mt-2 flex items-center gap-1.5 text-[11px] font-medium text-sand-500">
                <Clock size={12} /> Today, 10:00 AM · Video consultation
              </div>
            </div>

            {/* floating stat chip */}
            <div className="glass absolute -right-3 top-8 hidden px-4 py-3 md:block">
              <div className="text-[22px] font-extrabold leading-none text-sand-900">500+</div>
              <div className="mt-1 text-[11px] font-semibold text-sand-500">Verified doctors</div>
            </div>
          </div>
        </div>
      </section>

      {/* ================= SPECIALTIES ================= */}
      <section className="mx-auto max-w-[1200px] px-6 py-16">
        <div className="flex flex-wrap items-end justify-between gap-4">
          <div>
            <span className="eyebrow">Departments</span>
            <h2 className="mt-2 text-[32px] font-extrabold tracking-[-0.03em] text-sand-900">
              Find the right specialist
            </h2>
          </div>
          <Link to="/login" className="text-sm font-bold text-primary-600 hover:text-primary-700">
            View all doctors →
          </Link>
        </div>

        <div className="mt-8 grid grid-cols-2 gap-4 md:grid-cols-3 lg:grid-cols-6">
          {specialties.map((s) => (
            <Link
              key={s.name}
              to="/login"
              className="group rounded-2xl border border-sand-200/70 bg-white p-5 text-center shadow-soft
                         transition-all duration-300 hover:-translate-y-1 hover:border-primary-200 hover:shadow-card"
            >
              <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-sand-50 text-2xl transition-colors group-hover:bg-primary-50">
                {s.emoji}
              </div>
              <div className="mt-3 text-[13px] font-bold text-sand-800">{s.name}</div>
            </Link>
          ))}
        </div>
      </section>

      {/* ================= FEATURES ================= */}
      <section className="border-y border-sand-200/70 bg-sand-50">
        <div className="mx-auto max-w-[1200px] px-6 py-16">
          <div className="max-w-xl">
            <span className="eyebrow">Why MediBridge</span>
            <h2 className="mt-2 text-[32px] font-extrabold tracking-[-0.03em] text-sand-900">
              Built around how care actually works
            </h2>
          </div>

          <div className="mt-10 grid gap-6 md:grid-cols-3">
            {features.map((f) => (
              <div
                key={f.title}
                className="rounded-2xl border border-sand-200/70 bg-white p-6 shadow-soft
                           transition-all duration-300 hover:-translate-y-1 hover:shadow-card"
              >
                <div className={`inline-flex rounded-xl p-3 ${f.tone}`}>
                  <f.icon size={21} strokeWidth={2.2} />
                </div>
                <h3 className="mt-5 text-[17px] font-bold tracking-tight text-sand-900">{f.title}</h3>
                <p className="mt-2 text-[14px] leading-relaxed text-sand-600">{f.text}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ================= HOW IT WORKS ================= */}
      <section className="mx-auto max-w-[1200px] px-6 py-16">
        <div className="grid items-center gap-14 lg:grid-cols-2">
          <div className="relative order-2 lg:order-1">
            <div className="overflow-hidden rounded-[28px] border border-sand-200/70 shadow-lift">
              <img src={CARE_IMG} alt="Clinical care" className="h-[400px] w-full object-cover" loading="lazy" />
            </div>
            <div className="glass absolute -right-4 bottom-6 hidden px-5 py-4 md:block">
              <div className="flex items-center gap-2 text-[13px] font-bold text-sand-900">
                <ShieldCheck size={16} className="text-primary-600" /> Records encrypted
              </div>
              <div className="mt-1 text-[11px] font-medium text-sand-500">
                Only you and your doctor can access them
              </div>
            </div>
          </div>

          <div className="order-1 lg:order-2">
            <span className="eyebrow">How it works</span>
            <h2 className="mt-2 text-[32px] font-extrabold tracking-[-0.03em] text-sand-900">
              Three steps to care
            </h2>

            <div className="mt-8 space-y-7">
              {steps.map((s) => (
                <div key={s.n} className="flex gap-5">
                  <span className="text-[28px] font-extrabold leading-none tracking-tight text-primary-200">
                    {s.n}
                  </span>
                  <div>
                    <h3 className="text-[16px] font-bold tracking-tight text-sand-900">{s.title}</h3>
                    <p className="mt-1 text-[14px] leading-relaxed text-sand-600">{s.text}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* ================= CTA ================= */}
      <section className="px-6 pb-20">
        <div className="relative mx-auto max-w-[1200px] overflow-hidden rounded-[28px] bg-gradient-to-br from-primary-700 via-primary-600 to-primary-800 px-8 py-14 text-center">
          <div aria-hidden className="pointer-events-none absolute inset-0 bg-mesh-teal opacity-60" />
          <div className="relative">
            <h2 className="text-[32px] font-extrabold tracking-[-0.03em] text-white">
              Your next appointment is a minute away
            </h2>
            <p className="mx-auto mt-3 max-w-lg text-[15px] text-primary-50/90">
              Join thousands of patients managing their healthcare with MediBridge.
            </p>
            <Link
              to="/login"
              className="mt-8 inline-flex items-center gap-2 rounded-full bg-white px-8 py-3.5 text-sm font-bold
                         text-primary-700 shadow-lg transition-transform hover:scale-[1.02]"
            >
              Get started free <ArrowRight size={17} />
            </Link>
          </div>
        </div>
      </section>

      <footer className="border-t border-sand-200/70 bg-sand-50 py-8">
        <div className="mx-auto max-w-[1200px] px-6 text-center text-[13px] text-sand-500">
          © {new Date().getFullYear()} MediBridge · Digital Healthcare Platform
        </div>
      </footer>
    </div>
  )
}
