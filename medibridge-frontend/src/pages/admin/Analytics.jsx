import { useEffect, useState } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { Download } from 'lucide-react'
import DashboardLayout from '../../components/layout/DashboardLayout'
import Card from '../../components/common/Card'
import { adminNav } from './adminNav'
import { fetchAdminAnalytics } from '../../features/admin/adminSlice'
import { adminService } from '../../services/adminService'
import { exportCsv, datedFilename } from '../../utils/exportCsv'

export default function Analytics() {
  const dispatch = useDispatch()
  const analytics = useSelector((s) => s.admin.analytics)
  const [exportMsg, setExportMsg] = useState(null)

  useEffect(() => { dispatch(fetchAdminAnalytics()) }, [dispatch])

  const note = (text) => {
    setExportMsg(text)
    setTimeout(() => setExportMsg(null), 4000)
  }

  // Reports pull fresh rows rather than reusing whatever the dashboard happens
  // to hold, so an export is never a stale snapshot.
  const exportPatients = async () => {
    const rows = await adminService.getPatients()
    const ok = exportCsv(datedFilename('patients'), rows, [
      { key: 'patient_id', label: 'ID' },
      { key: 'full_name', label: 'Name' },
      { key: 'email', label: 'Email' },
      { key: 'phone', label: 'Phone' },
      { key: 'join_date', label: 'Joined' },
      { key: 'appointments', label: 'Appointments' },
      { key: 'status', label: 'Status' },
    ])
    note(ok ? 'Patient report downloaded.' : 'No patients to export.')
  }

  const exportDoctors = async () => {
    const rows = await adminService.getDoctors()
    const ok = exportCsv(datedFilename('doctor-performance'), rows, [
      { key: 'full_name', label: 'Doctor' },
      { key: 'specialization', label: 'Specialty' },
      { key: 'license_number', label: 'Licence' },
      { key: 'patients', label: 'Patients Seen' },
      { key: 'rating', label: 'Rating' },
      { key: 'status', label: 'Status' },
    ])
    note(ok ? 'Doctor performance report downloaded.' : 'No doctors to export.')
  }

  const exportRevenue = () => {
    const daily = analytics?.dailyRevenue || {}
    const rows = Object.entries(daily).map(([date, amount]) => ({ date, amount }))
    const ok = exportCsv(datedFilename('revenue'), rows, [
      { key: 'date', label: 'Date' },
      { key: 'amount', label: 'Revenue (INR)' },
    ])
    note(ok ? 'Revenue report downloaded.' : 'No revenue data to export.')
  }

  if (!analytics) return <DashboardLayout badge="Admin" navItems={adminNav}><div className="p-10 text-slate-400">Loading...</div></DashboardLayout>

  const { monthly, revenue } = analytics
  const row = (label, value, accent) => (
    <div className="flex items-center justify-between py-2.5">
      <span className="text-slate-500">{label}</span>
      <span className={`font-bold ${accent || 'text-slate-900'}`}>{value}</span>
    </div>
  )

  return (
    <DashboardLayout badge="Admin" navItems={adminNav}>
      <h1 className="text-3xl font-extrabold text-slate-900">Analytics &amp; Reports</h1>

      <div className="mt-6 grid gap-6 md:grid-cols-2">
        <Card>
          <h2 className="text-lg font-bold text-slate-900">Monthly Statistics</h2>
          <div className="mt-4 divide-y divide-slate-100">
            {row('New Patients', monthly.newPatients)}
            {row('New Doctors', monthly.newDoctors)}
            {row('Total Appointments', monthly.totalAppointments.toLocaleString())}
            {row('Completion Rate', monthly.completionRate, 'text-green-600')}
          </div>
        </Card>
        <Card>
          <h2 className="text-lg font-bold text-slate-900">Revenue Breakdown</h2>
          <div className="mt-4 divide-y divide-slate-100">
            {row('Consultations', `$${revenue.consultations.toLocaleString()}`)}
            {row('Follow-ups', `$${revenue.followUps.toLocaleString()}`)}
            {row('New Patients', `$${revenue.newPatients.toLocaleString()}`)}
            {row('Total Revenue', `$${revenue.total.toLocaleString()}`, 'text-primary-600')}
          </div>
        </Card>
      </div>

      <Card className="mt-6">
        <h2 className="text-lg font-bold text-slate-900">Generate Reports</h2>
        <div className="mt-4 grid gap-3 sm:grid-cols-3">
          <button onClick={exportPatients}
            className="flex items-center justify-center gap-2 rounded-lg bg-primary-600 py-3 text-sm font-semibold text-white hover:bg-primary-700">
            <Download size={16} /> Patient Report
          </button>
          <button onClick={exportDoctors}
            className="flex items-center justify-center gap-2 rounded-lg bg-green-600 py-3 text-sm font-semibold text-white hover:bg-green-700">
            <Download size={16} /> Doctor Performance
          </button>
          <button onClick={exportRevenue}
            className="flex items-center justify-center gap-2 rounded-lg bg-purple-600 py-3 text-sm font-semibold text-white hover:bg-purple-700">
            <Download size={16} /> Revenue Report
          </button>
        </div>
        {exportMsg && (
          <div className="mt-3 text-sm text-slate-500">{exportMsg}</div>
        )}
      </Card>
    </DashboardLayout>
  )
}
