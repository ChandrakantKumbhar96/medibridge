import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import {
  ArrowLeft, ClipboardCheck, Download, FileText, CheckCircle2, AlertTriangle,
} from 'lucide-react'
import DashboardLayout from '../../components/layout/DashboardLayout'
import Card from '../../components/common/Card'
import Button from '../../components/common/Button'
import Avatar from '../../components/common/Avatar'
import { Field } from '../../components/common/Input'
import { doctorNav } from './doctorNav'
import { appointmentService } from '../../services/appointmentService'
import { opinionService } from '../../services/opinionService'
import { recordService } from '../../services/recordService'

const BLANK = {
  original_diagnosis: '',
  findings: '',
  agrees_with_original: null,
  recommendation: '',
  suggested_tests: '',
}

/**
 * The reviewing specialist's write-up for a second opinion.
 *
 * Deliberately not the prescription form with the medicine table removed. A
 * second opinion answers a different question — does the original diagnosis
 * hold, and what should happen instead — and it very often prescribes nothing
 * at all. Routing it through a medicines form would make the commonest correct
 * outcome look like an unfinished record.
 */
export default function WriteOpinion() {
  const { appointmentId } = useParams()
  const navigate = useNavigate()

  const [appointment, setAppointment] = useState(null)
  const [reports, setReports] = useState([])
  const [form, setForm] = useState(BLANK)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState(null)
  const [issued, setIssued] = useState(null)

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }))

  useEffect(() => {
    appointmentService.getDoctorAppointments()
      .then((list) => {
        const match = list.find((a) => String(a.appointment_id) === String(appointmentId))
        setAppointment(match || null)
        // The reports are the whole reason this consultation exists, so they are
        // pulled onto the same screen rather than left a navigation away.
        if (match?.patient_id) {
          recordService.getPatientRecords(match.patient_id)
            .then(setReports)
            .catch(() => setReports([]))
        }
      })
      .catch(() => setAppointment(null))
  }, [appointmentId])

  const submit = async (e) => {
    e.preventDefault()
    setError(null)

    if (form.agrees_with_original === null) {
      setError('Please state whether you agree with the original diagnosis.')
      return
    }

    setSaving(true)
    try {
      setIssued(await opinionService.create({
        appointment_id: Number(appointmentId),
        original_diagnosis: form.original_diagnosis,
        findings: form.findings,
        agrees_with_original: form.agrees_with_original,
        recommendation: form.recommendation,
        suggested_tests: form.suggested_tests || null,
      }))
    } catch (err) {
      setError(err?.response?.data?.message || 'Could not issue the opinion.')
    } finally {
      setSaving(false)
    }
  }

  if (issued) {
    return (
      <DashboardLayout badge="Doctor" navItems={doctorNav}>
        <Card className="mx-auto mt-10 max-w-lg text-center">
          <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-success-100 text-success-600">
            <ClipboardCheck size={26} />
          </div>
          <h1 className="mt-4 text-2xl font-extrabold text-sand-900">Opinion issued</h1>
          <p className="mt-2 text-sm text-sand-500">
            {issued.patient || appointment?.patient} can now download the signed
            opinion from their Medical Records.
          </p>
          <div className="mt-4 inline-flex items-center gap-1.5 rounded-full bg-sand-100 px-3 py-1 text-xs font-bold text-sand-700">
            {issued.verdict}
          </div>
          <div className="mt-6 flex justify-center gap-3">
            <Button onClick={() => opinionService.downloadPdf(issued.opinion_id)} className="px-5">
              <Download size={16} /> Download PDF
            </Button>
            <button onClick={() => navigate('/doctor/appointments')}
              className="rounded-xl border border-sand-300 px-5 text-sm font-semibold text-sand-700 hover:bg-sand-50">
              Back to appointments
            </button>
          </div>
        </Card>
      </DashboardLayout>
    )
  }

  const textarea =
    'w-full rounded-xl border border-sand-200 bg-sand-50/60 px-4 py-3 text-sm font-medium ' +
    'text-sand-900 outline-none transition placeholder:font-normal placeholder:text-sand-400 ' +
    'hover:bg-white focus:border-primary-400 focus:bg-white focus:ring-4 focus:ring-primary-500/10'

  return (
    <DashboardLayout badge="Doctor" navItems={doctorNav}>
      <button onClick={() => navigate('/doctor/appointments')}
        className="flex items-center gap-1.5 text-sm font-medium text-sand-500 hover:text-primary-600">
        <ArrowLeft size={16} /> Back to appointments
      </button>

      <span className="eyebrow mt-3 block">Second opinion</span>
      <h1 className="mt-1 text-display-sm text-sand-900">Write opinion</h1>
      <p className="mt-1 text-sand-500">
        Review the case and issue an independent written evaluation.
      </p>

      {error && (
        <div className="mt-5 rounded-xl bg-danger-50 px-4 py-3 text-sm text-danger-600">{error}</div>
      )}

      {appointment && (
        <Card className="mt-6 flex items-center gap-4">
          <Avatar name={appointment.patient} size={48} />
          <div>
            <div className="font-bold text-sand-900">{appointment.patient}</div>
            <div className="text-sm text-sand-500">
              {appointment.age} yrs • {appointment.appointment_date} at {appointment.time}
            </div>
            {appointment.reason && (
              <div className="mt-1 text-sm text-sand-600">Asked about: {appointment.reason}</div>
            )}
          </div>
        </Card>
      )}

      {/* What the patient uploaded. Booking is refused without at least one, so
          this list is never empty for a real second opinion. */}
      <Card className="mt-4">
        <h2 className="flex items-center gap-2 text-sm font-bold text-sand-900">
          <FileText size={15} className="text-primary-600" />
          Patient's records ({reports.length})
        </h2>
        <div className="mt-3 space-y-2">
          {reports.length === 0 && (
            <p className="text-sm text-sand-500">No documents available for this patient.</p>
          )}
          {reports.map((r) => (
            <div key={r.report_id}
              className="flex items-center justify-between rounded-xl border border-sand-100 px-3 py-2">
              <div className="min-w-0">
                <div className="truncate text-sm font-semibold text-sand-800">{r.report_name}</div>
                <div className="text-xs text-sand-500">{r.report_type} • {r.upload_date}</div>
              </div>
              <button type="button"
                onClick={() => recordService.download(r.report_id, r.report_name)}
                className="shrink-0 text-xs font-bold text-primary-600 hover:text-primary-700">
                Open
              </button>
            </div>
          ))}
        </div>
      </Card>

      <form onSubmit={submit}>
        <Card className="mt-6">
          <h2 className="text-lg font-bold text-sand-900">Your evaluation</h2>
          <div className="mt-5 space-y-4">

            <Field label="Diagnosis under review">
              <textarea required rows={2} className={textarea}
                value={form.original_diagnosis} onChange={set('original_diagnosis')}
                placeholder="What the patient was originally told, in their treating doctor's words" />
            </Field>

            <Field label="Clinical findings">
              <textarea required rows={4} className={textarea}
                value={form.findings} onChange={set('findings')}
                placeholder="What the records show, and what you concluded from them" />
            </Field>

            {/* The verdict is the answer being paid for, so it is an explicit
                choice with no default. A pre-selected "agree" would be a
                clinical judgement the specialist never made. */}
            <Field label="Do you agree with the original diagnosis?">
              <div className="flex flex-wrap gap-3">
                <button type="button"
                  onClick={() => setForm((f) => ({ ...f, agrees_with_original: true }))}
                  className={`inline-flex items-center gap-2 rounded-xl border px-4 py-2.5 text-sm font-bold transition ${
                    form.agrees_with_original === true
                      ? 'border-success-500 bg-success-50 text-success-700'
                      : 'border-sand-200 text-sand-600 hover:border-success-300'}`}>
                  <CheckCircle2 size={16} /> I agree
                </button>
                <button type="button"
                  onClick={() => setForm((f) => ({ ...f, agrees_with_original: false }))}
                  className={`inline-flex items-center gap-2 rounded-xl border px-4 py-2.5 text-sm font-bold transition ${
                    form.agrees_with_original === false
                      ? 'border-warning-500 bg-warning-50 text-warning-700'
                      : 'border-sand-200 text-sand-600 hover:border-warning-300'}`}>
                  <AlertTriangle size={16} /> I differ
                </button>
              </div>
            </Field>

            <Field label="Recommendation">
              <textarea required rows={4} className={textarea}
                value={form.recommendation} onChange={set('recommendation')}
                placeholder="What you advise the patient to do next. 'No change to current treatment' is a complete answer." />
            </Field>

            <Field label="Suggested investigations (optional)">
              <textarea rows={2} className={textarea}
                value={form.suggested_tests} onChange={set('suggested_tests')}
                placeholder="Any further tests or scans you would want" />
            </Field>
          </div>
        </Card>

        <div className="mt-6 flex justify-end gap-3">
          <button type="button" onClick={() => navigate('/doctor/appointments')}
            className="rounded-xl border border-sand-300 px-5 py-2.5 text-sm font-semibold text-sand-700 hover:bg-sand-50">
            Cancel
          </button>
          <Button type="submit" disabled={saving}
            variant={saving ? 'disabled' : 'primary'} className="px-6 py-2.5">
            <ClipboardCheck size={16} /> {saving ? 'Issuing…' : 'Issue opinion'}
          </Button>
        </div>
      </form>
    </DashboardLayout>
  )
}
