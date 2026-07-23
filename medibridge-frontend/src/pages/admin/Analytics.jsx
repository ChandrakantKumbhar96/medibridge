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

  // Defaulted rather than destructured bare: a field the API stops sending
  // would otherwise crash the page on .toLocaleString() of undefined, which is
  // exactly what happened when the revenue shape changed.
  const monthly = analytics.monthly || {}
  const revenue = analytics.revenue || {}
  const daily = analytics.dailyRevenue || {}

  const money = (n) => `₹${Number(n ?? 0).toLocaleString('en-IN')}`
  const num = (n) => Number(n ?? 0).toLocaleString('en-IN')

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
            {row('Total Patients', num(monthly.newPatients))}
            {row('Total Doctors', num(monthly.newDoctors))}
            {row('Total Appointments', num(monthly.totalAppointments))}
            {row('Cancelled', num(monthly.cancelledAppointments), 'text-red-600')}
            {row('Completion Rate', monthly.completionRate ?? '0%', 'text-green-600')}
          </div>
        </Card>
        <Card>
          <h2 className="text-lg font-bold text-slate-900">Revenue Breakdown</h2>
          <div className="mt-4 divide-y divide-slate-100">
            {row('Collected', money(revenue.total))}
            {row('Refunded', `− ${money(revenue.refunded)}`, 'text-red-600')}
            {row('Net revenue', money(revenue.net), 'text-primary-600')}
            {row('This month', money(revenue.monthToDate))}
          </div>
        </Card>
      </div>

      <Card className="mt-6">
        <h2 className="text-lg font-bold text-slate-900">Revenue — last 7 days</h2>
        <div className="mt-4 space-y-2">
          {Object.entries(daily).map(([day, amount]) => {
            const max = Math.max(...Object.values(daily).map(Number), 1)
            const pct = (Number(amount) / max) * 100
            return (
              <div key={day} className="flex items-center gap-3 text-sm">
                <span className="w-24 shrink-0 text-slate-500">{day}</span>
                <div className="h-2 flex-1 overflow-hidden rounded-full bg-slate-100">
                  <div className="h-full rounded-full bg-primary-600"
                    style={{ width: `${pct}%` }} />
                </div>
                <span className="w-24 shrink-0 text-right font-medium text-slate-700">
                  {money(amount)}
                </span>
              </div>
            )
          })}
          {Object.keys(daily).length === 0 && (
            <div className="text-sm text-slate-500">No revenue data yet.</div>
          )}
        </div>
      </Card>

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
