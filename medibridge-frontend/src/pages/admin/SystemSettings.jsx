import { useEffect, useState } from 'react'
import DashboardLayout from '../../components/layout/DashboardLayout'
import Card from '../../components/common/Card'
import Input, { Field } from '../../components/common/Input'
import Button from '../../components/common/Button'
import { adminNav } from './adminNav'
import { settingsService } from '../../services/profileService'

const EMPTY = {
  platform_name: '',
  support_email: '',
  max_appointments_per_day: 50,
  two_factor_enabled: true,
  session_timeout_minutes: 30,
}

export default function SystemSettings() {
  const [settings, setSettings] = useState(EMPTY)
  const [msg, setMsg] = useState(null)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    settingsService.get()
      .then((s) => setSettings({ ...EMPTY, ...s }))
      .catch(() => setMsg({ error: true, text: 'Could not load system settings.' }))
  }, [])

  const set = (k, v) => setSettings((p) => ({ ...p, [k]: v }))

  // The backend stores settings as typed key/value rows and validates each
  // value against its declared type, so everything is sent as a string.
  const save = async (next = settings) => {
    setMsg(null)
    setSaving(true)
    try {
      const saved = await settingsService.update({
        platform_name: String(next.platform_name),
        support_email: String(next.support_email),
        max_appointments_per_day: String(next.max_appointments_per_day),
        two_factor_enabled: String(next.two_factor_enabled),
        session_timeout_minutes: String(next.session_timeout_minutes),
      })
      setSettings({ ...EMPTY, ...saved })
      setMsg({ text: 'Settings saved.' })
    } catch (err) {
      setMsg({ error: true, text: err?.response?.data?.message || 'Could not save settings.' })
    } finally {
      setSaving(false)
    }
  }

  return (
    <DashboardLayout badge="Admin" navItems={adminNav}>
      <h1 className="text-3xl font-extrabold text-slate-900">System Settings</h1>

      {msg && (
        <div className={`mt-5 rounded-lg px-4 py-2.5 text-sm ${
          msg.error ? 'bg-red-50 text-red-600' : 'bg-green-50 text-green-700'}`}>{msg.text}</div>
      )}

      <Card className="mt-6">
        <h2 className="text-lg font-bold text-slate-900">General Settings</h2>
        <div className="mt-5 max-w-2xl space-y-4">
          <Field label="Platform Name">
            <Input value={settings.platform_name}
              onChange={(e) => set('platform_name', e.target.value)} />
          </Field>
          <Field label="Support Email">
            <Input type="email" value={settings.support_email}
              onChange={(e) => set('support_email', e.target.value)} />
          </Field>
          <Field label="Maximum Appointments Per Day">
            <Input type="number" min="1" value={settings.max_appointments_per_day}
              onChange={(e) => set('max_appointments_per_day', e.target.value)} />
          </Field>
        </div>
        <Button onClick={() => save()} disabled={saving}
          variant={saving ? 'disabled' : 'primary'} className="mt-5">
          {saving ? 'Saving…' : 'Save Settings'}
        </Button>
      </Card>

      <Card className="mt-6">
        <h2 className="text-lg font-bold text-slate-900">Security Settings</h2>
        <div className="mt-5 space-y-3">
          <div className="flex items-center justify-between rounded-xl bg-slate-50 p-4">
            <div>
              <div className="font-semibold text-slate-800">Two-Factor Authentication</div>
              <div className="text-sm text-slate-500">Require 2FA for admin accounts</div>
            </div>
            {/* Security toggles save immediately - leaving one visibly "on"
                but unsaved would misrepresent the system's actual state. */}
            <button type="button" className="toggle" data-on={settings.two_factor_enabled}
              onClick={() => {
                const next = { ...settings, two_factor_enabled: !settings.two_factor_enabled }
                setSettings(next)
                save(next)
              }}>
              <span />
            </button>
          </div>
          <div className="flex items-center justify-between rounded-xl bg-slate-50 p-4">
            <div>
              <div className="font-semibold text-slate-800">Session Timeout</div>
              <div className="text-sm text-slate-500">Auto logout after inactivity</div>
            </div>
            <select value={settings.session_timeout_minutes}
              onChange={(e) => {
                const next = { ...settings, session_timeout_minutes: e.target.value }
                setSettings(next)
                save(next)
              }}
              className="rounded-lg border border-slate-300 px-3 py-2 text-sm">
              <option value={15}>15 minutes</option>
              <option value={30}>30 minutes</option>
              <option value={60}>60 minutes</option>
            </select>
          </div>
        </div>
      </Card>
    </DashboardLayout>
  )
}
