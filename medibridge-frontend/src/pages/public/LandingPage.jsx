import { Link } from 'react-router-dom'
import {
  Calendar, FileText, ShieldCheck, Video, Star, ArrowRight,
  Stethoscope, CheckCircle2, Clock, Mail, Phone, MapPin, PhoneOff,
} from 'lucide-react'
import PublicNavbar from '../../components/layout/PublicNavbar'
import Avatar from '../../components/common/Avatar'
import Reveal from '../../components/common/Reveal'
import StatCounter from '../../components/common/StatCounter'

/* Free-licence clinical photography (Unsplash) — deliberately an environment,
   not a portrait — presenting a stock face as a named doctor with a
   registration number would be misrepresentation, even in a demo. */
const CARE_IMG =
  'https://images.unsplash.com/photo-1631217868264-e5b90bb7e133?auto=format&fit=crop&w=1000&q=80'

const onlineDoctors = ['Dr. Aditya Nair', 'Dr. Kavya Rao', 'Dr. Rohan Mehta']

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

const stats = [
  { value: 10000, suffix: '+', label: 'Patients treated' },
  { value: 500, suffix: '+', label: 'Verified doctors' },
  { value: 25000, suffix: '+', label: 'Consultations completed' },
  { value: 4.9, suffix: '/5', decimals: 1, label: 'Average rating' },
]

