import { useEffect, useState } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { Search, Plus, Edit, UserCog } from 'lucide-react'
import DashboardLayout from '../../components/layout/DashboardLayout'
import Card from '../../components/common/Card'
import Badge from '../../components/common/Badge'
import Input from '../../components/common/Input'
import { adminNav } from './adminNav'
import { fetchAdminDoctors } from '../../features/admin/adminSlice'

export default function ManageDoctors() {
  const dispatch = useDispatch()
  const doctors = useSelector((s) => s.admin.doctors)
  const [q, setQ] = useState('')
  useEffect(() => { dispatch(fetchAdminDoctors()) }, [dispatch])

  const rows = doctors.filter((d) => `${d.full_name} ${d.specialization} ${d.license_number}`.toLowerCase().includes(q.toLowerCase()))

  return (
    <DashboardLayout badge="Admin" navItems={adminNav}>
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-extrabold text-slate-900">Manage Doctors</h1>
        <button className="flex items-center gap-2 rounded-lg bg-primary-600 px-5 py-2.5 text-sm font-semibold text-white hover:bg-primary-700"><Plus size={16} /> Add Doctor</button>
      </div>

      <Card className="mt-6">
        <Input icon={Search} placeholder="Search doctors by name, specialty, or license..." value={q} onChange={(e) => setQ(e.target.value)} />
      </Card>

      <Card className="mt-5 overflow-x-auto p-0">
        <table className="w-full text-left text-sm">
          <thead>
            <tr className="border-b border-slate-100 text-slate-500">
              {['Name', 'Email', 'Specialty', 'License', 'Patients', 'Status', 'Actions'].map((h) => <th key={h} className="px-6 py-4 font-semibold">{h}</th>)}
            </tr>
          </thead>
          <tbody>
            {rows.map((d) => (
              <tr key={d.doctor_id} className="border-b border-slate-50 last:border-0">
                <td className="px-6 py-4 font-semibold text-slate-800">{d.full_name}</td>
                <td className="px-6 py-4 text-slate-600">{d.email}</td>
                <td className="px-6 py-4 text-slate-600">{d.specialization}</td>
                <td className="px-6 py-4 text-slate-600">{d.license_number}</td>
                <td className="px-6 py-4 text-slate-600">{d.patients}</td>
                <td className="px-6 py-4"><Badge status={d.status} /></td>
                <td className="px-6 py-4">
                  <div className="flex items-center gap-3">
                    <button className="text-primary-600 hover:text-primary-700"><Edit size={17} /></button>
                    <button className="text-red-500 hover:text-red-600"><UserCog size={17} /></button>
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
