import { useEffect, useState } from 'react'
import DashboardLayout from '../../components/layout/DashboardLayout'
import Card from '../../components/common/Card'
import Input, { Field } from '../../components/common/Input'
import Button from '../../components/common/Button'
import { doctorNav } from './doctorNav'
import { doctorProfileService } from '../../services/profileService'
import { doctorService } from '../../services/doctorService'

const EMPTY = {
  full_name: '', email: '', phone: '', specialization: '', license_number: '',
  experience_years: '', consultation_fee: '', consultation_duration_min: 30, bio: '',
}

export default function DoctorSettings() {
  const [form, setForm] = useState(EMPTY)
  const [specializations, setSpecializations] = useState([])
  const [msg, setMsg] = useState(null)
  const [saving, setSaving] = useState(false)

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value })

  useEffect(() => {
    doctorProfileService.get()
      .then((d) => setForm({ ...EMPTY, ...d }))
      .catch(() => setMsg({ error: true, text: 'Could not load your profile.' }))

    doctorService.getSpecializations().then(setSpecializations).catch(() => setSpecializations([]))
  }, [])

  // Profile and consultation settings are one PUT - they live on the same row,
  // so splitting them into two calls would just risk a partial save.
  const save = async (e) => {
    e.preventDefault()
    setMsg(null)
    setSaving(true)
    try {
      const updated = await doctorProfileService.update({
        full_name: form.full_name,
        phone: form.phone,
        specialization: form.specialization,
        experience_years: Number(form.experience_years),
        consultation_fee: Number(form.consultation_fee),
        consultation_duration_min: Number(form.consultation_duration_min),
        bio: form.bio || null,
      })
      setForm({ ...EMPTY, ...updated })
      setMsg({ text: 'Profile updated successfully.' })
    } catch (err) {
      setMsg({ error: true, text: err?.response?.data?.message || 'Could not save changes.' })
    } finally {
      setSaving(false)
    }
  }

  return (
    <DashboardLayout badge="Doctor" navItems={doctorNav}>
      <h1 className="text-3xl font-extrabold text-slate-900">Profile Settings</h1>

      {msg && (
        <div className={`mt-5 rounded-lg px-4 py-2.5 text-sm ${
          msg.error ? 'bg-red-50 text-red-600' : 'bg-green-50 text-green-700'}`}>{msg.text}</div>
      )}

      <form onSubmit={save}>
        <Card className="mt-6">
          <h2 className="text-lg font-bold text-slate-900">Professional Information</h2>
          <div className="mt-5 grid gap-4 sm:grid-cols-2">
            <Field label="Full Name">
              <Input required value={form.full_name} onChange={set('full_name')} />
            </Field>
            <Field label="Specialization">
              <select value={form.specialization} onChange={set('specialization')}
                className="w-full rounded-lg border border-slate-300 px-3.5 py-2.5 text-sm">
                {specializations.map((s) => <option key={s}>{s}</option>)}
              </select>
            </Field>
            <Field label="License Number">
              {/* Not editable: changing a medical registration number after
                  verification would defeat the point of verifying it. */}
              <Input value={form.license_number} disabled className="bg-slate-50 text-slate-500" />
            </Field>
            <Field label="Experience (Years)">
              <Input required type="number" min="0" max="70"
                value={form.experience_years} onChange={set('experience_years')} />
            </Field>
            <Field label="Email">
              <Input value={form.email} disabled className="bg-slate-50 text-slate-500" />
            </Field>
            <Field label="Phone">
              <Input required value={form.phone} onChange={set('phone')} />
            </Field>
          </div>
          <div className="mt-4">
            <Field label="Bio">
              <textarea rows={3} value={form.bio || ''} onChange={set('bio')}
                className="w-full rounded-lg border border-slate-300 px-3.5 py-2.5 text-sm" />
            </Field>
          </div>
        </Card>

        <Card className="mt-6">
          <h2 className="text-lg font-bold text-slate-900">Consultation Settings</h2>
          <div className="mt-5 space-y-4">
            <Field label="Consultation Fee (per session)">
              <div className="relative">
                <span className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400">₹</span>
                <input required type="number" min="0" value={form.consultation_fee}
                  onChange={set('consultation_fee')}
                  className="w-full rounded-lg border border-slate-300 py-2.5 pl-7 pr-3 text-sm" />
              </div>
            </Field>
            <Field label="Average Consultation Duration">
              {/* Drives slot generation: changing this reshapes the bookable
                  slots produced from the weekly availability pattern. */}
              <select value={form.consultation_duration_min} onChange={set('consultation_duration_min')}
                className="w-full rounded-lg border border-slate-300 px-3.5 py-2.5 text-sm">
                <option value={15}>15 minutes</option>
                <option value={30}>30 minutes</option>
                <option value={45}>45 minutes</option>
                <option value={60}>60 minutes</option>
              </select>
            </Field>
          </div>
          <Button type="submit" disabled={saving} variant={saving ? 'disabled' : 'primary'} className="mt-5">
            {saving ? 'Saving…' : 'Save Changes'}
          </Button>
        </Card>
      </form>
    </DashboardLayout>
  )
}