const testimonials = [
  { name: 'Sana Iyer', role: 'Patient · Pune', rating: 5,
    quote: 'Booked a cardiologist in two minutes and had my prescription in hand before I even left the video call.' },
  { name: 'Rahul Deshmukh', role: 'Patient · Mumbai', rating: 5,
    quote: 'No more sitting in a waiting room. All my reports and past prescriptions are in one place now.' },
  { name: 'Dr. Priya Menon', role: 'Dermatologist', rating: 5,
    quote: 'The scheduling actually reflects my real calendar — no more double bookings from the front desk.' },
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
                           px-7 py-3.5 text-sm font-bold text-white shadow-[0_8px_24px_-8px_rgba(37,99,235,.6)]
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

            {/* online-doctors avatar stack */}
            <div className="mt-6 flex items-center gap-3">
              <div className="flex -space-x-3">
                {onlineDoctors.map((n) => (
                  <Avatar key={n} name={n} size={34} ring={false} className="ring-2 ring-white" />
                ))}
              </div>
              <span className="inline-flex items-center gap-1.5 text-[13px] font-semibold text-sand-600">
                +138 doctors online
                <span className="h-2 w-2 rounded-full bg-success-500 shadow-[0_0_0_3px_rgba(34,197,94,.18)]" />
              </span>
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
            {/* illustrated in-call mockup — no stock portraits, see Avatar.jsx */}
            <div className="relative h-[420px] w-full overflow-hidden rounded-[28px] border border-white/70 bg-gradient-to-br from-primary-700 via-primary-600 to-primary-900 shadow-lift">
              <div aria-hidden className="absolute inset-0 bg-mesh-teal opacity-30" />

              <div className="absolute left-5 top-5 flex items-center gap-1.5 text-[13px] font-extrabold text-white">
                <span className="h-1.5 w-1.5 rounded-full bg-accent-400" /> MediBridge
              </div>

              <div className="flex h-full flex-col items-center justify-center gap-3">
                <Avatar name="Dr. Aditya Nair" size={124} ring={false} color="solid" />
                <div className="text-[15px] font-bold text-white">Dr. Aditya Nair</div>
                <div className="text-[12px] font-medium text-primary-100">Cardiology · Live now</div>
              </div>

              {/* inset participant */}
              <div className="absolute right-5 top-5 h-28 w-24 overflow-hidden rounded-2xl border-2 border-white/70 bg-sand-100 shadow-lg">
                <div className="flex h-full items-center justify-center">
                  <Avatar name="Meera Joshi" size={64} ring={false} />
                </div>
              </div>

              {/* call timer */}
              <div className="absolute left-5 bottom-24 flex items-center gap-1.5 rounded-full bg-black/25 px-3 py-1.5 text-[11px] font-semibold text-white backdrop-blur-sm">
                <span className="h-1.5 w-1.5 animate-pulse rounded-full bg-accent-400" /> 04:12
              </div>

              {/* hang-up control */}
              <button
                type="button"
                aria-label="End call"
                className="absolute bottom-6 left-1/2 flex h-12 w-12 -translate-x-1/2 items-center justify-center
                           rounded-full bg-accent-600 text-white shadow-lg transition-transform hover:scale-105"
              >
                <PhoneOff size={19} />
              </button>
            </div>

            {/* floating appointment card */}
            <div className="glass absolute -bottom-6 -left-4 w-[268px] p-4 md:-left-8">
              <div className="flex items-center gap-3">
                <Avatar name="Dr. Aditya Nair" size={44} ring={false} />
                <div className="min-w-0">
                  <div className="truncate text-[13px] font-bold text-sand-900">Dr. Aditya Nair</div>
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

      {/* ================= STATS ================= */}
      <section className="border-y border-sand-200/70 bg-white">
        <div className="mx-auto grid max-w-[1200px] grid-cols-2 gap-6 px-6 py-10 sm:grid-cols-4">
          {stats.map((s) => (
            <Reveal key={s.label} className="text-center">
              <div className="text-[28px] font-extrabold tracking-[-0.02em] text-primary-700 sm:text-[34px]">
                <StatCounter value={s.value} suffix={s.suffix} decimals={s.decimals || 0} />
              </div>
              <div className="mt-1.5 text-[12.5px] font-semibold text-sand-500">{s.label}</div>
            </Reveal>
          ))}
        </div>
      </section>

      {/* ================= SPECIALTIES ================= */}
      <section id="services" className="mx-auto max-w-[1200px] scroll-mt-24 px-6 py-16">
        <Reveal className="flex flex-wrap items-end justify-between gap-4">
          <div>
            <span className="eyebrow">Departments</span>
            <h2 className="mt-2 text-[32px] font-extrabold tracking-[-0.03em] text-sand-900">
              Find the right specialist
            </h2>
          </div>
          <Link to="/login" className="text-sm font-bold text-primary-600 hover:text-primary-700">
            View all doctors →
          </Link>
        </Reveal>

        <div className="mt-8 grid grid-cols-2 gap-4 md:grid-cols-3 lg:grid-cols-6">
          {specialties.map((s, i) => (
            <Reveal key={s.name} delay={i * 60}>
              <Link
                to="/login"
                className="group block rounded-2xl border border-sand-200/70 bg-white p-5 text-center shadow-soft
                           transition-all duration-300 hover:-translate-y-1 hover:border-primary-200 hover:shadow-card"
              >
                <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-sand-50 text-2xl transition-colors group-hover:bg-primary-50">
                  {s.emoji}
                </div>
                <div className="mt-3 text-[13px] font-bold text-sand-800">{s.name}</div>
              </Link>
            </Reveal>
          ))}
        </div>
      </section>

      {/* ================= FEATURES / ABOUT ================= */}
      <section id="about" className="scroll-mt-24 border-y border-sand-200/70 bg-sand-50">
        <div className="mx-auto max-w-[1200px] px-6 py-16">
          <Reveal className="max-w-xl">
            <span className="eyebrow">Why MediBridge</span>
            <h2 className="mt-2 text-[32px] font-extrabold tracking-[-0.03em] text-sand-900">
              Built around how care actually works
            </h2>
          </Reveal>

          <div className="mt-10 grid gap-6 md:grid-cols-3">
            {features.map((f, i) => (
              <Reveal key={f.title} delay={i * 80}>
                <div
                  className="h-full rounded-2xl border border-sand-200/70 bg-white p-6 shadow-soft
                             transition-all duration-300 hover:-translate-y-1 hover:shadow-card"
                >
                  <div className={`inline-flex rounded-xl p-3 ${f.tone}`}>
                    <f.icon size={21} strokeWidth={2.2} />
                  </div>
                  <h3 className="mt-5 text-[17px] font-bold tracking-tight text-sand-900">{f.title}</h3>
                  <p className="mt-2 text-[14px] leading-relaxed text-sand-600">{f.text}</p>
                </div>
              </Reveal>
            ))}
          </div>
        </div>
      </section>

      {/* ================= HOW IT WORKS ================= */}
      <section className="mx-auto max-w-[1200px] px-6 py-16">
        <div className="grid items-center gap-14 lg:grid-cols-2">
          <Reveal className="relative order-2 lg:order-1">
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
          </Reveal>

          <Reveal delay={120} className="order-1 lg:order-2">
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
          </Reveal>
        </div>
      </section>

      {/* ================= TESTIMONIALS ================= */}
      <section className="mx-auto max-w-[1200px] px-6 py-16">
        <Reveal className="max-w-xl">
          <span className="eyebrow">Testimonials</span>
          <h2 className="mt-2 text-[32px] font-extrabold tracking-[-0.03em] text-sand-900">
            Loved by patients and doctors alike
          </h2>
        </Reveal>

        <div className="mt-10 grid gap-6 md:grid-cols-3">
          {testimonials.map((t, i) => (
            <Reveal key={t.name} delay={i * 80}>
              <div className="flex h-full flex-col rounded-2xl border border-sand-200/70 bg-white p-6 shadow-soft">
                <div className="flex items-center gap-0.5 text-warning-500">
                  {Array.from({ length: t.rating }).map((_, idx) => (
                    <Star key={idx} size={14} fill="currentColor" />
                  ))}
                </div>
                <p className="mt-4 flex-1 text-[14px] leading-relaxed text-sand-700">“{t.quote}”</p>
                <div className="mt-5 flex items-center gap-3">
                  <Avatar name={t.name} size={38} ring={false} />
                  <div>
                    <div className="text-[13px] font-bold text-sand-900">{t.name}</div>
                    <div className="text-[11.5px] font-medium text-sand-500">{t.role}</div>
                  </div>
                </div>
              </div>
            </Reveal>
          ))}
        </div>
      </section>

      {/* ================= CTA ================= */}
      <section className="px-6 pb-20">
        <Reveal className="relative mx-auto max-w-[1200px] overflow-hidden rounded-[28px] bg-gradient-to-br from-primary-700 via-primary-600 to-primary-800 px-8 py-14 text-center">
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
        </Reveal>
      </section>

      {/* ================= CONTACT / FOOTER ================= */}
      <footer id="contact" className="scroll-mt-24 border-t border-sand-200/70 bg-white">
        <div className="mx-auto max-w-[1200px] px-6 py-14">
          <div className="grid gap-10 md:grid-cols-[1.4fr_1fr_1fr]">
            {/* brand + contact */}
            <div>
              <div className="text-xl font-extrabold tracking-tight text-sand-900">MediBridge</div>
              <p className="mt-2 max-w-xs text-sm leading-relaxed text-sand-500">
                Consult verified doctors by video, book real slots, and keep every
                record in one secure place.
              </p>
              <div className="mt-5 space-y-2.5 text-sm">
                <a href="mailto:support@medibridge.com"
                  className="flex items-center gap-2.5 text-sand-600 transition-colors hover:text-primary-700">
                  <Mail size={16} className="text-primary-600" /> support@medibridge.com
                </a>
                <a href="tel:+911800123456"
                  className="flex items-center gap-2.5 text-sand-600 transition-colors hover:text-primary-700">
                  <Phone size={16} className="text-primary-600" /> 1800-123-456 (toll free)
                </a>
                <div className="flex items-center gap-2.5 text-sand-600">
                  <MapPin size={16} className="text-primary-600" /> Pune, Maharashtra, India
                </div>
              </div>
            </div>

            {/* for patients */}
            <div>
              <div className="text-[13px] font-bold uppercase tracking-wider text-sand-400">For patients</div>
              <ul className="mt-4 space-y-2.5 text-sm">
                <li><Link to="/login" className="text-sand-600 hover:text-primary-700">Find a doctor</Link></li>
                <li><Link to="/login" className="text-sand-600 hover:text-primary-700">Book a consultation</Link></li>
                <li><a href="#services" className="text-sand-600 hover:text-primary-700">Departments</a></li>
                <li><a href="#about" className="text-sand-600 hover:text-primary-700">How it works</a></li>
              </ul>
            </div>

            {/* for doctors */}
            <div>
              <div className="text-[13px] font-bold uppercase tracking-wider text-sand-400">For doctors</div>
              <ul className="mt-4 space-y-2.5 text-sm">
                <li><Link to="/login" className="text-sand-600 hover:text-primary-700">Join as a doctor</Link></li>
                <li><Link to="/login" className="text-sand-600 hover:text-primary-700">Doctor sign in</Link></li>
                <li><Link to="/admin/login" className="text-sand-600 hover:text-primary-700">Administrator</Link></li>
              </ul>
            </div>
          </div>

          <div className="mt-12 flex flex-col items-center justify-between gap-3 border-t border-sand-200/70 pt-6 text-[13px] text-sand-500 sm:flex-row">
            <span>© {new Date().getFullYear()} MediBridge · Digital Healthcare Platform</span>
            <span className="inline-flex items-center gap-1.5">
              <ShieldCheck size={14} className="text-success-500" /> Encrypted &amp; secure
            </span>
          </div>
        </div>
      </footer>
    </div>
  )
}
