import { useEffect, useState } from 'react'
import { Search, Eye, Edit } from 'lucide-react'
import DashboardLayout from '../../components/layout/DashboardLayout'
import Card from '../../components/common/Card'
import Avatar from '../../components/common/Avatar'
import { doctorNav } from './doctorNav'
import { doctorProfileService } from '../../services/profileService'

export default function PatientRecordsDoctor() {
  const [q, setQ] = useState('')
  const [patients, setPatients] = useState([])
  const [error, setError] = useState(null)

  // Returns only patients this doctor has actually treated - the backend builds
  // the list from appointment history, not from the patient table.
  useEffect(() => {
    doctorProfileService.getPatients()
      .then(setPatients)
      .catch(() => setError('Could not load patient records.'))
  }, [])

  const rows = patients.filter((r) => (r.name || '').toLowerCase().includes(q.toLowerCase()))

  return (
    <DashboardLayout badge="Doctor" navItems={doctorNav}>
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h1 className="text-3xl font-extrabold text-slate-900">Patient Records</h1>
        <div className="relative w-72">
          <Search className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={18} />
          <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Search patients..."
            className="w-full rounded-lg border border-slate-300 py-2.5 pl-10 pr-3 text-sm" />
        </div>
      </div>

      <Card className="mt-6 overflow-x-auto p-0">
        <table className="w-full text-left text-sm">
          <thead>
            <tr className="border-b border-slate-100 text-slate-500">
              <th className="px-6 py-4 font-semibold">Patient Name</th>
              <th className="px-6 py-4 font-semibold">Age</th>
              <th className="px-6 py-4 font-semibold">Last Visit</th>
              <th className="px-6 py-4 font-semibold">Condition</th>
              <th className="px-6 py-4 font-semibold">Next Appointment</th>
              <th className="px-6 py-4 font-semibold">Actions</th>
            </tr>
          </thead>
          <tbody>
            {rows.length === 0 && (
              <tr>
                <td colSpan={6} className="px-6 py-8 text-center text-sm text-slate-500">
                  {error || 'No patients yet. Patients appear here once they book with you.'}
                </td>
              </tr>
            )}
            {rows.map((r) => (
              <tr key={r.patient_id} className="border-b border-slate-50 last:border-0">
                <td className="px-6 py-4">
                  <div className="flex items-center gap-3">
                    <Avatar size={34} />
                    <span className="font-semibold text-slate-800">{r.name}</span>
                  </div>
                </td>
                <td className="px-6 py-4 text-slate-600">{r.age}</td>
                <td className="px-6 py-4 text-slate-600">{r.last_visit}</td>
                <td className="px-6 py-4"><span className="rounded-md bg-blue-50 px-2.5 py-1 text-xs font-medium text-primary-600">{r.condition}</span></td>
                <td className="px-6 py-4 text-slate-600">{r.next}</td>
                <td className="px-6 py-4">
                  <div className="flex items-center gap-3">
                    <button className="text-primary-600 hover:text-primary-700"><Eye size={18} /></button>
                    <button className="text-green-600 hover:text-green-700"><Edit size={18} /></button>
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
