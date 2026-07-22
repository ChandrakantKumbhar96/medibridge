import { useEffect, useRef, useState } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { FileText, Download, Upload, Trash2, FileDown, Pill } from 'lucide-react'
import DashboardLayout from '../../components/layout/DashboardLayout'
import Card from '../../components/common/Card'
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
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-3xl font-extrabold text-slate-900">Medical Records</h1>
          <p className="mt-1 text-slate-500">Manage your medical documents and reports</p>
        </div>
        <div className="flex items-center gap-3">
          <button onClick={downloadHistory} disabled={busy === 'history'}
            className="flex items-center gap-2 rounded-lg border border-slate-300 px-4 py-2.5 text-sm font-semibold text-slate-700 hover:bg-slate-50 disabled:opacity-60">
            <FileDown size={16} /> {busy === 'history' ? 'Generating…' : 'Full History PDF'}
          </button>
          <input ref={fileInput} type="file" onChange={upload} className="hidden"
            accept="application/pdf,image/jpeg,image/png,image/webp" />
          <button onClick={() => fileInput.current?.click()} disabled={busy === 'upload'}
            className="flex items-center gap-2 rounded-lg bg-primary-600 px-5 py-2.5 text-sm font-semibold text-white hover:bg-primary-700 disabled:opacity-60">
            <Upload size={16} /> {busy === 'upload' ? 'Uploading…' : 'Upload New'}
          </button>
        </div>
      </div>

      {msg && (
        <div className={`mt-5 rounded-lg px-4 py-2.5 text-sm ${
          msg.error ? 'bg-red-50 text-red-600' : 'bg-green-50 text-green-700'}`}>{msg.text}</div>
      )}

      <Card className="mt-6 space-y-3">
        {records.length === 0 && (
          <div className="py-6 text-center text-sm text-slate-500">
            No documents yet. Upload a report to get started.
          </div>
        )}
        {records.map((r) => (
          <div key={r.report_id} className="flex items-center justify-between rounded-xl border border-slate-100 p-4">
            <div className="flex items-center gap-3">
              <div className="flex h-11 w-11 items-center justify-center rounded-lg bg-blue-100 text-primary-600"><FileText size={20} /></div>
              <div>
                <div className="font-semibold text-slate-800">{r.report_name}</div>
                <div className="text-sm text-slate-500">{r.report_type} • {r.upload_date} • {r.size}</div>
              </div>
            </div>
            <div className="flex items-center gap-4">
              <button onClick={() => download(r)} disabled={busy === `d-${r.report_id}`}
                title="Download" className="text-green-600 hover:text-green-700 disabled:opacity-50">
                <Download size={18} />
              </button>
              <button onClick={() => remove(r)} disabled={busy === `x-${r.report_id}`}
                title="Delete" className="text-slate-400 hover:text-red-600 disabled:opacity-50">
                <Trash2 size={18} />
              </button>
            </div>
          </div>
        ))}
      </Card>

      {prescriptions.length > 0 && (
        <>
          <h2 className="mt-8 text-xl font-bold text-slate-900">Prescriptions</h2>
          <Card className="mt-4 space-y-3">
            {prescriptions.map((p) => (
              <div key={p.prescription_id} className="flex items-center justify-between rounded-xl border border-slate-100 p-4">
                <div className="flex items-center gap-3">
                  <div className="flex h-11 w-11 items-center justify-center rounded-lg bg-purple-100 text-purple-600"><Pill size={20} /></div>
                  <div>
                    <div className="font-semibold text-slate-800">{p.diagnosis}</div>
                    <div className="text-sm text-slate-500">
                      {p.doctor_name} • {p.date_issued} • {p.medicines?.length || 0} medicines
                    </div>
                  </div>
                </div>
                <button onClick={() => downloadPrescription(p.prescription_id)}
                  disabled={busy === `p-${p.prescription_id}`}
                  className="flex items-center gap-2 rounded-lg border border-slate-300 px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50 disabled:opacity-60">
                  <Download size={16} /> {busy === `p-${p.prescription_id}` ? 'Generating…' : 'PDF'}
                </button>
              </div>
            ))}
          </Card>
        </>
      )}
    </DashboardLayout>
  )
}
