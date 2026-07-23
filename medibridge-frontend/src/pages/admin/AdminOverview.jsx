import { useEffect } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { Users, UserCheck, Calendar, CalendarDays, FileText, TrendingUp, UserPlus, UserX } from 'lucide-react'
import DashboardLayout from '../../components/layout/DashboardLayout'
import Card from '../../components/common/Card'
import { adminNav } from './adminNav'
import { fetchAdminDashboard } from '../../features/admin/adminSlice'

const activityIcon = { patient: { i: Users, c: 'bg-blue-100 text-primary-600' }, consult: { i: FileText, c: 'bg-green-100 text-green-600' }, doctor: { i: UserPlus, c: 'bg-purple-100 text-purple-600' }, cancel: { i: UserX, c: 'bg-red-100 text-red-500' } }

export default function AdminOverview() {
  const dispatch = useDispatch()
  const { stats, activity } = useSelector((s) => s.admin)
  useEffect(() => { dispatch(fetchAdminDashboard()) }, [dispatch])
  if (!stats) return <DashboardLayout badge="Admin" navItems={adminNav}><div className="p-10 text-slate-400">Loading...</div></DashboardLayout>

  // Coerced through Number() rather than calling .toLocaleString() on the raw
  // field: a value the API stops sending would otherwise crash the whole page.
  const num = (n) => Number(n ?? 0).toLocaleString('en-IN')
  const money = (n) => `₹${Number(n ?? 0).toLocaleString('en-IN')}`

  const bigStats = [
    { icon: Users, value: num(stats.totalPatients), label: 'Total Patients', grad: 'from-blue-500 to-blue-600' },
    { icon: UserCheck, value: num(stats.activeDoctors), label: 'Active Doctors', grad: 'from-green-500 to-green-600' },
    { icon: Calendar, value: num(stats.totalAppointments), label: 'Total Appointments', grad: 'from-purple-500 to-purple-600' },
  ]
  const small = [
    { label: 'Active Today', value: num(stats.activeToday), icon: CalendarDays, c: 'bg-amber-100 text-amber-500' },
    { label: 'Completed Today', value: num(stats.completedToday), icon: FileText, c: 'bg-green-100 text-green-600' },
    { label: 'Revenue (MTD)', value: money(stats.revenueMTD), icon: TrendingUp, c: 'bg-blue-100 text-primary-600' },
  ]

  return (
    <DashboardLayout badge="Admin" navItems={adminNav}>
      <div className="grid gap-4 md:grid-cols-3">
        {bigStats.map((s) => (
          <div key={s.label} className={`rounded-2xl bg-gradient-to-br ${s.grad} p-6 text-white shadow-sm`}>
            <s.icon size={26} className="opacity-90" />
            <div className="mt-6 text-4xl font-extrabold">{s.value}</div>
            <div className="mt-1 text-sm text-white/90">{s.label}</div>
          </div>
        ))}
      </div>

      <div className="mt-4 grid gap-4 md:grid-cols-3">
        {small.map((s) => (
          <Card key={s.label} className="flex items-center justify-between">
            <div>
              <div className="text-sm text-slate-500">{s.label}</div>
              <div className="mt-1 text-2xl font-extrabold text-slate-900">{s.value}</div>
            </div>
            <div className={`flex h-11 w-11 items-center justify-center rounded-lg ${s.c}`}><s.icon size={20} /></div>
          </Card>
        ))}
      </div>

      <Card className="mt-6">
        <h2 className="text-lg font-bold text-slate-900">Recent Activity</h2>
        <div className="mt-4 space-y-3">
          {activity.map((a) => {
            const { i: Icon, c } = activityIcon[a.type] || activityIcon.patient
            return (
              <div key={a.id} className="flex items-center gap-3 rounded-xl border border-slate-100 p-4">
                <div className={`flex h-10 w-10 items-center justify-center rounded-full ${c}`}><Icon size={18} /></div>
                <div>
                  <div className="font-semibold text-slate-800">{a.name}</div>
                  <div className="text-sm text-slate-500">{a.text}</div>
                  <div className="text-xs text-slate-400">{a.time}</div>
                </div>
              </div>
            )
          })}
        </div>
      </Card>
    </DashboardLayout>
  )
}
