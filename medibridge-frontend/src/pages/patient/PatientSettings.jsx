import { useEffect, useState } from 'react'
import { useDispatch } from 'react-redux'
import { UserCog, Lock } from 'lucide-react'
import DashboardLayout from '../../components/layout/DashboardLayout'
import Input, { Field } from '../../components/common/Input'
import Button from '../../components/common/Button'
import { patientNav } from './patientNav'
import { patientProfileService } from '../../services/profileService'
import { userUpdated } from '../../features/auth/authSlice'

const EMPTY = {
  full_name: '', email: '', phone: '', another_number: '',
  address: '', date_of_birth: '', gender: 'Male', blood_group: 'O+',
}

export default function PatientSettings() {
  const dispatch = useDispatch()
  const [form, setForm] = useState(EMPTY)
  const [pw, setPw] = useState({ current_password: '', new_password: '', confirm: '' })
  const [profileMsg, setProfileMsg] = useState(null)
  const [pwMsg, setPwMsg] = useState(null)
  const [saving, setSaving] = useState(false)

  // An account that signed up by phone has no email yet, so the field has to be
  // fillable exactly once. Everyone else keeps it read-only: it is a login
  // identifier, and changing one needs a verification step this screen has no
  // business doing.
  const [emailLocked, setEmailLocked] = useState(true)

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value })

  useEffect(() => {
    patientProfileService.get()
      .then((p) => {
        setEmailLocked(Boolean(p.email))
        // Google and phone accounts arrive with holes in them, and a null in a
        // controlled input makes React drop the field to uncontrolled.
        setForm({
          ...EMPTY, ...p,
          email: p.email || '',
          phone: p.phone || '',
          gender: p.gender || 'Male',
          blood_group: p.blood_group || 'O+',
        })
      })
      .catch(() => setProfileMsg({ error: true, text: 'Could not load your profile.' }))
  }, [])

  const saveProfile = async (e) => {
    e.preventDefault()
    setProfileMsg(null)
    setSaving(true)
    try {
      const updated = await patientProfileService.update({
        full_name: form.full_name,
        // Ignored by the server once set, so sending it unconditionally is safe.
        email: form.email || null,
        phone: form.phone,
        another_number: form.another_number || null,
        address: form.address || null,
        date_of_birth: form.date_of_birth,
        gender: form.gender,
        blood_group: form.blood_group,
      })
      setEmailLocked(Boolean(updated.email))
      setForm({ ...EMPTY, ...updated, email: updated.email || '' })

      // The session copy of these three is what the topbar, the greeting and
      // the profile-completion redirect read; without this they keep showing
      // whatever was true at login.
      //
      // Completeness keys off the email because the server's rule (Patient
      // .isProfileComplete) also wants date of birth, gender, blood group and
      // phone - and those four are @NotBlank/@NotNull on the update request,
      // so a 200 already proves them. Email is the one it accepts as absent,
      // which makes it the only field still in question here.
      dispatch(userUpdated({
        name: updated.full_name,
        email: updated.email || null,
        profile_complete: Boolean(updated.email),
      }))
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
    <div className={`mt-4 rounded-xl px-4 py-2.5 text-sm ${
      msg.error ? 'bg-danger-50 text-danger-600' : 'bg-success-50 text-success-700'}`}>{msg.text}</div>
  ) : null

  return (
    <DashboardLayout navItems={patientNav}>
      <span className="eyebrow">Account</span>
      <h1 className="mt-1 text-display-sm text-sand-900">Settings</h1>

      {/* Where LoginPage sends a fresh phone signup. Without this they arrive on
          a settings screen with no idea why, holding an account named
          "New Patient". */}
      {!emailLocked && (
        <div className="mt-5 rounded-xl border border-primary-100 bg-primary-50 px-4 py-3 text-sm text-primary-800">
          <span className="font-bold">Welcome to MediBridge.</span> Your mobile number
          is verified — add your name and email below so doctors know who they are
          treating and your records reach you.
        </div>
      )}

      <div className="surface mt-6 p-6">
        <h2 className="flex items-center gap-2 text-lg font-extrabold text-sand-900">
          <UserCog size={18} className="text-primary-600" /> Profile information
        </h2>
        <Banner msg={profileMsg} />
        <form onSubmit={saveProfile}>
          <div className="mt-5 grid gap-4 sm:grid-cols-2">
            <Field label="Full Name">
              <Input required value={form.full_name} onChange={set('full_name')} />
            </Field>
            <Field label="Email" hint={emailLocked ? undefined : 'Used for records and receipts.'}>
              {emailLocked
                ? <Input value={form.email} disabled className="bg-sand-50 text-sand-500" />
                : <Input required type="email" placeholder="you@example.com"
                    value={form.email} onChange={set('email')} />}
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
              <select className="w-full rounded-xl border border-sand-200 bg-sand-50/60 px-4 py-3 text-sm font-medium text-sand-900 outline-none transition-all hover:border-sand-300 hover:bg-white focus:border-primary-400 focus:bg-white focus:ring-4 focus:ring-primary-500/10"
                value={form.gender} onChange={set('gender')}>
                <option>Male</option><option>Female</option><option>Other</option>
              </select>
            </Field>
            <Field label="Blood Group">
              <select className="w-full rounded-xl border border-sand-200 bg-sand-50/60 px-4 py-3 text-sm font-medium text-sand-900 outline-none transition-all hover:border-sand-300 hover:bg-white focus:border-primary-400 focus:bg-white focus:ring-4 focus:ring-primary-500/10"
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
      </div>

      <div className="surface mt-6 p-6">
        <h2 className="flex items-center gap-2 text-lg font-extrabold text-sand-900">
          <Lock size={18} className="text-primary-600" /> Change password
        </h2>
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
      </div>
    </DashboardLayout>
  )
}
