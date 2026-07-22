import { useEffect, useState } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { Download, Edit } from 'lucide-react'
import DashboardLayout from '../../components/layout/DashboardLayout'
import Card from '../../components/common/Card'
import Badge from '../../components/common/Badge'
import { adminNav } from './adminNav'
import { fetchAdminAppointments } from '../../features/admin/adminSlice'
import { exportCsv, datedFilename } from '../../utils/exportCsv'

export default function AdminAppointments() {
  const dispatch = useDispatch()
  const appts = useSelector((s) => s.admin.appointments)
  const [status, setStatus] = useState('All Status')
  const [patient, setPatient] = useState('')
  const [doctor, setDoctor] = useState('')
  useEffect(() => { dispatch(fetchAdminAppointments()) }, [dispatch])

  const rows = appts.filter((a) =>
    (status === 'All Status' || a.status === status.toLowerCase()) &&
    a.patient.toLowerCase().includes(patient.toLowerCase()) &&
    a.doctor.toLowerCase().includes(doctor.toLowerCase())
  )

  return (
    <DashboardLayout badge="Admin" navItems={adminNav}>
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-extrabold text-slate-900">All Appointments</h1>
        <button
          onClick={() => exportCsv(datedFilename('appointments'), rows, [
            { key: 'appointment_id', label: 'ID' },
            { key: 'patient', label: 'Patient' },
            { key: 'doctor', label: 'Doctor' },
            { key: 'date', label: 'Date' },
            { key: 'time', label: 'Time' },
            { key: 'type', label: 'Type' },
            { key: 'status', label: 'Status' },
          ])}
          className="flex items-center gap-2 rounded-lg bg-green-600 px-5 py-2.5 text-sm font-semibold text-white hover:bg-green-700">
          <Download size={16} /> Export Report
        </button>
      </div>

      <Card className="mt-6 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <select className="rounded-lg border border-slate-300 px-4 py-2.5 text-sm" value={status} onChange={(e) => setStatus(e.target.value)}>
          <option>All Status</option><option>Confirmed</option><option>Pending</option><option>Cancelled</option>
        </select>
        <input type="date" className="rounded-lg border border-slate-300 px-4 py-2.5 text-sm" />
        <input placeholder="Search patient..." className="rounded-lg border border-slate-300 px-4 py-2.5 text-sm" value={patient} onChange={(e) => setPatient(e.target.value)} />
        <input placeholder="Search doctor..." className="rounded-lg border border-slate-300 px-4 py-2.5 text-sm" value={doctor} onChange={(e) => setDoctor(e.target.value)} />
      </Card>

      <Card className="mt-5 overflow-x-auto p-0">
        <table className="w-full text-left text-sm">
          <thead>
            <tr className="border-b border-slate-100 text-slate-500">
              {['Patient', 'Doctor', 'Date', 'Time', 'Type', 'Status', 'Actions'].map((h) => <th key={h} className="px-6 py-4 font-semibold">{h}</th>)}
            </tr>
          </thead>
          <tbody>
            {rows.map((a) => (
              <tr key={a.appointment_id} className="border-b border-slate-50 last:border-0">
                <td className="px-6 py-4 font-semibold text-slate-800">{a.patient}</td>
                <td className="px-6 py-4 text-slate-600">{a.doctor}</td>
                <td className="px-6 py-4 text-slate-600">{a.date}</td>
                <td className="px-6 py-4 text-slate-600">{a.time}</td>
                <td className="px-6 py-4 text-slate-600">{a.type}</td>
                <td className="px-6 py-4"><Badge status={a.status} /></td>
                <td className="px-6 py-4"><button className="text-primary-600 hover:text-primary-700"><Edit size={17} /></button></td>
              </tr>
            ))}
          </tbody>
        </table>
      </Card>
    </DashboardLayout>
  )
}
