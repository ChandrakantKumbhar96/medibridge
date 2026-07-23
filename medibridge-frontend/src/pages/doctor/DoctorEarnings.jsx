import { useEffect, useState } from 'react'
import { Wallet, Clock, TrendingUp, Percent, Download } from 'lucide-react'
import DashboardLayout from '../../components/layout/DashboardLayout'
import Card from '../../components/common/Card'
import Badge from '../../components/common/Badge'
import { doctorNav } from './doctorNav'
import { earningsService } from '../../services/payoutService'
import { exportCsv, datedFilename } from '../../utils/exportCsv'

const money = (n) => `₹${Number(n ?? 0).toLocaleString('en-IN')}`

export default function DoctorEarnings() {
  const [summary, setSummary] = useState(null)
  const [earnings, setEarnings] = useState([])
  const [payouts, setPayouts] = useState([])
  const [tab, setTab] = useState('earnings')
  const [error, setError] = useState(null)

  useEffect(() => {
    Promise.all([
      earningsService.getSummary(),
      earningsService.getEarnings(),
      earningsService.getPayouts(),
    ])
      .then(([s, e, p]) => { setSummary(s); setEarnings(e); setPayouts(p) })
      .catch(() => setError('Could not load your earnings.'))
  }, [])

  const stats = summary ? [
    { icon: Clock, label: 'Awaiting payout', value: money(summary.pendingAmount),
      sub: `${summary.pendingConsultations} consultation(s)`,
      grad: 'from-amber-400 to-amber-500' },
    { icon: Wallet, label: 'Paid out', value: money(summary.paidAmount),
      sub: 'settled to date', grad: 'from-green-500 to-green-600' },
    { icon: TrendingUp, label: 'Lifetime earnings', value: money(summary.lifetimeEarnings),
      sub: 'after commission', grad: 'from-blue-500 to-blue-600' },
    { icon: Percent, label: 'Platform commission', value: `${summary.commissionRate}%`,
      sub: `paid every ${summary.payoutCycleDays} days`, grad: 'from-purple-500 to-purple-600' },
  ] : []

  return (
    <DashboardLayout badge="Doctor" navItems={doctorNav}>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-3xl font-extrabold text-slate-900">Earnings</h1>
          <p className="mt-1 text-slate-500">
            Your consultation income and settlement history
          </p>
        </div>
        {earnings.length > 0 && (
          <button
            onClick={() => exportCsv(datedFilename('my-earnings'), earnings, [
              { key: 'consultation_date', label: 'Date' },
              { key: 'patient', label: 'Patient' },
              { key: 'gross_amount', label: 'Consultation Fee' },
              { key: 'commission_amount', label: 'Commission' },
              { key: 'net_amount', label: 'You Earned' },
              { key: 'status', label: 'Status' },
            ])}
            className="flex items-center gap-2 rounded-lg border border-slate-300 px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50">
            <Download size={15} /> Export
          </button>
        )}
      </div>

      {error && (
        <div className="mt-5 rounded-lg bg-red-50 px-4 py-2.5 text-sm text-red-600">{error}</div>
      )}

      <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {stats.map((s) => (
          <div key={s.label}
            className={`rounded-2xl bg-gradient-to-br ${s.grad} p-5 text-white shadow-sm`}>
            <s.icon size={22} className="opacity-90" />
            <div className="mt-5 text-2xl font-extrabold">{s.value}</div>
            <div className="mt-1 text-sm text-white/90">{s.label}</div>
            <div className="text-xs text-white/70">{s.sub}</div>
          </div>
        ))}
      </div>

      {/* How the split works - a doctor seeing a smaller number than the
          patient paid deserves an explanation, not a support ticket. */}
      {summary && (
        <Card className="mt-6 bg-slate-50">
          <div className="text-sm text-slate-600">
            <span className="font-semibold text-slate-800">How your earnings are calculated: </span>
            the patient pays your consultation fee plus a small platform fee.
            MediBridge retains {summary.commissionRate}% of the consultation fee as
            commission; the rest is yours. The platform fee is charged to the
            patient and is not deducted from you.
          </div>
        </Card>
      )}

      <div className="mt-6 flex gap-6 border-b border-slate-200">
        {[['earnings', 'Consultations'], ['payouts', 'Payouts']].map(([key, label]) => (
          <button key={key} onClick={() => setTab(key)}
            className={`-mb-px border-b-2 pb-2.5 text-sm font-semibold transition ${
              tab === key ? 'border-primary-600 text-primary-600'
                          : 'border-transparent text-slate-400'}`}>
            {label}
          </button>
        ))}
      </div>

      {tab === 'earnings' && (
        <Card className="mt-5 overflow-x-auto p-0">
          <table className="w-full text-left text-sm">
            <thead>
              <tr className="border-b border-slate-100 text-slate-500">
                {['Date', 'Patient', 'Fee', 'Commission', 'You earned', 'Status']
                  .map((h) => <th key={h} className="px-6 py-4 font-semibold">{h}</th>)}
              </tr>
            </thead>
            <tbody>
              {earnings.length === 0 && (
                <tr><td colSpan={6} className="px-6 py-8 text-center text-slate-500">
                  No earnings yet. Complete a consultation to start earning.
                </td></tr>
              )}
              {earnings.map((e) => (
                <tr key={e.earning_id} className="border-b border-slate-50 last:border-0">
                  <td className="px-6 py-4 text-slate-600">{e.consultation_date}</td>
                  <td className="px-6 py-4 font-medium text-slate-800">{e.patient}</td>
                  <td className="px-6 py-4 text-slate-600">{money(e.gross_amount)}</td>
                  <td className="px-6 py-4 text-slate-500">
                    −{money(e.commission_amount)}
                    <span className="ml-1 text-xs text-slate-400">
                      ({e.commission_rate}%)
                    </span>
                  </td>
                  <td className="px-6 py-4 font-bold text-slate-900">{money(e.net_amount)}</td>
                  <td className="px-6 py-4">
                    <span className={`rounded-full px-2.5 py-1 text-xs font-medium ${
                      e.status === 'SETTLED' ? 'bg-green-100 text-green-700'
                        : e.status === 'REVERSED' ? 'bg-red-100 text-red-600'
                        : 'bg-yellow-100 text-yellow-700'}`}>
                      {e.status === 'SETTLED' ? 'Paid'
                        : e.status === 'REVERSED' ? 'Reversed' : 'Pending'}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </Card>
      )}

      {tab === 'payouts' && (
        <Card className="mt-5 overflow-x-auto p-0">
          <table className="w-full text-left text-sm">
            <thead>
              <tr className="border-b border-slate-100 text-slate-500">
                {['Period', 'Consultations', 'Gross', 'Commission', 'Net paid',
                  'Status', 'Reference']
                  .map((h) => <th key={h} className="px-6 py-4 font-semibold">{h}</th>)}
              </tr>
            </thead>
            <tbody>
              {payouts.length === 0 && (
                <tr><td colSpan={7} className="px-6 py-8 text-center text-slate-500">
                  No payouts yet. Earnings are settled on a regular cycle.
                </td></tr>
              )}
              {payouts.map((p) => (
                <tr key={p.payout_id} className="border-b border-slate-50 last:border-0">
                  <td className="px-6 py-4 text-slate-600">
                    {p.period_start} → {p.period_end}
                  </td>
                  <td className="px-6 py-4 text-slate-600">{p.consultations}</td>
                  <td className="px-6 py-4 text-slate-600">{money(p.gross_amount)}</td>
                  <td className="px-6 py-4 text-slate-500">−{money(p.commission)}</td>
                  <td className="px-6 py-4 font-bold text-slate-900">{money(p.net_amount)}</td>
                  <td className="px-6 py-4">
                    <span className={`rounded-full px-2.5 py-1 text-xs font-medium ${
                      p.status === 'PAID' ? 'bg-green-100 text-green-700'
                        : 'bg-yellow-100 text-yellow-700'}`}>
                      {p.status === 'PAID' ? 'Paid' : 'Processing'}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-xs text-slate-500">
                    {p.payout_ref || '—'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </Card>
      )}
    </DashboardLayout>
  )
}
