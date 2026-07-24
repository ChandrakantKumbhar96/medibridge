import DashboardTopbar from './DashboardTopbar'
import Sidebar from './Sidebar'

/**
 * Dashboard shell.
 *
 * The page sits on warm sand with a very faint grid texture rather than flat
 * sand-50 — enough to give white cards something to sit against without
 * competing with content.
 */
export default function DashboardLayout({ badge, navItems, children }) {
  return (
    <div className="relative min-h-screen bg-sand-50">
      {/* subtle background texture, held behind everything */}
      <div
        aria-hidden
        className="pointer-events-none fixed inset-0 bg-grid-sand bg-grid-sand opacity-[0.35]"
      />
      <div className="relative">
        <DashboardTopbar badge={badge} />
        <div className="mx-auto flex max-w-[1400px] gap-7 px-6 py-7">
          <Sidebar items={navItems} />
          <main className="min-w-0 flex-1 animate-fade-up">{children}</main>
        </div>
      </div>
    </div>
  )
}
