import { useEffect, useState } from 'react'
import { Clock, Sun, Sunset, CalendarCheck } from 'lucide-react'
import DashboardLayout from '../../components/layout/DashboardLayout'
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

  const activeDays = days.filter((d) => d.available).length

  return (
    <DashboardLayout badge="Doctor" navItems={doctorNav}>
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <span className="eyebrow">Availability</span>
          <h1 className="mt-1 text-display-sm text-sand-900">Manage schedule</h1>
          <p className="mt-1 text-sand-500">
            Toggle the windows you consult in. Saving publishes bookable slots for the next 30 days.
          </p>
        </div>
        <Button onClick={save} disabled={saving} variant={saving ? 'disabled' : 'primary'} className="px-5">
          {saving ? 'Saving…' : 'Save changes'}
        </Button>
      </div>

      {msg && (
        <div className={`mt-5 rounded-xl px-4 py-2.5 text-sm ${
          msg.error ? 'bg-danger-50 text-danger-600' : 'bg-success-50 text-success-700'}`}>{msg.text}</div>
      )}

      {days.length > 0 && (
        <div className="mt-5 inline-flex items-center gap-2 rounded-full bg-primary-50 px-4 py-2 text-sm font-semibold text-primary-700">
          <CalendarCheck size={16} /> Consulting {activeDays} day{activeDays === 1 ? '' : 's'} a week
        </div>
      )}

      <div className="mt-5 space-y-3">
        {days.map((d, i) => (
          <div key={d.day}
            className={`surface p-5 transition ${d.available ? '' : 'opacity-70'}`}>
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <span className={`flex h-9 w-9 items-center justify-center rounded-xl text-sm font-bold ${
                  d.available ? 'bg-primary-600 text-white' : 'bg-sand-200 text-sand-500'}`}>
                  {d.day.slice(0, 2)}
                </span>
                <div className="font-extrabold text-sand-900">{d.day}</div>
              </div>
              <label className="flex items-center gap-2 text-sm font-semibold text-sand-600">
                <input type="checkbox" checked={!!d.available} onChange={() => update(i, 'available')}
                  className="h-4 w-4 rounded accent-primary-600" /> Available
              </label>
            </div>
            <div className="mt-4 grid gap-3 sm:grid-cols-2">
              <div className={`flex items-center justify-between rounded-xl border px-4 py-3 ${
                d.morning && d.available ? 'border-primary-200 bg-primary-50/50' : 'border-sand-100 bg-sand-25'}`}>
                <span className="flex items-center gap-2 text-sm font-medium text-sand-600">
                  <Sun size={16} className="text-warning-500" /> 09:00 AM – 12:00 PM
                </span>
                <Toggle on={!!d.morning} onClick={() => update(i, 'morning')} />
              </div>
              <div className={`flex items-center justify-between rounded-xl border px-4 py-3 ${
                d.afternoon && d.available ? 'border-primary-200 bg-primary-50/50' : 'border-sand-100 bg-sand-25'}`}>
                <span className="flex items-center gap-2 text-sm font-medium text-sand-600">
                  <Sunset size={16} className="text-accent-500" /> 02:00 PM – 05:00 PM
                </span>
                <Toggle on={!!d.afternoon} onClick={() => update(i, 'afternoon')} />
              </div>
            </div>
          </div>
        ))}
        {days.length === 0 && !msg && (
          <div className="flex items-center gap-2 text-sm text-sand-500">
            <Clock size={16} className="text-sand-300" /> Loading schedule…
          </div>
        )}
      </div>
    </DashboardLayout>
  )
}
