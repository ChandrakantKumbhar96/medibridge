import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Pill, FileText, Video, Activity, ArrowLeft } from 'lucide-react'
import DashboardLayout from '../../components/layout/DashboardLayout'
import { patientNav } from './patientNav'
import { appointmentService } from '../../services/appointmentService'
import { prescriptionService } from '../../services/prescriptionService'
import { recordService } from '../../services/recordService'

const TONE = {
  consult: { icon: Video, cls: 'bg-primary-50 text-primary-600', ring: 'border-primary-200' },
  prescription: { icon: Pill, cls: 'bg-accent-50 text-accent-600', ring: 'border-accent-200' },
  record: { icon: FileText, cls: 'bg-success-50 text-success-600', ring: 'border-success-200' },
}

/**
 * A single chronological view of a patient's history, built entirely from data
 * the app already stores — past consultations, issued prescriptions and uploaded
 * reports — merged and sorted newest-first. No new backend needed.
 */
export default function HealthTimeline() {
  const navigate = useNavigate()
  const [events, setEvents] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    Promise.all([
      appointmentService.getPatientAppointments().catch(() => ({ past: [], upcoming: [] })),
      prescriptionService.getMyPrescriptions().catch(() => []),
      recordService.getRecords().catch(() => []),
    ]).then(([appts, prescriptions, records]) => {
      const past = Array.isArray(appts?.past) ? appts.past : []
      const merged = [
        ...past.map((a) => ({
          key: `a-${a.appointment_id}`, date: a.appointment_date, type: 'consult',
          title: `Consultation with ${a.doctor}`,
          subtitle: `${a.specialization || ''}${a.reason ? ` • ${a.reason}` : ''}`,
        })),
        ...(Array.isArray(prescriptions) ? prescriptions : []).map((p) => ({
          key: `p-${p.prescription_id}`, date: p.date_issued, type: 'prescription',
          title: p.diagnosis,
          subtitle: `${p.doctor_name} • ${p.medicines?.length || 0} medicine(s)`,
          action: () => prescriptionService.downloadPdf(p.prescription_id),
          actionLabel: 'Download PDF',
        })),
        ...(Array.isArray(records) ? records : []).map((r) => ({
          key: `r-${r.report_id}`, date: r.upload_date, type: 'record',
          title: r.report_name,
          subtitle: `${r.report_type} • ${r.size}`,
          action: () => recordService.download(r.report_id, r.report_name),
          actionLabel: 'Download',
        })),
      ].sort((x, y) => String(y.date).localeCompare(String(x.date)))

      setEvents(merged)
      setLoading(false)
    })
  }, [])

  return (
    <DashboardLayout navItems={patientNav}>
      <button onClick={() => navigate('/patient/records')}
        className="mb-4 inline-flex items-center gap-1.5 text-sm font-semibold text-sand-500 transition hover:text-primary-600">
        <ArrowLeft size={16} /> Back to records
      </button>

      <span className="eyebrow">Your history</span>
      <h1 className="mt-1 text-display-sm text-sand-900">Health timeline</h1>
      <p className="mt-1 text-sand-500">Every consultation, prescription and report in one place.</p>

      <div className="surface mt-6 p-6">
        {loading && <div className="text-sm text-sand-500">Loading your timeline…</div>}

        {!loading && events.length === 0 && (
          <div className="flex flex-col items-center gap-2 py-10 text-center">
            <Activity size={26} className="text-sand-300" />
            <p className="text-sm text-sand-500">Nothing here yet — your history builds up as you use MediBridge.</p>
          </div>
        )}

        {!loading && events.length > 0 && (
          <ol className="relative ml-3 border-l-2 border-sand-100">
            {events.map((e) => {
              const tone = TONE[e.type] || TONE.record
              return (
                <li key={e.key} className="mb-6 ml-6 last:mb-0">
                  <span className={`absolute -left-[13px] flex h-6 w-6 items-center justify-center rounded-full ring-4 ring-white ${tone.cls}`}>
                    <tone.icon size={13} />
                  </span>
                  <div className={`rounded-2xl border ${tone.ring} bg-sand-25 p-4`}>
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <div className="min-w-0">
                        <div className="font-bold text-sand-900">{e.title}</div>
                        {e.subtitle && <div className="text-sm text-sand-500">{e.subtitle}</div>}
                      </div>
                      <span className="text-xs font-semibold text-sand-400">{e.date}</span>
                    </div>
                    {e.action && (
                      <button onClick={e.action}
                        className="mt-2 text-xs font-bold text-primary-600 hover:text-primary-700">
                        {e.actionLabel} →
                      </button>
                    )}
                  </div>
                </li>
              )
            })}
          </ol>
        )}
      </div>
    </DashboardLayout>
  )
}
