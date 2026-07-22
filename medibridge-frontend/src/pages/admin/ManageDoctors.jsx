import { useEffect, useState } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { Search, CheckCircle2, Ban, RotateCcw, Star } from 'lucide-react'
import DashboardLayout from '../../components/layout/DashboardLayout'
import Card from '../../components/common/Card'
import Badge from '../../components/common/Badge'
import Input from '../../components/common/Input'
import { adminNav } from './adminNav'
import { fetchAdminDoctors } from '../../features/admin/adminSlice'
import { adminService } from '../../services/adminService'

export default function ManageDoctors() {
  const dispatch = useDispatch()
  const doctors = useSelector((s) => s.admin.doctors)
  const [q, setQ] = useState('')
  const [busy, setBusy] = useState(null)
  const [msg, setMsg] = useState(null)

  useEffect(() => { dispatch(fetchAdminDoctors()) }, [dispatch])

  const notify = (text, error = false) => {
    setMsg({ text, error })
    setTimeout(() => setMsg(null), 5000)
  }

  /**
   * A doctor self-registers as 'pending' and cannot log in until approved -
   * the platform is asserting that their medical registration was checked.
   */
  const setStatus = async (doctor, status, confirmText) => {
    if (confirmText && !window.confirm(confirmText)) return

    setBusy(doctor.doctor_id)
    try {
      await adminService.setDoctorStatus(doctor.doctor_id, status)
      dispatch(fetchAdminDoctors())
      notify(`${doctor.full_name} is now ${status}.`)
    } catch (err) {
      notify(err?.response?.data?.message || 'Could not update this doctor.', true)
    } finally {
      setBusy(null)
    }
  }

  const rows = doctors.filter((d) =>
    `${d.full_name} ${d.specialization} ${d.license_number}`
      .toLowerCase().includes(q.toLowerCase()))

  const pendingCount = doctors.filter((d) => d.status === 'pending').length

  return (
    <DashboardLayout badge="Admin" navItems={adminNav}>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-3xl font-extrabold text-slate-900">Manage Doctors</h1>
          <p className="mt-1 text-slate-500">
            Verify medical registration before approving an account
          </p>
        </div>
        {pendingCount > 0 && (
          <span className="rounded-full bg-yellow-100 px-4 py-2 text-sm font-semibold text-yellow-800">
            {pendingCount} awaiting approval
          </span>
        )}
      </div>

      {msg && (
        <div className={`mt-5 rounded-lg px-4 py-2.5 text-sm ${
          msg.error ? 'bg-red-50 text-red-600' : 'bg-green-50 text-green-700'}`}>{msg.text}</div>
      )}

      <Card className="mt-6">
        <Input icon={Search} placeholder="Search doctors by name, specialty, or license..."
          value={q} onChange={(e) => setQ(e.target.value)} />
      </Card>

      <Card className="mt-5 overflow-x-auto p-0">
        <table className="w-full text-left text-sm">
          <thead>
            <tr className="border-b border-slate-100 text-slate-500">
              {['Name', 'Email', 'Specialty', 'License', 'Rating', 'Patients', 'Status', 'Actions']
                .map((h) => <th key={h} className="px-6 py-4 font-semibold">{h}</th>)}
            </tr>
          </thead>
          <tbody>
            {rows.length === 0 && (
              <tr><td colSpan={8} className="px-6 py-8 text-center text-slate-500">
                No doctors found.
              </td></tr>
            )}
            {rows.map((d) => (
              <tr key={d.doctor_id} className="border-b border-slate-50 last:border-0">
                <td className="px-6 py-4 font-semibold text-slate-800">{d.full_name}</td>
                <td className="px-6 py-4 text-slate-600">{d.email}</td>
                <td className="px-6 py-4 text-slate-600">{d.specialization}</td>
                <td className="px-6 py-4 text-slate-600">{d.license_number}</td>
                <td className="px-6 py-4 text-slate-600">
                  {Number(d.rating) > 0
                    ? <span className="flex items-center gap-1 text-amber-500">
                        <Star size={13} fill="currentColor" /> {d.rating}
                      </span>
                    : <span className="text-slate-400">—</span>}
                </td>
                <td className="px-6 py-4 text-slate-600">{d.patients}</td>
                <td className="px-6 py-4"><Badge status={d.status} /></td>
                <td className="px-6 py-4">
                  <div className="flex items-center gap-2">
                    {d.status === 'pending' && (
                      <button disabled={busy === d.doctor_id}
                        onClick={() => setStatus(d, 'active',
                          `Approve ${d.full_name}?\n\nConfirm their licence `
                          + `(${d.license_number}) has been verified. They will be `
                          + `able to sign in and accept bookings.`)}
                        className="flex items-center gap-1.5 rounded-lg bg-green-600 px-3 py-1.5 text-xs font-semibold text-white hover:bg-green-700 disabled:opacity-50">
                        <CheckCircle2 size={14} /> Approve
                      </button>
                    )}
                    {d.status === 'active' && (
                      <button disabled={busy === d.doctor_id}
                        onClick={() => setStatus(d, 'suspended',
                          `Suspend ${d.full_name}?\n\nThey will be unable to sign in `
                          + `and will stop appearing in search. Existing appointments `
                          + `are not cancelled automatically.`)}
                        className="flex items-center gap-1.5 rounded-lg border border-red-200 px-3 py-1.5 text-xs font-semibold text-red-600 hover:bg-red-50 disabled:opacity-50">
                        <Ban size={14} /> Suspend
                      </button>
                    )}
                    {(d.status === 'suspended' || d.status === 'inactive') && (
                      <button disabled={busy === d.doctor_id}
                        onClick={() => setStatus(d, 'active', null)}
                        className="flex items-center gap-1.5 rounded-lg border border-slate-300 px-3 py-1.5 text-xs font-semibold text-slate-700 hover:bg-slate-50 disabled:opacity-50">
                        <RotateCcw size={14} /> Reinstate
                      </button>
                    )}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </Card>
    </DashboardLayout>
  )
}
