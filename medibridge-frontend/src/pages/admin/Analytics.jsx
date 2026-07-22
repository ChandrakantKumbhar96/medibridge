import { useEffect } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { Download } from 'lucide-react'
import DashboardLayout from '../../components/layout/DashboardLayout'
import Card from '../../components/common/Card'
import { adminNav } from './adminNav'
import { fetchAdminAnalytics } from '../../features/admin/adminSlice'

export default function Analytics() {
  const dispatch = useDispatch()
  const analytics = useSelector((s) => s.admin.analytics)
  useEffect(() => { dispatch(fetchAdminAnalytics()) }, [dispatch])
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
          <button className="flex items-center justify-center gap-2 rounded-lg bg-primary-600 py-3 text-sm font-semibold text-white hover:bg-primary-700"><Download size={16} /> Patient Report</button>
          <button className="flex items-center justify-center gap-2 rounded-lg bg-green-600 py-3 text-sm font-semibold text-white hover:bg-green-700"><Download size={16} /> Doctor Performance</button>
          <button className="flex items-center justify-center gap-2 rounded-lg bg-purple-600 py-3 text-sm font-semibold text-white hover:bg-purple-700"><Download size={16} /> Revenue Report</button>
        </div>
      </Card>
    </DashboardLayout>
  )
}
