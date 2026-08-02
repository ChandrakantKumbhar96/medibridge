import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { useDispatch, useSelector } from 'react-redux'
import { FileText, Download, Upload, Trash2, FileDown, Pill, FolderOpen, Activity, ClipboardCheck, Users } from 'lucide-react'
import DashboardLayout from '../../components/layout/DashboardLayout'
import { patientNav } from './patientNav'
import { fetchRecords } from '../../features/records/recordsSlice'
import { fetchFamily } from '../../features/family/familySlice'
import { recordService } from '../../services/recordService'
import { prescriptionService } from '../../services/prescriptionService'
import { opinionService } from '../../services/opinionService'

export default function MedicalRecords() {
  const dispatch = useDispatch()
  const records = useSelector((s) => s.records.list)
  const family = useSelector((s) => s.family.list)
  const fileInput = useRef(null)

  // 'all' | 'self' | a family_member_id. Doubles as the upload target, so the
  // file lands on whoever's tab is open rather than always on the account
  // holder - filing a child's report under the parent is the mistake this
  // feature exists to prevent.
  const [subject, setSubject] = useState('all')

  const [prescriptions, setPrescriptions] = useState([])
  const [opinions, setOpinions] = useState([])
  const [busy, setBusy] = useState(null)
  const [msg, setMsg] = useState(null)

  useEffect(() => {
    dispatch(fetchFamily())
    prescriptionService.getMyPrescriptions().then(setPrescriptions).catch(() => setPrescriptions([]))
    opinionService.getMyOpinions().then(setOpinions).catch(() => setOpinions([]))
  }, [dispatch])

  useEffect(() => {
    dispatch(fetchRecords(subject === 'all' ? undefined : subject))
  }, [dispatch, subject])

  /** Which person a new upload belongs to; undefined files it under the holder. */
  const uploadTarget = subject === 'all' || subject === 'self' ? undefined : subject

  const nameOf = (id) =>
    family.find((m) => String(m.family_member_id) === String(id))?.full_name || 'this profile'

  const notify = (text, error = false) => {
    setMsg({ text, error })
    setTimeout(() => setMsg(null), 4000)
  }

  const upload = async (e) => {
    const file = e.target.files?.[0]
    if (!file) return

    setBusy('upload')
    try {
      await recordService.uploadRecord({
        file,
        // Strip the extension: the server generates its own stored filename,
        // this is only the human-readable label.
        report_name: file.name.replace(/\.[^.]+$/, ''),
        report_type: file.type === 'application/pdf' ? 'Lab Report' : 'Imaging',
        family_member_id: uploadTarget,
      })
      dispatch(fetchRecords(subject === 'all' ? undefined : subject))
      notify(uploadTarget
        ? `Document uploaded to ${nameOf(uploadTarget)}'s records.`
        : 'Document uploaded.')
    } catch (err) {
      notify(err?.response?.data?.message || 'Upload failed.', true)
    } finally {
      setBusy(null)
      e.target.value = ''   // allow re-selecting the same file
    }
  }

  const download = async (r) => {
    setBusy(`d-${r.report_id}`)
    try {
      await recordService.download(r.report_id, r.report_name)
    } catch (err) {
      notify(err?.response?.data?.message || 'Could not download this file.', true)
    } finally {
      setBusy(null)
    }
  }

  const remove = async (r) => {
    setBusy(`x-${r.report_id}`)
    try {
      await recordService.deleteRecord(r.report_id)
      dispatch(fetchRecords(subject === 'all' ? undefined : subject))
      notify('Document deleted.')
    } catch (err) {
      notify(err?.response?.data?.message || 'Could not delete this file.', true)
    } finally {
      setBusy(null)
    }
  }

  const downloadHistory = async () => {
    setBusy('history')
    try {
      await recordService.downloadMedicalHistoryPdf()
    } catch (err) {
      notify(err?.response?.data?.message || 'Could not generate the report.', true)
    } finally {
      setBusy(null)
    }
  }

  const downloadPrescription = async (id) => {
    setBusy(`p-${id}`)
    try {
      await prescriptionService.downloadPdf(id)
    } catch (err) {
      notify(err?.response?.data?.message || 'Could not generate the prescription.', true)
    } finally {
      setBusy(null)
    }
  }

  const downloadOpinion = async (id) => {
    setBusy(`o-${id}`)
    try {
      await opinionService.downloadPdf(id)
    } catch (err) {
      notify(err?.response?.data?.message || 'Could not generate the opinion.', true)
    } finally {
      setBusy(null)
    }
  }

  return (
    <DashboardLayout navItems={patientNav}>
      {/* ---- Header ---- */}
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <span className="eyebrow">Your documents</span>
          <h1 className="mt-1 text-display-sm text-sand-900">Medical records</h1>
          <p className="mt-1 text-sand-500">Reports and prescriptions only you and your doctor can open.</p>
        </div>
        <div className="flex items-center gap-3">
          <Link to="/patient/timeline"
            className="inline-flex items-center gap-2 rounded-full border border-sand-300 px-4 py-2.5 text-sm font-bold text-sand-700 transition hover:bg-sand-50">
            <Activity size={16} /> Timeline
          </Link>
          <button onClick={downloadHistory} disabled={busy === 'history'}
            className="inline-flex items-center gap-2 rounded-full border border-sand-300 px-4 py-2.5 text-sm font-bold text-sand-700 transition hover:bg-sand-50 disabled:opacity-60">
            <FileDown size={16} /> {busy === 'history' ? 'Generating…' : 'Full history PDF'}
          </button>
          <input ref={fileInput} type="file" onChange={upload} className="hidden"
            accept="application/pdf,image/jpeg,image/png,image/webp" />
          <button onClick={() => fileInput.current?.click()} disabled={busy === 'upload'}
            className="inline-flex items-center gap-2 rounded-full bg-primary-600 px-5 py-2.5 text-sm font-bold text-white shadow-soft transition hover:bg-primary-700 disabled:opacity-60">
            <Upload size={16} /> {busy === 'upload' ? 'Uploading…' : 'Upload new'}
          </button>
        </div>
      </div>

      {msg && (
        <div className={`mt-5 rounded-xl px-4 py-2.5 text-sm ${
          msg.error ? 'bg-danger-50 text-danger-600' : 'bg-success-50 text-success-700'}`}>{msg.text}</div>
      )}

      {/* ---- Documents ---- */}
      <div className="surface mt-6 p-6">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-lg font-extrabold text-sand-900">Documents</h2>
          {family.length > 0 && (
            <Link to="/patient/family"
              className="inline-flex items-center gap-1.5 text-sm font-bold text-primary-600 hover:text-primary-700">
              <Users size={15} /> Manage family
            </Link>
          )}
        </div>

        {/* Only worth the row once there is more than one person on the account -
            a lone "Everyone / Me" pair is two chips that always agree. */}
        {family.length > 0 && (
          <div className="mt-4 flex flex-wrap gap-2">
            {[{ key: 'all', label: 'Everyone' }, { key: 'self', label: 'Me' },
              ...family.map((m) => ({ key: String(m.family_member_id), label: m.full_name }))
            ].map((chip) => (
              <button key={chip.key} onClick={() => setSubject(chip.key)}
                className={`rounded-full px-4 py-1.5 text-sm font-bold transition ${
                  subject === chip.key
                    ? 'bg-primary-600 text-white shadow-soft'
                    : 'border border-sand-200 text-sand-600 hover:bg-sand-50'}`}>
                {chip.label}
              </button>
            ))}
          </div>
        )}

        <div className="mt-4 grid gap-3 sm:grid-cols-2">
          {records.length === 0 && (
            <div className="col-span-full flex flex-col items-center gap-2 rounded-2xl border border-dashed border-sand-200 py-10 text-center">
              <FolderOpen size={26} className="text-sand-300" />
              <p className="text-sm text-sand-500">No documents yet — upload a report to get started.</p>
            </div>
          )}
          {records.map((r) => (
            <div key={r.report_id}
              className="flex items-center justify-between gap-3 rounded-2xl border border-sand-100 p-4 transition hover:border-primary-200">
              <div className="flex min-w-0 items-center gap-3">
                <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-primary-50 text-primary-600">
                  <FileText size={20} />
                </div>
                <div className="min-w-0">
                  <div className="truncate font-bold text-sand-800">{r.report_name}</div>
                  <div className="text-xs text-sand-500">{r.report_type} • {r.upload_date} • {r.size}</div>
                  {/* Shown only for a dependent's document. The server omits the
                      field entirely on the holder's own, so its absence - not an
                      id comparison - is what means "mine". */}
                  {r.family_member_id && (
                    <div className="mt-1 inline-flex items-center gap-1 rounded-full bg-accent-50 px-2 py-0.5 text-[11px] font-bold text-accent-700">
                      {r.patient_name}{r.relation ? ` • ${r.relation}` : ''}
                    </div>
                  )}
                </div>
              </div>
              <div className="flex shrink-0 items-center gap-1">
                <button onClick={() => download(r)} disabled={busy === `d-${r.report_id}`}
                  title="Download"
                  className="flex h-9 w-9 items-center justify-center rounded-xl text-primary-600 transition hover:bg-primary-50 disabled:opacity-50">
                  <Download size={17} />
                </button>
                <button onClick={() => remove(r)} disabled={busy === `x-${r.report_id}`}
                  title="Delete"
                  className="flex h-9 w-9 items-center justify-center rounded-xl text-sand-400 transition hover:bg-danger-50 hover:text-danger-600 disabled:opacity-50">
                  <Trash2 size={17} />
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* ---- Prescriptions ---- */}
      {prescriptions.length > 0 && (
        <div className="surface mt-6 p-6">
          <h2 className="text-lg font-extrabold text-sand-900">Prescriptions</h2>
          <div className="mt-4 grid gap-3 sm:grid-cols-2">
            {prescriptions.map((p) => (
              <div key={p.prescription_id}
                className="flex items-center justify-between gap-3 rounded-2xl border border-sand-100 p-4 transition hover:border-accent-200">
                <div className="flex min-w-0 items-center gap-3">
                  <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-accent-50 text-accent-600">
                    <Pill size={20} />
                  </div>
                  <div className="min-w-0">
                    <div className="truncate font-bold text-sand-800">{p.diagnosis}</div>
                    <div className="text-xs text-sand-500">
                      {p.doctor_name} • {p.date_issued} • {p.medicines?.length || 0} medicines
                    </div>
                  </div>
                </div>
                <button onClick={() => downloadPrescription(p.prescription_id)}
                  disabled={busy === `p-${p.prescription_id}`}
                  className="inline-flex shrink-0 items-center gap-2 rounded-full border border-sand-300 px-4 py-2 text-sm font-bold text-sand-700 transition hover:bg-sand-50 disabled:opacity-60">
                  <Download size={15} /> {busy === `p-${p.prescription_id}` ? 'Generating…' : 'PDF'}
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* ---- Second opinions ----
          Kept apart from Prescriptions on purpose: this is a judgement on
          another doctor's diagnosis, and filing it beside a medicine list would
          bury the one line the patient came back for — the verdict. */}
      {opinions.length > 0 && (
        <div className="surface mt-6 p-6">
          <h2 className="text-lg font-extrabold text-sand-900">Second opinions</h2>
          <div className="mt-4 space-y-3">
            {opinions.map((o) => (
              <div key={o.opinion_id}
                className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-sand-100 p-4 transition hover:border-primary-200">
                <div className="flex min-w-0 items-center gap-3">
                  <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-primary-50 text-primary-600">
                    <ClipboardCheck size={20} />
                  </div>
                  <div className="min-w-0">
                    <div className="truncate font-bold text-sand-800">
                      {o.doctor} • {o.specialization}
                    </div>
                    <div className="text-xs text-sand-500">{o.issued_on}</div>
                    <div className={`mt-1.5 inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-bold ${
                      o.agrees_with_original
                        ? 'bg-success-50 text-success-700'
                        : 'bg-warning-50 text-warning-700'}`}>
                      {o.verdict}
                    </div>
                  </div>
                </div>
                <button onClick={() => downloadOpinion(o.opinion_id)}
                  disabled={busy === `o-${o.opinion_id}`}
                  className="inline-flex shrink-0 items-center gap-2 rounded-full border border-sand-300 px-4 py-2 text-sm font-bold text-sand-700 transition hover:bg-sand-50 disabled:opacity-60">
                  <Download size={15} /> {busy === `o-${o.opinion_id}` ? 'Generating…' : 'PDF'}
                </button>
              </div>
            ))}
          </div>
        </div>
      )}
    </DashboardLayout>
  )
}
