import { useEffect, useState } from 'react'
import DashboardLayout from '../../components/layout/DashboardLayout'
import Card from '../../components/common/Card'
import Input, { Field } from '../../components/common/Input'
import Button from '../../components/common/Button'
import { patientNav } from './patientNav'
import { patientProfileService } from '../../services/profileService'

const EMPTY = {
  full_name: '', email: '', phone: '', another_number: '',
  address: '', date_of_birth: '', gender: 'Male', blood_group: 'O+',
}

export default function PatientSettings() {
  const [form, setForm] = useState(EMPTY)
  const [pw, setPw] = useState({ current_password: '', new_password: '', confirm: '' })
  const [profileMsg, setProfileMsg] = useState(null)
  const [pwMsg, setPwMsg] = useState(null)
  const [saving, setSaving] = useState(false)

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value })

  useEffect(() => {
    patientProfileService.get()
      .then((p) => setForm({ ...EMPTY, ...p }))
      .catch(() => setProfileMsg({ error: true, text: 'Could not load your profile.' }))
  }, [])

  const saveProfile = async (e) => {
    e.preventDefault()
    setProfileMsg(null)
    setSaving(true)
    try {
      const updated = await patientProfileService.update({
        full_name: form.full_name,
        phone: form.phone,
        another_number: form.another_number || null,
        address: form.address || null,
        date_of_birth: form.date_of_birth,
        gender: form.gender,
        blood_group: form.blood_group,
      })
      setForm({ ...EMPTY, ...updated })
      setProfileMsg({ text: 'Profile updated successfully.' })
    } catch (err) {
      setProfileMsg({ error: true, text: err?.response?.data?.message || 'Could not save changes.' })
    } finally {
      setSaving(false)
    }
  }

  const savePassword = async (e) => {
    e.preventDefault()
    setPwMsg(null)

    // Checked here as well as on the server so the user finds out before a round trip.
    if (pw.new_password !== pw.confirm) {
      setPwMsg({ error: true, text: 'New passwords do not match.' })
      return
    }

    try {
      const res = await patientProfileService.changePassword({
        current_password: pw.current_password,
        new_password: pw.new_password,
      })
      setPw({ current_password: '', new_password: '', confirm: '' })
      setPwMsg({ text: res.message || 'Password updated.' })
    } catch (err) {
      setPwMsg({ error: true, text: err?.response?.data?.message || 'Could not update password.' })
    }
  }

  const Banner = ({ msg }) => msg ? (
    <div className={`mt-4 rounded-lg px-4 py-2.5 text-sm ${
      msg.error ? 'bg-red-50 text-red-600' : 'bg-green-50 text-green-700'}`}>{msg.text}</div>
  ) : null

  return (
    <DashboardLayout navItems={patientNav}>
      <h1 className="text-3xl font-extrabold text-slate-900">Settings</h1>

      <Card className="mt-6">
        <h2 className="text-lg font-bold text-slate-900">Profile Information</h2>
        <Banner msg={profileMsg} />
        <form onSubmit={saveProfile}>
          <div className="mt-5 grid gap-4 sm:grid-cols-2">
            <Field label="Full Name">
              <Input required value={form.full_name} onChange={set('full_name')} />
            </Field>
            <Field label="Email">
              {/* Read-only: the email is the login identifier. */}
              <Input value={form.email} disabled className="bg-slate-50 text-slate-500" />
            </Field>
            <Field label="Phone Number">
              <Input required value={form.phone} onChange={set('phone')} />
            </Field>
            <Field label="Alternate Number">
              <Input value={form.another_number || ''} onChange={set('another_number')} placeholder="Optional" />
            </Field>
            <Field label="Date of Birth">
              <Input required type="date" value={form.date_of_birth || ''} onChange={set('date_of_birth')} />
            </Field>
            <Field label="Gender">
              <select className="w-full rounded-lg border border-slate-300 px-3.5 py-2.5 text-sm"
                value={form.gender} onChange={set('gender')}>
                <option>Male</option><option>Female</option><option>Other</option>
              </select>
            </Field>
            <Field label="Blood Group">
              <select className="w-full rounded-lg border border-slate-300 px-3.5 py-2.5 text-sm"
                value={form.blood_group} onChange={set('blood_group')}>
                {['O+','O-','A+','A-','B+','B-','AB+','AB-'].map((b) => <option key={b}>{b}</option>)}
              </select>
            </Field>
          </div>
          <div className="mt-4">
            <Field label="Address">
              <Input value={form.address || ''} onChange={set('address')} placeholder="Street, City, State" />
            </Field>
          </div>
          <Button type="submit" disabled={saving} variant={saving ? 'disabled' : 'primary'} className="mt-5">
            {saving ? 'Saving…' : 'Save Changes'}
          </Button>
        </form>
      </Card>

      <Card className="mt-6">
        <h2 className="text-lg font-bold text-slate-900">Change Password</h2>
        <Banner msg={pwMsg} />
        <form onSubmit={savePassword}>
          <div className="mt-5 max-w-md space-y-4">
            <Field label="Current Password">
              <Input required type="password" value={pw.current_password}
                onChange={(e) => setPw({ ...pw, current_password: e.target.value })} />
            </Field>
            <Field label="New Password">
              <Input required type="password" minLength={8} value={pw.new_password}
                onChange={(e) => setPw({ ...pw, new_password: e.target.value })} />
            </Field>
            <Field label="Confirm New Password">
              <Input required type="password" value={pw.confirm}
                onChange={(e) => setPw({ ...pw, confirm: e.target.value })} />
            </Field>
          </div>
          <Button type="submit" className="mt-5">Update Password</Button>
        </form>
      </Card>
    </DashboardLayout>
  )
}
