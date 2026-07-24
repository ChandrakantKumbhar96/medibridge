import { useEffect, useState } from 'react'
import { Search, Eye, X, Download, FileText } from 'lucide-react'
import DashboardLayout from '../../components/layout/DashboardLayout'
import Card from '../../components/common/Card'
import Avatar from '../../components/common/Avatar'
import { doctorNav } from './doctorNav'
import { doctorProfileService } from '../../services/profileService'
import { recordService } from '../../services/recordService'
import axiosClient from '../../api/axiosClient'

export default function PatientRecordsDoctor() {
  const [q, setQ] = useState('')
  const [patients, setPatients] = useState([])
  const [error, setError] = useState(null)
  const [viewing, setViewing] = useState(null)
  const [records, setRecords] = useState([])
  const [loadingRecords, setLoadingRecords] = useState(false)

  /**
   * Loads the selected patient's uploaded documents. The backend refuses this
   * unless the doctor has actually treated them, so a doctor cannot browse a
   * stranger's records by guessing an id.
   */
  useEffect(() => {
    if (!viewing) return

    setLoadingRecords(true)
    setRecords([])
    axiosClient.get(`/doctor/patients/${viewing.patient_id}/records`)
      .then(({ data }) => setRecords(data))
      .catch(() => setRecords([]))
      .finally(() => setLoadingRecords(false))
  }, [viewing])

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
        <h1 className="text-3xl font-extrabold text-sand-900">Patient Records</h1>
        <div className="relative w-72">
          <Search className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-sand-400" size={18} />
          <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Search patients..."
            className="w-full rounded-lg border border-sand-300 py-2.5 pl-10 pr-3 text-sm" />
        </div>
      </div>

      <Card className="mt-6 overflow-x-auto p-0">
        <table className="w-full text-left text-sm">
          <thead>
            <tr className="border-b border-sand-100 text-sand-500">
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
                <td colSpan={6} className="px-6 py-8 text-center text-sm text-sand-500">
                  {error || 'No patients yet. Patients appear here once they book with you.'}
                </td>
              </tr>
            )}
            {rows.map((r) => (
              <tr key={r.patient_id} className="border-b border-sand-50 last:border-0">
                <td className="px-6 py-4">
                  <div className="flex items-center gap-3">
                    <Avatar size={34} />
                    <span className="font-semibold text-sand-800">{r.name}</span>
                  </div>
                </td>
                <td className="px-6 py-4 text-sand-600">{r.age}</td>
                <td className="px-6 py-4 text-sand-600">{r.last_visit}</td>
                <td className="px-6 py-4"><span className="rounded-md bg-primary-50 px-2.5 py-1 text-xs font-medium text-primary-600">{r.condition}</span></td>
                <td className="px-6 py-4 text-sand-600">{r.next}</td>
                <td className="px-6 py-4">
                  <div className="flex items-center gap-3">
                    <button title="View uploaded records"
                      onClick={() => setViewing(r)}
                      className="text-primary-600 hover:text-primary-700">
                      <Eye size={18} />
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </Card>

      {viewing && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-sand-900/50 p-4"
          onClick={() => setViewing(null)}>
          <div className="w-full max-w-lg rounded-2xl bg-white p-6 shadow-xl"
            onClick={(e) => e.stopPropagation()}>
            <div className="flex items-start justify-between">
              <div>
                <h2 className="text-lg font-bold text-sand-900">{viewing.name}</h2>
                <p className="text-sm text-sand-500">
                  {viewing.age} yrs • {viewing.gender} • {viewing.blood_group}
                </p>
              </div>
              <button onClick={() => setViewing(null)}
                className="text-sand-400 hover:text-sand-600">
                <X size={20} />
              </button>
            </div>

            <div className="mt-4 rounded-lg bg-sand-50 p-3 text-sm">
              <div className="flex justify-between">
                <span className="text-sand-500">Last visit</span>
                <span className="font-medium text-sand-800">{viewing.last_visit}</span>
              </div>
              <div className="mt-1.5 flex justify-between">
                <span className="text-sand-500">Next appointment</span>
                <span className="font-medium text-sand-800">{viewing.next}</span>
              </div>
            </div>

            <h3 className="mt-5 text-sm font-semibold text-sand-700">Uploaded documents</h3>
            <div className="mt-3 max-h-64 space-y-2 overflow-y-auto">
              {loadingRecords && <div className="text-sm text-sand-500">Loading…</div>}
              {!loadingRecords && records.length === 0 && (
                <div className="text-sm text-sand-500">
                  This patient has not uploaded any documents.
                </div>
              )}
              {records.map((r) => (
                <div key={r.report_id}
                  className="flex items-center justify-between rounded-lg border border-sand-100 p-3">
                  <div className="flex items-center gap-2.5">
                    <FileText size={16} className="text-primary-600" />
                    <div>
                      <div className="text-sm font-medium text-sand-800">{r.report_name}</div>
                      <div className="text-xs text-sand-500">
                        {r.report_type} • {r.upload_date} • {r.size}
                      </div>
                    </div>
                  </div>
                  <button title="Download"
                    onClick={() => recordService.download(r.report_id, r.report_name)}
                    className="text-success-600 hover:text-success-700">
                    <Download size={16} />
                  </button>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}
    </DashboardLayout>
  )
}
