import { useEffect, useState } from 'react'
import { Clock } from 'lucide-react'
import DashboardLayout from '../../components/layout/DashboardLayout'
import Card from '../../components/common/Card'
import Button from '../../components/common/Button'
import { doctorNav } from './doctorNav'
import { doctorProfileService } from '../../services/profileService'

function Toggle({ on, onClick }) {
  return <button type="button" className="toggle" data-on={on} onClick={onClick}><span /></button>
}

export default function ManageSchedule() {
  const [days, setDays] = useState([])
  const [saving, setSaving] = useState(false)
  const [msg, setMsg] = useState(null)

  useEffect(() => {
    doctorProfileService.getSchedule()
      .then(setDays)
      .catch(() => setMsg({ error: true, text: 'Could not load your schedule.' }))
  }, [])

  const update = (i, key) =>
    setDays((p) => p.map((d, idx) => (idx === i ? { ...d, [key]: !d[key] } : d)))

  // Saving regenerates bookable slots for the next 30 days from this pattern,
  // which is why patients can see availability immediately afterwards.
  const save = async () => {
    setMsg(null)
    setSaving(true)
    try {
      setDays(await doctorProfileService.updateSchedule(days))
      setMsg({ text: 'Schedule saved. Appointment slots have been updated.' })
    } catch (err) {
      setMsg({ error: true, text: err?.response?.data?.message || 'Could not save schedule.' })
    } finally {
      setSaving(false)
    }
  }

  return (
    <DashboardLayout badge="Doctor" navItems={doctorNav}>
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-extrabold text-slate-900">Manage Schedule</h1>
          <p className="mt-1 text-slate-500">Set your availability for appointments</p>
        </div>
        <Button onClick={save} disabled={saving} variant={saving ? 'disabled' : 'primary'} className="px-5">
          {saving ? 'Saving…' : 'Save Changes'}
        </Button>
      </div>

      {msg && (
        <div className={`mt-5 rounded-lg px-4 py-2.5 text-sm ${
          msg.error ? 'bg-red-50 text-red-600' : 'bg-green-50 text-green-700'}`}>{msg.text}</div>
      )}

      <div className="mt-6 space-y-4">
        {days.map((d, i) => (
          <Card key={d.day}>
            <div className="flex items-center justify-between">
              <div className="font-bold text-slate-900">{d.day}</div>
              <label className="flex items-center gap-2 text-sm font-medium text-slate-600">
                <input type="checkbox" checked={!!d.available} onChange={() => update(i, 'available')}
                  className="h-4 w-4 rounded accent-purple-600" /> Available
              </label>
            </div>
            <div className="mt-4 grid gap-4 sm:grid-cols-2">
              <div className="flex items-center justify-between rounded-lg bg-slate-50 px-4 py-3">
                <span className="flex items-center gap-2 text-sm text-slate-600">
                  <Clock size={16} className="text-slate-400" /> 09:00 AM - 12:00 PM
                </span>
                <Toggle on={!!d.morning} onClick={() => update(i, 'morning')} />
              </div>
              <div className="flex items-center justify-between rounded-lg bg-slate-50 px-4 py-3">
                <span className="flex items-center gap-2 text-sm text-slate-600">
                  <Clock size={16} className="text-slate-400" /> 02:00 PM - 05:00 PM
                </span>
                <Toggle on={!!d.afternoon} onClick={() => update(i, 'afternoon')} />
              </div>
            </div>
          </Card>
        ))}
        {days.length === 0 && !msg && (
          <div className="text-sm text-slate-500">Loading schedule…</div>
        )}
      </div>
    </DashboardLayout>
  )
}
