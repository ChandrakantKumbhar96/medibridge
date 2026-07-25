import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { useDispatch, useSelector } from 'react-redux'
import { FileText, Download, Upload, Trash2, FileDown, Pill, FolderOpen, Activity } from 'lucide-react'
import DashboardLayout from '../../components/layout/DashboardLayout'
import { patientNav } from './patientNav'
import { fetchRecords } from '../../features/records/recordsSlice'
import { recordService } from '../../services/recordService'
import { prescriptionService } from '../../services/prescriptionService'

export default function MedicalRecords() {
  const dispatch = useDispatch()
  const records = useSelector((s) => s.records.list)
  const fileInput = useRef(null)

  const [prescriptions, setPrescriptions] = useState([])
  const [busy, setBusy] = useState(null)
  const [msg, setMsg] = useState(null)

  useEffect(() => {
    dispatch(fetchRecords())
    prescriptionService.getMyPrescriptions().then(setPrescriptions).catch(() => setPrescriptions([]))
  }, [dispatch])

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
      })
      dispatch(fetchRecords())
      notify('Document uploaded.')
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
      dispatch(fetchRecords())
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
        <h2 className="text-lg font-extrabold text-sand-900">Documents</h2>
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
    </DashboardLayout>
  )
}
