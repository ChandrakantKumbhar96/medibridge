import { useEffect, useState } from 'react'
import { PlayCircle, CheckCircle2, Wallet, Percent, Clock, Download } from 'lucide-react'
import DashboardLayout from '../../components/layout/DashboardLayout'
import Card from '../../components/common/Card'
import Input, { Field } from '../../components/common/Input'
import Button from '../../components/common/Button'
import { adminNav } from './adminNav'
import { payoutAdminService } from '../../services/payoutService'
import { exportCsv, datedFilename } from '../../utils/exportCsv'

const money = (n) => `₹${Number(n ?? 0).toLocaleString('en-IN')}`
const today = () => new Date().toISOString().split('T')[0]
const daysAgo = (n) =>
  new Date(Date.now() - n * 86400000).toISOString().split('T')[0]

export default function Payouts() {
  const [summary, setSummary] = useState(null)
  const [payouts, setPayouts] = useState([])
  const [from, setFrom] = useState(daysAgo(30))
  const [to, setTo] = useState(today())
  const [busy, setBusy] = useState(false)
  const [msg, setMsg] = useState(null)
  const [paying, setPaying] = useState(null)
  const [reference, setReference] = useState('')

  const load = () => Promise.all([
    payoutAdminService.getSummary(),
    payoutAdminService.getPayouts(),
  ]).then(([s, p]) => { setSummary(s); setPayouts(p) })
    .catch(() => notify('Could not load payout data.', true))

  useEffect(() => { load() }, [])

  const notify = (text, error = false) => {
    setMsg({ text, error })
    setTimeout(() => setMsg(null), 6000)
  }

  /**
   * Creating a batch is separate from recording the transfer. A batch says
   * "this is owed"; marking it paid says "the money left". Collapsing the two
   * would have the database claim a transfer that may never have happened.
   */
  const runSettlement = async () => {
    setBusy(true)
    try {
      const created = await payoutAdminService.runSettlement(from, to)
      await load()
      notify(created.length === 0
        ? 'Nothing to settle — all earnings in this period are already paid out.'
        : `Created ${created.length} payout batch(es).`)
    } catch (err) {
      notify(err?.response?.data?.message || 'Settlement run failed.', true)
    } finally {
      setBusy(false)
    }
  }

  const confirmPaid = async (payout) => {
    if (!reference.trim()) {
      notify('Enter the bank or UPI reference for this transfer.', true)
      return
    }
    try {
      await payoutAdminService.markPaid(payout.payout_id, reference.trim(), null)
      setPaying(null)
      setReference('')
      await load()
      notify(`Payout #${payout.payout_id} marked paid.`)
    } catch (err) {
      notify(err?.response?.data?.message || 'Could not mark this payout paid.', true)
    }
  }

  const stats = summary ? [
    { icon: Percent, label: 'Platform commission earned',
      value: money(summary.totalCommission), grad: 'from-accent-500 to-accent-600' },
    { icon: Clock, label: 'Owed to doctors',
      value: money(summary.pendingPayouts), grad: 'from-warning-400 to-warning-500' },
    { icon: Wallet, label: 'Paid to doctors',
      value: money(summary.paidPayouts), grad: 'from-success-500 to-success-600' },
  ] : []

  return (
    <DashboardLayout badge="Admin" navItems={adminNav}>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-3xl font-extrabold text-sand-900">Doctor Payouts</h1>
          <p className="mt-1 text-sand-500">
            Settle consultation earnings and record transfers
          </p>
        </div>
        {payouts.length > 0 && (
          <button
            onClick={() => exportCsv(datedFilename('payouts'), payouts, [
              { key: 'payout_id', label: 'Payout ID' },
              { key: 'doctor', label: 'Doctor' },
              { key: 'period_start', label: 'From' },
              { key: 'period_end', label: 'To' },
              { key: 'consultations', label: 'Consultations' },
              { key: 'gross_amount', label: 'Gross' },
              { key: 'commission', label: 'Commission' },
              { key: 'net_amount', label: 'Net Paid' },
              { key: 'status', label: 'Status' },
              { key: 'payout_ref', label: 'Reference' },
            ])}
            className="flex items-center gap-2 rounded-lg border border-sand-300 px-4 py-2 text-sm font-semibold text-sand-700 hover:bg-sand-50">
            <Download size={15} /> Export
          </button>
        )}
      </div>

      {msg && (
        <div className={`mt-5 rounded-lg px-4 py-2.5 text-sm ${
          msg.error ? 'bg-danger-50 text-danger-600' : 'bg-success-50 text-success-700'}`}>{msg.text}</div>
      )}

      <div className="mt-6 grid gap-4 sm:grid-cols-3">
        {stats.map((s) => (
          <div key={s.label}
            className={`rounded-2xl bg-gradient-to-br ${s.grad} p-5 text-white shadow-sm`}>
            <s.icon size={22} className="opacity-90" />
            <div className="mt-5 text-2xl font-extrabold">{s.value}</div>
            <div className="mt-1 text-sm text-white/90">{s.label}</div>
          </div>
        ))}
      </div>

      <Card className="mt-6">
        <h2 className="text-lg font-bold text-sand-900">Run settlement</h2>
        <p className="mt-1 text-sm text-sand-500">
          Creates a payout batch for every doctor with unsettled earnings in this
          period. Safe to re-run — a period already settled is skipped.
        </p>
        <div className="mt-4 flex flex-wrap items-end gap-4">
          <div className="w-44">
            <Field label="From">
              <Input type="date" value={from} onChange={(e) => setFrom(e.target.value)} />
            </Field>
          </div>
          <div className="w-44">
            <Field label="To">
              <Input type="date" value={to} onChange={(e) => setTo(e.target.value)} />
            </Field>
          </div>
          <Button onClick={runSettlement} disabled={busy}
            variant={busy ? 'disabled' : 'primary'} className="px-5 py-2.5">
            <PlayCircle size={16} /> {busy ? 'Running…' : 'Run settlement'}
          </Button>
        </div>
      </Card>

      <Card className="mt-6 overflow-x-auto p-0">
        <table className="w-full text-left text-sm">
          <thead>
            <tr className="border-b border-sand-100 text-sand-500">
              {['#', 'Doctor', 'Period', 'Consults', 'Gross', 'Commission',
                'Net payable', 'Status', 'Action']
                .map((h) => <th key={h} className="px-5 py-4 font-semibold">{h}</th>)}
            </tr>
          </thead>
          <tbody>
            {payouts.length === 0 && (
              <tr><td colSpan={9} className="px-6 py-8 text-center text-sand-500">
                No payouts yet. Run a settlement to create the first batch.
              </td></tr>
            )}
            {payouts.map((p) => (
              <tr key={p.payout_id} className="border-b border-sand-50 last:border-0">
                <td className="px-5 py-4 text-sand-500">#{p.payout_id}</td>
                <td className="px-5 py-4 font-semibold text-sand-800">{p.doctor}</td>
                <td className="px-5 py-4 text-xs text-sand-500">
                  {p.period_start}<br />→ {p.period_end}
                </td>
                <td className="px-5 py-4 text-sand-600">{p.consultations}</td>
                <td className="px-5 py-4 text-sand-600">{money(p.gross_amount)}</td>
                <td className="px-5 py-4 text-accent-600">+{money(p.commission)}</td>
                <td className="px-5 py-4 font-bold text-sand-900">{money(p.net_amount)}</td>
                <td className="px-5 py-4">
                  <span className={`rounded-full px-2.5 py-1 text-xs font-medium ${
                    p.status === 'PAID' ? 'bg-success-100 text-success-700'
                      : 'bg-warning-100 text-warning-700'}`}>
                    {p.status === 'PAID' ? 'Paid' : 'Pending'}
                  </span>
                </td>
                <td className="px-5 py-4">
                  {p.status === 'PAID' ? (
                    <span className="text-xs text-sand-500">{p.payout_ref}</span>
                  ) : paying === p.payout_id ? (
                    <div className="flex items-center gap-2">
                      <input value={reference} onChange={(e) => setReference(e.target.value)}
                        placeholder="NEFT / UPI ref"
                        className="w-32 rounded-lg border border-sand-300 px-2 py-1.5 text-xs" />
                      <button onClick={() => confirmPaid(p)}
                        className="rounded-lg bg-success-600 px-2.5 py-1.5 text-xs font-semibold text-white hover:bg-success-700">
                        Save
                      </button>
                      <button onClick={() => { setPaying(null); setReference('') }}
                        className="text-xs text-sand-500 hover:text-sand-700">
                        Cancel
                      </button>
                    </div>
                  ) : (
                    <button onClick={() => setPaying(p.payout_id)}
                      className="flex items-center gap-1.5 rounded-lg border border-success-200 px-3 py-1.5 text-xs font-semibold text-success-700 hover:bg-success-50">
                      <CheckCircle2 size={14} /> Mark paid
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </Card>
    </DashboardLayout>
  )
}
