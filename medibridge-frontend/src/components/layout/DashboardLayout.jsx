import DashboardTopbar from './DashboardTopbar'
import Sidebar from './Sidebar'

export default function DashboardLayout({ badge, navItems, children }) {
  return (
    <div className="min-h-screen bg-slate-50">
      <DashboardTopbar badge={badge} />
      <div className="mx-auto flex max-w-7xl gap-6 px-6 py-6">
        <Sidebar items={navItems} />
        <main className="min-w-0 flex-1">{children}</main>
      </div>
    </div>
  )
}
