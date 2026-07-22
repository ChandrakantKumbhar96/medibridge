import { useEffect, useState } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { Search, Plus, Edit, Trash2 } from 'lucide-react'
import DashboardLayout from '../../components/layout/DashboardLayout'
import Card from '../../components/common/Card'
import Badge from '../../components/common/Badge'
import Input from '../../components/common/Input'
import { adminNav } from './adminNav'
import { fetchAdminPatients } from '../../features/admin/adminSlice'

export default function ManagePatients() {
  const dispatch = useDispatch()
  const patients = useSelector((s) => s.admin.patients)
  const [q, setQ] = useState('')
  const [status, setStatus] = useState('All Status')
  useEffect(() => { dispatch(fetchAdminPatients()) }, [dispatch])

  const rows = patients.filter((p) => {
    const mq = `${p.full_name} ${p.email} ${p.phone}`.toLowerCase().includes(q.toLowerCase())
    const ms = status === 'All Status' || p.status === status.toLowerCase()
    return mq && ms
  })

  return (
    <DashboardLayout badge="Admin" navItems={adminNav}>
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-extrabold text-slate-900">Manage Patients</h1>
        <button className="flex items-center gap-2 rounded-lg bg-primary-600 px-5 py-2.5 text-sm font-semibold text-white hover:bg-primary-700"><Plus size={16} /> Add Patient</button>
      </div>

      <Card className="mt-6 flex flex-col gap-3 sm:flex-row">
        <div className="flex-1"><Input icon={Search} placeholder="Search patients by name, email, or phone..." value={q} onChange={(e) => setQ(e.target.value)} /></div>
        <select className="rounded-lg border border-slate-300 px-4 py-2.5 text-sm" value={status} onChange={(e) => setStatus(e.target.value)}>
          <option>All Status</option><option>Active</option><option>Inactive</option>
        </select>
      </Card>

      <Card className="mt-5 overflow-x-auto p-0">
        <table className="w-full text-left text-sm">
          <thead>
            <tr className="border-b border-slate-100 text-slate-500">
              {['Name', 'Email', 'Phone', 'Join Date', 'Appointments', 'Status', 'Actions'].map((h) => <th key={h} className="px-6 py-4 font-semibold">{h}</th>)}
            </tr>
          </thead>
          <tbody>
            {rows.map((p) => (
              <tr key={p.patient_id} className="border-b border-slate-50 last:border-0">
                <td className="px-6 py-4 font-semibold text-slate-800">{p.full_name}</td>
                <td className="px-6 py-4 text-slate-600">{p.email}</td>
                <td className="px-6 py-4 text-slate-600">{p.phone}</td>
                <td className="px-6 py-4 text-slate-600">{p.join_date}</td>
                <td className="px-6 py-4 text-slate-600">{p.appointments}</td>
                <td className="px-6 py-4"><Badge status={p.status} /></td>
                <td className="px-6 py-4">
                  <div className="flex items-center gap-3">
                    <button className="text-primary-600 hover:text-primary-700"><Edit size={17} /></button>
                    <button className="text-red-500 hover:text-red-600"><Trash2 size={17} /></button>
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
