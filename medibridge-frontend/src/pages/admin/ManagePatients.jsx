import { useEffect, useState } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { Search, Ban, RotateCcw } from 'lucide-react'
import DashboardLayout from '../../components/layout/DashboardLayout'
import Card from '../../components/common/Card'
import Badge from '../../components/common/Badge'
import Input from '../../components/common/Input'
import { adminNav } from './adminNav'
import { fetchAdminPatients } from '../../features/admin/adminSlice'
import { adminService } from '../../services/adminService'

export default function ManagePatients() {
  const dispatch = useDispatch()
  const patients = useSelector((s) => s.admin.patients)
  const [q, setQ] = useState('')
  const [status, setStatus] = useState('All Status')   // filter, not the action
  const [busy, setBusy] = useState(null)
  const [msg, setMsg] = useState(null)

  useEffect(() => { dispatch(fetchAdminPatients()) }, [dispatch])

  const notify = (text, error = false) => {
    setMsg({ text, error })
    setTimeout(() => setMsg(null), 5000)
  }

  const changeStatus = async (patient, next, confirmText) => {
    if (confirmText && !window.confirm(confirmText)) return

    setBusy(patient.patient_id)
    try {
      await adminService.setPatientStatus(patient.patient_id, next)
      dispatch(fetchAdminPatients())
      notify(`${patient.full_name} is now ${next}.`)
    } catch (err) {
      notify(err?.response?.data?.message || 'Could not update this patient.', true)
    } finally {
      setBusy(null)
    }
  }

  const rows = patients.filter((p) => {
    const mq = `${p.full_name} ${p.email} ${p.phone}`.toLowerCase().includes(q.toLowerCase())
    const ms = status === 'All Status' || p.status === status.toLowerCase()
    return mq && ms
  })

  return (
    <DashboardLayout badge="Admin" navItems={adminNav}>
      {/* No "Add Patient": patients self-register, and an admin creating an
          account would mean setting someone else's password. */}
      <div>
        <span className="eyebrow">Members</span>
        <h1 className="mt-1 text-display-sm text-sand-900">Manage patients</h1>
        <p className="mt-1 text-sand-500">{patients.length} registered patients</p>
      </div>

      <Card className="mt-6 flex flex-col gap-3 sm:flex-row">
        <div className="flex-1"><Input icon={Search} placeholder="Search patients by name, email, or phone..." value={q} onChange={(e) => setQ(e.target.value)} /></div>
        <select className="rounded-xl border border-sand-300 px-4 py-2.5 text-sm" value={status} onChange={(e) => setStatus(e.target.value)}>
          <option>All Status</option><option>Active</option><option>Inactive</option>
        </select>
      </Card>

      {msg && (
        <div className={`mt-5 rounded-xl px-4 py-2.5 text-sm ${
          msg.error ? 'bg-danger-50 text-danger-600' : 'bg-success-50 text-success-700'}`}>{msg.text}</div>
      )}

      <Card className="mt-5 overflow-x-auto p-0">
        <table className="w-full text-left text-sm">
          <thead>
            <tr className="border-b border-sand-100 text-sand-500">
              {['Name', 'Email', 'Phone', 'Join Date', 'Appointments', 'Status', 'Actions'].map((h) => <th key={h} className="px-6 py-4 font-semibold">{h}</th>)}
            </tr>
          </thead>
          <tbody>
            {rows.map((p) => (
              <tr key={p.patient_id} className="border-b border-sand-50 last:border-0">
                <td className="px-6 py-4 font-semibold text-sand-800">{p.full_name}</td>
                <td className="px-6 py-4 text-sand-600">{p.email}</td>
                <td className="px-6 py-4 text-sand-600">{p.phone}</td>
                <td className="px-6 py-4 text-sand-600">{p.join_date}</td>
                <td className="px-6 py-4 text-sand-600">{p.appointments}</td>
                <td className="px-6 py-4"><Badge status={p.status} /></td>
                <td className="px-6 py-4">
                  {/* Deactivate rather than delete: appointments, prescriptions
                      and payments reference this patient, and medical records
                      must not vanish because an account was closed. */}
                  {p.status === 'active' ? (
                    <button disabled={busy === p.patient_id}
                      onClick={() => changeStatus(p,'inactive',
                        `Deactivate ${p.full_name}?\n\nThey will be unable to sign in `
                        + `or book. Their records are retained.`)}
                      className="flex items-center gap-1.5 rounded-xl border border-danger-200 px-3 py-1.5 text-xs font-semibold text-danger-600 hover:bg-danger-50 disabled:opacity-50">
                      <Ban size={14} /> Deactivate
                    </button>
                  ) : (
                    <button disabled={busy === p.patient_id}
                      onClick={() => changeStatus(p,'active', null)}
                      className="flex items-center gap-1.5 rounded-xl border border-sand-300 px-3 py-1.5 text-xs font-semibold text-sand-700 hover:bg-sand-50 disabled:opacity-50">
                      <RotateCcw size={14} /> Reactivate
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </Card>
    </DashboardLayout>
  )
}
